package com.yonlab.vision;
import java.util.concurrent.atomic.AtomicBoolean;

/** Terminal event classification and at-most-one write attempt, independent of the latest UI owner. */
final class VideoEventPolicy {
 private VideoEventPolicy(){}
 static String terminalState(String reason,boolean finished,boolean ownerClosed,boolean error){
  if(error||!ownerClosed||"ERROR".equals(reason))return "ERROR";
  if(finished&&"COMPLETE".equals(reason))return "COMPLETED";
  if("BACKGROUND".equals(reason)||"DESTROYED".equals(reason)||"RESTART".equals(reason)||"SELECT_OTHER".equals(reason))return "CANCELED";
  return "STOPPED";
 }
 static final class Once {
  private final AtomicBoolean claimed=new AtomicBoolean();
  boolean claim(){return claimed.compareAndSet(false,true);}
  boolean attempted(){return claimed.get();}
 }
}
