package com.yonlab.offlinevoice;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Cancellation owns and closes the actual source, including one returned after the caller timed out. */
public final class InputCancellation {
 private final AtomicBoolean canceled=new AtomicBoolean(),closing=new AtomicBoolean();private volatile Closeable source;private volatile boolean closed;
 public boolean canceled(){return canceled.get();}public void markCanceled(){canceled.set(true);}public boolean sourceClosed(){return closed;}public boolean sourceAttached(){return source!=null;}
 public void attach(Closeable value)throws IOException{
  if(value==null)throw new AudioInputException("SOURCE_UNAVAILABLE");synchronized(this){if(source!=null&&source!=value)throw new AudioInputException("SOURCE_OWNERSHIP");source=value;}
  if(canceled.get()){closeOwned();throw new InterruptedIOException("Canceled");}
 }
 public void closeOwned()throws IOException{
  Closeable value=source;if(value==null||closed||!closing.compareAndSet(false,true))return;
  try{value.close();closed=true;}finally{closing.set(false);}
 }
 public static <T>T await(Future<T> pending,long timeoutMs,PcmAudio.Cancel cancellation,InputCancellation owner)throws Exception{
  if(timeoutMs<1||timeoutMs>120000)throw new AudioInputException("SOURCE_TIMEOUT");long start=System.nanoTime(),limit=TimeUnit.MILLISECONDS.toNanos(timeoutMs);
  while(true){
   if(cancellation.canceled()||owner.canceled()||Thread.currentThread().isInterrupted()){owner.markCanceled();pending.cancel(true);throw new InterruptedIOException("Canceled");}
   long remaining=limit-(System.nanoTime()-start);if(remaining<=0){owner.markCanceled();pending.cancel(true);throw new AudioInputException("SOURCE_TIMEOUT");}
   try{return pending.get(Math.min(remaining,TimeUnit.MILLISECONDS.toNanos(100)),TimeUnit.NANOSECONDS);}
   catch(TimeoutException ignored){}
   catch(ExecutionException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw(Exception)cause;if(cause instanceof Error)throw(Error)cause;throw new IOException("Source open",cause);}
   catch(CancellationException e){throw new InterruptedIOException("Canceled");}
  }
 }
}
