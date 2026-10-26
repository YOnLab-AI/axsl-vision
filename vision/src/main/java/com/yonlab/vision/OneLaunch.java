package com.yonlab.vision;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
/** All features route inside this single application, never to another APK. */
public final class OneLaunch {
 private OneLaunch(){}
 public static void open(Activity a,String feature,String modelId,Uri input,String mediaType,String demoId){open(a,feature,modelId,input,mediaType,demoId,"ALL");}
 public static void open(Activity a,String feature,String modelId,Uri input,String mediaType,String demoId,String group){
  open(a,feature,modelId,input,mediaType,demoId,group,false);
 }
 /** Explicit model-first entry; sample language cannot substitute another model. */
 public static void openTest(Activity a,String feature,String modelId,Uri input,String mediaType,String demoId,String group){
  if(modelId==null||modelId.isEmpty()){Ui.error(a,"시험할 모델을 먼저 선택하세요",null);return;}
  try{OneModels.resolve(a,feature,modelId);}catch(IllegalArgumentException e){Ui.error(a,"시험할 모델을 다시 선택하세요",e);return;}
  open(a,feature,modelId,input,mediaType,demoId,group,true);
 }
 private static void open(Activity a,String feature,String modelId,Uri input,String mediaType,String demoId,String group,boolean keepModel){
  if(!DemoRules.feature(feature)){Ui.error(a,"기능을 다시 선택하세요",null);return;}
  boolean preserveModel=OneModelTestPolicy.keepModel(modelId,keepModel);
  String type=mediaType==null?"":mediaType;String mode="faces".equals(feature)?"face":feature;String target;
  if("sound".equals(feature))target="com.yonlab.vision.SoundBenchActivity";
  else if("speech".equals(feature))target=keepModel?"com.yonlab.offlinevoice.VoiceCompareActivity":"com.yonlab.offlinevoice.OfflineVoiceActivity";
  else if(keepModel&&type.startsWith("image")&&!"signage".equals(feature))target="com.yonlab.vision.ModelBenchActivity";
  else if("compare".equals(feature))target="com.yonlab.vision.CompareActivity";
  else if("signage".equals(feature))target="com.yonlab.vision.SignageActivity";
  else if(type.startsWith("video"))target="com.yonlab.vision.VideoAnalysisActivity";
  else if(java.util.Arrays.asList("classify","segment","faces","detect","pose","ocr","qr","document","studio","search","coach").contains(feature))target="com.yonlab.vision.ModelBenchActivity";
  else if(input!=null)target="com.yonlab.vision.PhotoActivity";
  else target="com.yonlab.vision.LiveActivity";
  Intent i=new Intent().setClassName(a,target).putExtra("feature",feature).putExtra("mode",mode).putExtra("compare_all",false).putExtra("auto_start",input!=null).putExtra("one_input",true).putExtra("keep_model",preserveModel).putExtra("group",OneModelTestPolicy.launchGroup(feature,null,group,preserveModel));
  if(modelId!=null&&!modelId.isEmpty())i.putExtra("model_id",modelId);if(demoId!=null&&!demoId.isEmpty()){
   i.putExtra("demo_id",demoId);try{DemoStore.Item item=DemoStore.find(a,demoId);if(item!=null){String inputGroup=OneModelTestPolicy.launchGroup(feature,item.group,group,preserveModel);i.putExtra("group",inputGroup);if(!preserveModel&&("ocr".equals(feature)||"document".equals(feature))&&!inputGroup.equals(OneDeviceSupportPolicy.group(group)))i.putExtra("model_id",OneModels.selected(a,feature,inputGroup));i.putExtra("pair_demo_id",item.pairId);i.putExtra("reference_text",item.reference);i.putExtra("reference_verified",item.referenceVerified);if(!preserveModel&&"speech".equals(feature)){i.removeExtra("model_id");try{Class<?> voice=Class.forName("com.yonlab.offlinevoice.VoiceModels");String chosen=(String)voice.getMethod("selected",android.content.Context.class,String.class).invoke(null,a,inputGroup);if(chosen!=null)i.putExtra("model_id",chosen);}catch(ReflectiveOperationException ignored){}}}}catch(Exception ignored){}
  }
  if(input!=null){if(!"content".equals(input.getScheme())){Ui.error(a,"파일을 다시 선택하세요",null);return;}String mime=type.contains("/")?type:type.startsWith("video")?"video/*":type.startsWith("audio")?"audio/*":"image/*";i.setDataAndType(input,mime).putExtra("input_uri",input.toString()).putExtra(Intent.EXTRA_STREAM,input).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(ClipData.newRawUri("AXSL Vision input",input));if("signage".equals(feature))i.setAction(Intent.ACTION_SEND);}
  String actualModel=i.getStringExtra("model_id");String actualGroup=OneDeviceSupportPolicy.group(i.getStringExtra("group"));OneDeviceSupport.guard(a,feature,actualModel,actualGroup,()->{try{a.startActivity(i);}catch(ActivityNotFoundException|SecurityException e){OneDeviceSupport.observe(a,feature,actualModel,"ERROR",e.getClass().getSimpleName(),"activity-entry");Ui.error(a,"화면을 열 수 없습니다",e);}});
 }
 public static void camera(Activity a,String feature,String modelId,int request){camera(a,feature,modelId,request,"ALL");}
 public static void camera(Activity a,String feature,String modelId,int request,String group){if(!DemoRules.feature(feature))return;Intent i=new Intent(a,CaptureActivity.class).putExtra("mode","faces".equals(feature)?"face":feature).putExtra("feature",feature).putExtra("model_id",modelId);OneDeviceSupport.camera(a,()->OneDeviceSupport.guard(a,feature,modelId,group,()->a.startActivityForResult(i,request)));}
}

