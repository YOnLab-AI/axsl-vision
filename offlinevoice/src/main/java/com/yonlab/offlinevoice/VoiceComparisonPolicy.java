package com.yonlab.offlinevoice;
import java.util.*;
/** Module-local one-shot startup; offlinevoice keeps its independent dependency direction. */
public final class VoiceComparisonPolicy {
 private VoiceComparisonPolicy(){}
 public static final class StartGate {
  private boolean pending;
  public StartGate(boolean requested,boolean restored,boolean hasInput){pending=requested&&!restored&&hasInput;}
  public boolean take(){boolean value=pending;pending=false;return value;}
  public void cancel(){pending=false;}
 }
 public static List<String> selection(Collection<String> available,Collection<String> previous,String recommended){
  LinkedHashSet<String> result=new LinkedHashSet<>();for(String id:previous)if(available.contains(id))result.add(id);
  if(result.isEmpty()&&!previous.isEmpty()&&available.contains(recommended))result.add(recommended);
  return new ArrayList<>(result);
 }
 public static List<String> initialSelection(Collection<String> available,String requested,String recommended,boolean compareAll){
  if(compareAll)return new ArrayList<>(available);
  String selected=requested==null||requested.isEmpty()?recommended:requested;
  return available.contains(selected)?Collections.singletonList(selected):Collections.emptyList();
 }
}
