package com.yonlab.vision;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;

/** Bounded metadata projection; never reads transcript/reference or arbitrary report files. */
final class OneReportRows {
 static final String[] HEADERS={"시각","기능","모델","상태","준비 ms","추론 ms","전체 ms","입력 SHA256","내용"};
 static final class Row {long at;Double verifiedCer;String feature,model,state,summary,input,source,load,inference,elapsed,report;String[] cells(){return new String[]{time(at),feature,model,state,load,inference,elapsed,input,summary};}}
 private OneReportRows(){}
 static List<Row> read(Context c){List<Row> rows=new ArrayList<>();JSONArray one=OneRunLog.read(c);for(int i=0;i<one.length();i++){JSONObject j=one.optJSONObject(i);if(j!=null)rows.add(from(j,"분석"));}
  JSONArray events=Records.events(c);for(int i=0;i<events.length();i++){JSONObject j=events.optJSONObject(i);if(j==null)continue;JSONObject d=j.optJSONObject("details");if(d==null)d=new JSONObject();Row row=from(d,"이벤트");row.at=j.optLong("at",0);row.feature=eventFeature(d.optString("feature"),j.optString("type"));row.state=state(j.optString("state"));row.elapsed=number(d,"analysisMs");rows.add(row);}
  File voice=new File(c.getFilesDir(),"one-asr-runs.jsonl");if(voice.isFile()&&voice.length()<=4*1024*1024)try(BufferedReader input=new BufferedReader(new InputStreamReader(new FileInputStream(voice),StandardCharsets.UTF_8))){ArrayDeque<JSONObject> recent=new ArrayDeque<>();String line;while((line=input.readLine())!=null){if(line.length()>65536)continue;try{recent.addLast(new JSONObject(line));while(recent.size()>500)recent.removeFirst();}catch(JSONException ignored){}}for(JSONObject j:recent)rows.add(from(j,"음성"));}catch(IOException ignored){}
  rows.sort((a,b)->Long.compare(b.at,a.at));return Collections.unmodifiableList(new ArrayList<>(rows.subList(0,Math.min(rows.size(),1000))));
 }
 static Row from(JSONObject j,String source){Row r=new Row();r.source=source;r.at=at(j.opt("createdAt"));if(r.at==0)r.at=j.optLong("at",0);r.feature=feature(j.optString("feature",j.optString("type")));r.model=text(j,"modelId",120);if(r.model.isEmpty())r.model=text(j,"model_id",120);r.state=state(j.optString("state"));r.load=number(j,"loadMs");r.inference=number(j,"inferenceMs");r.elapsed=number(j,"elapsedMs");String sha=j.optString("inputSha256");r.input=sha.matches("[a-fA-F0-9]{64}")?sha:"";r.report=text(j,"reportPath",512);r.summary=text(j,"summary",600);if(r.summary.isEmpty())r.summary=metrics(j);if(j.optBoolean("referenceVerified")&&j.optInt("referenceCharacters",0)>0&&j.has("cer")&&!number(j,"cer").isEmpty()){r.verifiedCer=((Number)j.opt("cer")).doubleValue();r.summary+=(r.summary.isEmpty()?"":" · ")+"CER "+number(j,"cer");}return r;}
 static String metrics(JSONObject j){StringBuilder b=new StringBuilder();String[][] keys={{"faces","얼굴"},{"objects","객체"},{"analysisFrames","분석 프레임"},{"distinctIdentities","고유 관측"},{"visitSessions","방문 세션"},{"flowIn","유입"},{"flowOut","유출"},{"resultCharacters","출력 문자"},{"sampleCount","샘플"},{"similarity","유사도"},{"score","점수"}};for(String[] k:keys){String value=number(j,k[0]);if(!value.isEmpty()){if(b.length()>0)b.append(" · ");b.append(k[1]).append(' ').append(value);}}return b.toString();}
 static String number(JSONObject j,String key){return numberValue(j.opt(key),key);}
 static String numberValue(Object v,String key){if(!(v instanceof Number))return "";double n=((Number)v).doubleValue();if(!Double.isFinite(n)||(n<0&&!"similarity".equals(key)))return "";return n==Math.rint(n)?Long.toString((long)n):String.format(Locale.KOREA,"%.2f",n);}
 private static String text(JSONObject j,String key,int max){Object v=j.opt(key);if(!(v instanceof String))return "";String s=(String)v;return s.length()>max?s.substring(0,max)+"…":s;}
 private static long at(Object value){if(value instanceof Number)return ((Number)value).longValue();if(!(value instanceof String))return 0;String s=((String)value).replaceFirst("(\\.\\d{3})\\d+(?=Z|[+-])","$1");try{return Long.parseLong(s);}catch(NumberFormatException ignored){}for(String pattern:new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSSXXX","yyyy-MM-dd'T'HH:mm:ssXXX"})try{SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.ROOT);f.setLenient(false);return f.parse(s).getTime();}catch(ParseException ignored){}return 0;}
 static String time(long value){return value<=0?"":DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.MEDIUM,Locale.KOREA).format(new Date(value));}
 static String eventFeature(String declared,String type){String canonical=OneModelPolicy.canonicalFeature(declared);return OneCatalog.find(canonical)!=null?feature(canonical):feature(type);}
 static String feature(String id){String f=id==null?"":id;for(String prefix:new String[]{"video-","photo-","live-","tool-"})if(f.startsWith(prefix))f=f.substring(prefix.length());if(f.equals("face"))f="faces";if(f.contains("asr")||f.contains("speech"))f="speech";OneCatalog.Feature feature=OneCatalog.find(f);return feature==null?"분석 기록":feature.title;}
 static String state(String raw){String s=raw.toUpperCase(Locale.ROOT);if(s.contains("ERROR")||s.contains("FAIL"))return "오류";if(s.contains("CANCEL"))return "취소";if(s.contains("STOP")||s.equals("CLOSED"))return "중지";if(s.contains("COMPLETE")||s.contains("GENERATED")||s.contains("SAVE")||s.contains("ENROLL"))return "완료";if(s.contains("RUN")||s.contains("LISTEN")||s.contains("START"))return "진행";return raw.isEmpty()?"기록":raw;}
}
