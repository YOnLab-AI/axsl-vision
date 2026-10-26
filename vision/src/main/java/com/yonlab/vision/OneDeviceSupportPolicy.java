package com.yonlab.vision;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Device notices describe observed failures; a weak recognition result is not a failure. */
public final class OneDeviceSupportPolicy {
 private OneDeviceSupportPolicy(){}
 public static String key(String fingerprint,String version,String feature,String model){
  try{byte[] hash=MessageDigest.getInstance("SHA-256").digest((String.valueOf(fingerprint)+"\u0000"+String.valueOf(version)+"\u0000"+String.valueOf(feature)+"\u0000"+String.valueOf(model)).getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:hash)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
 }
 public static String outcome(String state,String error,String reason){
  if("CANCELED".equals(state)||"CANCELLED".equals(state)||"STOPPED".equals(state))return "";
  if("ERROR".equals(state)||"FAILED".equals(state)||"UNSUPPORTED".equals(state))return "ERROR";
  if("CLOSED".equals(state)&&error!=null&&!error.isEmpty())return "ERROR";
  if("COMPLETED".equals(state)||"CLOSED".equals(state)&&"COMPLETE".equals(reason))return "COMPLETED";
  return "";
 }
 public static String group(String value){return "KO".equals(value)?"KO":"GO".equals(value)?"GO":"ALL";}
 public static String pipelineOutcome(String state,String error,int completedModels){
  if("CANCELED".equals(state)||"CANCELLED".equals(state)||"STOPPED".equals(state))return "";
  boolean terminal="ERROR".equals(state)||"FAILED".equals(state)||"COMPLETED".equals(state)||"PARTIAL_ERROR".equals(state);
  if(!terminal)return "";
  if(error!=null&&!error.isEmpty())return "ERROR";
  return completedModels>0&&("COMPLETED".equals(state)||"PARTIAL_ERROR".equals(state)||"ERROR".equals(state))?"COMPLETED":"";
 }
 public static String liveOutcome(String state,String error,long frames,boolean ownerClosed,boolean cameraClosed,boolean workerClear,boolean engineCloseReturned){
  if(!"CLOSED".equals(state))return outcome(state,error,"");
  if(error!=null&&!error.isEmpty())return "ERROR";
  return frames>0&&ownerClosed&&cameraClosed&&workerClear&&engineCloseReturned?"COMPLETED":"";
 }
 public static boolean noticeRequired(String latestState){return "ERROR".equals(latestState);}
 public static String code(String error){return error!=null&&error.matches("[A-Za-z0-9_.-]{1,96}")?error:"RUN_FAILED";}
}
