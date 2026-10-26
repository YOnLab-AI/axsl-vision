package com.yonlab.offlinevoice;
import java.io.IOException;
public final class VoiceCleanupHost {
 static int count;static void check(boolean b,String n){if(!b)throw new AssertionError(n);count++;}
 public static void main(String[] args)throws Exception{
  int[] closed={0};IOException primary=new IOException("injected model open failure");RuntimeException close=new IllegalStateException("injected native close failure");
  try{VoiceCleanup.guardedOpen(()->{throw primary;},()->{closed[0]++;throw close;});throw new AssertionError("open must fail");}catch(IOException actual){check(actual==primary,"original open failure retained");check(closed[0]==1,"actual cleanup attempted once");check(actual.getSuppressed().length==1,"cleanup marker attached");check(actual.getSuppressed()[0].getCause()==close,"actual cleanup cause retained");check(!VoiceCleanup.reportedClosed(actual,true),"caller cannot report model closed after cleanup failure");check(!VoiceCleanup.reportedClosed(actual,false),"existing false remains false");}
  IOException clean=new IOException("injected open failure with successful cleanup");try{VoiceCleanup.guardedOpen(()->{throw clean;},()->closed[0]++);}catch(IOException actual){check(VoiceCleanup.reportedClosed(actual,true),"successful failed-open cleanup remains closed");}
  String value=VoiceCleanup.guardedOpen(()->"opened",()->closed[0]++);check(value.equals("opened")&&closed[0]==2,"successful open retains worker ownership without cleanup");
  System.out.println("Voice cleanup PASS "+count+" assertions; actual injected open + close failures");
 }
}
