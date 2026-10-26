package com.yonlab.vision;
import android.graphics.*;import com.google.android.gms.tasks.*;import com.google.mlkit.vision.common.InputImage;import com.google.mlkit.vision.pose.*;import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;
/** ML Kit base and accurate are different shipped pose models; both single-image CPU. */
final class OnePoseEngine implements AutoCloseable {
 private static final Object GATE=new Object();private static Task<?> inflight;private final PoseDetector detector;private final AtomicBoolean closed=new AtomicBoolean();private int pending;
 OnePoseEngine(boolean accurate){detector=accurate?PoseDetection.getClient(new AccuratePoseDetectorOptions.Builder().setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE).setPreferredHardwareConfigs(AccuratePoseDetectorOptions.CPU).build()):PoseDetection.getClient(new PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE).setPreferredHardwareConfigs(PoseDetectorOptions.CPU).build());}
 List<PoseGeometry.Point> pose(Bitmap bitmap)throws Exception {final Bitmap snapshot;final Task<Pose> task;synchronized(GATE){if(closed.get()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("canceled");if(inflight!=null&&!inflight.isComplete())throw new IOException("이전 자세 분석을 마치는 중입니다");snapshot=bitmap.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)throw new IOException("image snapshot");pending++;try{task=detector.process(InputImage.fromBitmap(snapshot,0));inflight=task;}catch(RuntimeException|Error e){pending--;snapshot.recycle();throw e;}}
  task.addOnCompleteListener(Runnable::run,done->{snapshot.recycle();synchronized(GATE){pending--;if(inflight==done)inflight=null;if(closed.get()&&pending==0)detector.close();}});
  Pose result=Tasks.await(task,15,TimeUnit.SECONDS);List<PoseGeometry.Point> points=new ArrayList<>();for(PoseLandmark p:result.getAllPoseLandmarks()){PointF v=p.getPosition();points.add(new PoseGeometry.Point(p.getLandmarkType(),v.x/bitmap.getWidth(),v.y/bitmap.getHeight(),p.getInFrameLikelihood()));}return points;
 }
 public void close(){if(!closed.compareAndSet(false,true))return;synchronized(GATE){if(pending==0)detector.close();}}
}
