package com.yonlab.vision;
import java.util.*;
/** Per-image, class-aware greedy IoU matching; not dataset mAP. */
public final class OneDetectionEval {
 private OneDetectionEval(){}
 public static long[] evaluate(float[][] predictions,float[][] truth,float threshold){if(predictions==null||truth==null||predictions.length>1000||truth.length>1000||!OneModelMath.finite(threshold)||threshold<=0||threshold>1)throw new IllegalArgumentException("evaluation contract");
  for(float[] row:predictions){validate(row,6);if(row[4]<0||row[4]>1||row[5]<0||row[5]!=Math.rint(row[5]))throw new IllegalArgumentException("prediction class/score");}
  for(float[] row:truth){validate(row,5);if(row[4]<0||row[4]!=Math.rint(row[4]))throw new IllegalArgumentException("truth class");}
  List<float[]> order=new ArrayList<>(Arrays.asList(predictions));order.sort((a,b)->Float.compare(b[4],a[4]));boolean[] used=new boolean[truth.length];long tp=0,fp=0;
  for(float[] p:order){int best=-1;float overlap=threshold;for(int n=0;n<truth.length;n++){if(used[n]||p[5]!=truth[n][4])continue;float value=iou(p,truth[n]);if(value>=overlap){overlap=value;best=n;}}if(best>=0){used[best]=true;tp++;}else fp++;}return new long[]{tp,fp,truth.length-tp};
 }
 private static void validate(float[] row,int length){if(row==null||row.length!=length)throw new IllegalArgumentException("box shape");for(float f:row)if(!OneModelMath.finite(f))throw new IllegalArgumentException("nonfinite box");if(row[0]<0||row[1]<0||row[2]>1||row[3]>1||row[2]<=row[0]||row[3]<=row[1])throw new IllegalArgumentException("normalized box");}
 private static float iou(float[] a,float[] b){float w=Math.max(0,Math.min(a[2],b[2])-Math.max(a[0],b[0])),h=Math.max(0,Math.min(a[3],b[3])-Math.max(a[1],b[1])),area=w*h;return area/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-area);}
}
