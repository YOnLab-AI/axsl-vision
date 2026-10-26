package com.yonlab.offlinevoice;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exact published assets -> hash-verified private model directory; no model downloads at runtime. */
public final class VoiceAssetStore {
 public static final String EN_ZIP_SHA="30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498";
 public static final String EN_MANIFEST_SHA="65bf3ff10e0f0e71c6a39f3a6d4ffe54f16131e21b3b7f506a10b8f91798b80e";
 public static final String MOON_MANIFEST_SHA="ca3ba5660f1d0b852f6ba1354415094a8edfc8d7ebc59bd1eb541e373309f20b";
 private VoiceAssetStore(){}
 public static String manifestSha(String id){return VoiceModelPolicy.KO.equals(id)?KoreanModelStore.MANIFEST_SHA256:VoiceModelPolicy.EN.equals(id)?EN_MANIFEST_SHA:VoiceModelPolicy.MOON.equals(id)?MOON_MANIFEST_SHA:VoiceModelPolicy.WHISPER.equals(id)?WhisperModelStore.MANIFEST_SHA:(VoiceModelPolicy.WHISPER_BASE.equals(id)||VoiceModelPolicy.WHISPER_SMALL.equals(id))?PlusWhisperModels.MANIFEST_SHA:"";}
 /** SHA-256 of the ordered, fixed-length per-file SHA-256 ASCII values in the manifest. */
 public static String weightsFingerprint(String id){return VoiceModelPolicy.WHISPER_BASE.equals(id)||VoiceModelPolicy.WHISPER_SMALL.equals(id)?PlusWhisperModels.fingerprint(id):"";}
 public static String archiveSha(String id){return VoiceModelPolicy.KO.equals(id)?KoreanModelStore.ZIP_SHA256:VoiceModelPolicy.EN.equals(id)?EN_ZIP_SHA:VoiceModelPolicy.WHISPER.equals(id)?WhisperModelStore.ZIP_SHA:"";}
 public static File prepare(Context c,String id,SafeModelZip.Cancel cancel)throws Exception{
  if(!VoiceModelPolicy.known(id))throw new IOException("Unknown voice model");
  if(VoiceModelPolicy.WHISPER.equals(id))return WhisperModelStore.prepare(c,cancel);
  if(VoiceModelPolicy.WHISPER_BASE.equals(id)||VoiceModelPolicy.WHISPER_SMALL.equals(id))return PlusWhisperModels.prepare(c,id,cancel);
  synchronized(KoreanModelStore.class){
   if(VoiceModelPolicy.KO.equals(id))return KoreanModelStore.prepare(c,cancel);
   boolean english=VoiceModelPolicy.EN.equals(id);String asset=english?"english-model-files.json":"moonshine-model-files.json";
   byte[] raw;try(InputStream in=c.getAssets().open("offlinevoice/"+asset);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){SafeModelZip.check(cancel);if(out.size()+n>65536)throw new IOException("Model manifest size");out.write(b,0,n);}raw=out.toByteArray();}
   if(!manifestSha(id).equals(SafeModelZip.hex(MessageDigest.getInstance("SHA-256").digest(raw))))throw new IOException("Model manifest hash");
   JSONArray files=new JSONObject(new String(raw,StandardCharsets.UTF_8)).getJSONArray("files");Map<String,SafeModelZip.Expected> expected=new LinkedHashMap<>();long total=0;
   for(int i=0;i<files.length();i++){JSONObject f=files.getJSONObject(i);SafeModelZip.Expected e=new SafeModelZip.Expected(f.getLong("bytes"),f.getString("sha256"));if(expected.put(f.getString("path"),e)!=null)throw new IOException("Duplicate model manifest");total+=e.bytes;}
   if(files.length()!=(english?14:4)||total!=(english?70898967L:72128206L))throw new IOException("Model manifest shape");
   File base=new File(c.getNoBackupFilesDir(),"offlinevoice-models");if(!base.isDirectory()&&!base.mkdirs())throw new IOException("Model storage");SafeModelZip.cleanupPending(base,cancel);
   File target=new File(base,(english?"en-0.15-":"moon-ko-")+manifestSha(id).substring(0,12));if(SafeModelZip.verify(target,expected,cancel))return target;if(target.exists())SafeModelZip.removeOwned(base,target);
   if(base.getUsableSpace()<total+(english?41205931L:0)+16*1024*1024L)throw new IOException("Model storage space");
   String uid=UUID.randomUUID().toString();File pending=new File(base,"pending-"+uid),archive=new File(base,"pending-"+uid+".zip");
   try{
    if(english){copy(c,"offlinevoice/vosk-model-small-en-us-0.15.zip",archive,41205931L,EN_ZIP_SHA,cancel);SafeModelZip.unpack(archive,pending,expected,cancel,"vosk-model-small-en-us-0.15/");}
    else{if(!pending.mkdirs())throw new IOException("Model staging");for(Map.Entry<String,SafeModelZip.Expected> f:expected.entrySet()){if(!f.getKey().matches("[A-Za-z0-9_.]+"))throw new IOException("Model asset name");copy(c,"offlinevoice/moonshine-ko/"+f.getKey(),new File(pending,f.getKey()),f.getValue().bytes,f.getValue().sha256,cancel);}}
    SafeModelZip.check(cancel);if(!SafeModelZip.verify(pending,expected,cancel))throw new IOException("Model hash");if(target.exists()||!pending.renameTo(target))throw new IOException("Model commit");return target;
   }finally{if(archive.exists())SafeModelZip.removeOwned(base,archive);if(pending.exists())SafeModelZip.removeOwned(base,pending);}
  }
 }
 private static void copy(Context c,String asset,File file,long expected,String sha,SafeModelZip.Cancel cancel)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");long count=0;try(InputStream in=c.getAssets().open(asset);FileOutputStream out=new FileOutputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){SafeModelZip.check(cancel);count+=n;if(count>expected)throw new IOException("Model asset size");digest.update(b,0,n);out.write(b,0,n);}out.getFD().sync();}if(count!=expected||!sha.equals(SafeModelZip.hex(digest.digest())))throw new IOException("Model asset hash");}
}
