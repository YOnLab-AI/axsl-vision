package com.yonlab.offlinevoice;
import android.content.*;
import android.net.Uri;

/** Optional unified UI bridge; there is no offlinevoice -> vision compile dependency. */
final class VoiceNavigation {
 private VoiceNavigation(){}
 static Intent picker(Context c,String group){try{Intent i=(Intent)Class.forName("com.yonlab.vision.OneInputs").getMethod("picker",Context.class,String.class,String.class).invoke(null,c,"audio","speech");return i.putExtra("group",group);}catch(ReflectiveOperationException|LinkageError e){return new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);}}
 static Uri input(Intent i){if(i==null)return null;Uri uri=i.getData();if(uri==null){String s=i.getStringExtra("input_uri");if(s!=null&&!s.isEmpty())uri=Uri.parse(s);}return uri!=null&&"content".equals(uri.getScheme())?uri:null;}
 static String group(Intent i){return i!=null&&"GO".equals(i.getStringExtra("group"))?"GO":"KO";}
 static Reference reference(Context c,String demo,String actualEncodedSha){
  if(demo==null||demo.isEmpty()||actualEncodedSha==null)return new Reference("",false);
  try{Object item=Class.forName("com.yonlab.vision.DemoStore").getMethod("find",Context.class,String.class).invoke(null,c,demo);if(item==null)return new Reference("",false);Class<?> type=item.getClass();String sha=(String)type.getField("sha256").get(item),text=(String)type.getField("reference").get(item);boolean verified=type.getField("referenceVerified").getBoolean(item);return new Reference(text==null?"":text,verified&&actualEncodedSha.equals(sha));}catch(ReflectiveOperationException|LinkageError e){return new Reference("",false);}
 }
 static final class Reference {final String text;final boolean verified;Reference(String t,boolean v){text=t;verified=v;}}
}
