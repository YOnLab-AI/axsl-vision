package com.yonlab.vision;
import java.util.Arrays;
import java.util.Collections;

public final class ExpressionPolicyHost {
    private static int checks;
    private static void check(boolean yes,String name){if(!yes)throw new AssertionError(name);checks++;}
    private static void near(float x,float y,String name){check(Math.abs(x-y)<.00001f,name);}
    private static Observations.Face face(int index,float smile){Observations.Face f=new Observations.Face(0,0,1,1);float[] logits=new float[7];logits[index]=4;f.expressionProbabilities=ExpressionPolicy.softmax(logits);f.expressionIndex=ExpressionPolicy.argmax(f.expressionProbabilities);f.expressionConfidence=f.expressionProbabilities[f.expressionIndex];f.smile=smile;return f;}
    public static void main(String[] args){
        float[] zeros=new float[7],p=ExpressionPolicy.softmax(zeros);for(float x:p)near(x,1f/7,"uniform softmax");
        check(ExpressionPolicy.argmax(p)==0,"stable first-index tie");
        float[] logits={10000,10001,9999,9998,9997,9996,9995};p=ExpressionPolicy.softmax(logits);float sum=0;for(float x:p){check(x>=0&&x<=1,"bounded probability");sum+=x;}near(sum,1,"probability sum");
        check(ExpressionPolicy.argmax(p)==1,"large logits stable argmax");
        check(logits[0]==10000,"input logits not modified");
        float[] shifted={0,1,-1,-2,-3,-4,-5},q=ExpressionPolicy.softmax(shifted);for(int i=0;i<7;i++)near(p[i],q[i],"translation invariant");
        float[] extreme={Float.MAX_VALUE,-Float.MAX_VALUE,0,0,0,0,0};p=ExpressionPolicy.softmax(extreme);near(p[0],1,"extreme finite logits remain stable");
        boolean rejected=false;try{ExpressionPolicy.softmax(new float[6]);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"wrong dimension rejected");
        rejected=false;try{ExpressionPolicy.softmax(new float[]{0,0,0,Float.NaN,0,0,0});}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"NaN logits rejected");
        rejected=false;try{ExpressionPolicy.softmax(new float[]{0,0,0,Float.POSITIVE_INFINITY,0,0,0});}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"infinite logits rejected");
        check(ExpressionPolicy.argmax(null)==-1,"missing probabilities unknown");
        check(ExpressionPolicy.argmax(new float[]{-1,0,0,0,0,0,2})==-1,"invalid probability vector unknown");
        ExpressionPolicy.Snapshot s=ExpressionPolicy.summarize(Arrays.asList(face(3,.8f),face(4,.1f),face(3,-1),new Observations.Face(),null));
        check(s.visible==4&&s.evaluated==3&&s.missing==1,"no fabricated expression for missing face result");
        check(s.counts[3]==2&&s.counts[4]==1,"current frame argmax histogram");
        check(s.smileJoint[3][0]==1&&s.smileJoint[3][2]==1&&s.smileJoint[4][1]==1,"smile yes no unknown preserved in joint histogram");
        float mean=0;for(float x:s.meanProbabilities)mean+=x;near(mean,1,"mean probability distribution");
        s=ExpressionPolicy.summarize(Collections.emptyList());check(s.visible==0&&s.evaluated==0&&s.missing==0,"empty frame clears prior distributions");
        for(float x:s.meanProbabilities)near(x,0,"empty frame has no probabilities");
        check("happy".equals(ExpressionPolicy.label(3))&&"행복".equals(ExpressionPolicy.korean(3)),"official class order");
        check(ExpressionPolicy.label(-1).isEmpty(),"unknown label empty");
        System.out.println("Expression policy PASS "+checks+" checks (production softmax/aggregate; no model inference)");
    }
}
