package com.yonlab.offlinevoice;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** App-private immutable verified model cache. Native model ownership belongs to a recognition worker. */
public final class KoreanModelStore {
 public static final String MODEL="vosk-model-small-ko-0.22";
 public static final String ZIP_SHA256="eea36124087fed26c59996a4761519458e3bd185e8ea9d9865ad8760c4a1d989";
 public static final String MANIFEST_SHA256="c4e5597129c61ba39760a1621ace86e9b21926660d79687f9ff9b00ff0cc7f78";
 public static final long ZIP_BYTES=86914329L,UNPACKED_BYTES=264364726L;
 private KoreanModelStore(){}
 private static byte[] manifest(Context context)throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=context.getAssets().open("offlinevoice/model-files.json")){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>65536)throw new IOException("Model manifest size");out.write(b,0,n);}}
  byte[] bytes=out.toByteArray();if(!MANIFEST_SHA256.equals(SafeModelZip.hex(MessageDigest.getInstance("SHA-256").digest(bytes))))throw new IOException("Model manifest hash");return bytes;
 }
 public static synchronized File prepare(Context context,SafeModelZip.Cancel cancel)throws Exception{
  SafeModelZip.check(cancel);JSONObject document=new JSONObject(new String(manifest(context),java.nio.charset.StandardCharsets.UTF_8));JSONArray list=document.getJSONArray("files");Map<String,SafeModelZip.Expected> expected=new LinkedHashMap<>();
  long total=0;for(int i=0;i<list.length();i++){JSONObject entry=list.getJSONObject(i);String name=entry.getString("path");SafeModelZip.Expected item=new SafeModelZip.Expected(entry.getLong("bytes"),entry.getString("sha256"));if(expected.put(name,item)!=null)throw new IOException("Duplicate manifest entry");total+=item.bytes;}
  if(total!=UNPACKED_BYTES||expected.size()!=15)throw new IOException("Model manifest shape");
  File base=new File(context.getNoBackupFilesDir(),"offlinevoice-models");if(!base.isDirectory()&&!base.mkdirs())throw new IOException("한국어 모델을 저장할 공간을 확인하세요");
  SafeModelZip.cleanupPending(base,cancel);
  File target=new File(base,"ko-0.22-"+ZIP_SHA256.substring(0,12));if(SafeModelZip.verify(target,expected,cancel))return target;
  if(target.exists())SafeModelZip.removeOwned(base,target);
  if(base.getUsableSpace()<ZIP_BYTES+UNPACKED_BYTES+16*1024*1024L)throw new IOException("한국어 모델을 준비할 저장 공간이 부족합니다");
  String id=UUID.randomUUID().toString();File archive=new File(base,"pending-"+id+".zip"),pending=new File(base,"pending-"+id);
  try{
   MessageDigest digest=MessageDigest.getInstance("SHA-256");long bytes=0;
   try(InputStream in=context.getAssets().open("offlinevoice/"+MODEL+".zip");FileOutputStream out=new FileOutputStream(archive)){
    byte[] block=new byte[65536];int n;while((n=in.read(block))!=-1){SafeModelZip.check(cancel);bytes+=n;if(bytes>ZIP_BYTES)throw new IOException("Model archive size");digest.update(block,0,n);out.write(block,0,n);}out.getFD().sync();
   }
   if(bytes!=ZIP_BYTES||!ZIP_SHA256.equals(SafeModelZip.hex(digest.digest())))throw new IOException("Model archive hash");
   SafeModelZip.unpack(archive,pending,expected,cancel);SafeModelZip.check(cancel);
   if(target.exists()||!pending.renameTo(target))throw new IOException("Model commit failed");return target;
  }finally{if(archive.exists())SafeModelZip.removeOwned(base,archive);if(pending.exists())SafeModelZip.removeOwned(base,pending);}
 }
}
