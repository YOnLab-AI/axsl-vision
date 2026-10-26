package com.yonlab.vision;

import android.content.Context;
import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.*;

/** Pinned app-owned model materialization; no network or shared-storage model loading. */
public final class ModelPlusAssets {
 private ModelPlusAssets(){}
 public static synchronized File file(Context context,String asset,String expectedSha,long maximumBytes)throws IOException{
  if(asset==null||!asset.startsWith("one/modelplus/")||asset.contains("..")||asset.contains("\\")||expectedSha==null||!expectedSha.matches("[a-f0-9]{64}")||maximumBytes<1||maximumBytes>1024L*1024*1024)throw new IOException("모델 파일 정보를 확인하세요");
  File folder=new File(context.getNoBackupFilesDir(),"modelplus");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("모델 저장 공간을 확인하세요");
  File target=new File(folder,expectedSha+"-"+asset.substring(asset.lastIndexOf('/')+1));
  if(target.isFile()&&target.length()>0&&target.length()<=maximumBytes&&sha(target).equals(expectedSha))return target;
  File pending=File.createTempFile("model-",".pending",folder);
  try{long bytes=0;try(InputStream in=context.getAssets().open(asset);FileOutputStream out=new FileOutputStream(pending)){byte[] block=new byte[65536];int n;while((n=in.read(block))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("모델 준비 취소");bytes+=n;if(bytes>maximumBytes)throw new IOException("모델 파일 크기를 확인하세요");out.write(block,0,n);}out.getFD().sync();}
   if(bytes<1||!sha(pending).equals(expectedSha))throw new IOException("모델 파일 해시가 다릅니다");if(!pending.renameTo(target))throw new IOException("모델 파일을 준비하지 못했습니다");return target;
  }finally{if(pending.exists())pending.delete();}
 }
 public static MappedByteBuffer map(Context c,String asset,String sha,long maximumBytes)throws IOException{File f=file(c,asset,sha,maximumBytes);try(FileInputStream in=new FileInputStream(f)){return in.getChannel().map(FileChannel.MapMode.READ_ONLY,0,f.length());}}
 public static String sha(File file)throws IOException{try(InputStream in=new FileInputStream(file)){return sha(in);}}
 public static String sha(InputStream in)throws IOException{try{MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("모델 준비 취소");digest.update(b,0,n);}return hex(digest.digest());}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}}
 public static String fingerprint(String... weights){try{MessageDigest d=MessageDigest.getInstance("SHA-256");for(String weight:weights)d.update((weight+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));return hex(d.digest());}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private static String hex(byte[] data){StringBuilder out=new StringBuilder();for(byte value:data)out.append(String.format(Locale.ROOT,"%02x",value&255));return out.toString();}
}
