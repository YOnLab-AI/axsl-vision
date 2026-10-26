package com.yonlab.vision;

import java.util.List;

/** Official FER class order, stable softmax and current-frame aggregates. No synthetic attributes. */
public final class ExpressionPolicy {
    public static final int CLASSES=7;
    private static final String[] LABELS={"angry","disgust","fearful","happy","neutral","sad","surprised"};
    private static final String[] KOREAN={"분노","혐오","두려움","행복","중립","슬픔","놀람"};
    private ExpressionPolicy(){}
    public static String label(int index){return index>=0&&index<CLASSES?LABELS[index]:"";}
    public static String korean(int index){return index>=0&&index<CLASSES?KOREAN[index]:"";}
    private static boolean finite(float x){return !Float.isNaN(x)&&!Float.isInfinite(x);}
    /** Mathematical softmax, not a calibration or an accuracy estimate. */
    public static float[] softmax(float[] logits){
        if(logits==null||logits.length!=CLASSES)throw new IllegalArgumentException("표정 모델의 출력 차원을 확인해 주세요");
        double max=-Double.MAX_VALUE;for(float x:logits){if(!finite(x))throw new IllegalArgumentException("표정 모델의 출력 값을 확인해 주세요");max=Math.max(max,x);}
        double[] values=new double[CLASSES];double sum=0;for(int i=0;i<CLASSES;i++){values[i]=Math.exp((double)logits[i]-max);sum+=values[i];}
        if(!(sum>0)||Double.isInfinite(sum)||Double.isNaN(sum))throw new IllegalArgumentException("표정 모델의 출력 값을 확인해 주세요");
        float[] result=new float[CLASSES];for(int i=0;i<CLASSES;i++)result[i]=(float)(values[i]/sum);return result;
    }
    public static int argmax(float[] probabilities){
        if(probabilities==null||probabilities.length!=CLASSES)return -1;
        int best=0;double sum=0;for(int i=0;i<CLASSES;i++){float x=probabilities[i];if(!finite(x)||x<0||x>1)return -1;sum+=x;if(x>probabilities[best])best=i;}
        return Math.abs(sum-1)<.005?best:-1;
    }
    public static final class Snapshot {
        public final int visible,evaluated,missing;
        public final int[] counts;
        public final float[] meanProbabilities;
        /** Columns: observed smile >=.5, observed smile <.5, unknown smile. */
        public final int[][] smileJoint;
        private Snapshot(int visible,int evaluated,int[] counts,float[] mean,int[][] joint){this.visible=visible;this.evaluated=evaluated;this.missing=visible-evaluated;this.counts=counts;meanProbabilities=mean;smileJoint=joint;}
    }
    public static Snapshot summarize(List<Observations.Face> faces){
        int visible=0,evaluated=0;int[] counts=new int[CLASSES];float[] mean=new float[CLASSES];int[][] joint=new int[CLASSES][3];
        if(faces!=null)for(Observations.Face f:faces){
            if(f==null)continue;visible++;int index=argmax(f.expressionProbabilities);if(index<0)continue;evaluated++;counts[index]++;
            for(int k=0;k<CLASSES;k++)mean[k]+=f.expressionProbabilities[k];
            int smile=!finite(f.smile)||f.smile<0||f.smile>1?2:f.smile>=.5f?0:1;joint[index][smile]++;
        }
        if(evaluated>0)for(int k=0;k<CLASSES;k++)mean[k]/=evaluated;
        return new Snapshot(visible,evaluated,counts,mean,joint);
    }
}
