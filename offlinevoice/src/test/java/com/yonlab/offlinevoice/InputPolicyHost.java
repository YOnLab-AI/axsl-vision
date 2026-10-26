package com.yonlab.offlinevoice;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Tests the same production input/parser/extractor code used by the Activity. */
public final class InputPolicyHost {
 static int count;static void check(boolean x,String name){if(!x)throw new AssertionError(name);count++;}
 static void le(OutputStream o,long n,int bytes)throws IOException{for(int i=0;i<bytes;i++)o.write((int)(n>>(i*8))&255);}
 static byte[] wave(int hz,int channels,int bits,int format,int samples,boolean junk)throws Exception{ByteArrayOutputStream body=new ByteArrayOutputStream();body.write("WAVE".getBytes("US-ASCII"));if(junk){body.write("JUNK".getBytes("US-ASCII"));le(body,3,4);body.write(new byte[]{1,2,3,0});}body.write("fmt ".getBytes("US-ASCII"));le(body,16,4);le(body,format,2);le(body,channels,2);le(body,hz,4);le(body,hz*channels*bits/8,4);le(body,channels*bits/8,2);le(body,bits,2);body.write("data".getBytes("US-ASCII"));le(body,samples*channels*bits/8,4);body.write(new byte[samples*channels*bits/8]);ByteArrayOutputStream out=new ByteArrayOutputStream();out.write("RIFF".getBytes("US-ASCII"));le(out,body.size(),4);body.writeTo(out);return out.toByteArray();}
 static void bad(byte[] wav,String name)throws Exception{boolean rejected=false;try{PcmWave.read(new ByteArrayInputStream(wav),wav.length,()->false);}catch(IOException expected){rejected=true;}check(rejected,name);}
 static String hash(byte[] bytes)throws Exception{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 static File zip(File dir,String name,String entry,byte[] bytes)throws Exception{File file=new File(dir,name);try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(file))){out.putNextEntry(new ZipEntry(entry));out.write(bytes);out.closeEntry();}return file;}
 public static void main(String[] args)throws Exception{
  byte[] wav=wave(16000,1,16,1,16000,false);PcmWave.Header h=PcmWave.read(new ByteArrayInputStream(wav),wav.length,()->false);check(h.dataBytes==32000&&h.durationMs==1000,"real PCM16 header parsed");
  wav=wave(16000,1,16,1,16000,true);h=PcmWave.read(new ByteArrayInputStream(wav),wav.length,()->false);check(h.dataBytes==32000,"odd padded unknown chunk supported");
  bad(wave(8000,1,16,1,100,false),"8k rejected");bad(wave(16000,2,16,1,100,false),"stereo rejected");bad(wave(16000,1,8,1,100,false),"8bit rejected");bad(wave(16000,1,16,3,100,false),"float format rejected");bad(wave(16000,1,16,1,480001,false),"longer than30s rejected");
  wav=wave(16000,1,16,1,100,false);wav[0]='X';bad(wav,"non RIFF rejected");bad(new byte[11],"truncated header rejected");
  wav=wave(16000,1,16,1,100,false);wav[40]=(byte)255;wav[41]=(byte)255;wav[42]=(byte)255;wav[43]=(byte)127;bad(wav,"oversized data length rejected");
  boolean canceled=false;try{PcmWave.read(new ByteArrayInputStream(wave(16000,1,16,1,100,false)),244,()->true);}catch(InterruptedIOException expected){canceled=true;}check(canceled,"WAV cancellation observed");
  File dir=Files.createTempDirectory("yon-voice-policy-").toFile();byte[] content="model fixture".getBytes("UTF-8");Map<String,SafeModelZip.Expected> expected=new HashMap<>();expected.put("conf/model.conf",new SafeModelZip.Expected(content.length,hash(content)));
  File archive=zip(dir,"ok.zip","vosk-model-small-ko-0.22/conf/model.conf",content),target=new File(dir,"good");SafeModelZip.unpack(archive,target,expected,()->false);check(new File(target,"conf/model.conf").isFile(),"allowlisted model entry extracted");check(SafeModelZip.verify(target,expected,()->false),"extracted SHA verifies");
  try(FileOutputStream out=new FileOutputStream(new File(target,"conf/model.conf"))){out.write(0);}check(!SafeModelZip.verify(target,expected,()->false),"changed cached file rejected");
  for(String path:new String[]{"../escape","vosk-model-small-ko-0.22/../../escape","/absolute","vosk-model-small-ko-0.22/\\evil","vosk-model-small-ko-0.22/C:drive","other/conf/model.conf"}){boolean rejected=false;try{SafeModelZip.unpack(zip(dir,"bad"+count+".zip",path,content),new File(dir,"bad"+count),expected,()->false);}catch(IOException e){rejected=true;}check(rejected,"unsafe path rejected "+path);}
  boolean rejected=false;try{SafeModelZip.unpack(zip(dir,"extra.zip","vosk-model-small-ko-0.22/conf/extra",content),new File(dir,"extra"),expected,()->false);}catch(IOException e){rejected=true;}check(rejected,"extra file rejected");
  rejected=false;try{SafeModelZip.unpack(zip(dir,"hash.zip","vosk-model-small-ko-0.22/conf/model.conf",new byte[content.length]),new File(dir,"hash"),expected,()->false);}catch(IOException e){rejected=true;}check(rejected,"SHA mismatch rejected");
  canceled=false;try{SafeModelZip.unpack(archive,new File(dir,"canceled"),expected,()->true);}catch(InterruptedIOException e){canceled=true;}check(canceled,"unpack cancellation observed");
  File cache=new File(dir,"bounded-cache");SafeModelZip.unpack(archive,cache,expected,()->false);for(int i=0;i<260;i++)new File(cache,"empty"+i).mkdirs();check(!SafeModelZip.verify(cache,expected,()->false),"empty directory flood bounded");
  File leftovers=new File(dir,"leftovers");leftovers.mkdirs();File old=new File(leftovers,"pending-"+UUID.randomUUID());old.mkdirs();new File(old,"part").createNewFile();File oldZip=new File(leftovers,"pending-"+UUID.randomUUID()+".zip");oldZip.createNewFile();File unrelated=new File(leftovers,"pending-not-owned");unrelated.mkdir();SafeModelZip.cleanupPending(leftovers,()->false);check(!old.exists()&&!oldZip.exists(),"interrupted unpack leftovers cleaned");check(unrelated.exists(),"cleanup preserves unrelated names");
  canceled=false;try{SafeModelZip.cleanupPending(leftovers,()->true);}catch(InterruptedIOException e){canceled=true;}check(canceled,"leftover cleanup cancellation observed");
  System.out.println("Offline voice input policy PASS "+count+" checks (production WAV/ZIP code; no ASR inference)");
 }
}
