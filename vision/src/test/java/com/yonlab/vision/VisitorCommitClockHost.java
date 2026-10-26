package com.yonlab.vision;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/** Controlled two-writer regression, using the exact production commit-time planning entry point. */
public final class VisitorCommitClockHost {
    private static int checks;
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;}
    private static float[] feature(){float[] vector=new float[128];vector[0]=1;return vector;}
    public static void main(String[] args)throws Exception{
        Observations.Face f=new Observations.Face(0,0,1,1);f.embedding=feature();
        VisitorPolicy.Row original=new VisitorPolicy.Row(7,Registry.MODEL_SIGNATURE,feature(),1000,2000,1000,2);
        VisitorPolicy.Plan stale=VisitorPolicy.plan(Arrays.asList(original),Arrays.asList(f),1500,500,.5f,86400000,1000);
        check(stale.expiredRows==1&&stale.newVisitors==1,"original stale-sample failure reproduced");
        AtomicInteger reads=new AtomicInteger();
        VisitorPolicy.Plan fresh=VisitorPolicy.planAtCommit(Arrays.asList(original),Arrays.asList(f),()->{reads.incrementAndGet();return new VisitorPolicy.Stamp(2500,1500);},.5f,86400000,1000);
        check(fresh.visitorIds[0]==7&&fresh.expiredRows==0,"commit-time clock preserves fresh row");
        check(reads.get()==1&&fresh.commitWall==2500&&fresh.commitElapsed==1500,"one clock pair used for matching and persistence");
        ReentrantLock writer=new ReentrantLock();CountDownLatch observed=new CountDownLatch(1),latestCommitted=new CountDownLatch(1);
        AtomicReference<VisitorPolicy.Row> row=new AtomicReference<>(original);
        AtomicReference<VisitorPolicy.Plan> oldResult=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();
        Thread old=new Thread(()->{try{long staleObservedWall=1500;observed.countDown();latestCommitted.await();writer.lock();try{oldResult.set(VisitorPolicy.planAtCommit(Arrays.asList(row.get()),Arrays.asList(f),()->{if(!writer.isHeldByCurrentThread())throw new AssertionError("clock before writer admission");return new VisitorPolicy.Stamp(3000,2000);},.5f,86400000,1000));}finally{writer.unlock();}}catch(Throwable e){failure.set(e);}},"older-observation");
        old.start();observed.await();writer.lock();try{VisitorPolicy.Plan latest=VisitorPolicy.planAtCommit(Arrays.asList(row.get()),Arrays.asList(f),()->new VisitorPolicy.Stamp(2700,1700),.5f,86400000,1000);row.set(latest.updates.get(0));}finally{writer.unlock();latestCommitted.countDown();}
        old.join(5000);check(!old.isAlive()&&failure.get()==null,"controlled interleaving completes");
        check(oldResult.get().visitorIds[0]==7&&oldResult.get().expiredRows==0,"older observation after newer commit does not expire row");
        check(oldResult.get().updates.get(0).lastWall==3000&&oldResult.get().updates.get(0).lastElapsed==2000,"older observation cannot move persisted last seen backward");
        System.out.println("Visitor commit clock PASS "+checks+" checks (production planning; controlled writer lock, not SQLite)");
    }
}
