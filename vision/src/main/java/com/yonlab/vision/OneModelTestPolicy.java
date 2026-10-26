package com.yonlab.vision;

import java.util.*;

/** A deliberate model test changes the input, never silently changes the engine. */
public final class OneModelTestPolicy {
 private OneModelTestPolicy(){}
 public static boolean keepModel(String explicitModel,boolean modelTest){return modelTest||explicitModel!=null&&!explicitModel.isEmpty();}
 public static String modelAfterInput(String tested,String preferred,Collection<String> available,boolean keepModel){
  String id=keepModel?tested:preferred;
  if(id==null||id.isEmpty()||!available.contains(id))throw new IllegalArgumentException("시험할 모델을 다시 선택하세요");
  return id;
 }
 public static List<String> selectionAfterInput(Collection<String> available,Collection<String> previous){
  LinkedHashSet<String> selected=new LinkedHashSet<>();
  if(previous!=null)for(String id:previous)if(available.contains(id))selected.add(id);
  return new ArrayList<>(selected);
 }
 public static String launchGroup(String feature,String dataGroup,String currentGroup,boolean keepModel){
  if(keepModel&&"speech".equals(feature))return "ALL";
  if(Arrays.asList("speech","ocr","document").contains(feature)&&("KO".equals(dataGroup)||"GO".equals(dataGroup)))return dataGroup;
  return "KO".equals(currentGroup)?"KO":"GO".equals(currentGroup)?"GO":"ALL";
 }
}
