package com.yonlab.vision;

import android.content.Context;
import android.os.SystemClock;
import ai.onnxruntime.*;
import org.json.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Separate CNN6 / CNN10 AudioSet networks, with their exact checkpoint frontend. */
final class PannSoundEngine implements AutoCloseable {
 final String id,sha256;final String[] labels;final long loadMs;
 private final OrtEnvironment environment;private OrtSession session;private PannFrontend frontend;private boolean closed;
 PannSoundEngine(Context c,String model,SoundMath.Cancel cancel)throws Exception{
  long start=SystemClock.elapsedRealtime();id=model;sha256=PannModels.sha(model);environment=OrtEnvironment.getEnvironment();
  try{
   SoundMath.check(cancel);File network=ModelPlusAssets.file(c,"one/modelplus/audio/"+id+".onnx",sha256,24L*1024*1024);
   SoundMath.check(cancel);File weights=ModelPlusAssets.file(c,"one/modelplus/audio/"+id+"-frontend.bin",PannModels.frontendSha(id),PannFrontend.WEIGHT_BYTES);
   frontend=new PannFrontend(weights);labels=readLabels(c);SoundMath.check(cancel);
   try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
    options.setIntraOpNumThreads(OneExecutionSettings.threads(c));options.setInterOpNumThreads(1);session=environment.createSession(network.getAbsolutePath(),options);
   }
   if(session.getNumInputs()!=1||session.getNumOutputs()!=1||!session.getInputNames().contains("logmel")||!session.getOutputNames().contains("probabilities"))throw new IOException("소리 모델 입출력 이름");
   validate(session.getInputInfo().get("logmel"),new long[]{1,1,PannFrontend.FRAMES,PannFrontend.BINS});validate(session.getOutputInfo().get("probabilities"),new long[]{1,527});
   SoundMath.check(cancel);loadMs=SystemClock.elapsedRealtime()-start;
  }catch(Exception|LinkageError|OutOfMemoryError error){try{close();}catch(Exception|LinkageError cleanup){error.addSuppressed(cleanup);}throw error;}
 }
 private static void validate(NodeInfo node,long[] shape)throws IOException{
  if(node==null||!(node.getInfo() instanceof TensorInfo))throw new IOException("소리 모델 텐서 형식");TensorInfo info=(TensorInfo)node.getInfo();if(info.type!=OnnxJavaType.FLOAT||!Arrays.equals(info.getShape(),shape))throw new IOException("소리 모델 텐서 크기");
 }
 private static String[] readLabels(Context c)throws Exception{
  File file=ModelPlusAssets.file(c,"one/modelplus/audio/panns-labels.json",PannModels.LABELS_SHA,64*1024);byte[] raw=new byte[(int)file.length()];try(DataInputStream in=new DataInputStream(new FileInputStream(file))){in.readFully(raw);}
  JSONArray values=new JSONArray(new String(raw,StandardCharsets.UTF_8));if(values.length()!=527)throw new IOException("소리 분류 목록");String[] labels=new String[527];for(int i=0;i<labels.length;i++){labels[i]=values.getString(i);if(labels[i].isEmpty())throw new IOException("소리 분류 항목");}return labels;
 }
 JSONObject analyze(float[] commonPcm,float[] sourcePcm,int sourceRate,SoundMath.Cancel cancel)throws Exception{
  if(closed)throw new IOException("소리 엔진이 종료되었습니다");long start=SystemClock.elapsedRealtime();SoundMath.check(cancel);
  float[] nativePcm=SoundMath.resample(sourcePcm==null?commonPcm:sourcePcm,sourcePcm==null?SoundMath.RATE:sourceRate,PannFrontend.RATE,cancel);
  float[] sum=new float[527],peak=new float[527];int[] starts=SoundMath.starts(commonPcm.length);JSONArray windows=new JSONArray();
  for(int offset:starts){
   SoundMath.check(cancel);float[] features=frontend.transform(nativePcm,offset*2,cancel);float[] scores;
   try(OnnxTensor input=OnnxTensor.createTensor(environment,FloatBuffer.wrap(features),new long[]{1,1,PannFrontend.FRAMES,PannFrontend.BINS});OrtSession.Result result=session.run(Collections.singletonMap("logmel",input))){
    SoundMath.check(cancel);Object value=result.get("probabilities").orElseThrow(()->new IOException("소리 모델 출력")).getValue();if(!(value instanceof float[][])||((float[][])value).length!=1||((float[][])value)[0].length!=527)throw new IOException("소리 모델 출력 크기");scores=((float[][])value)[0].clone();
   }
   for(int i=0;i<sum.length;i++){float score=scores[i];if(!Float.isFinite(score)||score<-.0001f||score>1.0001f)throw new IOException("소리 모델 출력 값");sum[i]+=score;peak[i]=Math.max(peak[i],score);}
   windows.put(new JSONObject().put("startSec",offset/(double)SoundMath.RATE).put("endSec",Math.min(commonPcm.length,offset+SoundMath.WINDOW)/(double)SoundMath.RATE).put("top",scores(scores,peak,3)));
  }
  for(int i=0;i<sum.length;i++)sum[i]/=starts.length;
  return new JSONObject().put("modelId",id).put("modelSha256",sha256).put("frontendSha256",PannModels.frontendSha(id)).put("state","COMPLETED").put("loadMs",loadMs).put("inferenceMs",SystemClock.elapsedRealtime()-start).put("timingScope","NATIVE_RESAMPLE_AND_ALL_WINDOWS_FRONTEND_INFER_AND_RANK_EXCLUDING_AUDIO_DECODE_AND_MODEL_LOAD").put("sampleRate",PannFrontend.RATE).put("windowSamples",PannFrontend.WINDOW).put("hopSamples",PannFrontend.HOP).put("windowCount",starts.length).put("zeroPadFinalWindow",true).put("classCount",labels.length).put("top5",scores(sum,peak,5)).put("windows",windows).put("allClassMeans",new JSONArray(sum)).put("inputPcmSha256",SoundMath.pcmSha(commonPcm)).put("nativePcmSha256",SoundMath.pcmSha(nativePcm)).put("sourceSampleRate",sourcePcm==null?SoundMath.RATE:sourceRate).put("nativeSampleCount",nativePcm.length);
 }
 private JSONArray scores(float[] mean,float[] peak,int count)throws Exception{JSONArray out=new JSONArray();for(int index:SoundMath.top(mean,count))out.put(new JSONObject().put("classId",index).put("label",labels[index]).put("score",mean[index]).put("peak",peak[index]));return out;}
 @Override public synchronized void close(){if(closed)return;OrtSession prior=session;session=null;frontend=null;if(prior!=null)try{prior.close();}catch(OrtException error){throw new IllegalStateException("소리 모델 종료",error);}closed=true;}
 boolean closed(){return closed;}
}
