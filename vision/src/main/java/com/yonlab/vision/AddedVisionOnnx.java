package com.yonlab.vision;

import android.content.Context;
import android.graphics.*;
import ai.onnxruntime.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.*;

/** One CPU session per owner; exact preprocessing is chosen by the actual network. */
final class AddedVisionOnnx implements AutoCloseable {
 interface Decoder<T>{T read(OrtSession.Result result)throws Exception;}
 final String id,sha256;private final OrtEnvironment env;private final OrtSession session;private final String input;private final Set<String> selected;
 private boolean closed;
 AddedVisionOnnx(Context context,String id)throws Exception{
  this.id=id;sha256=AddedVisionModels.sha(id);File file=ModelPlusAssets.file(context,AddedVisionModels.asset(id),sha256,64L*1024*1024);
  env=OrtEnvironment.getEnvironment();OrtSession initialized=null;
  try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
   options.setIntraOpNumThreads(OneExecutionSettings.threads(context));options.setInterOpNumThreads(1);
   initialized=env.createSession(file.getAbsolutePath(),options);if(initialized.getNumInputs()!=1)throw new IOException("모델 입력을 확인하세요");
   input=initialized.getInputNames().iterator().next();Set<String> requested=new LinkedHashSet<>();if(AddedVisionModels.retrieval(id))requested.add("yon_embedding");else if("resnet18-v1".equals(id))requested.add("resnetv15_dense0_fwd");else if("squeezenet1-1".equals(id))requested.add("squeezenet0_flatten0_reshape0");else if(id.endsWith("modnet"))requested.add("output");else if(id.endsWith("selfie-landscape"))requested.add("alphas");else requested.addAll(Arrays.asList("792","795","814","817","836","839"));if(!initialized.getOutputNames().containsAll(requested))throw new IOException("모델 출력을 확인하세요");selected=Collections.unmodifiableSet(requested);session=initialized;
  }catch(Exception|LinkageError|OutOfMemoryError error){if(initialized!=null)try{initialized.close();}catch(Exception cleanup){error.addSuppressed(cleanup);}throw error;}
 }
 synchronized <T>T run(Bitmap image,Decoder<T> decoder)throws Exception{
  if(closed||Thread.currentThread().isInterrupted())throw new InterruptedIOException("분석 취소");
  if(image==null||image.isRecycled()||(long)image.getWidth()*image.getHeight()>16000000)throw new IOException("사진을 확인하세요");
  int width=224,height=224;boolean nano="nanodet-plus".equals(id),matte=id.endsWith("modnet"),landscape=id.endsWith("selfie-landscape");
  if(nano){width=height=416;}else if(landscape){width=256;height=144;}else if(matte){float scale=Math.min(512f/Math.min(image.getWidth(),image.getHeight()),768f/Math.max(image.getWidth(),image.getHeight()));width=Math.max(32,Math.min(768,Math.round(image.getWidth()*scale/32)*32));height=Math.max(32,Math.min(768,Math.round(image.getHeight()*scale/32)*32));}
  Bitmap prepared=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);try{
   Canvas canvas=new Canvas(prepared);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
   if(nano){canvas.drawColor(Color.BLACK);float scale=Math.min(width/(float)image.getWidth(),height/(float)image.getHeight());int rw=Math.max(1,(int)(image.getWidth()*scale)),rh=Math.max(1,(int)(image.getHeight()*scale)),left=(width-rw)/2,top=(height-rh)/2;canvas.drawBitmap(image,null,new RectF(left,top,left+rw,top+rh),paint);}
   else if(matte||landscape)canvas.drawBitmap(image,null,new RectF(0,0,width,height),paint);
   else{float side=Math.min(image.getWidth(),image.getHeight())*224f/256f,x=(image.getWidth()-side)/2,y=(image.getHeight()-side)/2;canvas.drawBitmap(image,new Rect(Math.round(x),Math.round(y),Math.round(x+side),Math.round(y+side)),new RectF(0,0,width,height),paint);}
   int area=width*height;int[] pixels=new int[area];prepared.getPixels(pixels,0,width,0,0,width,height);float[] tensor=new float[area*3];
   float[] mean=nano?new float[]{103.53f,116.28f,123.675f}:new float[]{.485f,.456f,.406f},std=nano?new float[]{57.375f,57.12f,58.395f}:new float[]{.229f,.224f,.225f};
   for(int i=0;i<area;i++){if((i&16383)==0&&Thread.currentThread().isInterrupted())throw new InterruptedIOException("분석 취소");for(int c=0;c<3;c++){float value=(pixels[i]>>>(16-8*c))&255;if(nano)value=(value-mean[c])/std[c];else{value/=255f;if(matte)value=(value-.5f)/.5f;else if(!landscape)value=(value-mean[c])/std[c];}tensor[c*area+i]=value;}}
   try(OnnxTensor inputTensor=OnnxTensor.createTensor(env,FloatBuffer.wrap(tensor),new long[]{1,3,height,width});OrtSession.Result output=session.run(Collections.singletonMap(input,inputTensor),selected)){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("분석 취소");return decoder.read(output);}
  }finally{prepared.recycle();}
 }
 static Object output(OrtSession.Result result,String name)throws Exception{return result.get(name).orElseThrow(()->new IOException("모델 출력 정보를 확인하세요")).getValue();}
 public synchronized void close(){if(closed)return;closed=true;try{session.close();}catch(OrtException e){throw new IllegalStateException("모델 종료",e);}}
}
