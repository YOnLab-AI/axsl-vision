package com.yonlab.offlinevoice;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/** Metadata only. Transcript and reference text are exported solely by explicit report action. */
final class AsrRuns {
 private AsrRuns(){}
 static String now(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.ROOT);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
 static JSONObject metric(String id,String sha,String state,long load,long inference,long total,long samples,String demo)throws JSONException{
  VoiceModels.Entry e=VoiceModels.find(id);if(e==null)throw new IllegalArgumentException("Unknown model");
  return new JSONObject().put("type","asr").put("feature","speech").put("modelId",id).put("group",e.group).put("language",e.language).put("engine",e.engine).put("inputSha256",sha==null?"":sha).put("state",state).put("createdAt",now()).put("loadMs",load).put("inferenceMs",inference).put("elapsedMs",total).put("sampleCount",samples).put("durationMs",samples*1000/16000).put("rtf",samples>0?inference/(samples/16.0):JSONObject.NULL).put("demoId",demo==null?"":demo).put("summary",e.title+" · "+state).put("modelManifestSha256",VoiceAssetStore.manifestSha(id)).put("modelWeightsFingerprint",VoiceAssetStore.weightsFingerprint(id)).put("modelArchiveSha256",VoiceAssetStore.archiveSha(id)).put("transcriptJournaled",false);
 }
 static synchronized void append(Context c,JSONObject row)throws IOException{
  if(row.has("transcript")||row.has("reference")||row.has("recognizedText")||row.has("editedText"))throw new IOException("Transcript is explicit-report only");
  byte[] bytes=(row.toString()+"\n").getBytes(StandardCharsets.UTF_8);if(bytes.length>16384)throw new IOException("ASR metrics size");File f=new File(c.getFilesDir(),"one-asr-runs.jsonl");
  if(f.length()>1024*1024){File old=new File(c.getFilesDir(),"one-asr-runs.previous.jsonl");if(old.exists()&&!old.delete())throw new IOException("Metrics rotation");if(!f.renameTo(old))throw new IOException("Metrics rotation");}
  try(FileOutputStream out=new FileOutputStream(f,true)){out.write(bytes);out.getFD().sync();}
  if(!row.has("reportPath"))try{Class.forName("com.yonlab.vision.OneDeviceSupport").getMethod("observe",Context.class,String.class,String.class,String.class,String.class,String.class).invoke(null,c,"speech",row.optString("modelId"),row.optString("state"),row.optString("failureCode"),"voice-run");}catch(ReflectiveOperationException|RuntimeException|LinkageError|OutOfMemoryError ignored){}
 }
}
