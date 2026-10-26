package com.yonlab.vision;
import java.util.List;

/** Intel output contract and current-frame age/gender/expression combinations. */
public final class AgeGenderPolicy {
 private AgeGenderPolicy(){}
 private static boolean finite(float x){return !Float.isNaN(x)&&!Float.isInfinite(x);}
 public static void applyAge(float[] ageFraction,Observations.Face target){
  if(target==null||ageFraction==null||ageFraction.length!=1||!finite(ageFraction[0])||ageFraction[0]<0||ageFraction[0]>1)throw new IllegalArgumentException("연령 모델의 출력을 확인해 주세요");
  target.ageYears=ageFraction[0]*100;target.genderIndex=-1;target.femaleProbability=Float.NaN;target.maleProbability=Float.NaN;
 }
 public static void apply(float[] ageFraction,float[] femaleMale,Observations.Face target){
  if(target==null||ageFraction==null||ageFraction.length!=1||!finite(ageFraction[0])||ageFraction[0]<0||ageFraction[0]>1||femaleMale==null||femaleMale.length!=2)throw new IllegalArgumentException("연령 모델의 출력을 확인해 주세요");
  float female=femaleMale[0],male=femaleMale[1];if(!finite(female)||!finite(male)||female<0||female>1||male<0||male>1||Math.abs((double)female+male-1)>.005)throw new IllegalArgumentException("성별 모델의 출력을 확인해 주세요");
  target.ageYears=ageFraction[0]*100;target.femaleProbability=female;target.maleProbability=male;target.genderIndex=male>female?1:0;
 }
 public static int ageGroup(float age){return !finite(age)||age<0||age>100?-1:age<30?0:age<50?1:2;}
 public static String ageGroupLabel(int i){return i==0?"30세 미만":i==1?"30~49세":i==2?"50세 이상":"";}
 public static String genderLabel(int i){return i==0?"여성":i==1?"남성":"";}
 public static final class Snapshot {
  public final int visible,ages,genders,complete;
  public final int[] ageCounts,genderCounts;public final float[] genderMean;public final int[][][] combinations;
  private Snapshot(int visible,int ages,int genders,int complete,int[] ageCounts,int[] genderCounts,float[] genderMean,int[][][] combinations){this.visible=visible;this.ages=ages;this.genders=genders;this.complete=complete;this.ageCounts=ageCounts;this.genderCounts=genderCounts;this.genderMean=genderMean;this.combinations=combinations;}
 }
 public static Snapshot summarize(List<Observations.Face> faces){
  int visible=0,ages=0,genders=0,complete=0;int[] ageCounts=new int[3],genderCounts=new int[2];float[] genderMean=new float[2];int[][][] combinations=new int[3][2][ExpressionPolicy.CLASSES];
  if(faces!=null)for(Observations.Face f:faces){if(f==null)continue;visible++;int age=ageGroup(f.ageYears);if(age>=0){ages++;ageCounts[age]++;}int gender=f.genderIndex;
   boolean known=gender>=0&&gender<2&&finite(f.femaleProbability)&&finite(f.maleProbability)&&f.femaleProbability>=0&&f.femaleProbability<=1&&f.maleProbability>=0&&f.maleProbability<=1&&Math.abs((double)f.femaleProbability+f.maleProbability-1)<=.005;
   if(known){genders++;genderCounts[gender]++;genderMean[0]+=f.femaleProbability;genderMean[1]+=f.maleProbability;}int expression=ExpressionPolicy.argmax(f.expressionProbabilities);if(age>=0&&known&&expression>=0){combinations[age][gender][expression]++;complete++;}
  }
  if(genders>0){genderMean[0]/=genders;genderMean[1]/=genders;}return new Snapshot(visible,ages,genders,complete,ageCounts,genderCounts,genderMean,combinations);
 }
}
