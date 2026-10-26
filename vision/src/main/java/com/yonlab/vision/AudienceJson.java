package com.yonlab.vision;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Aggregate observation diagnostics. No enrollment or anonymous feature vectors are serialized. */
public final class AudienceJson {
 private AudienceJson(){}
 public static JSONObject summary(List<Observations.Face> faces)throws JSONException{
  ExpressionPolicy.Snapshot e=ExpressionPolicy.summarize(faces);AgeGenderPolicy.Snapshot a=AgeGenderPolicy.summarize(faces);
  JSONArray expressions=new JSONArray(),mean=new JSONArray(),smileJoint=new JSONArray(),age=new JSONArray(),gender=new JSONArray(),genderMean=new JSONArray(),joint=new JSONArray();
  for(int i=0;i<7;i++){expressions.put(e.counts[i]);mean.put((double)e.meanProbabilities[i]);JSONArray row=new JSONArray();for(int x:e.smileJoint[i])row.put(x);smileJoint.put(row);}
  for(int x:a.ageCounts)age.put(x);for(int x:a.genderCounts)gender.put(x);for(float x:a.genderMean)genderMean.put((double)x);
  for(int ageIndex=0;ageIndex<3;ageIndex++){JSONArray sexes=new JSONArray();for(int g=0;g<2;g++){JSONArray emotions=new JSONArray();for(int x:a.combinations[ageIndex][g])emotions.put(x);sexes.put(emotions);}joint.put(sexes);}
  return new JSONObject().put("scope","CURRENT_FRAME").put("expressionModelSha256",FaceEngine.EXPRESSION_SHA256).put("ageGenderModelSha256",FaceEngine.AGE_GENDER_SHA256).put("visible",a.visible).put("expressionFaces",e.evaluated).put("ageFaces",a.ages).put("genderFaces",a.genders).put("completeCombinationFaces",a.complete).put("expressionClassCounts",expressions).put("expressionMeanProbabilities",mean).put("expressionSmileJointCounts",smileJoint).put("ageGroupCounts",age).put("genderCounts",gender).put("genderMeanProbabilities",genderMean).put("ageGenderExpressionCounts",joint);
 }
 public static JSONObject face(Observations.Face f)throws JSONException{
  JSONObject j=new JSONObject();if(ExpressionPolicy.argmax(f.expressionProbabilities)>=0){JSONArray probabilities=new JSONArray();for(float x:f.expressionProbabilities)probabilities.put((double)x);j.put("expressionIndex",f.expressionIndex).put("expressionLabel",ExpressionPolicy.label(f.expressionIndex)).put("expressionConfidence",f.expressionConfidence).put("expressionProbabilities",probabilities);}
  if(AgeGenderPolicy.ageGroup(f.ageYears)>=0)j.put("ageYears",f.ageYears);
  if(f.genderIndex>=0&&f.genderIndex<2)j.put("genderIndex",f.genderIndex).put("femaleProbability",f.femaleProbability).put("maleProbability",f.maleProbability);
  return j;
 }
}
