package com.yonlab.offlinevoice;
import java.io.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;

public final class InputCancellationHost {
 static int checks;static void check(boolean v,String message){if(!v)throw new AssertionError(message);checks++;}
 static final class Stalled extends InputStream {final CountDownLatch entered=new CountDownLatch(1);boolean closed;public synchronized int read()throws IOException{entered.countDown();while(!closed)try{wait();}catch(InterruptedException ignored){}throw new IOException("closed");}public synchronized void close(){closed=true;notifyAll();}}
 public static void main(String[] args)throws Exception{
  InputCancellation owner=new InputCancellation();Stalled source=new Stalled();owner.attach(source);Thread reader=new Thread(()->{try{source.read();}catch(IOException expected){}});reader.setDaemon(true);reader.start();
  check(source.entered.await(1,TimeUnit.SECONDS),"provider enters blocking read");owner.markCanceled();owner.closeOwned();reader.join(1000);check(!reader.isAlive()&&source.closed&&owner.sourceClosed(),"actual source close unblocks pending read");
  InputCancellation late=new InputCancellation();late.markCanceled();Stalled lateSource=new Stalled();boolean rejected=false;try{late.attach(lateSource);}catch(InterruptedIOException expected){rejected=true;}check(rejected&&lateSource.closed,"late source is closed before use after cancellation");
  InputCancellation failed=new InputCancellation();failed.attach(()->{throw new IOException("close-failed");});boolean closeError=false;try{failed.closeOwned();}catch(IOException expected){closeError=true;}check(closeError&&!failed.sourceClosed(),"failed close is not reported released");
  InputCancellation timeout=new InputCancellation();CountDownLatch release=new CountDownLatch(1);Stalled eventual=new Stalled();FutureTask<Closeable> pending=new FutureTask<>(()->{while(true)try{release.await();break;}catch(InterruptedException ignored){}timeout.attach(eventual);return eventual;});Thread opener=new Thread(pending);opener.setDaemon(true);opener.start();long start=System.nanoTime();boolean timed=false;
  try{InputCancellation.await(pending,80,()->false,timeout);}catch(AudioInputException e){timed="SOURCE_TIMEOUT".equals(e.code);}finally{release.countDown();opener.join(1000);}
  check(timed&&(System.nanoTime()-start)/1000000<1000,"uncooperative provider open does not occupy the caller indefinitely");check(timeout.canceled()&&eventual.closed,"late open completion closes its owned resource");
  FutureTask<String> ready=new FutureTask<>(()->"ready");ready.run();check("ready".equals(InputCancellation.await(ready,1000,()->false,new InputCancellation())),"completed source open transfers normally");
  FutureTask<String> canceled=new FutureTask<>(()->"unused");boolean stop=false;try{InputCancellation.await(canceled,1000,()->true,new InputCancellation());}catch(InterruptedIOException expected){stop=true;}check(stop&&canceled.isCancelled(),"caller cancellation cancels pending open");
  System.out.println("Input cancellation PASS "+checks+" checks; actual close and late-open ownership, no Android provider run");
 }
}
