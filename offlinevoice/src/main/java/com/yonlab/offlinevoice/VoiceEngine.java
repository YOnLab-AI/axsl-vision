package com.yonlab.offlinevoice;
import android.content.Context;
import com.k2fsa.sherpa.onnx.*;
import org.json.JSONObject;
import org.vosk.*;
import java.io.*;

/** A worker owns the native recognizer through close; the UI never releases a running decoder. */
final class VoiceEngine implements AutoCloseable {
 private Model voskModel;private Recognizer vosk;
 private OfflineRecognizer moon;private OfflineStream stream;private ByteArrayOutputStream pending;
 private final PcmAudio.Cancel cancel;private String finalJson,detectedLanguage="";
 private VoiceEngine(PcmAudio.Cancel cancel){this.cancel=cancel;}
 static VoiceEngine open(Context context,String id,PcmAudio.Cancel cancel)throws Exception{
  return open(context,id,cancel,cancel);
 }
 static VoiceEngine open(Context context,String id,PcmAudio.Cancel loadingCancel,PcmAudio.Cancel runningCancel)throws Exception{
  VoiceEngine e=new VoiceEngine(runningCancel);
  return VoiceCleanup.guardedOpen(()->{
   File path=VoiceAssetStore.prepare(context,id,loadingCancel::canceled);PcmAudio.check(loadingCancel);
   if(VoiceModelPolicy.whisper(id)){
    e.moon=new OfflineRecognizer(null,WhisperConfig.create(path,executionThreads(context),VoiceModelPolicy.whisperSize(id)));e.stream=e.moon.createStream();e.pending=new ByteArrayOutputStream();
   }else if(VoiceModelPolicy.MOON.equals(id)){
    OfflineMoonshineModelConfig model=new OfflineMoonshineModelConfig();model.setEncoder(new File(path,"encoder_model.ort").getAbsolutePath());model.setMergedDecoder(new File(path,"decoder_model_merged.ort").getAbsolutePath());
    OfflineModelConfig models=new OfflineModelConfig();models.setMoonshine(model);models.setTokens(new File(path,"tokens.txt").getAbsolutePath());models.setNumThreads(2);models.setProvider("cpu");models.setDebug(false);
    FeatureConfig features=new FeatureConfig();features.setSampleRate(16000);
    OfflineRecognizerConfig config=new OfflineRecognizerConfig();config.setModelConfig(models);config.setFeatConfig(features);config.setDecodingMethod("greedy_search");
    e.moon=new OfflineRecognizer(null,config);e.stream=e.moon.createStream();e.pending=new ByteArrayOutputStream();
   }else{LibVosk.setLogLevel(LogLevel.WARNINGS);e.voskModel=new Model(path.getAbsolutePath());PcmAudio.check(loadingCancel);e.vosk=new Recognizer(e.voskModel,16000);}
   PcmAudio.check(loadingCancel);return e;
  },e::close);
 }
 private static int executionThreads(Context c){try{Object n=Class.forName("com.yonlab.vision.OneExecutionSettings").getMethod("threads",Context.class).invoke(null,c);if(n instanceof Integer&&((Integer)n==1||(Integer)n==2||(Integer)n==4))return (Integer)n;}catch(ReflectiveOperationException|LinkageError ignored){}return 2;}
 boolean acceptWaveForm(byte[] block,int count)throws Exception{PcmAudio.check(cancel);if(count<0||count>block.length||(count&1)!=0)throw new IOException("PCM alignment");if(vosk!=null)return vosk.acceptWaveForm(block,count);if(pending.size()+count>PcmAudio.MAX_SECONDS*32000)throw new AudioInputException("TOO_LONG");pending.write(block,0,count);return false;}
 String getPartialResult(){return vosk!=null?vosk.getPartialResult():"{\"partial\":\"\"}";}
 String getResult(){return vosk!=null?vosk.getResult():"{\"text\":\"\"}";}
 String getFinalResult()throws Exception{
  if(finalJson!=null)return finalJson;PcmAudio.check(cancel);
  if(vosk!=null)return finalJson=vosk.getFinalResult();
  byte[] bytes=pending.toByteArray();if(bytes.length==0)return finalJson="{\"text\":\"\"}";float[] samples=new float[bytes.length/2];for(int i=0;i<samples.length;i++){if((i&8191)==0)PcmAudio.check(cancel);samples[i]=(short)((bytes[i*2]&255)|(bytes[i*2+1]<<8))/32768f;}
  stream.acceptWaveform(samples,16000);PcmAudio.check(cancel);
  // sherpa v2.0 exposes synchronous decode only. Cancellation is checked again after native return;
  // resources remain worker-owned throughout this call, avoiding concurrent native deletion.
  moon.decode(stream);PcmAudio.check(cancel);OfflineRecognizerResult result=moon.getResult(stream);String nativeLanguage=result.getLang();detectedLanguage=nativeLanguage==null?"":nativeLanguage;JSONObject value=new JSONObject().put("text",result.getText());if(!detectedLanguage.isEmpty())value.put("lang",detectedLanguage);return finalJson=value.toString();
 }
 String getDetectedLanguage(){return detectedLanguage;}
 @Override public void close(){Throwable failure=null;try{if(vosk!=null)vosk.close();}catch(RuntimeException|LinkageError e){failure=e;}finally{vosk=null;}try{if(voskModel!=null)voskModel.close();}catch(RuntimeException|LinkageError e){if(failure==null)failure=e;else failure.addSuppressed(e);}finally{voskModel=null;}try{if(stream!=null)stream.release();}catch(RuntimeException|LinkageError e){if(failure==null)failure=e;else failure.addSuppressed(e);}finally{stream=null;}try{if(moon!=null)moon.release();}catch(RuntimeException|LinkageError e){if(failure==null)failure=e;else failure.addSuppressed(e);}finally{moon=null;pending=null;}if(failure instanceof LinkageError)throw (LinkageError)failure;if(failure!=null)throw (RuntimeException)failure;}
}
