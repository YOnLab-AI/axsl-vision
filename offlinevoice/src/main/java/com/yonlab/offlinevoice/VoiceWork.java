package com.yonlab.offlinevoice;
import java.util.concurrent.*;
final class VoiceWork {
 private VoiceWork(){}
 static final ThreadPoolExecutor EXECUTOR=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"YOnVoiceWorker");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
}
