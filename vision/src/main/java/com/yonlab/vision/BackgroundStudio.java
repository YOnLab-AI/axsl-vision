package com.yonlab.vision;
import android.content.Context;import android.graphics.*;import android.os.SystemClock;
import com.google.android.gms.tasks.*;import com.google.mlkit.vision.common.InputImage;import com.google.mlkit.vision.segmentation.*;import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;
import java.io.*;import java.nio.*;import java.util.*;import java.util.concurrent.*;
import org.opencv.android.*;import org.opencv.core.*;import org.opencv.imgproc.Imgproc;

/** Actual person masks drive a new photo, not a recolored detection overlay. */
final class BackgroundStudio {
 private static Mask inferMask(Context c,String id,Bitmap input,OneModelRunner.Cancel cancel,OneModelRunner.Result r)throws Exception{
  if(AddedVisionModels.portrait(id))return AddedPortraitEngine.infer(c,id,input,cancel,r);
  if(!Arrays.asList("studio-pphuman","studio-selfie").contains(id))throw new IllegalArgumentException("studio model");
  long started=SystemClock.elapsedRealtime();Mask mask;
  if("studio-pphuman".equals(id)){
   OneOnnx engine=new OneOnnx(c,"pphumanseg-2023");r.engineCreated=true;r.modelSha256=engine.sha256;r.loadMs=SystemClock.elapsedRealtime()-started;
   try{long infer=SystemClock.elapsedRealtime();float[][][] values=engine.run(input,output->{Object raw=OneOnnx.value(output,"save_infer_model/scale_0.tmp_1");if(!(raw instanceof float[][][][]))throw new IOException("person mask rank");return ((float[][][][])raw)[0];});if(values.length!=2||values[0].length!=192||values[1].length!=192||values[0][0].length!=192||values[1][0].length!=192)throw new IOException("person mask shape");float[] alpha=new float[192*192];for(int y=0;y<192;y++)for(int x=0;x<192;x++){float bg=values[0][y][x],fg=values[1][y][x];if(!finite(bg)||!finite(fg))throw new IOException("person mask numeric");float sum=bg+fg;alpha[y*192+x]=bg>=0&&fg>=0&&Math.abs(sum-1)<.02?clamp(fg/sum):(float)(1/(1+Math.exp(Math.max(-80,Math.min(80,bg-fg)))));}mask=new Mask(alpha,192,192);r.inferenceMs=SystemClock.elapsedRealtime()-infer;}finally{engine.close();r.engineCloseReturned=true;}
  }else{
   Segmenter engine=Segmentation.getClient(new SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).enableRawSizeMask().build());r.engineCreated=true;r.loadMs=SystemClock.elapsedRealtime()-started;Bitmap snapshot=input.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null){engine.close();r.engineCloseReturned=true;throw new IOException("studio image snapshot");}Task<SegmentationMask> task=null;boolean finished=false;
   try{long infer=SystemClock.elapsedRealtime();task=engine.process(InputImage.fromBitmap(snapshot,0));SegmentationMask result=Tasks.await(task,20,TimeUnit.SECONDS);ByteBuffer buffer=result.getBuffer().order(ByteOrder.nativeOrder());buffer.rewind();float[] values=new float[result.getWidth()*result.getHeight()];for(int i=0;i<values.length;i++){float v=buffer.getFloat();if(!finite(v))throw new IOException("selfie mask numeric");values[i]=clamp(v);}mask=new Mask(values,result.getWidth(),result.getHeight());r.inferenceMs=SystemClock.elapsedRealtime()-infer;finished=true;
   }finally{if(finished||task==null||task.isComplete()){snapshot.recycle();engine.close();r.engineCloseReturned=true;}else{Task<SegmentationMask> pending=task;pending.addOnCompleteListener(Runnable::run,done->{snapshot.recycle();engine.close();});r.engineCloseReturned=false;}}
  }
  ExpansionImageRunner.check(cancel);return mask;
 }
 static void run(Context c,String id,Bitmap input,OneModelRunner.Cancel cancel,OneModelRunner.Result r)throws Exception{
  String effect=c.getSharedPreferences("YOnStudio",0).getString("effect","replace");if(!Arrays.asList("replace","blur","cutout").contains(effect))effect="replace";Mask mask=inferMask(c,id,input,cancel,r);
  ExpansionImageRunner.check(cancel);long compose=SystemClock.elapsedRealtime();int w=input.getWidth(),h=input.getHeight();Bitmap background=null,output=null;try{
   if("blur".equals(effect))background=blur(input);else if("replace".equals(effect)){background=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(background);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setShader(new LinearGradient(0,0,w,h,new int[]{0xffe1efff,0xffabc9ef,0xffd9e5f7},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,w,h,p);}
   int[] src=new int[w*h],dst=new int[w*h];input.getPixels(src,0,w,0,0,w,h);if(background!=null)background.getPixels(dst,0,w,0,0,w,h);long foreground=0;double alphaTotal=0;
   for(int y=0;y<h;y++){if((y&31)==0)ExpansionImageRunner.check(cancel);for(int x=0;x<w;x++){int i=y*w+x;float a=mask.at((x+.5f)*mask.w/w-.5f,(y+.5f)*mask.h/h-.5f);if(a>=.5f)foreground++;alphaTotal+=a;int color=src[i];if("cutout".equals(effect))dst[i]=(Math.round(a*((color>>>24)&255))<<24)|(color&0xffffff);else{int back=dst[i];int red=Math.round(((color>>>16)&255)*a+((back>>>16)&255)*(1-a)),green=Math.round(((color>>>8)&255)*a+((back>>>8)&255)*(1-a)),blue=Math.round((color&255)*a+(back&255)*(1-a));dst[i]=Color.rgb(red,green,blue);}}}
   output=Bitmap.createBitmap(dst,w,h,Bitmap.Config.ARGB_8888);r.image=output;output=null;r.inferenceMs+=SystemClock.elapsedRealtime()-compose;String title="blur".equals(effect)?"배경 흐림":"cutout".equals(effect)?"투명 배경":"배경 교체";r.summary=title+" · 인물 "+String.format(Locale.ROOT,"%.1f%%",100d*foreground/(w*h));r.detail=title+" 사진\n전경 마스크 "+mask.w+" × "+mask.h+"\nPNG로 저장하면 투명 배경을 유지합니다.";
   r.metrics.put("effect",effect).put("foregroundPixels",foreground).put("totalPixels",(long)w*h).put("foregroundFraction",foreground/(double)(w*h)).put("meanAlpha",alphaTotal/(w*h)).put("maskWidth",mask.w).put("maskHeight",mask.h).put("outputWidth",w).put("outputHeight",h).put("transparentOutput","cutout".equals(effect)).put("pipeline","person segmentation → bilinear soft alpha → selected photo composition");
  }finally{if(background!=null)background.recycle();if(output!=null)output.recycle();}
 }
 static void segment(Context c,Bitmap input,OneModelRunner.Cancel cancel,OneModelRunner.Result r)throws Exception{
  segment(c,"studio-selfie",input,cancel,r);
 }
 static void segment(Context c,String id,Bitmap input,OneModelRunner.Cancel cancel,OneModelRunner.Result r)throws Exception{
  Mask mask=inferMask(c,id,input,cancel,r);ExpansionImageRunner.check(cancel);long started=SystemClock.elapsedRealtime();int w=input.getWidth(),h=input.getHeight();int[] overlay=new int[w*h];long foreground=0;
  for(int y=0;y<h;y++){if((y&31)==0)ExpansionImageRunner.check(cancel);for(int x=0;x<w;x++){float alpha=mask.at((x+.5f)*mask.w/w-.5f,(y+.5f)*mask.h/h-.5f);if(alpha>=.5f){foreground++;overlay[y*w+x]=0x780068f8;}}}
  r.image=input.copy(Bitmap.Config.ARGB_8888,true);Bitmap colored=Bitmap.createBitmap(overlay,w,h,Bitmap.Config.ARGB_8888);try{new Canvas(r.image).drawBitmap(colored,0,0,null);}finally{colored.recycle();}r.inferenceMs+=SystemClock.elapsedRealtime()-started;r.summary="인물 영역 "+String.format(Locale.ROOT,"%.1f%%",100d*foreground/(w*h));r.detail="인물과 배경을 분리한 영역입니다.";r.metrics.put("foregroundPixels",foreground).put("totalPixels",(long)w*h).put("maskWidth",mask.w).put("maskHeight",mask.h).put("modelProvider",r.metrics.optString("maskProvider","Google ML Kit Selfie Segmenter 16.0.0-beta6")).put("maskThreshold",.5).put("pipeline","actual person soft mask → bilinear original-size mask → colored overlay");
 }
 private static boolean finite(float v){return !Float.isNaN(v)&&!Float.isInfinite(v);}
 private static float clamp(float v){return Math.max(0,Math.min(1,v));}
 static final class Mask {final float[] a;final int w,h;Mask(float[] v,int w,int h)throws IOException{if(w<1||h<1||v.length!=w*h)throw new IOException("mask size");a=v;this.w=w;this.h=h;}float at(float x,float y){x=Math.max(0,Math.min(w-1,x));y=Math.max(0,Math.min(h-1,y));int x0=(int)x,y0=(int)y,x1=Math.min(w-1,x0+1),y1=Math.min(h-1,y0+1);float dx=x-x0,dy=y-y0;return clamp((a[y0*w+x0]*(1-dx)+a[y0*w+x1]*dx)*(1-dy)+(a[y1*w+x0]*(1-dx)+a[y1*w+x1]*dx)*dy);}}
 private static Bitmap blur(Bitmap image)throws IOException{if(!OpenCVLoader.initLocal())throw new IOException("blur engine");Mat a=new Mat(),b=new Mat();Bitmap result=null;try{Utils.bitmapToMat(image,a);Imgproc.GaussianBlur(a,b,new Size(0,0),Math.max(7,Math.min(image.getWidth(),image.getHeight())/45d));result=Bitmap.createBitmap(image.getWidth(),image.getHeight(),Bitmap.Config.ARGB_8888);Utils.matToBitmap(b,result);Bitmap returned=result;result=null;return returned;}finally{if(result!=null)result.recycle();a.release();b.release();}}
}
