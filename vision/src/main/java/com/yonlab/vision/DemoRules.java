package com.yonlab.vision;
/** Pure boundary policy for the app-owned demo catalog. */
public final class DemoRules {
 private DemoRules(){}
 public static boolean safeAsset(String path){
  if(path==null||!path.startsWith("one/media/")||path.length()>180||!path.matches("[A-Za-z0-9_./-]+"))return false;
  for(String part:path.split("/",-1))if(part.isEmpty()||part.startsWith("."))return false;
  return true;
 }
 public static boolean feature(String id){return id!=null&&java.util.Arrays.asList("detect","classify","segment","faces","compare","access","drive","flow","vis","pose","ocr","qr","speech","signage","document","studio","search","sound","coach").contains(id);}
 public static boolean groupMatches(String item,String filter){
  if(!java.util.Arrays.asList("KO","GO","SHARED").contains(item))return false;
  return filter==null||filter.isEmpty()||"ALL".equals(filter)||item.equals(filter)||"SHARED".equals(item);
 }
 public static boolean typeMatches(String item,String filter){
  if(!java.util.Arrays.asList("image","video","audio").contains(item))return false;
  return filter==null||filter.isEmpty()||"any".equals(filter)||"*/*".equals(filter)||filter.equals(item)||filter.startsWith(item+"/");
 }
}
