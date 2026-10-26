package com.yonlab.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import java.io.*;
import java.util.*;

/** New task workflows share the existing same-input comparison/result contract. */
public final class ExpansionImageRunner {
 private ExpansionImageRunner(){}
 public static boolean supports(String f){return Arrays.asList("document","studio","search","coach").contains(f);}
 public static OneModelRunner.Result run(Context c,String feature,String id,Bitmap input,OneModelRunner.Cancel cancel){
  OneModelRunner.Result r=new OneModelRunner.Result(feature,id);long start=SystemClock.elapsedRealtime();
  try{check(cancel);if(input==null||input.isRecycled()||(long)input.getWidth()*input.getHeight()>16000000)throw new IOException("사진을 확인하세요");
   if("document".equals(feature))DocumentWorkflow.run(c,id,input,cancel,r);
   else if("studio".equals(feature))BackgroundStudio.run(c,id,input,cancel,r);
   else if("search".equals(feature))ImageRetrieval.run(c,id,input,cancel,r);
   else if("coach".equals(feature))FitnessCoach.run(c,id,input,cancel,r);
   else throw new IllegalArgumentException("사진 기능을 확인하세요");
   check(cancel);r.state="COMPLETED";
  }catch(Exception|LinkageError|OutOfMemoryError e){r.state=cancel.canceled()||Thread.currentThread().isInterrupted()?"CANCELED":"ERROR";r.errorType=e.getClass().getSimpleName();r.summary="CANCELED".equals(r.state)?"분석 취소":"다른 모델로 다시 분석하세요";android.util.Log.w("YOn","expansion_"+feature,e);}
  finally{r.elapsedMs=SystemClock.elapsedRealtime()-start;if(r.image==null&&input!=null&&!input.isRecycled())r.image=input.copy(Bitmap.Config.ARGB_8888,false);}
  return r;
 }
 static void check(OneModelRunner.Cancel cancel)throws InterruptedIOException{if(cancel.canceled()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("canceled");}
 static String assetSha(Context c,String path)throws Exception{try(InputStream in=c.getAssets().open(path)){return OneOnnx.hash(in);}}
}
