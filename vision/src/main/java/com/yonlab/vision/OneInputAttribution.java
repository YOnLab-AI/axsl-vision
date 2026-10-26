package com.yonlab.vision;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Packaged provenance only. Known source SHA can recover attribution after an immutable cross-feature handoff. */
final class OneInputAttribution {
 private static JSONObject credits;private static OneAttributionIndex index;
 private OneInputAttribution(){}
 static final class Entry {
  final JSONObject data;
  Entry(JSONObject value){data=value;}
  boolean empty(){for(String key:new String[]{"author","sourceUrl","licenseName","licenseUrl","changes","attribution"})if(!data.optString(key).trim().isEmpty())return false;return true;}
  String field(String name){return data.optString(name);}
  JSONObject json()throws JSONException{return new JSONObject(data.toString());}
  String text(){if(empty())return "";StringBuilder b=new StringBuilder(field("attribution"));String[][] fields={{"author","Author"},{"sourceUrl","Source"},{"licenseName","Source media license"},{"licenseUrl","Source license URL"},{"derivedLicenseName","Derived media license"},{"derivedLicenseUrl","Derived license URL"},{"changes","Source/demo changes"},{"exportChangesNotice","Export notice"}};for(String[] f:fields){String value=field(f[0]);if(!value.isEmpty()){if(b.length()>0)b.append('\n');b.append(f[1]).append(": ").append(value);}}return b.toString();}
  String html(){if(empty())return "";StringBuilder b=new StringBuilder("<details class='attribution'><summary>자료 출처·이용 조건</summary><dl>");String[][] fields={{"author","저작자"},{"sourceUrl","원본 출처"},{"licenseName","이용 조건"},{"licenseUrl","라이선스 원문"},{"changes","원본·데모 변경"}};for(String[] f:fields){String value=field(f[0]);if(!value.isEmpty())b.append("<dt>").append(OneExportCore.escape(f[1])).append("</dt><dd>").append(OneExportCore.escape(value)).append("</dd>");}b.append("</dl>");if(!field("attribution").isEmpty())b.append("<pre>").append(OneExportCore.escape(field("attribution"))).append("</pre>");return b.append("</details>").toString();}
  Map<String,String> png(String outputChanges){Map<String,String> values=new LinkedHashMap<>();if(empty())return values;String license=field("derivedLicenseName").isEmpty()?field("licenseName"):field("derivedLicenseName"),url=field("derivedLicenseUrl").isEmpty()?field("licenseUrl"):field("derivedLicenseUrl");put(values,"Author",field("author"));put(values,"Source",field("sourceUrl"));put(values,"License",license+(url.isEmpty()?"":" · "+url));put(values,"Changes",field("changes")+(outputChanges.isEmpty()?"":"; "+outputChanges));put(values,"Attribution",text());return values;}
  private static void put(Map<String,String> values,String key,String value){if(value!=null&&!value.trim().isEmpty())values.put(key,value);}
 }
 static Entry find(Context context,String demo){return find(context,demo,"");}
 static synchronized Entry find(Context context,String demo,String sourceSha){
  try{if(credits==null)credits=new JSONObject(read(context,"one/data-attribution.json",3*1024*1024));if(index==null){JSONArray catalog=new JSONObject(read(context,"one/demos.json",2*1024*1024)).getJSONArray("items");Map<String,String> hashes=new LinkedHashMap<>();for(int n=0;n<catalog.length();n++){JSONObject item=catalog.getJSONObject(n);hashes.put(item.optString("id"),item.optString("sha256"));}index=new OneAttributionIndex(hashes);}String resolved=index.resolve(demo,sourceSha);boolean actual=sourceSha!=null&&sourceSha.matches("[a-fA-F0-9]{64}");
   if(resolved.isEmpty())return new Entry(new JSONObject());JSONObject original=credits.getJSONObject("items").optJSONObject(resolved);if(original==null||actual&&original.optString("sha256").matches("[a-fA-F0-9]{64}")&&!sourceSha.equalsIgnoreCase(original.optString("sha256")))return new Entry(new JSONObject());JSONObject result=sanitize(original);result.put("verified",original.optBoolean("verified")).put("resolvedDemoId",resolved).put("sourceMediaLicense",original.optString("licenseName")).put("derivedMediaLicense",original.optString("derivedLicenseName")).put("attributionScope","Source media and any included derived media; standalone numeric observations retain their own scope");if(actual)result.put("inputSha256",sourceSha).put("catalogHashMatched",true);return new Entry(result);
  }catch(Exception unavailable){return new Entry(new JSONObject());}
 }
 private static JSONObject sanitize(JSONObject original)throws JSONException{JSONObject out=new JSONObject();for(String key:new String[]{"author","sourceUrl","licenseName","licenseUrl","changes","attribution","derivedLicenseName","derivedLicenseUrl","exportChangesNotice","rightsBasis","rightsStatement","creatorCodeSha256","licenseScope","sha256","type"}){Object value=original.opt(key);if(value instanceof String)out.put(key,value);}if(original.has("shareAlikeRequired"))out.put("shareAlikeRequired",original.optBoolean("shareAlikeRequired"));JSONArray components=original.optJSONArray("components");if(components!=null){JSONArray selected=new JSONArray();for(int n=0;n<Math.min(components.length(),64);n++){JSONObject component=components.optJSONObject(n);if(component==null)continue;JSONObject row=new JSONObject();for(String key:new String[]{"author","sourceUrl","licenseName","licenseUrl","changes","attribution","sha256","demoId"}){Object value=component.opt(key);if(value instanceof String)row.put(key,value);}if(component.has("verified"))row.put("verified",component.optBoolean("verified"));selected.put(row);}out.put("components",selected);}return out;}
 private static String read(Context context,String path,int limit)throws IOException{try(InputStream in=context.getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("Attribution size");out.write(buffer,0,n);}String value=out.toString(StandardCharsets.UTF_8.name());return value.startsWith("\ufeff")?value.substring(1):value;}}
}
