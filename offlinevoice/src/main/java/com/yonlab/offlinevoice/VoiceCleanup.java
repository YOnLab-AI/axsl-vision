package com.yonlab.offlinevoice;
/** Carries failed-open cleanup evidence to the caller without replacing the original failure. */
final class VoiceCleanup {
 interface Open<T>{T run()throws Exception;}
 interface Close{void run()throws Exception;}
 static final class Incomplete extends Exception{Incomplete(Throwable cause){super("Native cleanup incomplete",cause);}}
 private VoiceCleanup(){}
 static <T>T guardedOpen(Open<T> open,Close close)throws Exception{try{return open.run();}catch(Exception|LinkageError|OutOfMemoryError failure){try{close.run();}catch(Exception|LinkageError|OutOfMemoryError cleanup){failure.addSuppressed(new Incomplete(cleanup));}throw failure;}}
 static boolean reportedClosed(Throwable failure,boolean previous){for(Throwable detail:failure.getSuppressed())if(detail instanceof Incomplete)return false;return previous;}
}
