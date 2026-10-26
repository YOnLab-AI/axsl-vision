package com.yonlab.vision;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.*;
import java.util.*;

/** PoseNet COCO17 and BlazePose33 use distinct networks and their actual likelihood outputs. */
final class PlusPoseEngine implements AutoCloseable {
    static final String POSENET_SHA="7bcc1efa3010de45640406a4ce0acc2ef89a742021f11adacdca2fd6e494fa07",BLAZE_SHA="e9a5c5cb17f736fafd4c2ec1da3b3d331d6edbe8a0d32395855aeb2cdfd64b9f";
    final String sha256;private final boolean blaze;private final PlusTensorModel model;private final PlusBlazeDetector detector;
    static boolean supports(String id){return Arrays.asList("posenet-mobilenet","blazepose-full","coach-posenet","coach-blazepose").contains(id);}
    PlusPoseEngine(Context context,String id)throws Exception{
        if(!supports(id))throw new IllegalArgumentException("추가 자세 모델");blaze=id.contains("blazepose");PlusTensorModel next=new PlusTensorModel(context,blaze?"blazepose-full.tflite":"posenet-mobile.tflite",blaze?BLAZE_SHA:POSENET_SHA);PlusBlazeDetector box=null;try{if(blaze){box=new PlusBlazeDetector(context,"pose");if(next.shapes.length<2||!Arrays.equals(next.shapes[0],new int[]{1,195}))throw new IOException("BlazePose 출력 규격");}else if(next.shapes.length<2||!Arrays.equals(next.shapes[0],new int[]{1,9,9,17})||!Arrays.equals(next.shapes[1],new int[]{1,9,9,34}))throw new IOException("PoseNet 출력 규격");}catch(Exception|Error error){next.close();if(box!=null)box.close();throw error;}model=next;detector=box;sha256=blaze?ModelPlusAssets.fingerprint(PlusBlazeDetector.POSE_SHA,BLAZE_SHA):POSENET_SHA;
    }
    synchronized List<PoseGeometry.Point> pose(Bitmap image)throws Exception{
        if(!blaze)return posenet(image);List<PlusBlazeDetector.Box> found=detector.detect(image);if(found.isEmpty())return Collections.emptyList();PlusBlazeDetector.Box box=found.get(0);float w=image.getWidth(),h=image.getHeight(),cx=box.keys[0][0]*w,cy=box.keys[0][1]*h,dx=(box.keys[1][0]-box.keys[0][0])*w,dy=(box.keys[1][1]-box.keys[0][1])*h;float radius=(float)Math.hypot(dx,dy);if(radius<1||radius>Math.max(w,h)*1.5f)return Collections.emptyList();float angle=(float)(Math.atan2(dy,dx)+Math.PI/2);PlusTensorModel.Roi roi=new PlusTensorModel.Roi(cx,cy,radius*2*1.25f,angle,(int)w,(int)h);float[][] output=model.run(image,roi,0,255);if(!Float.isFinite(output[1][0])||output[1][0]<.5f)return Collections.emptyList();List<PoseGeometry.Point> result=new ArrayList<>();for(int i=0;i<33;i++){int at=i*5;float[] point=roi.normalized(output[0][at]/model.width,output[0][at+1]/model.height);float confidence=Math.min(PlusTensorModel.sigmoid(output[0][at+3]),PlusTensorModel.sigmoid(output[0][at+4]));if(Float.isFinite(point[0])&&Float.isFinite(point[1]))result.add(new PoseGeometry.Point(i,point[0],point[1],confidence));}return result;
    }
    private List<PoseGeometry.Point> posenet(Bitmap image)throws Exception{
        PlusTensorModel.Roi roi=PlusTensorModel.Roi.whole(image);float[][] out=model.run(image,roi,128f,128f);int[] mapped=OneTensorMath.cocoToMlKit();List<PoseGeometry.Point> result=new ArrayList<>();int rows=model.shapes[0][1],columns=model.shapes[0][2];float strideY=(model.height-1f)/(rows-1),strideX=(model.width-1f)/(columns-1);
        for(int joint=0;joint<17;joint++){int best=joint;for(int pos=joint;pos<out[0].length;pos+=17)if(out[0][pos]>out[0][best])best=pos;float score=PlusTensorModel.sigmoid(out[0][best]);int cell=best/17,row=cell/columns,col=cell%columns;float y=row*strideY+out[1][cell*34+joint],x=col*strideX+out[1][cell*34+17+joint];float[] point=roi.normalized(x/model.width,y/model.height);if(Float.isFinite(point[0])&&Float.isFinite(point[1]))result.add(new PoseGeometry.Point(mapped[joint],point[0],point[1],score));}return result;
    }
    public synchronized void close(){try{model.close();}finally{if(detector!=null)detector.close();}}
}
