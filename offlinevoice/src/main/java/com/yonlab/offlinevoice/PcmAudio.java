package com.yonlab.offlinevoice;
import java.io.InterruptedIOException;

/** Bounded streaming downmix, downsampling low-pass and linear resampling into mono PCM16 at 16kHz. */
public final class PcmAudio {
 public static final int RATE=16000,MAX_SECONDS=30,MAX_OUTPUT_SAMPLES=RATE*MAX_SECONDS,MAX_FILE_BYTES=32*1024*1024;
 public interface Cancel {boolean canceled();}
 public interface Sink {void write(byte[] pcm,int count)throws Exception;}
 public final int sourceRate,channels,bits,frameBytes;public final boolean floating;
 private final Sink sink;private final Cancel cancel;private final byte[] frame,output=new byte[3200];
 private final LowPass[] lowPass;
 private int filled,outputBytes;private long inputFrames,outputSamples,clipped;private double previous;private boolean finished;
 public PcmAudio(int rate,int channels,int bits,boolean floating,Sink sink,Cancel cancel)throws AudioInputException{
  if(rate<8000||rate>192000||channels<1||channels>8||(floating?bits!=32:bits!=8&&bits!=16&&bits!=24&&bits!=32)||sink==null||cancel==null)throw new AudioInputException("UNSUPPORTED_FORMAT");
  this.sourceRate=rate;this.channels=channels;this.bits=bits;this.floating=floating;this.sink=sink;this.cancel=cancel;frameBytes=channels*(bits/8);frame=new byte[frameBytes];
  lowPass=rate>RATE?new LowPass[]{new LowPass(rate,.541196100146197),new LowPass(rate,1.306562964876377)}:new LowPass[0];
 }
 static void check(Cancel cancel)throws InterruptedIOException{if(cancel.canceled()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Canceled");}
 public void accept(byte[] bytes,int offset,int count)throws Exception{
  if(finished||bytes==null||offset<0||count<0||offset>bytes.length-count)throw new AudioInputException("MALFORMED");
  int end=offset+count;while(offset<end){check(cancel);int n=Math.min(frameBytes-filled,end-offset);System.arraycopy(bytes,offset,frame,filled,n);offset+=n;filled+=n;if(filled==frameBytes){convertFrame();filled=0;}}
 }
 private double channel(int offset)throws AudioInputException{
  if(floating){int raw=(frame[offset]&255)|((frame[offset+1]&255)<<8)|((frame[offset+2]&255)<<16)|(frame[offset+3]<<24);float value=Float.intBitsToFloat(raw);if(Float.isNaN(value)||Float.isInfinite(value))throw new AudioInputException("INVALID_PCM");return value;}
  if(bits==8)return ((frame[offset]&255)-128)/128.0;
  if(bits==16)return (short)((frame[offset]&255)|((frame[offset+1]&255)<<8))/32768.0;
  if(bits==24){int raw=(frame[offset]&255)|((frame[offset+1]&255)<<8)|(frame[offset+2]<<16);return raw/8388608.0;}
  int raw=(frame[offset]&255)|((frame[offset+1]&255)<<8)|((frame[offset+2]&255)<<16)|(frame[offset+3]<<24);return raw/2147483648.0;
 }
 private void convertFrame()throws Exception{
  if(inputFrames>=(long)sourceRate*MAX_SECONDS)throw new AudioInputException("TOO_LONG");
  double value=0;for(int i=0;i<channels;i++)value+=channel(i*(bits/8));value/=channels;
  if(value>1||value< -1){clipped++;value=Math.max(-1,Math.min(1,value));}
  for(LowPass filter:lowPass)value=filter.apply(value);
  long current=inputFrames++;
  while(outputSamples*(long)sourceRate<=current*RATE){
   double result=current==0?value:previous+(value-previous)*(outputSamples*(long)sourceRate-(current-1)*RATE)/(double)RATE;
   emit(result);
  }
  previous=value;
 }
 private void emit(double value)throws Exception{
  check(cancel);if(outputSamples>=MAX_OUTPUT_SAMPLES)throw new AudioInputException("TOO_LONG");int q=(int)Math.max(-32768,Math.min(32767,Math.round(value*32768)));
  output[outputBytes++]=(byte)q;output[outputBytes++]=(byte)(q>>8);outputSamples++;if(outputBytes==output.length)flush();
 }
 private void flush()throws Exception{if(outputBytes>0){sink.write(output,outputBytes);outputBytes=0;}}
 public void finish()throws Exception{
  check(cancel);if(finished)return;if(filled!=0)throw new AudioInputException("TRUNCATED");if(inputFrames==0)throw new AudioInputException("EMPTY_AUDIO");
  long expected=(inputFrames*RATE+sourceRate-1)/sourceRate;while(outputSamples<expected)emit(previous);flush();finished=true;
 }
 public long inputFrames(){return inputFrames;}public long outputSamples(){return outputSamples;}public long clippedFrames(){return clipped;}
 public long durationMs(){return inputFrames*1000/sourceRate;}
 /** Fourth-order Butterworth cascade; 6.5kHz cutoff prevents high-rate content folding into speech. */
 private static final class LowPass {
  final double b0,b1,b2,a1,a2;double x1,x2,y1,y2;boolean initialized;
  LowPass(int rate,double q){double w=2*Math.PI*6500/rate,c=Math.cos(w),alpha=Math.sin(w)/(2*q),den=1+alpha;b0=(1-c)/2/den;b1=(1-c)/den;b2=b0;a1=-2*c/den;a2=(1-alpha)/den;}
  double apply(double x){if(!initialized){x1=x2=y1=y2=x;initialized=true;return x;}double y=b0*x+b1*x1+b2*x2-a1*y1-a2*y2;x2=x1;x1=x;y2=y1;y1=y;return y;}
 }
}
