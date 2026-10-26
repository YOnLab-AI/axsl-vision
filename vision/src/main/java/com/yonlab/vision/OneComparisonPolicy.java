package com.yonlab.vision;

import java.util.*;

/** Input context and one-shot startup rules, independent of Android lifecycle callbacks. */
public final class OneComparisonPolicy {
 private OneComparisonPolicy(){}
 public static String group(String value){return "KO".equals(value)?"KO":"GO".equals(value)?"GO":"ALL";}
 public static String groupLabel(String value){return "KO".equals(value)?"한국 (KO)":"GO".equals(value)?"글로벌 (GO)":"전체";}
 public static String model(String requested,String preferred,Collection<String> available){
  String id=requested==null||requested.isEmpty()?preferred:requested;
  if(!available.contains(id))throw new IllegalArgumentException("이 기능에 맞는 모델을 선택하세요");
  return id;
 }
 public static List<String> selection(Collection<String> available,Collection<String> restored,String selected,boolean compareAll){
  LinkedHashSet<String> result=new LinkedHashSet<>();
  if(restored!=null){for(String id:restored)if(available.contains(id))result.add(id);}
  else for(String id:available)if(compareAll||id.equals(selected))result.add(id);
  return new ArrayList<>(result);
 }
 public static final class StartGate {
  private boolean pending;
  public StartGate(boolean requested,boolean restored,boolean hasInput){pending=requested&&!restored&&hasInput;}
  public boolean take(){boolean value=pending;pending=false;return value;}
  public void cancel(){pending=false;}
  public void inputSelected(boolean comparison){pending=!comparison;}
 }
 public static boolean canRestore(String run,String sha,String group,String storedRun,String storedSha,String storedGroup,String state,boolean workerDone){
  return run!=null&&!run.isEmpty()&&sha!=null&&sha.matches("[a-f0-9]{64}")&&run.equals(storedRun)&&sha.equals(storedSha)&&group(group).equals(group(storedGroup))&&workerDone&&Arrays.asList("COMPLETED","PARTIAL_ERROR","ERROR").contains(state);
 }
 public static String restoredPhase(boolean wasRunning,boolean restoredResults){return wasRunning?"INTERRUPTED":restoredResults?"COMPLETED":"READY";}
}
