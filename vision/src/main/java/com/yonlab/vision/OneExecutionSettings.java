package com.yonlab.vision;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import java.util.Map;
import org.json.JSONObject;

/** Each engine resolves CPU limits at creation; no global native session is retained. */
public final class OneExecutionSettings {
 private OneExecutionSettings(){}
 public static String mode(Context c){
  Map<String,?> values=c.getSharedPreferences("YOn",0).getAll();Object mode=values.get("executionMode");
  if(mode instanceof String&&valid((String)mode))return (String)mode;
  Object legacy=values.get("analysisThreads");
  if(mode==null&&legacy instanceof Integer&&((Integer)legacy==1||(Integer)legacy==4)&&!Boolean.TRUE.equals(values.get("boardOptimization")))return "cpu"+legacy;
  return "auto";
 }
 public static boolean valid(String mode){return "auto".equals(mode)||"cpu1".equals(mode)||"cpu2".equals(mode)||"cpu4".equals(mode);}
 public static void select(Context c,String mode){if(!valid(mode))throw new IllegalArgumentException("실행 프로필을 선택하세요");c.getSharedPreferences("YOn",0).edit().putString("executionMode",mode).apply();sync(c);}
 public static void sync(Context c){c.getSharedPreferences("YOn",0).edit().putInt("analysisThreads",threads(c)).apply();}
 private static final class State {
  int cores=Runtime.getRuntime().availableProcessors(),thermal=-1;long total,available;boolean lowRam,lowMemory,powerSaver,board;
  boolean constrained(){return lowRam||lowMemory||powerSaver||thermal>=3;}
 }
 private static State state(Context c){
  State s=new State();Object stored=c.getSharedPreferences("YOn",0).getAll().get("boardOptimization");s.board=OneBoardPolicy.detected(Build.HARDWARE,Build.BOARD,Build.MODEL)&&Boolean.TRUE.equals(stored);
  ActivityManager am=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
  if(am!=null){s.lowRam=am.isLowRamDevice();ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();am.getMemoryInfo(info);s.total=info.totalMem;s.available=info.availMem;s.lowMemory=info.lowMemory;}
  PowerManager pm=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
  if(pm!=null){s.powerSaver=pm.isPowerSaveMode();if(Build.VERSION.SDK_INT>=29)s.thermal=pm.getCurrentThermalStatus();}
  return s;
 }
 public static int threads(Context c){State s=state(c);return OneExecutionPolicy.threads(mode(c),s.board,s.cores,s.constrained());}
 public static JSONObject snapshot(Context c){
  State s=state(c);JSONObject j=new JSONObject();try{j.put("mode",mode(c)).put("requestedCpuThreads",OneExecutionPolicy.threads(mode(c),s.board,s.cores,s.constrained())).put("cpuCores",s.cores).put("totalMemoryBytes",s.total).put("availableMemoryBytes",s.available).put("lowRam",s.lowRam).put("lowMemory",s.lowMemory).put("powerSaver",s.powerSaver).put("thermalStatus",s.thermal).put("resourceConstrained",s.constrained()).put("boardOptimization",s.board).put("device",Build.MANUFACTURER+" "+Build.MODEL).put("api",Build.VERSION.SDK_INT).put("accelerator","CPU").put("scope","Requested CPU settings for controllable engines; SDK-managed model scheduling is owned by the SDK");}catch(org.json.JSONException impossible){throw new IllegalStateException(impossible);}return j;
 }
}
