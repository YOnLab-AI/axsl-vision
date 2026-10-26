package com.yonlab.vision;
import android.app.Activity;import android.os.Build;import android.view.View;import androidx.core.graphics.Insets;import androidx.core.view.ViewCompat;import androidx.core.view.WindowCompat;import androidx.core.view.WindowInsetsCompat;
/** Target35+ edge-to-edge insets, including cutouts and the keyboard, for programmatic native screens. */
public abstract class YOnActivity extends Activity {
 @Override public void setContentView(View view){
  if(Build.VERSION.SDK_INT>=35){
   WindowCompat.setDecorFitsSystemWindows(getWindow(),false);final int left=view.getPaddingLeft(),top=view.getPaddingTop(),right=view.getPaddingRight(),bottom=view.getPaddingBottom();
   ViewCompat.setOnApplyWindowInsetsListener(view,(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());v.setPadding(left+bars.left,top+bars.top,right+bars.right,bottom+Math.max(bars.bottom,ime.bottom));return WindowInsetsCompat.CONSUMED;});
  }
  super.setContentView(view);if(Build.VERSION.SDK_INT>=35)ViewCompat.requestApplyInsets(view);
 }
}
