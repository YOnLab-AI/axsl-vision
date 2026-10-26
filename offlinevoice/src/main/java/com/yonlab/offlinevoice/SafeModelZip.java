package com.yonlab.offlinevoice;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Exact allowlist extraction. ZIP entries never create links or choose arbitrary destination paths. */
public final class SafeModelZip {
 public static final long MAX_UNPACKED=384L*1024*1024;
 private static final String PREFIX="vosk-model-small-ko-0.22/";
 public interface Cancel {boolean canceled();}
 public static final class Expected {public final long bytes;public final String sha256;public Expected(long bytes,String sha){if(bytes<0||bytes>MAX_UNPACKED||sha==null||!sha.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("manifest");this.bytes=bytes;sha256=sha;}}
 private SafeModelZip(){}
 static void check(Cancel cancel)throws InterruptedIOException{if(cancel.canceled()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Canceled");}
 public static String sha(File file,Cancel cancel)throws IOException{try{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] block=new byte[65536];int n;while((n=in.read(block))!=-1){check(cancel);d.update(block,0,n);}}return hex(d.digest());}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}}
 static String hex(byte[] bytes){StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
 private static File target(File root,String relative)throws IOException{
  if(relative==null||relative.isEmpty()||relative.length()>300||relative.startsWith("/")||relative.indexOf('\\')>=0||relative.indexOf(':')>=0||relative.indexOf('\0')>=0)throw new IOException("Invalid model entry");
  for(String segment:relative.split("/",-1))if(segment.equals(".")||segment.equals("..")||segment.isEmpty())throw new IOException("Invalid model entry");
  File file=new File(root,relative);if(!file.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))throw new IOException("Model path escaped");return file;
 }
 public static void unpack(File archive,File root,Map<String,Expected> expected,Cancel cancel)throws IOException{
  unpack(archive,root,expected,cancel,PREFIX);
 }
 public static void unpack(File archive,File root,Map<String,Expected> expected,Cancel cancel,String prefix)throws IOException{
  if(!PREFIX.equals(prefix)&&!"vosk-model-small-en-us-0.15/".equals(prefix)&&!"sherpa-whisper-tiny-int8/".equals(prefix))throw new IOException("Unsupported model prefix");
  if(expected==null||expected.isEmpty()||expected.size()>128)throw new IOException("Invalid model manifest");if(root.exists()&&root.list()!=null&&root.list().length>0)throw new IOException("Model staging is not empty");if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Model storage");
  for(String path:expected.keySet())target(root,path);Set<String> names=new HashSet<>(),seen=new HashSet<>();long total=0;int count=0;
  try(ZipFile zip=new ZipFile(archive)){Enumeration<? extends ZipEntry> entries=zip.entries();while(entries.hasMoreElements()){
   check(cancel);ZipEntry entry=entries.nextElement();String name=entry.getName();if(++count>256||!names.add(name)||!name.startsWith(prefix))throw new IOException("Invalid model archive");
   String relative=name.substring(prefix.length());if(entry.isDirectory()){if(relative.isEmpty())continue;if(!relative.endsWith("/"))throw new IOException("Invalid model directory");target(root,relative.substring(0,relative.length()-1));continue;}
   File file=target(root,relative);Expected item=expected.get(relative);if(item==null||entry.getSize()!=item.bytes||!seen.add(relative))throw new IOException("Unexpected model file");total+=item.bytes;if(total>MAX_UNPACKED)throw new IOException("Model exceeds size limit");
   File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Model storage");target(root,relative);long copied=0;MessageDigest digest;try{digest=MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
   try(InputStream in=zip.getInputStream(entry);FileOutputStream out=new FileOutputStream(file)){byte[] block=new byte[65536];int n;while((n=in.read(block))!=-1){check(cancel);copied+=n;if(copied>item.bytes)throw new IOException("Model size mismatch");digest.update(block,0,n);out.write(block,0,n);}out.getFD().sync();}
   if(copied!=item.bytes||!item.sha256.equals(hex(digest.digest())))throw new IOException("Model hash mismatch");
  }}
  if(seen.size()!=expected.size())throw new IOException("Model files missing");
 }
 public static boolean verify(File root,Map<String,Expected> expected,Cancel cancel)throws IOException{
  if(!root.isDirectory()||expected==null||expected.isEmpty()||expected.size()>128)return false;List<File> actual=new ArrayList<>();if(!collect(root,root,actual,0,new int[]{0},cancel))return false;if(actual.size()!=expected.size())return false;
  for(Map.Entry<String,Expected> entry:expected.entrySet()){check(cancel);File file=target(root,entry.getKey());Expected e=entry.getValue();if(!file.isFile()||file.length()!=e.bytes||!e.sha256.equals(sha(file,cancel)))return false;}return true;
 }
 private static boolean collect(File base,File file,List<File> files,int depth,int[] count,Cancel cancel)throws IOException{check(cancel);if(depth>12||files.size()>128||++count[0]>256)return false;if(!file.getCanonicalPath().equals(base.getCanonicalPath())&&!file.getCanonicalPath().startsWith(base.getCanonicalPath()+File.separator))return false;if(file.isDirectory()){File[] children=file.listFiles();if(children==null||children.length>256)return false;for(File child:children)if(!collect(base,child,files,depth+1,count,cancel))return false;}else files.add(file);return true;}
 /** Removes only this store's UUID-named interrupted transactions, before a new transaction starts. */
 public static void cleanupPending(File base,Cancel cancel)throws IOException{
  check(cancel);File[] children=base.listFiles();if(children==null||children.length>64)throw new IOException("Model cache entries");int pending=0;
  for(File child:children){check(cancel);if(!child.getName().matches("pending-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}(\\.zip)?"))continue;if(++pending>32)throw new IOException("Model pending entries");removeOwned(base,child);}
 }
 public static void removeOwned(File parent,File item)throws IOException{removeOwned(parent,item,0,new int[]{0});}
 private static void removeOwned(File parent,File item,int depth,int[] count)throws IOException{
  if(depth>12||++count[0]>256||!item.getCanonicalPath().startsWith(parent.getCanonicalPath()+File.separator))throw new IOException("Cleanup path invalid");
  if(item.isDirectory()){File[] children=item.listFiles();if(children==null)throw new IOException("Cleanup storage");for(File child:children)removeOwned(parent,child,depth+1,count);}if(item.exists()&&!item.delete())throw new IOException("Cleanup storage");
 }
}
