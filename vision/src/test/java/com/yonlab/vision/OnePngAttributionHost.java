package com.yonlab.vision;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public final class OnePngAttributionHost {
 private static int checks;private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 private static void chunk(DataOutputStream out,String name,byte[] value)throws IOException{byte[] type=name.getBytes(StandardCharsets.US_ASCII);CRC32 crc=new CRC32();crc.update(type);crc.update(value);out.writeInt(value.length);out.write(type);out.write(value);out.writeInt((int)crc.getValue());}
 private static byte[] png()throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.write(new byte[]{(byte)137,80,78,71,13,10,26,10});ByteArrayOutputStream header=new ByteArrayOutputStream();DataOutputStream h=new DataOutputStream(header);h.writeInt(1);h.writeInt(1);h.write(new byte[]{8,6,0,0,0});chunk(out,"IHDR",header.toByteArray());ByteArrayOutputStream compressed=new ByteArrayOutputStream();try(DeflaterOutputStream z=new DeflaterOutputStream(compressed)){z.write(new byte[]{0,20,40,60,0});}chunk(out,"IDAT",compressed.toByteArray());chunk(out,"IEND",new byte[0]);return bytes.toByteArray();}
 private static List<String> nonText(byte[] source)throws IOException{List<String> parts=new ArrayList<>();DataInputStream in=new DataInputStream(new ByteArrayInputStream(source));byte[] signature=new byte[8];in.readFully(signature);while(in.available()>0){int count=in.readInt();byte[] type=new byte[4],data=new byte[count];in.readFully(type);in.readFully(data);int expected=in.readInt();CRC32 crc=new CRC32();crc.update(type);crc.update(data);check((int)crc.getValue()==expected,"every chunk CRC valid");String name=new String(type,StandardCharsets.US_ASCII);if(!name.equals("iTXt"))parts.add(name+":"+Base64.getEncoder().encodeToString(data)+":"+expected);}return parts;}
 public static void main(String[] args)throws Exception{
  byte[] original=png();Map<String,String> fields=new LinkedHashMap<>();fields.put("Author","한국 연구자");fields.put("Source","https://example.org/photo");fields.put("License","CC BY-SA 4.0");fields.put("Changes","YOn model output");ByteArrayOutputStream target=new ByteArrayOutputStream();OnePngAttribution.copy(new ByteArrayInputStream(original),target,fields,()->false);byte[] result=target.toByteArray();check(nonText(original).equals(nonText(result)),"IHDR/IDAT/IEND and compressed pixels preserved byte for byte");check(new String(result,StandardCharsets.UTF_8).contains("한국 연구자"),"international text is UTF8");check(result.length>original.length,"attribution text physically embedded");
  target.reset();OnePngAttribution.copy(new ByteArrayInputStream(original),target,Collections.emptyMap(),()->false);check(Arrays.equals(original,target.toByteArray()),"no attribution produces the exact original PNG");
  byte[] damaged=original.clone();damaged[29]^=1;boolean rejected=false;try{OnePngAttribution.copy(new ByteArrayInputStream(damaged),new ByteArrayOutputStream(),fields,()->false);}catch(IOException e){rejected=true;}check(rejected,"bad source CRC rejected");
  rejected=false;try{OnePngAttribution.copy(new ByteArrayInputStream(original),new ByteArrayOutputStream(),fields,()->true);}catch(InterruptedIOException e){rejected=true;}check(rejected,"canceled export aborts before copying chunks");
  byte[] extra=Arrays.copyOf(original,original.length+1);rejected=false;try{OnePngAttribution.copy(new ByteArrayInputStream(extra),new ByteArrayOutputStream(),fields,()->false);}catch(IOException e){rejected=true;}check(rejected,"trailing bytes rejected");
  rejected=false;try{OnePngAttribution.copy(new ByteArrayInputStream(original),new ByteArrayOutputStream(),Collections.singletonMap("Bad\u0000Key","data"),()->false);}catch(IOException e){rejected=true;}check(rejected,"invalid text keyword refused");
  rejected=false;try{OnePngAttribution.copy(new ByteArrayInputStream(original),new ByteArrayOutputStream(),Collections.singletonMap("Author","x".repeat(65537)),()->false);}catch(IOException e){rejected=true;}check(rejected,"oversized text is bounded");
  if(args.length==1){try(FileOutputStream out=new FileOutputStream(new File(args[0],"original.png"))){out.write(original);}try(FileOutputStream out=new FileOutputStream(new File(args[0],"credited.png"))){out.write(result);}}
  System.out.println("One PNG attribution PASS "+checks+" checks");
 }
}
