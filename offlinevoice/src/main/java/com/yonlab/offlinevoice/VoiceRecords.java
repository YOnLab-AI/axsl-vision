package com.yonlab.offlinevoice;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.json.JSONObject;

/** The journal contains counts/state only, never audio, transcript, selected URI or credentials. */
final class VoiceRecords {
 private static final Object GATE=new Object();private static String newest="";private static long newestSequence;
 private static final ThreadPoolExecutor WRITER=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(4),r->{Thread t=new Thread(r,"YOnVoiceJournal");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
 static void begin(String id){synchronized(GATE){newest=id;newestSequence=0;}}
 static void later(Context context,String id,JSONObject value){try{WRITER.execute(()->write(context,id,value));}catch(RejectedExecutionException e){android.util.Log.w("YOn","offline_voice_record_busy");}}
 static void write(Context context,String id,JSONObject value){
  long sequence=value.optLong("recordSequence",0);synchronized(GATE){if(!id.equals(newest)||sequence<newestSequence)return;newestSequence=sequence;}File pending=new File(context.getFilesDir(),"offlinevoice-result."+id+"."+sequence+".new"),target=new File(context.getFilesDir(),"offlinevoice-result.json");
  try{byte[] bytes=value.toString(2).getBytes(StandardCharsets.UTF_8);if(bytes.length>8192)throw new IOException("record size");try(FileOutputStream out=new FileOutputStream(pending)){out.write(bytes);out.getFD().sync();}synchronized(GATE){if(!id.equals(newest)||sequence!=newestSequence)return;if(!pending.renameTo(target))throw new IOException("record commit");}}catch(Exception e){android.util.Log.w("YOn","offline_voice_record_failed",e);}finally{if(pending.exists()&&!pending.delete())android.util.Log.w("YOn","offline_voice_record_cleanup_failed");}
 }
}
