package com.yonlab.offlinevoice;
import android.content.Context;

/** Catalog region is used only when both selected demo ID and actual encoded source SHA agree. */
final class VoiceSampleMeta {
 private VoiceSampleMeta(){}
 static String group(Context context,String demo,String sourceSha){
  if(demo==null||demo.isEmpty()||sourceSha==null||!sourceSha.matches("[0-9a-f]{64}"))return "";
  try{Object item=Class.forName("com.yonlab.vision.DemoStore").getMethod("find",Context.class,String.class).invoke(null,context,demo);if(item==null||!sourceSha.equals(item.getClass().getField("sha256").get(item)))return "";Object group=item.getClass().getField("group").get(item);return "KO".equals(group)||"GO".equals(group)||"SHARED".equals(group)?(String)group:"";}catch(ReflectiveOperationException|LinkageError e){return "";}
 }
}
