package com.yonlab.offlinevoice;
import java.io.*;
import java.util.*;

/** Real conversion/parser tests; no Android codec or ASR substitute. */
public final class AudioConversionHost {
 static int checks;static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);checks++;}
 static void le(OutputStream out,long n,int size)throws IOException{for(int i=0;i<size;i++)out.write((int)(n>>(8*i))&255);}
 static byte[] payload(int format,int bits,int channels,int frames,double sample)throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();for(int i=0;i<frames*channels;i++){
   long n=format==3?Float.floatToIntBits((float)sample):bits==8?Math.round(sample*128)+128:Math.round(sample*Math.pow(2,bits-1));le(out,n,bits/8);
  }return out.toByteArray();
 }
 static byte[] wav(int format,int bits,int channels,int rate,byte[] data,boolean oddJunk,boolean extensible)throws Exception{
  ByteArrayOutputStream body=new ByteArrayOutputStream();body.write("WAVE".getBytes("US-ASCII"));
  if(oddJunk){body.write("JUNK".getBytes("US-ASCII"));le(body,3,4);body.write(new byte[]{1,2,3,0});}
  body.write("fmt ".getBytes("US-ASCII"));le(body,extensible?40:16,4);le(body,extensible?65534:format,2);le(body,channels,2);le(body,rate,4);le(body,(long)rate*channels*bits/8,4);le(body,channels*bits/8,2);le(body,bits,2);
  if(extensible){le(body,22,2);le(body,bits,2);le(body,channels==1?4:3,4);le(body,format,4);le(body,0,2);le(body,16,2);body.write(new byte[]{(byte)128,0,0,(byte)170,0,56,(byte)155,113});}
  body.write("data".getBytes("US-ASCII"));le(body,data.length,4);body.write(data);if((data.length&1)!=0)body.write(0);
  ByteArrayOutputStream all=new ByteArrayOutputStream();all.write("RIFF".getBytes("US-ASCII"));le(all,body.size(),4);body.writeTo(all);return all.toByteArray();
 }
 static byte[] decode(byte[] input)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();WavePcm.decode(new ByteArrayInputStream(input),input.length,(b,n)->out.write(b,0,n),()->false);return out.toByteArray();}
 static int sample(byte[] pcm,int index){return (short)((pcm[index*2]&255)|((pcm[index*2+1]&255)<<8));}
 static void bad(byte[] input,String code)throws Exception{try{decode(input);throw new AssertionError("accepted "+code);}catch(AudioInputException e){check(code.equals(e.code),"reason "+code+" got "+e.code);}}
 public static void main(String[] args)throws Exception{
  byte[] pcm=new byte[]{0,(byte)128,0,0,(byte)255,127};check(Arrays.equals(pcm,decode(wav(1,16,1,16000,pcm,false,false))),"PCM16 identity including extrema");
  byte[] stereo=new byte[4800*4];for(int i=0;i<4800;i++){stereo[4*i+1]=64;stereo[4*i+3]=(byte)192;}
  byte[] output=decode(wav(1,16,2,48000,stereo,false,false));check(output.length==3200,"stereo48k resampled length");check(sample(output,25)==0,"stereo opposite phases downmix to silence");
  output=decode(wav(1,8,1,8000,payload(1,8,1,80,.5),false,false));check(output.length==320&&sample(output,159)==16384,"unsigned8 upsampling and terminal sample");
  output=decode(wav(1,24,1,44100,payload(1,24,1,441,.5),false,false));check(output.length==320&&sample(output,40)==16384,"PCM24 44.1k converted");
  output=decode(wav(1,32,1,192000,payload(1,32,1,192,-.25),false,false));check(output.length==32&&sample(output,10)==-8192,"signed32 192k converted");
  output=decode(wav(3,32,2,16000,payload(3,32,2,160,.125),false,false));check(output.length==320&&sample(output,90)==4096,"float32 stereo converted");
  output=decode(wav(1,24,2,48000,payload(1,24,2,480,.25),false,true));check(output.length==320&&sample(output,10)==8192,"WAVE_FORMAT_EXTENSIBLE standard PCM subtype");
  output=decode(wav(1,16,1,16000,payload(1,16,1,10,.25),true,false));check(output.length==20,"odd unknown chunk padding skipped");
  bad(wav(3,32,1,16000,payload(3,32,1,1,Double.NaN),false,false),"INVALID_PCM");
  bad(wav(3,32,1,16000,payload(3,32,1,1,Double.POSITIVE_INFINITY),false,false),"INVALID_PCM");
  bad(wav(17,16,1,16000,payload(1,16,1,8,.1),false,false),"UNSUPPORTED_FORMAT");
  bad(wav(1,16,9,16000,payload(1,16,9,8,.1),false,false),"UNSUPPORTED_FORMAT");
  bad(wav(1,16,1,7999,payload(1,16,1,8,.1),false,false),"UNSUPPORTED_FORMAT");
  byte[] cut=wav(1,16,1,16000,payload(1,16,1,8,.1),false,false);bad(Arrays.copyOf(cut,cut.length-1),"TRUNCATED");
  byte[] wrong=wav(1,16,1,16000,payload(1,16,1,8,.1),false,false);wrong[28]=1;bad(wrong,"MALFORMED");
  bad(wav(1,16,1,16000,new byte[3],false,false),"MALFORMED");
  bad(wav(1,16,1,16000,payload(1,16,1,480001,.0),false,false),"TOO_LONG");
  boolean failed=false;try{WavePcm.decode(new ByteArrayInputStream(new byte[44]),PcmAudio.MAX_FILE_BYTES+1L,(b,n)->{},()->false);}catch(AudioInputException e){failed="TOO_LARGE".equals(e.code);}check(failed,"file size bounded before allocation");
  failed=false;try{byte[] w=wav(1,16,1,16000,new byte[20],false,false);WavePcm.decode(new ByteArrayInputStream(w),w.length,(b,n)->{},()->true);}catch(InterruptedIOException e){failed=true;}check(failed,"cancellation before reading");
  ByteArrayOutputStream chunks=new ByteArrayOutputStream();PcmAudio converter=new PcmAudio(16000,1,16,false,(b,n)->chunks.write(b,0,n),()->false);for(byte b:pcm)converter.accept(new byte[]{b},0,1);converter.finish();check(Arrays.equals(pcm,chunks.toByteArray()),"split byte buffers preserve frame boundaries");
  failed=false;try{PcmAudio c=new PcmAudio(16000,2,16,false,(b,n)->{},()->false);c.accept(new byte[3],0,3);c.finish();}catch(AudioInputException e){failed="TRUNCATED".equals(e.code);}check(failed,"incomplete PCM frame rejected");
  ByteArrayOutputStream tiny=new ByteArrayOutputStream();PcmAudio c=new PcmAudio(8000,1,16,false,(b,n)->tiny.write(b,0,n),()->false);c.accept(new byte[]{0,64},0,2);c.finish();check(tiny.size()==4&&sample(tiny.toByteArray(),1)==16384,"one frame upsampling held boundary");
  failed=false;try{PcmAudio s=new PcmAudio(16000,1,16,false,(b,n)->{throw new IOException("sink-stop");},()->false);s.accept(new byte[3200],0,3200);}catch(IOException e){failed="sink-stop".equals(e.getMessage());}check(failed,"sink failure propagated");
  int[] cancelCount={0};failed=false;try{PcmAudio s=new PcmAudio(16000,1,16,false,(b,n)->{},()->++cancelCount[0]>20);s.accept(new byte[3200],0,3200);}catch(InterruptedIOException e){failed=true;}check(failed,"conversion observes cancellation mid stream");
  ByteArrayOutputStream high=new ByteArrayOutputStream();for(int i=0;i<4800;i++)le(high,Math.round(Math.sin(2*Math.PI*12000*i/48000)*16384),2);
  byte[] filtered=decode(wav(1,16,1,48000,high.toByteArray(),false,false));double energy=0;for(int i=100;i<1500;i++){double s=sample(filtered,i)/32768.0;energy+=s*s;}check(Math.sqrt(energy/1400)<.03,"downsampling suppresses 12kHz alias instead of folding it into speech");
  System.out.println("Audio conversion PASS "+checks+" checks (real production parser/converter; Android codec and ASR not exercised)");
 }
}
