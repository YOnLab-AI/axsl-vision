package com.yonlab.vision;
import java.util.Arrays;
import java.util.Collections;
public final class AgeGenderPolicyHost {
 private static int count;
 private static void check(boolean x,String name){if(!x)throw new AssertionError(name);count++;}
 private static Observations.Face face(float age,float female,float male,int expression){Observations.Face f=new Observations.Face(0,0,1,1);AgeGenderPolicy.apply(new float[]{age/100},new float[]{female,male},f);float[] logits=new float[7];logits[expression]=5;f.expressionProbabilities=ExpressionPolicy.softmax(logits);return f;}
 public static void main(String[] args){
  Observations.Face f=face(37,.8f,.2f,3);check(Math.abs(f.ageYears-37)<.0001,"real age fraction times100");check(f.genderIndex==0&&f.femaleProbability==.8f&&f.maleProbability==.2f,"official female male order");
  f=face(52,.2f,.8f,4);check(f.genderIndex==1,"male argmax");f=face(25,.5f,.5f,4);check(f.genderIndex==0,"deterministic gender tie");
  check(AgeGenderPolicy.ageGroup(29.99f)==0&&AgeGenderPolicy.ageGroup(30)==1,"age30 boundary");check(AgeGenderPolicy.ageGroup(49.99f)==1&&AgeGenderPolicy.ageGroup(50)==2,"age50 boundary");
  check(AgeGenderPolicy.ageGroup(Float.NaN)==-1&&AgeGenderPolicy.ageGroup(-1)==-1,"unknown age not bucketed");
  AgeGenderPolicy.Snapshot s=AgeGenderPolicy.summarize(Arrays.asList(face(20,.8f,.2f,3),face(40,.1f,.9f,4),face(55,.9f,.1f,3),new Observations.Face(),null));
  check(s.visible==4&&s.ages==3&&s.genders==3&&s.complete==3,"missing attributes never counted as actual");
  check(Arrays.equals(s.ageCounts,new int[]{1,1,1}),"three age bins");check(Arrays.equals(s.genderCounts,new int[]{2,1}),"gender histogram");
  check(s.combinations[0][0][3]==1&&s.combinations[1][1][4]==1&&s.combinations[2][0][3]==1,"actual age gender expression joint counts");
  check(Math.abs(s.genderMean[0]-.6f)<.0001&&Math.abs(s.genderMean[1]-.4f)<.0001,"mean model probabilities");
  s=AgeGenderPolicy.summarize(Collections.emptyList());check(s.visible==0&&s.complete==0&&s.genderMean[0]==0,"empty frame clears aggregates");
  boolean rejected=false;try{AgeGenderPolicy.apply(new float[]{Float.NaN},new float[]{.5f,.5f},new Observations.Face());}catch(IllegalArgumentException e){rejected=true;}check(rejected,"NaN age rejected");
  rejected=false;try{AgeGenderPolicy.apply(new float[]{.3f},new float[]{.4f,.4f},new Observations.Face());}catch(IllegalArgumentException e){rejected=true;}check(rejected,"not a probability distribution rejected");
  rejected=false;try{AgeGenderPolicy.apply(new float[]{.3f},new float[]{-1,2},new Observations.Face());}catch(IllegalArgumentException e){rejected=true;}check(rejected,"out of range gender rejected");
  rejected=false;try{AgeGenderPolicy.apply(new float[]{.3f},new float[]{1},new Observations.Face());}catch(IllegalArgumentException e){rejected=true;}check(rejected,"gender dimension rejected");
  rejected=false;try{AgeGenderPolicy.apply(new float[]{1.2f},new float[]{.5f,.5f},new Observations.Face());}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid model age fraction rejected");
  f=face(10,.8f,.2f,3);check(f.ageYears==10,"output is not fabricated by clipping to training ages18to75");
  System.out.println("Age/gender policy PASS "+count+" checks (production decode/aggregate; not age accuracy)");
 }
}
