package com.yonlab.offlinevoice;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Arrays;

/** Bounded RIFF/WAVE PCM16 mono16k parser. The returned stream position is the PCM payload. */
public final class PcmWave {
 public static final int RATE=16000,MAX_PCM_BYTES=RATE*2*30,MAX_FILE_BYTES=2*1024*1024;
 public interface Cancel {boolean canceled();}
 public static final class Header {public final int dataBytes,durationMs;Header(int bytes){dataBytes=bytes;durationMs=(int)((long)bytes*1000/(RATE*2));}}
 private PcmWave(){}
 private static IOException invalid(){return new IOException("16kHz 모노 PCM16 WAV를 선택하세요. 최대30초까지 인식합니다");}
 private static long u32(byte[] b,int p){return ((long)b[p]&255)|(((long)b[p+1]&255)<<8)|(((long)b[p+2]&255)<<16)|(((long)b[p+3]&255)<<24);}
 private static int u16(byte[] b,int p){return (b[p]&255)|((b[p+1]&255)<<8);}
 private static void check(Cancel c)throws InterruptedIOException{if(c.canceled()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Canceled");}
 private static void fully(InputStream in,byte[] b,int count,Cancel cancel)throws IOException{int n=0;while(n<count){check(cancel);int read=in.read(b,n,count-n);if(read<0)throw invalid();if(read==0){int v=in.read();if(v<0)throw invalid();b[n++]=(byte)v;}else n+=read;}}
 public static Header read(InputStream in,long fileBytes,Cancel cancel)throws IOException{
  if(in==null||cancel==null||fileBytes<44||fileBytes>MAX_FILE_BYTES)throw invalid();byte[] head=new byte[12];fully(in,head,12,cancel);
  if(!Arrays.equals(Arrays.copyOfRange(head,0,4),new byte[]{'R','I','F','F'})||!Arrays.equals(Arrays.copyOfRange(head,8,12),new byte[]{'W','A','V','E'}))throw invalid();
  long limit=u32(head,4)+8;if(limit<44||limit>fileBytes||limit>MAX_FILE_BYTES)throw invalid();long offset=12;boolean format=false;int chunks=0;
  while(offset+8<=limit){if(++chunks>128)throw invalid();byte[] chunk=new byte[8];fully(in,chunk,8,cancel);offset+=8;long bytes=u32(chunk,4),padded=bytes+(bytes&1);if(bytes>MAX_FILE_BYTES||offset+padded>limit)throw invalid();
   String name=new String(chunk,0,4,java.nio.charset.StandardCharsets.US_ASCII);
   if("fmt ".equals(name)){if(format||bytes<16||bytes>128)throw invalid();byte[] data=new byte[(int)bytes];fully(in,data,data.length,cancel);
    if(u16(data,0)!=1||u16(data,2)!=1||u32(data,4)!=RATE||u32(data,8)!=RATE*2||u16(data,12)!=2||u16(data,14)!=16)throw invalid();format=true;if((bytes&1)!=0)fully(in,new byte[1],1,cancel);
   }else if("data".equals(name)){if(!format||bytes<2||bytes>MAX_PCM_BYTES||(bytes&1)!=0)throw invalid();return new Header((int)bytes);}
   else{byte[] block=new byte[8192];long remaining=padded;while(remaining>0){int n=(int)Math.min(block.length,remaining);fully(in,block,n,cancel);remaining-=n;}}
   offset+=padded;
  }
  throw invalid();
 }
}
