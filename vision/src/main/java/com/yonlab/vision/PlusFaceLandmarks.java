package com.yonlab.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import java.io.*;
import java.util.*;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;

/** Two distinct published face landmark weights; EAR is a ratio, never a learned eye probability. */
final class PlusFaceLandmarks implements AutoCloseable {
    static final String LEGACY_SHA="1055cb9d4a9ca8b8c688902a3a5194311138ba256bcc94e336d8373a5f30c814",V2_SHA="c7d54204ce0448474c7f3fa9af494787c0965cbdd6f20fc72867e43046bd43d5";
    final String fingerprint;final int count;private final PlusTensorModel landmarks;private final PlusBlazeDetector detector;private PlusBlazeDetector.Box prior;private long lastAt;private int tracking;
    static boolean supports(String id){return "face-mesh-468".equals(id)||"face-landmarker-478".equals(id);}
    PlusFaceLandmarks(Context context,String id)throws Exception{
        boolean v2="face-landmarker-478".equals(id);if(!supports(id))throw new IllegalArgumentException("얼굴 랜드마크 모델");count=v2?478:468;String sha=v2?V2_SHA:LEGACY_SHA;PlusBlazeDetector first=new PlusBlazeDetector(context,"short");PlusTensorModel next=null;try{next=new PlusTensorModel(context,v2?"face-landmarker-478.tflite":"face-mesh-468.tflite",sha);if(next.shapes.length<2||next.shapes[0].length<1||next.shapes[0][next.shapes[0].length-1]!=count*3)throw new IOException("얼굴 관측점 규격을 확인하세요");}catch(Exception|Error error){if(next!=null)next.close();first.close();throw error;}landmarks=next;detector=first;fingerprint=ModelPlusAssets.fingerprint(PlusBlazeDetector.SHORT_SHA,sha);
    }
    synchronized List<Observations.Face> analyze(Bitmap image)throws Exception{
        List<PlusBlazeDetector.Box> boxes=detector.detect(image);if(boxes.isEmpty()){prior=null;return Collections.emptyList();}PlusBlazeDetector.Box box=Collections.max(boxes,(a,b)->Float.compare((a.right-a.left)*(a.bottom-a.top),(b.right-b.left)*(b.bottom-b.top)));float w=image.getWidth(),h=image.getHeight();float[][] eye=box.keys;float dx=(eye[1][0]-eye[0][0])*w,dy=(eye[1][1]-eye[0][1])*h;if(dx<0){dx=-dx;dy=-dy;}float angle=Math.abs(dx)+Math.abs(dy)<1?0:(float)Math.atan2(dy,dx);float side=Math.max((box.right-box.left)*w,(box.bottom-box.top)*h)*1.5f;PlusTensorModel.Roi roi=new PlusTensorModel.Roi((box.left+box.right)*w/2,(box.top+box.bottom)*h/2,Math.max(2,side),angle,(int)w,(int)h);float[][] out=landmarks.run(image,roi,0,255);if(PlusTensorModel.sigmoid(out[1][0])<.5f){prior=null;return Collections.emptyList();}
        float[][] points=new float[count][2];for(int i=0;i<count;i++){points[i]=roi.normalized(out[0][i*3]/landmarks.width,out[0][i*3+1]/landmarks.height);if(!Float.isFinite(points[i][0])||!Float.isFinite(points[i][1]))throw new IOException("얼굴 관측점 값을 확인하세요");}
        Observations.Face face=box.face();long now=SystemClock.elapsedRealtime();if(prior==null||now-lastAt>1500||PlusBlazeDetector.overlap(prior.face(),box)<.25f)tracking++;face.trackingId=tracking;prior=box;lastAt=now;face.landmarkCount=count;
        face.leftEyeAspect=ear(points,new int[]{362,385,387,263,373,380},w,h);face.rightEyeAspect=ear(points,new int[]{33,160,158,133,153,144},w,h);float mouthWidth=distance(points[61],points[291],w,h);face.mouthOpen=mouthWidth>1?Math.min(1,distance(points[13],points[14],w,h)/mouthWidth):-1;pose(points,w,h,face);return Collections.singletonList(face);
    }
    private static float distance(float[] a,float[] b,float w,float h){return(float)Math.hypot((a[0]-b[0])*w,(a[1]-b[1])*h);}
    private static float ear(float[][] p,int[] ids,float w,float h){float horizontal=distance(p[ids[0]],p[ids[3]],w,h);if(horizontal<1)return Float.NaN;float value=(distance(p[ids[1]],p[ids[5]],w,h)+distance(p[ids[2]],p[ids[4]],w,h))/(2*horizontal);return Float.isFinite(value)&&value>=0&&value<=1?value:Float.NaN;}
    private static void pose(float[][] points,float w,float h,Observations.Face face){
        if(!OpenCVLoader.initLocal())return;int[] ids={1,152,33,263,61,291};Point[] pixels=new Point[ids.length];for(int i=0;i<ids.length;i++)pixels[i]=new Point(points[ids[i]][0]*w,points[ids[i]][1]*h);
        MatOfPoint2f image=new MatOfPoint2f(pixels);MatOfPoint3f canonical=new MatOfPoint3f(new Point3(0,0,0),new Point3(0,330,65),new Point3(-225,-170,135),new Point3(225,-170,135),new Point3(-150,150,125),new Point3(150,150,125));Mat camera=Mat.eye(3,3,CvType.CV_64F),rvec=new Mat(),tvec=new Mat(),rotation=new Mat();MatOfDouble distortion=new MatOfDouble(0,0,0,0);
        try{camera.put(0,0,w);camera.put(1,1,w);camera.put(0,2,w/2);camera.put(1,2,h/2);if(!Calib3d.solvePnP(canonical,image,camera,distortion,rvec,tvec,false,Calib3d.SOLVEPNP_ITERATIVE))return;Calib3d.Rodrigues(rvec,rotation);double[] r=new double[9];rotation.get(0,0,r);float pitch=(float)Math.toDegrees(Math.atan2(r[7],r[8])),yaw=(float)Math.toDegrees(Math.atan2(-r[6],Math.hypot(r[0],r[3]))),roll=(float)Math.toDegrees(Math.atan2(r[3],r[0]));if(Float.isFinite(pitch)&&Float.isFinite(yaw)&&Float.isFinite(roll)){face.pitch=pitch;face.yaw=yaw;face.roll=roll;}}
        catch(RuntimeException ignored){}finally{distortion.release();rotation.release();tvec.release();rvec.release();camera.release();canonical.release();image.release();}
    }
    public synchronized void close(){try{landmarks.close();}finally{detector.close();}}
}
