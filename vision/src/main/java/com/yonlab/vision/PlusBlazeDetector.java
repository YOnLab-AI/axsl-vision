package com.yonlab.vision;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.*;
import java.util.*;

/** Published BlazeFace/BlazePose SSD tensor contracts, letterbox projection and greedy NMS. */
final class PlusBlazeDetector implements AutoCloseable {
    static final String SHORT_SHA="3bc182eb9f33925d9e58b5c8d59308a760f4adea8f282370e428c51212c26633",FULL_SHA="2c3728e6da56f21e21a320433396fb06d40d9088f2247c05e5635a688d45dfe1",POSE_SHA="9ba9dd3d42efaaba86b4ff0122b06f29c4122e756b329d89dca1e297fd8f866c";
    final String sha;private final PlusTensorModel model;private final List<float[]> anchors=new ArrayList<>();private final boolean pose;private final int coords;private final float threshold;
    static final class Box {final float left,top,right,bottom,score;final float[][] keys;Box(float l,float t,float r,float b,float s,float[][] k){left=l;top=t;right=r;bottom=b;score=s;keys=k;}Observations.Face face(){return new Observations.Face(left,top,right,bottom);}}
    PlusBlazeDetector(Context context,String kind)throws Exception{
        pose="pose".equals(kind);boolean full="full".equals(kind);sha=pose?POSE_SHA:full?FULL_SHA:SHORT_SHA;String name=pose?"blazepose-detector.tflite":full?"blazeface-full-sparse.tflite":"blazeface-short.tflite";model=new PlusTensorModel(context,name,sha);coords=pose?12:16;threshold=full?.6f:.5f;
        int[] strides=pose?new int[]{8,16,32,32,32}:full?new int[]{4}:new int[]{8,16,16,16};int layer=0;while(layer<strides.length){int last=layer+1;while(last<strides.length&&strides[last]==strides[layer])last++;int cells=(model.width+strides[layer]-1)/strides[layer],per=full?1:2*(last-layer);for(int y=0;y<cells;y++)for(int x=0;x<cells;x++)for(int n=0;n<per;n++)anchors.add(new float[]{(x+.5f)/cells,(y+.5f)/cells});layer=last;}
        if(model.shapes.length!=2||!Arrays.equals(model.shapes[0],new int[]{1,anchors.size(),coords})||!Arrays.equals(model.shapes[1],new int[]{1,anchors.size(),1})){model.close();throw new IOException("Blaze 검출 출력 규격을 확인하세요");}
    }
    synchronized List<Box> detect(Bitmap image)throws Exception{
        PlusTensorModel.Roi roi=PlusTensorModel.Roi.whole(image);float[][] output=model.run(image,roi,127.5f,127.5f);List<Box> candidates=new ArrayList<>();int keypoints=(coords-4)/2;
        for(int i=0;i<anchors.size();i++){if((i&127)==0&&Thread.currentThread().isInterrupted())throw new InterruptedIOException("분석 취소");float score=PlusTensorModel.sigmoid(output[1][i]);if(score<threshold)continue;int at=i*coords;float[] a=anchors.get(i);float x=output[0][at]/model.width+a[0],y=output[0][at+1]/model.height+a[1],w=output[0][at+2]/model.width,h=output[0][at+3]/model.height;if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(w)||!Float.isFinite(h)||w<=0||h<=0)continue;float[] lt=roi.normalized(x-w/2,y-h/2),rb=roi.normalized(x+w/2,y+h/2);float[][] keys=new float[keypoints][2];boolean valid=true;for(int n=0;n<keypoints;n++){keys[n]=roi.normalized(output[0][at+4+n*2]/model.width+a[0],output[0][at+5+n*2]/model.height+a[1]);valid&=Float.isFinite(keys[n][0])&&Float.isFinite(keys[n][1]);}if(!valid)continue;float l=Math.max(0,lt[0]),t=Math.max(0,lt[1]),r=Math.min(1,rb[0]),b=Math.min(1,rb[1]);if(r>l&&b>t)candidates.add(new Box(l,t,r,b,score,keys));}
        candidates.sort((a,b)->Float.compare(b.score,a.score));List<Box> keep=new ArrayList<>();for(Box box:candidates){boolean overlap=false;for(Box kept:keep)if(iou(box,kept)>.3f){overlap=true;break;}if(!overlap){keep.add(box);if(keep.size()>=(pose?1:8))break;}}return keep;
    }
    static float overlap(Observations.Face a,Box b){float intersection=Math.max(0,Math.min(a.right,b.right)-Math.max(a.left,b.left))*Math.max(0,Math.min(a.bottom,b.bottom)-Math.max(a.top,b.top));float union=(a.right-a.left)*(a.bottom-a.top)+(b.right-b.left)*(b.bottom-b.top)-intersection;return union>0?intersection/union:0;}
    private static float iou(Box a,Box b){return overlap(a.face(),b);}
    public void close(){model.close();}
}
