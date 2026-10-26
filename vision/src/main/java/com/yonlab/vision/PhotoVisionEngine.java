package com.yonlab.vision;
import android.graphics.*;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.pose.*;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import java.io.IOException;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Published ML Kit APIs only. Each unresolved task retains its own snapshot until completion. */
final class PhotoVisionEngine implements AutoCloseable {
 static final class TextBox {final String text;final float left,top,right,bottom;TextBox(String t,Rect r,int w,int h){text=t;left=r.left/(float)w;top=r.top/(float)h;right=r.right/(float)w;bottom=r.bottom/(float)h;}}
 static final class Ocr {final String text;final List<TextBox> boxes;Ocr(String t,List<TextBox>b){text=t;boxes=Collections.unmodifiableList(b);}}
 private static final Object GATE=new Object();private static Task<?> inflight;private static final Executor DIRECT=Runnable::run;
 private final AtomicBoolean closed=new AtomicBoolean(),poisoned=new AtomicBoolean(),busy=new AtomicBoolean();private int pending;private TextRecognizer text;private PoseDetector pose;
 PhotoVisionEngine(String kind,boolean korean){if("ocr".equals(kind))text=TextRecognition.getClient(korean?new KoreanTextRecognizerOptions.Builder().build():TextRecognizerOptions.DEFAULT_OPTIONS);else if("pose".equals(kind))pose=PoseDetection.getClient(new PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE).setPreferredHardwareConfigs(PoseDetectorOptions.CPU).build());else throw new IllegalArgumentException("vision tool");}
 private void check(){if(closed.get()||poisoned.get()||Thread.currentThread().isInterrupted())throw new IllegalStateException("분석을 다시 시작하세요");}
 private interface Start<T>{Task<T> process(InputImage image);}
 private <T>T await(Bitmap bitmap,Start<T> start)throws Exception{final Bitmap snapshot;final Task<T> task;synchronized(GATE){check();if(inflight!=null&&!inflight.isComplete())throw new IllegalStateException("이전 사진 분석을 마치는 중입니다");snapshot=bitmap.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)throw new IOException("사진 준비 실패");pending++;try{task=start.process(InputImage.fromBitmap(snapshot,0));inflight=task;}catch(Exception|LinkageError|OutOfMemoryError e){pending--;snapshot.recycle();throw e;}}
  task.addOnCompleteListener(DIRECT,done->{snapshot.recycle();synchronized(GATE){pending--;if(inflight==done)inflight=null;if((closed.get()||poisoned.get())&&pending==0)closeOwned();}});
  try{return Tasks.await(task,15,TimeUnit.SECONDS);}catch(TimeoutException e){poisoned.set(true);throw new IOException("분석 시간이 초과됐습니다",e);}catch(InterruptedException e){poisoned.set(true);Thread.currentThread().interrupt();throw e;}
 }
 Ocr ocr(Bitmap bitmap)throws Exception{if(text==null)throw new IllegalStateException("text tool");if(!busy.compareAndSet(false,true))throw new IllegalStateException("busy");try{check();Text found=await(bitmap,text::process);List<TextBox> boxes=new ArrayList<>();for(Text.TextBlock block:found.getTextBlocks()){Rect r=block.getBoundingBox();if(r!=null&&!r.isEmpty())boxes.add(new TextBox(block.getText(),r,bitmap.getWidth(),bitmap.getHeight()));}return new Ocr(found.getText(),boxes);}finally{busy.set(false);}}
 List<PoseGeometry.Point> pose(Bitmap bitmap)throws Exception{if(pose==null)throw new IllegalStateException("pose tool");if(!busy.compareAndSet(false,true))throw new IllegalStateException("busy");try{check();Pose found=await(bitmap,pose::process);List<PoseGeometry.Point> result=new ArrayList<>();for(PoseLandmark point:found.getAllPoseLandmarks()){PointF p=point.getPosition();result.add(new PoseGeometry.Point(point.getLandmarkType(),p.x/bitmap.getWidth(),p.y/bitmap.getHeight(),point.getInFrameLikelihood()));}return result;}finally{busy.set(false);}}
 public void close(){if(!closed.compareAndSet(false,true))return;synchronized(GATE){if(pending==0)closeOwned();}}
 private void closeOwned(){TextRecognizer t=text;text=null;PoseDetector p=pose;pose=null;try{if(t!=null)t.close();}finally{if(p!=null)p.close();}}
}
