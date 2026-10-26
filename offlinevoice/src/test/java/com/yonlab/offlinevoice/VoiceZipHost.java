package com.yonlab.offlinevoice;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;
public final class VoiceZipHost{
 static int checks;static void check(boolean b,String n){if(!b)throw new AssertionError(n);checks++;}
 static File zip(File dir,String prefix)throws Exception{File z=new File(dir,UUID.randomUUID()+".zip");try(ZipOutputStream o=new ZipOutputStream(new FileOutputStream(z))){o.putNextEntry(new ZipEntry(prefix+"conf/x"));o.write(new byte[]{1});o.closeEntry();}return z;}
 public static void main(String[] a)throws Exception{File d=Files.createTempDirectory("voice-zip-").toFile();Map<String,SafeModelZip.Expected> m=new LinkedHashMap<>();m.put("conf/x",new SafeModelZip.Expected(1,"4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a"));
 File t=new File(d,"en");SafeModelZip.unpack(zip(d,"vosk-model-small-en-us-0.15/"),t,m,()->false,"vosk-model-small-en-us-0.15/");check(SafeModelZip.verify(t,m,()->false),"English prefix extracts and SHA verifies");
 for(String prefix:new String[]{"../","other/","vosk-model-small-en-us-0.15/../"}){boolean bad=false;try{SafeModelZip.unpack(zip(d,prefix),new File(d,UUID.randomUUID().toString()),m,()->false,"vosk-model-small-en-us-0.15/");}catch(IOException e){bad=true;}check(bad,"wrong entry prefix rejected");}
 boolean bad=false;try{SafeModelZip.unpack(zip(d,"vosk-model-small-en-us-0.15/"),new File(d,"bad-prefix"),m,()->false,"../");}catch(IOException e){bad=true;}check(bad,"unsafe configured prefix rejected");
 System.out.println("Voice ZIP PASS "+checks+" assertions");}
}
