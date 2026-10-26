package com.yonlab.vision;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class VideoEventPolicyTest {
 private static Class<?> policy(){try{return Class.forName("com.yonlab.vision.VideoEventPolicy");}catch(ClassNotFoundException e){throw new AssertionError("Missing terminal-event policy",e);}}
 private static String state(String reason,boolean finished,boolean clean,boolean error){try{return (String)policy().getDeclaredMethod("terminalState",String.class,boolean.class,boolean.class,boolean.class).invoke(null,reason,finished,clean,error);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 @Test public void onlyActualCleanCompletionIsCompleted(){assertEquals("COMPLETED",state("COMPLETE",true,true,false));assertEquals("STOPPED",state("COMPLETE",false,true,false));}
 @Test public void replacementAndBackgroundRemainCancellation(){assertEquals("CANCELED",state("BACKGROUND",false,true,false));assertEquals("CANCELED",state("RESTART",false,true,false));assertEquals("CANCELED",state("SELECT_OTHER",false,true,false));assertEquals("STOPPED",state("STOPPED",false,true,false));}
 @Test public void decoderOrCleanupFailureCannotBeReportedComplete(){assertEquals("ERROR",state("ERROR",false,true,true));assertEquals("ERROR",state("COMPLETE",true,false,false));assertEquals("ERROR",state("COMPLETE",true,true,true));}
 @Test public void concurrentTerminalCallbacksAdmitOnlyOneWriteAttempt()throws Exception{
  Class<?> gate=Class.forName(policy().getName()+"$Once");Object instance=gate.getDeclaredConstructor().newInstance();Method claim=gate.getDeclaredMethod("claim");AtomicInteger admitted=new AtomicInteger();ExecutorService pool=Executors.newFixedThreadPool(4);
  try{CountDownLatch start=new CountDownLatch(1);java.util.List<Future<?>> tasks=new java.util.ArrayList<>();for(int i=0;i<40;i++)tasks.add(pool.submit(()->{try{start.await();if((Boolean)claim.invoke(instance))admitted.incrementAndGet();}catch(Exception e){throw new RuntimeException(e);}}));start.countDown();for(Future<?> task:tasks)task.get(3,TimeUnit.SECONDS);assertEquals(1,admitted.get());assertEquals(false,claim.invoke(instance));}finally{pool.shutdownNow();}
 }
}
