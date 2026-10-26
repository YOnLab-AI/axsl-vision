package com.yonlab.offlinevoice;
import java.io.*;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/** RIFF PCM8/16/24/32 or IEEE float32, including standard WAVE_FORMAT_EXTENSIBLE subtypes. */
public final class WavePcm {
 private WavePcm(){}
 public static final class Format {public final int rate,channels,bits,bytes;public final boolean floating;Format(int rate,int channels,int bits,boolean floating,int bytes){this.rate=rate;this.channels=channels;this.bits=bits;this.floating=floating;this.bytes=bytes;}}
 private static int u16(byte[] b,int p){return (b[p]&255)|((b[p+1]&255)<<8);}
 private static long u32(byte[] b,int p){return ((long)b[p]&255)|(((long)b[p+1]&255)<<8)|(((long)b[p+2]&255)<<16)|(((long)b[p+3]&255)<<24);}
 private static void fully(InputStream in,byte[] b,int n,PcmAudio.Cancel cancel)throws IOException{int at=0;while(at<n){PcmAudio.check(cancel);int read=in.read(b,at,n-at);if(read<0)throw new AudioInputException("TRUNCATED");if(read==0){int value=in.read();if(value<0)throw new AudioInputException("TRUNCATED");b[at++]=(byte)value;}else at+=read;}}
 public static Format read(InputStream in,long fileBytes,PcmAudio.Cancel cancel)throws Exception{
  if(fileBytes>PcmAudio.MAX_FILE_BYTES)throw new AudioInputException("TOO_LARGE");if(in==null||cancel==null||fileBytes<44)throw new AudioInputException("TRUNCATED");
  byte[] head=new byte[12];fully(in,head,12,cancel);if(!Arrays.equals(Arrays.copyOfRange(head,0,4),new byte[]{'R','I','F','F'})||!Arrays.equals(Arrays.copyOfRange(head,8,12),new byte[]{'W','A','V','E'}))throw new AudioInputException("UNSUPPORTED_CONTAINER");
  long limit=u32(head,4)+8;if(limit>fileBytes)throw new AudioInputException("TRUNCATED");if(limit<44||limit>PcmAudio.MAX_FILE_BYTES)throw new AudioInputException("MALFORMED");
  int rate=0,channels=0,bits=0,align=0,chunks=0;boolean floating=false,format=false;long offset=12;
  while(offset+8<=limit){if(++chunks>128)throw new AudioInputException("MALFORMED");byte[] h=new byte[8];fully(in,h,8,cancel);offset+=8;long size=u32(h,4),padded=size+(size&1);if(size>PcmAudio.MAX_FILE_BYTES||offset+padded>limit)throw new AudioInputException("TRUNCATED");String name=new String(h,0,4,StandardCharsets.US_ASCII);
   if("fmt ".equals(name)){
    if(format||size<16||size>128)throw new AudioInputException("MALFORMED");byte[] f=new byte[(int)size];fully(in,f,f.length,cancel);int encoding=u16(f,0);channels=u16(f,2);long hz=u32(f,4);bits=u16(f,14);align=u16(f,12);
    if(encoding==65534){if(size<40||u16(f,16)<22||u16(f,18)!=bits||u32(f,24)>3||u16(f,28)!=0||u16(f,30)!=16||!Arrays.equals(Arrays.copyOfRange(f,32,40),new byte[]{(byte)128,0,0,(byte)170,0,56,(byte)155,113}))throw new AudioInputException("UNSUPPORTED_FORMAT");encoding=(int)u32(f,24);}
    if(encoding!=1&&encoding!=3)throw new AudioInputException("UNSUPPORTED_FORMAT");floating=encoding==3;
    if(hz<8000||hz>192000||channels<1||channels>8||(floating?bits!=32:bits!=8&&bits!=16&&bits!=24&&bits!=32))throw new AudioInputException("UNSUPPORTED_FORMAT");rate=(int)hz;
    if(align!=channels*(bits/8)||u32(f,8)!=(long)rate*align)throw new AudioInputException("MALFORMED");format=true;if((size&1)!=0)fully(in,new byte[1],1,cancel);
   }else if("data".equals(name)){
    if(!format||size==0||size%align!=0)throw new AudioInputException("MALFORMED");if(size/align>(long)rate*PcmAudio.MAX_SECONDS)throw new AudioInputException("TOO_LONG");return new Format(rate,channels,bits,floating,(int)size);
   }else{byte[] discard=new byte[8192];long remaining=padded;while(remaining>0){int n=(int)Math.min(discard.length,remaining);fully(in,discard,n,cancel);remaining-=n;}}
   offset+=padded;
  }throw new AudioInputException("MALFORMED");
 }
 public static PcmAudio decode(InputStream in,long bytes,PcmAudio.Sink sink,PcmAudio.Cancel cancel)throws Exception{
  Format f=read(in,bytes,cancel);PcmAudio converter=new PcmAudio(f.rate,f.channels,f.bits,f.floating,sink,cancel);int remaining=f.bytes;byte[] block=new byte[8192];while(remaining>0){int n=Math.min(block.length,remaining);fully(in,block,n,cancel);converter.accept(block,0,n);remaining-=n;}converter.finish();return converter;
 }
}
