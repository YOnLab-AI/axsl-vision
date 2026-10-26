package com.yonlab.vision;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.zip.CRC32;

/** Adds UTF-8 iTXt without decoding or changing IHDR/IDAT pixels. Original and added chunk CRCs are verified. */
public final class OnePngAttribution {
 private static final byte[] SIGNATURE={(byte)137,80,78,71,13,10,26,10};
 private static final int LIMIT=32*1024*1024;
 private OnePngAttribution(){}
 public static void copy(InputStream source,OutputStream destination,Map<String,String> fields,BooleanSupplier canceled)throws IOException{
  DataInputStream in=new DataInputStream(source);DataOutputStream out=new DataOutputStream(destination);byte[] signature=new byte[8];in.readFully(signature);if(!Arrays.equals(signature,SIGNATURE))throw new IOException("PNG signature");out.write(signature);long total=8;boolean first=true;byte[] buffer=new byte[16384];
  while(true){check(canceled);int length=in.readInt();if(length<0||length>LIMIT||total+length+12>LIMIT)throw new IOException("PNG chunk limit");byte[] type=new byte[4];in.readFully(type);for(byte c:type)if(!((c>='A'&&c<='Z')||(c>='a'&&c<='z')))throw new IOException("PNG chunk type");String name=new String(type,StandardCharsets.US_ASCII);if(first&&(!"IHDR".equals(name)||length!=13))throw new IOException("PNG header");first=false;CRC32 crc=new CRC32();crc.update(type);
   if("IEND".equals(name)){if(length!=0)throw new IOException("PNG end");int originalCrc=in.readInt();if((int)crc.getValue()!=originalCrc)throw new IOException("PNG CRC");for(Map.Entry<String,String> field:fields.entrySet()){check(canceled);writeText(out,field.getKey(),field.getValue());}out.writeInt(0);out.write(type);out.writeInt(originalCrc);if(in.read()!=-1)throw new IOException("PNG trailing data");return;}
   out.writeInt(length);out.write(type);int remaining=length;while(remaining>0){check(canceled);int n=in.read(buffer,0,Math.min(buffer.length,remaining));if(n<0)throw new EOFException("PNG data");crc.update(buffer,0,n);out.write(buffer,0,n);remaining-=n;}int originalCrc=in.readInt();if((int)crc.getValue()!=originalCrc)throw new IOException("PNG CRC");out.writeInt(originalCrc);total+=length+12;
  }
 }
 private static void writeText(DataOutputStream out,String key,String value)throws IOException{
  if(key==null||!key.matches("[A-Za-z][A-Za-z0-9 _-]{0,78}")||value==null||value.indexOf('\u0000')>=0)throw new IOException("PNG text field");byte[] text=value.getBytes(StandardCharsets.UTF_8);if(text.length>65536)throw new IOException("PNG text limit");ByteArrayOutputStream buffer=new ByteArrayOutputStream();buffer.write(key.getBytes(StandardCharsets.ISO_8859_1));buffer.write(new byte[]{0,0,0,0,0});buffer.write(text);byte[] data=buffer.toByteArray(),type="iTXt".getBytes(StandardCharsets.US_ASCII);CRC32 crc=new CRC32();crc.update(type);crc.update(data);out.writeInt(data.length);out.write(type);out.write(data);out.writeInt((int)crc.getValue());
 }
 private static void check(BooleanSupplier canceled)throws InterruptedIOException{if(canceled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Image export canceled");}
}
