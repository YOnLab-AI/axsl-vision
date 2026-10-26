package com.yonlab.offlinevoice;
import android.app.Activity;
import android.os.Build;
import android.view.View;

/** Native edge-to-edge ownership without a dependency on the vision module. */
abstract class VoiceActivity extends Activity {
 @Override public void setContentView(View root){if(Build.VERSION.SDK_INT>=35)CurrentInsets.apply(this,root);super.setContentView(root);if(Build.VERSION.SDK_INT>=35)root.requestApplyInsets();}
 @android.annotation.TargetApi(35) private static final class CurrentInsets {
  static void apply(Activity activity,View root){activity.getWindow().setDecorFitsSystemWindows(false);final int left=root.getPaddingLeft(),top=root.getPaddingTop(),right=root.getPaddingRight(),bottom=root.getPaddingBottom();root.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());android.graphics.Insets ime=insets.getInsets(android.view.WindowInsets.Type.ime());view.setPadding(left+bars.left,top+bars.top,right+bars.right,bottom+Math.max(bars.bottom,ime.bottom));return android.view.WindowInsets.CONSUMED;});}
 }
}
