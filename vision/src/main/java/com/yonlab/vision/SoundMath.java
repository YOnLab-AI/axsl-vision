package com.yonlab.vision;
import java.io.*;import java.nio.*;import java.security.MessageDigest;import java.util.*;
/** Deterministic audio/window math, independent of Android and model execution. */
public final class SoundMath {
 public static final int RATE=16000,WINDOW=15600,HOP=7680,MAX_SECONDS=30,CLASSES=521;
 private SoundMath(){}
 public interface Cancel {boolean canceled();}
 public static void check(Cancel c)throws InterruptedIOException{if(Thread.currentThread().isInterrupted()||(c!=null&&c.canceled()))throw new InterruptedIOException("canceled");}
 public static int[] starts(int length){if(length<1||length>RATE*MAX_SECONDS)throw new IllegalArgumentException("audio length");int count=1+(int)Math.ceil(Math.max(0,length-WINDOW)/(double)HOP);int[] out=new int[count];for(int i=0;i<count;i++)out[i]=i*HOP;return out;}
 public static float[] window(float[] pcm,int start){if(pcm==null||start<0||start>=pcm.length)throw new IllegalArgumentException("window");float[] out=new float[WINDOW];System.arraycopy(pcm,start,out,0,Math.min(WINDOW,pcm.length-start));return out;}
 /** Bandlimited windowed-sinc interpolation with a fixed32tap Hann kernel. */
 public static float[] resample(float[] input,int sourceRate,Cancel cancel)throws IOException{
  return resample(input,sourceRate,RATE,cancel);
 }
 /** Preserve native bandwidth when a 32 kHz model shares the comparison workspace. */
 public static float[] resample(float[] input,int sourceRate,int targetRate,Cancel cancel)throws IOException{
  if(input==null||input.length<1||sourceRate<4000||sourceRate>192000||(targetRate!=RATE&&targetRate!=32000)||input.length>(long)sourceRate*MAX_SECONDS)throw new IOException("30초 이하 음성을 선택하세요");
  if(sourceRate==targetRate){float[] copy=input.clone();for(int i=0;i<copy.length;i++){if((i&8191)==0)check(cancel);if(!Float.isFinite(copy[i]))throw new IOException("audio sample");}return copy;}
  int n=Math.max(1,(int)Math.round(input.length*(double)targetRate/sourceRate));float[] out=new float[n];double cutoff=Math.min(1,targetRate/(double)sourceRate)*.94;
  for(int i=0;i<n;i++){if((i&1023)==0)check(cancel);double position=i*(double)sourceRate/targetRate,sum=0,weight=0;int center=(int)Math.floor(position);for(int k=center-15;k<=center+16;k++){if(k<0||k>=input.length)continue;float value=input[k];if(!Float.isFinite(value))throw new IOException("audio sample");double delta=position-k,x=Math.PI*cutoff*delta,w=(Math.abs(x)<1e-8?cutoff:Math.sin(x)/(Math.PI*delta))*(.5+.5*Math.cos(Math.PI*delta/16));sum+=value*w;weight+=w;}out[i]=(float)Math.max(-1,Math.min(1,weight==0?0:sum/weight));}return out;
 }
 public static int[] top(float[] scores,int count){if(scores==null||count<1)throw new IllegalArgumentException("scores");Integer[] ids=new Integer[scores.length];for(int i=0;i<ids.length;i++){if(!Float.isFinite(scores[i]))throw new IllegalArgumentException("nonfinite score");ids[i]=i;}Arrays.sort(ids,(a,b)->{int c=Float.compare(scores[b],scores[a]);return c==0?Integer.compare(a,b):c;});int[] out=new int[Math.min(count,ids.length)];for(int i=0;i<out.length;i++)out[i]=ids[i];return out;}
 public static float[] waveform(float[] pcm,int bins){if(pcm==null||pcm.length==0||bins<1)throw new IllegalArgumentException("waveform");float[] out=new float[Math.min(bins,pcm.length)];for(int i=0;i<pcm.length;i++)out[Math.min(out.length-1,(int)((long)i*out.length/pcm.length))]=Math.max(out[Math.min(out.length-1,(int)((long)i*out.length/pcm.length))],Math.abs(pcm[i]));return out;}
 public static String pcmSha(float[] pcm)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");ByteBuffer bytes=ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);for(float f:pcm){if(!Float.isFinite(f))throw new IllegalArgumentException("sample");if(bytes.remaining()<4){digest.update(bytes.array(),0,bytes.position());bytes.clear();}bytes.putFloat(f);}digest.update(bytes.array(),0,bytes.position());return hex(digest.digest());}
 public static String hex(byte[] bytes){StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
 public static String[] csv(String line){List<String> out=new ArrayList<>();StringBuilder value=new StringBuilder();boolean quote=false;for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='"'){if(quote&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append('"');i++;}else quote=!quote;}else if(ch==','&&!quote){out.add(value.toString());value.setLength(0);}else value.append(ch);}if(quote)throw new IllegalArgumentException("csv quote");out.add(value.toString());return out.toArray(new String[0]);}
}
