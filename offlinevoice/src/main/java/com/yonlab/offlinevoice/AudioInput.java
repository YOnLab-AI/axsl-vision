package com.yonlab.offlinevoice;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.SystemClock;
import android.os.CancellationSignal;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.system.ErrnoException;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

/** User-selected audio is copied into one bounded owned file, then streamed through WAV or a device audio decoder. */
public final class AudioInput {
 private AudioInput(){}
 private static final ThreadPoolExecutor OPENER=new ThreadPoolExecutor(0,2,1,TimeUnit.SECONDS,new SynchronousQueue<>(),r->{Thread t=new Thread(r,"YOnVoiceSourceOpen");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
 private static final ThreadPoolExecutor CLOSER=new ThreadPoolExecutor(0,2,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(4),r->{Thread t=new Thread(r,"YOnVoiceSourceClose");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
 public static final class Operation {
  private final InputCancellation owner=new InputCancellation();private final CancellationSignal signal=new CancellationSignal();private volatile Future<AssetFileDescriptor> pending;private volatile boolean openFinished;
  public void cancel(){owner.markCanceled();Future<?> current=pending;if(current!=null)current.cancel(true);try{CLOSER.execute(()->{try{owner.closeOwned();}catch(IOException ignored){}try{signal.cancel();}catch(RuntimeException ignored){}});}catch(RejectedExecutionException ignored){}}
  private AssetFileDescriptor open(Context context,Uri uri,PcmAudio.Cancel cancellation)throws Exception{
   if(owner.canceled())throw new InterruptedIOException("Canceled");
   try{pending=OPENER.submit(()->{try{AssetFileDescriptor result=context.getContentResolver().openAssetFileDescriptor(uri,"r",signal);owner.attach(result);return result;}finally{openFinished=true;}});}catch(RejectedExecutionException e){openFinished=true;throw new AudioInputException("SOURCE_BUSY",e);}
   try{return InputCancellation.await(pending,15000,cancellation,owner);}finally{if(owner.canceled())cancel();}
  }
  private void check(PcmAudio.Cancel cancellation)throws InterruptedIOException{if(owner.canceled()||cancellation.canceled()||Thread.currentThread().isInterrupted()){cancel();throw new InterruptedIOException("Canceled");}}
  private void close()throws IOException{if(!openFinished){cancel();return;}owner.closeOwned();}
  private Boolean closed(){if(owner.sourceAttached())return Boolean.valueOf(owner.sourceClosed());if(openFinished)return Boolean.TRUE;return null;}
 }
 public static Prepared prepare(Context context,Uri uri,PcmAudio.Cancel cancel,Operation operation)throws Exception{
  if(uri==null||!"content".equals(uri.getScheme()))throw new AudioInputException("SOURCE_UNAVAILABLE");
  PcmAudio.check(cancel);File temp;
  try{temp=File.createTempFile("yon-voice-input-",".audio",context.getCacheDir());}catch(IOException e){throw new AudioInputException("INPUT_STORAGE",e).cleanup("NOT_CREATED",true,true);}
  boolean owned=false;Prepared prepared=null;AudioInputException failure=null;long started=SystemClock.elapsedRealtime();
  try{
   long bytes=0;AssetFileDescriptor source=operation.open(context,uri,cancel);FileDescriptor fd=source.getFileDescriptor();long declared=source.getDeclaredLength();if(declared>PcmAudio.MAX_FILE_BYTES)throw new AudioInputException("TOO_LARGE");
   int mode=Os.fstat(fd).st_mode;
   if(!OsConstants.S_ISREG(mode)&&!OsConstants.S_ISFIFO(mode)&&!OsConstants.S_ISSOCK(mode))throw new AudioInputException("SOURCE_READ",new IOException("Unsupported source descriptor kind"));
   if(OsConstants.S_ISREG(mode)||source.getStartOffset()>0)Os.lseek(fd,source.getStartOffset(),OsConstants.SEEK_SET);
   if(android.os.Build.VERSION.SDK_INT>=30)Api30.nonBlocking(fd);
   // API23-29 has no public fcntl setter. This is the sole reader: poll readiness first,
   // then one read of available bytes (never readFully). Closing the owned descriptor cancels it.
   StructPollfd poll=new StructPollfd();poll.fd=fd;poll.events=(short)(OsConstants.POLLIN|OsConstants.POLLHUP|OsConstants.POLLERR);
   try(FileOutputStream out=new FileOutputStream(temp)){byte[] block=new byte[32768];
    while(declared<0||bytes<declared){operation.check(cancel);if(SystemClock.elapsedRealtime()-started>30000)throw new AudioInputException("SOURCE_TIMEOUT");if(Os.poll(new StructPollfd[]{poll},100)==0)continue;operation.check(cancel);if((poll.revents&OsConstants.POLLNVAL)!=0)throw new AudioInputException("SOURCE_READ");if((poll.revents&(OsConstants.POLLIN|OsConstants.POLLHUP|OsConstants.POLLERR))==0)continue;int n;
     try{n=Os.read(fd,block,0,(int)Math.min(block.length,declared<0?block.length:declared-bytes));}catch(ErrnoException e){if(e.errno==OsConstants.EAGAIN||e.errno==OsConstants.EINTR)continue;throw e;}
     if(n==0)break;operation.check(cancel);bytes+=n;if(bytes>PcmAudio.MAX_FILE_BYTES)throw new AudioInputException("TOO_LARGE");out.write(block,0,n);
    }
   }
   if(declared>=0&&bytes!=declared)throw new AudioInputException("TRUNCATED");
   if(bytes==0)throw new AudioInputException("EMPTY_AUDIO");PcmAudio.check(cancel);prepared=new Prepared(temp,bytes);prepared.inspect(cancel);owned=true;return prepared;
  }catch(Exception e){failure=e instanceof AudioInputException?(AudioInputException)e:new AudioInputException(operation.owner.canceled()||e instanceof InterruptedIOException?"CANCELED":e instanceof SecurityException?"SOURCE_PERMISSION":e instanceof FileNotFoundException?"SOURCE_UNAVAILABLE":"SOURCE_READ",e);throw failure;}
  finally{try{operation.close();}catch(IOException e){if(failure!=null)failure.addSuppressed(e);else if(owned){owned=false;failure=new AudioInputException("SOURCE_CLEANUP",e);}}
   if(prepared!=null){prepared.selectedSourceClosed=operation.closed();prepared.providerOpenFinished=operation.openFinished;}
   if(failure!=null)failure.provider(operation.closed(),operation.openFinished);
   if(!owned){boolean existed=temp.exists();boolean deleteReturned=!existed||temp.delete();boolean remains=temp.exists();String state=remains?"REMAINS":existed&&deleteReturned?"DELETED":"ABSENT";
    Boolean extractor=prepared==null?true:prepared.extractorReleased,decoder=prepared==null?true:prepared.decoderReleased;
    if(failure!=null)failure.cleanup(state,extractor,decoder);
    if(remains){AudioInputException cleanup=new AudioInputException("INPUT_CLEANUP").cleanup(state,extractor,decoder).provider(operation.closed(),operation.openFinished);if(failure!=null)cleanup.addSuppressed(failure);throw cleanup;}
    if(failure!=null&&"SOURCE_CLEANUP".equals(failure.code))throw failure;
  }}
 }
 @android.annotation.TargetApi(30)
 private static final class Api30 {
  static void nonBlocking(FileDescriptor fd)throws ErrnoException{int flags=Os.fcntlInt(fd,OsConstants.F_GETFL,0);Os.fcntlInt(fd,OsConstants.F_SETFL,flags|OsConstants.O_NONBLOCK);}
 }
 public static final class Prepared implements AutoCloseable {
  private final File temp;public final long inputBytes;private boolean wave;
  public String mime="",encoding="";public int sampleRate,channels,bits;public long sourceFrames,outputSamples,clippedFrames,durationMs;
  public boolean extractorReleased=true,decoderReleased=true,tempDeleted;
  public Boolean selectedSourceClosed;public boolean providerOpenFinished;
  public String temporaryInputState="OWNED";
  private Prepared(File temp,long bytes){this.temp=temp;inputBytes=bytes;}
  public String sourceSha256(PcmAudio.Cancel cancel)throws IOException{return SafeModelZip.sha(temp,cancel::canceled);}
  private void inspect(PcmAudio.Cancel cancel)throws Exception{
   byte[] header=new byte[12];int n;try(InputStream in=new FileInputStream(temp)){n=in.read(header);}
   wave=n==12&&header[0]=='R'&&header[1]=='I'&&header[2]=='F'&&header[3]=='F'&&header[8]=='W'&&header[9]=='A'&&header[10]=='V'&&header[11]=='E';
   if(wave){try(InputStream in=new FileInputStream(temp)){WavePcm.Format format=WavePcm.read(in,inputBytes,cancel);mime="audio/wav";sampleRate=format.rate;channels=format.channels;bits=format.bits;encoding=format.floating?"FLOAT32":"PCM"+bits;durationMs=(long)format.bytes*1000/(format.rate*format.channels*(format.bits/8));}}
   else{
    MediaExtractor extractor=new MediaExtractor();extractorReleased=false;
    try{extractor.setDataSource(temp.getAbsolutePath());int track=audioTrack(extractor,cancel);MediaFormat format=extractor.getTrackFormat(track);mime=format.getString(MediaFormat.KEY_MIME);validateDuration(format);}
    catch(AudioInputException|InterruptedIOException e){throw e;}catch(IOException|RuntimeException e){throw new AudioInputException("UNSUPPORTED_CONTAINER",e);}
    finally{extractor.release();extractorReleased=true;}
   }
  }
  private static void validateDuration(MediaFormat format)throws AudioInputException{
   if(format.containsKey("is-encrypted")&&format.getInteger("is-encrypted")!=0)throw new AudioInputException("ENCRYPTED_AUDIO");
   if(format.containsKey(MediaFormat.KEY_DURATION)){long duration=format.getLong(MediaFormat.KEY_DURATION);if(duration<0)throw new AudioInputException("MALFORMED");if(duration>PcmAudio.MAX_SECONDS*1000000L)throw new AudioInputException("TOO_LONG");}
  }
  private static int audioTrack(MediaExtractor extractor,PcmAudio.Cancel cancel)throws Exception{
   int tracks=extractor.getTrackCount();if(tracks<1||tracks>64)throw new AudioInputException("UNSUPPORTED_CONTAINER");
   for(int i=0;i<tracks;i++){PcmAudio.check(cancel);MediaFormat f=extractor.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);if(mime!=null&&mime.startsWith("audio/"))return i;}
   throw new AudioInputException("NO_AUDIO_TRACK");
  }
  public void decode(PcmAudio.Sink sink,PcmAudio.Cancel cancel)throws Exception{
   if(tempDeleted||!temp.isFile())throw new AudioInputException("SOURCE_UNAVAILABLE");
   if(wave){try(InputStream in=new FileInputStream(temp)){remember(WavePcm.decode(in,inputBytes,sink,cancel));}}
   else decodeCodec(sink,cancel);
  }
  private void remember(PcmAudio converter){sampleRate=converter.sourceRate;channels=converter.channels;bits=converter.bits;encoding=converter.floating?"FLOAT32":"PCM"+bits;sourceFrames=converter.inputFrames();outputSamples=converter.outputSamples();clippedFrames=converter.clippedFrames();durationMs=converter.durationMs();}
  private PcmAudio converter(MediaFormat format,PcmAudio.Sink sink,PcmAudio.Cancel cancel)throws Exception{
   if(!format.containsKey(MediaFormat.KEY_SAMPLE_RATE)||!format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))throw new AudioInputException("UNSUPPORTED_FORMAT");
   int pcm=format.containsKey("pcm-encoding")?format.getInteger("pcm-encoding"):AudioFormat.ENCODING_PCM_16BIT;
   int bits;boolean floating=pcm==AudioFormat.ENCODING_PCM_FLOAT;
   if(floating)bits=32;else if(pcm==AudioFormat.ENCODING_PCM_16BIT)bits=16;else if(pcm==AudioFormat.ENCODING_PCM_8BIT)bits=8;else if(pcm==21)bits=24;else if(pcm==22)bits=32;else throw new AudioInputException("UNSUPPORTED_FORMAT");
   return new PcmAudio(format.getInteger(MediaFormat.KEY_SAMPLE_RATE),format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),bits,floating,sink,cancel);
  }
  private void decodeCodec(PcmAudio.Sink sink,PcmAudio.Cancel cancel)throws Exception{
   MediaExtractor extractor=new MediaExtractor();MediaCodec decoder=null;boolean started=false;extractorReleased=false;PcmAudio converter=null;Exception failure=null;long start=SystemClock.elapsedRealtime();
   try{
    extractor.setDataSource(temp.getAbsolutePath());int track=audioTrack(extractor,cancel);extractor.selectTrack(track);MediaFormat input=extractor.getTrackFormat(track);validateDuration(input);String codecMime=input.getString(MediaFormat.KEY_MIME);
    try{decoder=MediaCodec.createDecoderByType(codecMime);decoderReleased=false;}catch(IOException|RuntimeException e){throw new AudioInputException("CODEC_UNAVAILABLE",e);}
    decoder.configure(input,null,null,0);decoder.start();started=true;boolean inputEnded=false,outputEnded=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();byte[] block=new byte[8192];
    while(!outputEnded){
     PcmAudio.check(cancel);if(SystemClock.elapsedRealtime()-start>120000)throw new AudioInputException("DECODE_TIMEOUT");
     if(!inputEnded){int index=decoder.dequeueInputBuffer(10000);if(index>=0){ByteBuffer buffer=decoder.getInputBuffer(index);if(buffer==null)throw new AudioInputException("CODEC_ERROR");buffer.clear();int size=extractor.readSampleData(buffer,0);
       if(size<0){decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnded=true;}
       else{if(size>buffer.capacity())throw new AudioInputException("MALFORMED");long pts=extractor.getSampleTime();decoder.queueInputBuffer(index,0,size,Math.max(0,pts),0);extractor.advance();}
     }}
     int index=decoder.dequeueOutputBuffer(info,10000);
     if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){PcmAudio next=converter(decoder.getOutputFormat(),sink,cancel);if(converter!=null&&(converter.sourceRate!=next.sourceRate||converter.channels!=next.channels||converter.bits!=next.bits||converter.floating!=next.floating))throw new AudioInputException("FORMAT_CHANGED");if(converter==null)converter=next;}
     else if(index>=0){try{
       if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){if(converter==null)converter=converter(decoder.getOutputFormat(),sink,cancel);ByteBuffer buffer=decoder.getOutputBuffer(index);if(buffer==null||info.offset<0||info.size<0||info.offset>buffer.capacity()-info.size)throw new AudioInputException("CODEC_ERROR");buffer.position(info.offset);buffer.limit(info.offset+info.size);while(buffer.hasRemaining()){int n=Math.min(block.length,buffer.remaining());buffer.get(block,0,n);converter.accept(block,0,n);}}
       outputEnded=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
      }finally{decoder.releaseOutputBuffer(index,false);}}
    }
    if(converter==null)throw new AudioInputException("EMPTY_AUDIO");converter.finish();remember(converter);
   }catch(Exception e){failure=e;if(e instanceof AudioInputException||e instanceof InterruptedIOException)throw e;throw new AudioInputException("CODEC_ERROR",e);}
   finally{
    RuntimeException cleanup=null;
    if(decoder!=null){if(started)try{decoder.stop();}catch(RuntimeException ignored){}try{decoder.release();decoderReleased=true;}catch(RuntimeException e){cleanup=e;}}
    try{extractor.release();extractorReleased=true;}catch(RuntimeException e){if(cleanup==null)cleanup=e;else cleanup.addSuppressed(e);}
    if(cleanup!=null){if(failure!=null)failure.addSuppressed(cleanup);else throw new AudioInputException("DECODER_CLEANUP",cleanup);}
   }
  }
  public boolean tempExists(){return temp.exists();}
  @Override public void close()throws AudioInputException{boolean existed=temp.exists();if(existed&&!temp.delete())throw new AudioInputException("INPUT_CLEANUP");boolean remains=temp.exists();tempDeleted=existed&&!remains;temporaryInputState=remains?"REMAINS":existed?"DELETED":"ABSENT";if(remains)throw new AudioInputException("INPUT_CLEANUP");}
 }
}
