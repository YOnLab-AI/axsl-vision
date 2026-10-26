package com.yonlab.vision;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

/** Per-build/device last outcomes, without transcripts, pictures, names, vectors or input paths. */
public final class OneDeviceSupport {
 private OneDeviceSupport(){}
 private static final String PREFS="YOnDeviceSupportV1",INPUT="@input";
 private static String version(Context c)throws Exception{PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),0);return p.versionName+":"+(Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode);}
 private static String key(Context c,String f,String m)throws Exception{return "entry."+OneDeviceSupportPolicy.key(Build.FINGERPRINT,version(c),f,m);}
 private static boolean current(Context c,JSONObject r)throws Exception{return version(c).equals(r.optString("appVersion"))&&Build.FINGERPRINT.equals(r.optString("fingerprint"));}
 public static void observe(Context c,String feature,String model,String state,String error,String origin){
  try{String latest=OneDeviceSupportPolicy.outcome(state,error,"");if(latest.isEmpty()||feature==null||feature.isEmpty()||model==null||model.isEmpty())return;
   SharedPreferences p=c.getSharedPreferences(PREFS,0);String key=key(c,feature,model);JSONObject old=new JSONObject(p.getString(key,"{}"));long now=System.currentTimeMillis();JSONObject r=new JSONObject().put("feature",feature).put("modelId",model).put("scope",INPUT.equals(model)?"input-pipeline":"model").put("state",latest).put("errorCode","ERROR".equals(latest)?OneDeviceSupportPolicy.code(error):"").put("origin",origin==null?"":origin).put("at",now).put("device",Build.MANUFACTURER+" "+Build.MODEL).put("api",Build.VERSION.SDK_INT).put("fingerprint",Build.FINGERPRINT).put("appVersion",version(c));
   if("ERROR".equals(latest))r.put("lastFailureAt",now).put("lastFailureCode",r.optString("errorCode"));else if(old.has("lastFailureAt"))r.put("lastFailureAt",old.optLong("lastFailureAt")).put("lastFailureCode",old.optString("lastFailureCode"));
   p.edit().putString(key,r.toString()).apply();
  }catch(Exception|LinkageError|OutOfMemoryError ignored){}
 }
 /** Invoked only after the original journal commit succeeds. Never writes Records again. */
 public static void observeRecord(Context c,String name,JSONObject record){
  try{
   if(record==null||!java.util.Arrays.asList("model-bench-result.json","sound-bench-result.json","video-result.json","live-result.json","photo-result.json","compare-result.json","signage.json").contains(name))return;
   if("signage.json".equals(name)){String state=record.optString("state");if("ERROR".equals(state))observe(c,"signage","android-media","ERROR",record.optString("errorType","MEDIA_PLAYBACK"),"signage-playback");else if(("PLAYING_IMAGE".equals(state)||"PLAYING_VIDEO".equals(state))&&record.optBoolean("active")&&record.optBoolean("playing"))observe(c,"signage","android-media","COMPLETED","","signage-playback");return;}
   String feature=record.optString("feature",record.optString("mode"));if("compare-result.json".equals(name))feature="compare";feature=OneModelPolicy.canonicalFeature(feature);
   if(!DemoRules.feature(feature))return;String origin=name.substring(0,name.length()-5);JSONArray models=record.optJSONArray("models");
   if(models!=null){int completed=0;for(int i=0;i<models.length();i++){JSONObject row=models.optJSONObject(i);if(row==null)continue;observe(c,feature,row.optString("modelId"),row.optString("state"),row.optString("errorType",row.optString("failureCode")),origin);if("COMPLETED".equals(row.optString("state")))completed++;}
    String outer=record.optString("errorType");if(outer.isEmpty())outer=record.optString("failureCode");if(outer.isEmpty())outer=record.optString("cleanupErrorType");String pipeline=OneDeviceSupportPolicy.pipelineOutcome(record.optString("state"),outer,completed);if(!pipeline.isEmpty())observe(c,feature,INPUT,pipeline,outer,origin+"-input-pipeline");return;}
   String code=record.optString("errorType");if(code.isEmpty())code=record.optString("cleanupErrorType");if(code.isEmpty())code=record.optString("failureCode");if(code.isEmpty()&&record.optJSONObject("details")!=null)code=record.optJSONObject("details").optString("error");String latest="live-result.json".equals(name)?OneDeviceSupportPolicy.liveOutcome(record.optString("state"),code,record.optLong("frames"),record.optBoolean("ownerClosed"),record.optBoolean("cameraClosed"),record.optBoolean("workerClear"),record.optBoolean("engineCloseReturned")):OneDeviceSupportPolicy.outcome(record.optString("state"),code,record.optString("reason"));
   if(!latest.isEmpty())observe(c,feature,record.optString("modelId"),latest,code,origin);
  }catch(Exception|LinkageError|OutOfMemoryError ignored){}
 }
 private static JSONObject active(Context c,String feature,String model){try{JSONObject r=new JSONObject(c.getSharedPreferences(PREFS,0).getString(key(c,feature,model),"{}"));return current(c,r)&&OneDeviceSupportPolicy.noticeRequired(r.optString("state"))?r:null;}catch(Exception|LinkageError|OutOfMemoryError ignored){return null;}}
 private static String featureTitle(String feature){try{OneCatalog.Feature f=OneCatalog.find(feature);return f==null?feature:f.title;}catch(Exception ignored){return feature;}}
 private static String modelTitle(Context c,String f,String m){if(INPUT.equals(m))return "입력 준비·해제";try{return OneModels.title(c,f,m);}catch(Exception ignored){return m;}}
 private static String explanation(String code){
  if(code.contains("Permission")||code.contains("Security")||code.equals("PERMISSION"))return "이 실행에 필요한 권한이 거부됐습니다. 앱 권한에서 카메라·마이크를 확인하세요.";
  if(code.contains("OutOfMemory")||code.equals("MEMORY"))return "모델 실행 메모리가 부족했습니다. 다른 모델을 고르거나 CPU 프로필을 바꿔 다시 실행하세요.";
  if(code.contains("UnsatisfiedLink")||code.contains("NoClassDef")||code.contains("Linkage")||code.equals("NATIVE_RUNTIME"))return "이 모델의 실행 라이브러리를 불러오지 못했습니다. 다른 모델로 비교를 계속할 수 있습니다.";
  if(code.contains("IOException")||code.contains("INPUT")||code.contains("DECODE"))return "입력 또는 모델 파일을 읽는 단계에서 오류가 났습니다. 다른 자료를 선택하거나 같은 모델을 다시 실행하세요.";
  return "같은 모델을 다시 실행하거나 다른 모델을 선택할 수 있습니다.";
 }
 public static void guard(Activity a,String feature,String requested,Runnable proceed){guard(a,feature,requested,"ALL",proceed);}
 public static void guard(Activity a,String feature,String requested,String requestedGroup,Runnable proceed){
  if(a.isFinishing()||a.isDestroyed())return;String group=OneDeviceSupportPolicy.group(requestedGroup),selected=requested;try{if(selected==null||selected.isEmpty())selected=OneModels.selected(a,feature,group);}catch(Exception ignored){}final String model=selected==null?"":selected;JSONObject r=active(a,feature,model),input=active(a,feature,INPUT);
  if(r==null&&input==null){proceed.run();return;}
  StringBuilder message=new StringBuilder(featureTitle(feature));if(input!=null)message.append("\n입력 준비·해제 오류 · ").append(input.optString("errorCode")).append("\n입력을 읽거나 해제하는 단계에서 오류가 났습니다.\n");
  if(r!=null)message.append("\n").append(modelTitle(a,feature,model)).append("\n최근 실행: ").append(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(r.optLong("at")))).append("\n오류: ").append(r.optString("errorCode")).append("\n").append(explanation(r.optString("errorCode")));
  AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(Build.MODEL+" 실행 안내").setMessage(message.toString()).setPositiveButton("다시 시도",(d,w)->proceed.run()).setNegativeButton("닫기",null);
  if(input!=null)dialog.setNeutralButton("다른 자료 선택",(d,w)->workspace(a,feature,group));else dialog.setNeutralButton("다른 모델",(d,w)->choose(a,feature,model,group));dialog.show();
 }
 public static void guardComparison(Activity a,String feature,String requestedGroup,Runnable proceed){
  if(a.isFinishing()||a.isDestroyed())return;String group=OneDeviceSupportPolicy.group(requestedGroup);StringBuilder failures=new StringBuilder();JSONObject input=active(a,feature,INPUT);if(input!=null)failures.append("입력 준비·해제 · ").append(input.optString("errorCode")).append("\n모델 실행과 별도로 입력 단계에서 오류가 났습니다.\n\n");for(OneModels.Entry e:OneModels.list(a,feature,group)){JSONObject r=active(a,feature,e.id);if(r!=null)failures.append(e.title).append(" · ").append(r.optString("errorCode")).append("\n");}
  if(failures.length()==0){proceed.run();return;}
  AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(Build.MODEL+" 모델 비교 안내").setMessage("최근 실행 오류\n\n"+failures+"\n모든 모델을 다시 시험합니다. 메뉴와 비교 대상은 그대로 유지합니다.").setPositiveButton("모두 다시 시험",(d,w)->proceed.run()).setNegativeButton("닫기",null);if(input!=null)dialog.setNeutralButton("다른 자료 선택",(d,w)->workspace(a,feature,group));dialog.show();
 }
 private static void workspace(Activity a,String feature,String group){if(a instanceof OneActivity){((OneActivity)a).applySupportGroup(group);a.recreate();}else a.startActivity(new android.content.Intent(a,OneActivity.class).putExtra("feature",feature).putExtra("group",group));}
 private static void choose(Activity a,String feature,String current,String group){
  java.util.List<OneModels.Entry> list=OneModels.list(a,feature,group);java.util.List<String> ids=new java.util.ArrayList<>(),labels=new java.util.ArrayList<>();for(OneModels.Entry e:list)if(!e.id.equals(current)){ids.add(e.id);labels.add(e.title);}
  if(ids.isEmpty()){new AlertDialog.Builder(a).setTitle("다른 자료로 다시 실행").setMessage("이 기능의 모델은 그대로 보존했습니다. 자료를 바꾸거나 같은 모델을 다시 실행하세요.").setPositiveButton("확인",null).show();return;}
  new AlertDialog.Builder(a).setTitle("다음 실행 모델 선택").setItems(labels.toArray(new String[0]),(d,n)->{OneModels.select(a,feature,ids.get(n),group);workspace(a,feature,group);}).setNegativeButton("닫기",null).show();
 }
 public static void camera(Activity a,Runnable proceed){
  try{android.hardware.camera2.CameraManager cameras=(android.hardware.camera2.CameraManager)a.getSystemService(Context.CAMERA_SERVICE);if(cameras!=null&&cameras.getCameraIdList().length>0){proceed.run();return;}}catch(android.hardware.camera2.CameraAccessException|RuntimeException error){proceed.run();return;}
  if(a.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){proceed.run();return;}
  new AlertDialog.Builder(a).setTitle(Build.MODEL+" 카메라 안내").setMessage("현재 Android에서 카메라 장치를 찾지 못했습니다. 카메라를 연결해 다시 확인하거나 사진 파일을 선택하세요.").setPositiveButton("다시 확인",(d,w)->camera(a,proceed)).setNegativeButton("닫기",null).show();
 }
 public static void microphone(Activity a,Runnable proceed){
  try{android.media.AudioManager audio=(android.media.AudioManager)a.getSystemService(Context.AUDIO_SERVICE);if(audio!=null&&audio.getDevices(android.media.AudioManager.GET_DEVICES_INPUTS).length>0){proceed.run();return;}}catch(RuntimeException error){proceed.run();return;}
  if(a.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)){proceed.run();return;}
  new AlertDialog.Builder(a).setTitle(Build.MODEL+" 마이크 안내").setMessage("현재 Android에서 마이크 입력 장치를 찾지 못했습니다. 마이크를 연결해 다시 확인하거나 음성 파일을 선택하세요.").setPositiveButton("다시 확인",(d,w)->microphone(a,proceed)).setNegativeButton("닫기",null).show();
 }
 public static void show(Activity a){
  StringBuilder text=new StringBuilder(Build.MANUFACTURER+" "+Build.MODEL+" · Android "+Build.VERSION.RELEASE+"\n");int failures=0;
  try{for(Map.Entry<String,?> entry:a.getSharedPreferences(PREFS,0).getAll().entrySet()){if(!entry.getKey().startsWith("entry.")||!(entry.getValue() instanceof String))continue;JSONObject r=new JSONObject((String)entry.getValue());if(!current(a,r)||!OneDeviceSupportPolicy.noticeRequired(r.optString("state")))continue;failures++;text.append("\n").append(featureTitle(r.optString("feature"))).append(" · ").append(modelTitle(a,r.optString("feature"),r.optString("modelId"))).append("\n").append(r.optString("errorCode")).append(" · ").append(r.optString("origin")).append("\n");}}catch(Exception ignored){}
  if(failures==0)text.append("\n기록된 실행 오류가 없습니다.");text.append("\n\n모델과 기능 메뉴는 모두 보존합니다. 성공한 재실행은 해당 모델의 오류 안내를 해제합니다.");new AlertDialog.Builder(a).setTitle("기기별 실행 안내").setMessage(text.toString()).setPositiveButton("확인",null).show();
 }
}
