package com.yonlab.vision;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Bounded run metadata; explicit report files hold chosen output, never ambient microphone text. */
public final class OneRunLog {
 private OneRunLog(){}
 public static synchronized void append(Context c,JSONObject input)throws Exception{
  JSONObject safe=new JSONObject();String[] keys={"type","feature","modelId","model_id","inputSha256","state","createdAt","loadMs","inferenceMs","elapsedMs","summary","reportPath","modelSha256","demoId","group","cer","wer","referenceVerified","source","count","index","playing","intervalMs"};
  for(String k:keys)if(input.has(k)&&!input.isNull(k))safe.put(k,input.get(k));if(!safe.has("createdAt"))safe.put("createdAt",System.currentTimeMillis());
  String line=safe.toString();if(line.length()>32768)throw new IOException("기록 크기를 확인하세요");File f=new File(c.getFilesDir(),"one-runs.jsonl");
  if(f.exists()&&f.length()>1024*1024){JSONArray old=read(c);try(Writer out=new OutputStreamWriter(new FileOutputStream(f),StandardCharsets.UTF_8)){for(int n=Math.max(0,old.length()-200);n<old.length();n++)out.write(old.getJSONObject(n).toString()+"\n");}}
  try(Writer out=new OutputStreamWriter(new FileOutputStream(f,true),StandardCharsets.UTF_8)){out.write(line+"\n");}
 }
 public static synchronized JSONArray read(Context c){JSONArray out=new JSONArray();File f=new File(c.getFilesDir(),"one-runs.jsonl");if(!f.isFile()||f.length()>2*1024*1024)return out;
  try(BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),StandardCharsets.UTF_8))){for(String line:RecentRunLines.tail(r,1000))try{out.put(new JSONObject(line));}catch(JSONException ignored){}}catch(IOException ignored){}return out;
 }
}
