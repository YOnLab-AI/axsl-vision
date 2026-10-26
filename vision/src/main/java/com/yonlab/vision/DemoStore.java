package com.yonlab.vision;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Verified bundled inputs with optional verified app-external mirror. No global storage permission. */
public final class DemoStore {
 private DemoStore(){}
 public static final class Item {
  public final String id,title,group,type,summary,asset,sha256,reference,pairId,thumb,difficulty;public final String[] conditions;public final double priority;
  public final boolean referenceVerified;public final String[] features;public final long bytes;
  Item(JSONObject o)throws Exception{
   id=o.getString("id");title=o.getString("title");group=o.getString("group");type=o.getString("type");summary=o.optString("summary");asset=o.getString("asset");sha256=o.getString("sha256");bytes=o.getLong("bytes");reference=o.optString("reference");referenceVerified=o.optBoolean("referenceVerified");pairId=o.optString("pairId");thumb=o.optString("thumb");difficulty=o.optString("difficulty");JSONArray tags=o.optJSONArray("conditions");conditions=new String[tags==null?0:Math.min(12,tags.length())];for(int n=0;n<conditions.length;n++){conditions[n]=tags.optString(n);if(conditions[n].length()>80)throw new IOException("자료 조건을 확인하세요");}
   double rank=o.optDouble("priority",Double.MAX_VALUE);priority=Double.isFinite(rank)?rank:Double.MAX_VALUE;JSONArray fs=o.getJSONArray("features");features=new String[fs.length()];for(int i=0;i<features.length;i++){features[i]=fs.getString(i);if(!DemoRules.feature(features[i]))throw new IOException("자료 기능을 확인하세요");}
   if(!id.matches("[a-z0-9-]{1,80}")||title.length()>120||summary.length()>500||reference.length()>16000||!DemoRules.safeAsset(asset)||!sha256.matches("[0-9a-f]{64}")||bytes<1||bytes>64L*1024*1024||!DemoRules.groupMatches(group,"ALL")||!DemoRules.typeMatches(type,"any")||features.length==0)throw new IOException("데모 자료 목록을 확인하세요");
   if(!thumb.isEmpty()&&!thumb.matches("one/thumbs/[a-z0-9-]+\\.jpg"))throw new IOException("미리보기를 확인하세요");
  }
  public boolean supports(String feature){return feature==null||feature.isEmpty()||java.util.Arrays.asList(features).contains(feature);}
 }
 private static List<Item> cached;private static final Map<String,String> truths=new HashMap<>();
 private static synchronized List<Item> catalog(Context c)throws Exception{
  if(cached!=null)return cached;ByteArrayOutputStream out=new ByteArrayOutputStream();
  try(InputStream in=c.getAssets().open("one/demos.json")){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>1024*1024)throw new IOException("자료 목록 크기를 확인하세요");out.write(b,0,n);}}
  JSONArray array=new JSONObject(out.toString("UTF-8")).getJSONArray("items");if(array.length()>512)throw new IOException("자료 목록을 확인하세요");List<Item> result=new ArrayList<>();Set<String> ids=new HashSet<>();
  for(int i=0;i<array.length();i++){JSONObject raw=array.getJSONObject(i);Item item=new Item(raw);if(!ids.add(item.id))throw new IOException("중복된 자료입니다");if(raw.optJSONObject("detectionExpected")!=null)truths.put(item.id,raw.getJSONObject("detectionExpected").toString());result.add(item);}cached=Collections.unmodifiableList(result);return cached;
 }
 public static List<Item> items(Context c,String feature,String group,String type)throws Exception{
  if(feature!=null&&!feature.isEmpty()&&!DemoRules.feature(feature))throw new IOException("기능을 다시 선택하세요");List<Item> out=new ArrayList<>();
  for(Item i:catalog(c))if(i.supports(feature)&&DemoRules.groupMatches(i.group,group)&&DemoRules.typeMatches(i.type,type))out.add(i);if("sound".equals(feature))out.sort((a,b)->Double.compare(a.priority,b.priority));return out;
 }
 public static Item find(Context c,String id)throws Exception{for(Item i:catalog(c))if(i.id.equals(id))return i;return null;}
 public static String reference(Context c,String id){try{Item i=find(c,id);return i==null?"":i.reference;}catch(Exception e){return "";}}
 public static boolean referenceVerified(Context c,String id){try{Item i=find(c,id);return i!=null&&i.referenceVerified;}catch(Exception e){return false;}}
 public static String detectionTruth(Context c,String id){try{catalog(c);String s=truths.get(id);return s==null?"":s;}catch(Exception e){return "";}}
 private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(java.util.Locale.US,"%02x",v&255));return s.toString();}
 private static boolean matches(File f,Item i)throws Exception{
  if(!f.isFile()||f.length()!=i.bytes)return false;MessageDigest h=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)h.update(b,0,n);}return i.sha256.equals(hex(h.digest()));
 }
 public static synchronized Uri uri(Context c,Item i)throws Exception{
  if(i==null||!DemoRules.safeAsset(i.asset))throw new IOException("자료를 다시 선택하세요");File root=new File(c.getFilesDir(),"demos/v1");if(!root.isDirectory()&&!root.mkdirs())throw new IOException("자료 저장 공간을 확인하세요");
  String ext=i.asset.substring(i.asset.lastIndexOf('.'));File target=new File(root,i.id+"-"+i.sha256.substring(0,12)+ext);if(!target.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))throw new IOException("자료 경로를 확인하세요");
  if(!matches(target,i)){
   File tmp=new File(root,"pending-"+UUID.randomUUID()+".part");File mirrorRoot=c.getExternalFilesDir("demo-bank");File mirror=mirrorRoot==null?null:new File(mirrorRoot,i.asset.substring("one/media/".length()));boolean useMirror=mirror!=null&&mirror.getCanonicalPath().startsWith(mirrorRoot.getCanonicalPath()+File.separator)&&matches(mirror,i);
   try{
    MessageDigest hash=MessageDigest.getInstance("SHA-256");long total=0;
    try(InputStream in=useMirror?new FileInputStream(mirror):c.getAssets().open(i.asset);FileOutputStream out=new FileOutputStream(tmp)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("자료 준비를 중지했습니다");total+=n;if(total>i.bytes)throw new IOException("자료 크기를 확인하세요");hash.update(b,0,n);out.write(b,0,n);}out.getFD().sync();}
    if(total!=i.bytes||!i.sha256.equals(hex(hash.digest())))throw new IOException("자료 확인에 실패했습니다");
    if(target.exists()&&!target.delete())throw new IOException("자료를 갱신할 수 없습니다");if(!tmp.renameTo(target))throw new IOException("자료를 준비할 수 없습니다");
   }finally{if(tmp.exists())tmp.delete();}
  }
  return FileProvider.getUriForFile(c,c.getPackageName()+".files",target);
 }
 public static Uri pairUri(Context c,String demoId)throws Exception{Item i=find(c,demoId);return i==null||i.pairId.isEmpty()?null:uri(c,find(c,i.pairId));}
}
