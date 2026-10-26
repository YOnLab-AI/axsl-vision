package com.yonlab.vision;
import android.content.Context;
import android.content.Intent;
/** Explicit internal picker forOne; normal SAF stays under the import action. */
public final class OneInputs {
 private OneInputs(){}
 public static Intent picker(Context c,String type,String feature){
  Intent i=new Intent().setClassName(c,"com.yonlab.vision.DemoLibraryActivity").putExtra("media_type",type==null?"any":type);
  if(feature!=null&&!feature.isEmpty())i.putExtra("feature",feature);return i;
 }
}
