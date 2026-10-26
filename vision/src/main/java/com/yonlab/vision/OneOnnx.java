package com.yonlab.vision;
import android.content.Context;
import android.graphics.*;
import ai.onnxruntime.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.security.MessageDigest;
import java.util.*;
import org.opencv.android.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
/** A session owns its native runtime; all inputs/outputs close in the worker. */
final class OneOnnx implements AutoCloseable {
 private final OrtEnvironment env;private final OrtSession session;final String id,sha256;private boolean closed;
 interface Decoder<T>{T read(OrtSession.Result output)throws Exception;}
 static String expected(String id){switch(id){case "mobilenet-v2-2022":return "c0c3f76d93fa3fd6580652a45618618a220fced18babf65774ed169de0432ad5";case "yolo26n":return "2e947b787d9e787b93a16772a5f55b1d4d8c4d86f53146149c5d6a642442d6f7";case "yolo26n-seg":return "711e0ef837c677ae4006f65fe503a62a6b0529979f4828ed50b674a7482b7d2e";case "mobilenet-v4-fp32":return "7a003770e4f863bd8e19e20a8389c5212552e256e0dacd833afaf8d0b26cdc02";case "pphumanseg-2023":return "552d8a984054e59b5d773d24b9b12022b22046ceb2bbc4c9aaeaceb36a9ddf24";case "yunet-2026-sface":return "ebafce4e3c118d6554634be5c27ab333b4c047a9a8c3faf1d7cf93101c22f0f0";default:throw new IllegalArgumentException("ONNX model");}}
 OneOnnx(Context c,String model)throws Exception {if(android.os.Build.VERSION.SDK_INT<24)throw new IOException("Android 7 이상에서 실행하세요");id=model;sha256=expected(model);File file=materialize(c,model,sha256);env=OrtEnvironment.getEnvironment();int threads=OneExecutionSettings.threads(c);try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){options.setIntraOpNumThreads(threads);options.setInterOpNumThreads(1);session=env.createSession(file.getAbsolutePath(),options);}if(session.getNumInputs()!=1){session.close();throw new IOException("모델 입력 계약이 다릅니다");}}
 synchronized <T>T run(Bitmap image,Decoder<T> decode)throws Exception {if(closed)throw new IOException("모델이 종료되었습니다");if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("canceled");int side=id.startsWith("mobilenet")?224:"pphumanseg-2023".equals(id)?192:640;float[] pixels=tensor(image,side,id);try(OnnxTensor input=OnnxTensor.createTensor(env,FloatBuffer.wrap(pixels),new long[]{1,3,side,side});OrtSession.Result result=session.run(Collections.singletonMap(session.getInputNames().iterator().next(),input))){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("canceled");return decode.read(result);}}
 static float[] tensor(Bitmap image,int side,String id)throws Exception {
  if(image==null||image.isRecycled()||image.getWidth()<1||image.getHeight()<1||(long)image.getWidth()*image.getHeight()>16000000)throw new IOException("사진을 확인하세요");
  Bitmap scaled;
  if(id.startsWith("mobilenet")){
   if(!OpenCVLoader.initLocal())throw new IOException("이미지 엔진을 준비할 수 없습니다");Mat rgba=new Mat(),rgb=new Mat(),resized=new Mat(),crop=null;try{Utils.bitmapToMat(image,rgba);Imgproc.cvtColor(rgba,rgb,Imgproc.COLOR_RGBA2RGB);boolean v4="mobilenet-v4-fp32".equals(id);double factor=256d/Math.min(rgb.cols(),rgb.rows());int w=v4?(int)Math.round(rgb.cols()*factor):256,h=v4?(int)Math.round(rgb.rows()*factor):256;Imgproc.resize(rgb,resized,new Size(w,h),0,0,v4?Imgproc.INTER_CUBIC:Imgproc.INTER_LINEAR);crop=new Mat(resized,new org.opencv.core.Rect((w-224)/2,(h-224)/2,224,224));scaled=Bitmap.createBitmap(224,224,Bitmap.Config.ARGB_8888);Utils.matToBitmap(crop,scaled);}finally{if(crop!=null)crop.release();resized.release();rgb.release();rgba.release();}
  }else if(id.startsWith("yolo")||id.startsWith("yunet")){scaled=Bitmap.createBitmap(side,side,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(scaled);canvas.drawColor(id.startsWith("yolo")?Color.rgb(114,114,114):Color.BLACK);double f=Math.min(side/(double)image.getWidth(),side/(double)image.getHeight());int w=Math.max(1,(int)Math.round(image.getWidth()*f)),h=Math.max(1,(int)Math.round(image.getHeight()*f)),l=(side-w)/2,t=(side-h)/2;canvas.drawBitmap(image,null,new RectF(l,t,l+w,t+h),new Paint(Paint.FILTER_BITMAP_FLAG));}
  else scaled=Bitmap.createScaledBitmap(image,side,side,true);
  try{int area=side*side;int[] argb=new int[area];scaled.getPixels(argb,0,side,0,0,side,side);float[] out=new float[area*3];float[] mean={.485f,.456f,.406f},std={.229f,.224f,.225f};for(int i=0;i<area;i++){int rgb=argb[i];float rr=(rgb>>>16)&255,gg=(rgb>>>8)&255,bb=rgb&255;if(id.startsWith("yunet")){out[i]=bb;out[area+i]=gg;out[area*2+i]=rr;}else for(int ch=0;ch<3;ch++){float value=(ch==0?rr:ch==1?gg:bb)/255f;if(id.startsWith("mobilenet"))value=(value-mean[ch])/std[ch];else if(id.startsWith("pphuman"))value=(value-.5f)/.5f;out[ch*area+i]=value;}}return out;}finally{if(scaled!=image)scaled.recycle();}
 }
 private static synchronized File materialize(Context c,String id,String expected)throws Exception {File folder=new File(c.getNoBackupFilesDir(),"one-models");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("모델 저장 공간을 확인하세요");File target=new File(folder,id+".onnx");if(target.isFile()&&hash(target).equals(expected))return target;File pending=new File(folder,id+"."+UUID.randomUUID()+".pending");try{try(InputStream in=c.getAssets().open("one/models/"+id+".onnx");FileOutputStream out=new FileOutputStream(pending)){byte[] block=new byte[65536];long total=0;int n;while((n=in.read(block))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("canceled");total+=n;if(total>16*1024*1024)throw new IOException("model size");out.write(block,0,n);}out.getFD().sync();}if(!hash(pending).equals(expected))throw new IOException("모델 해시가 다릅니다");if(!pending.renameTo(target))throw new IOException("모델 저장 실패");return target;}finally{if(pending.exists())pending.delete();}}
 static String hash(File file)throws Exception {try(InputStream in=new FileInputStream(file)){return hash(in);}}
 static String hash(InputStream in)throws Exception {MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);StringBuilder s=new StringBuilder();for(byte v:d.digest())s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
 public synchronized void close(){if(closed)return;closed=true;try{session.close();}catch(OrtException e){throw new IllegalStateException("ONNX cleanup",e);}}
 static Object value(OrtSession.Result result,String name)throws Exception {if(!result.get(name).isPresent())throw new IOException("모델 출력 계약: "+name);return result.get(name).get().getValue();}
}
