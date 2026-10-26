package com.yonlab.vision;
/** Source timestamps are elapsed media milliseconds, independent of the device calendar. */
public final class VideoTimePolicy {
 public static final long INTERVAL_MS=334,MAX_DURATION_MS=600000,CONTINUITY_GAP_MS=1500;
 public static final long TERMINAL_NULL_WINDOW_MS=500;
 private VideoTimePolicy(){}
 /** A null at the metadata tail may be verified by re-decoding the last successful request.
  * Never grants completion for an empty stream, middle failure, stale prior frame or decoder throw. */
 public static long terminalRetryAt(long duration,long requested,long lastValid,long frames){
  if(duration<1||frames<1||requested<0||requested>=duration||lastValid<0||lastValid>requested||lastValid>=duration)return -1;
  if(duration-requested>TERMINAL_NULL_WINDOW_MS||duration-lastValid>CONTINUITY_GAP_MS)return -1;
  return lastValid;
 }
 public static long sourceAt(long wall,long anchor,long base,long duration){
  if(wall<0||anchor<0||base<0||duration<1)throw new IllegalArgumentException("video time");
  long elapsed=wall>=anchor?wall-anchor:0;
  long remaining=Math.max(0,duration-1-Math.min(base,duration-1));
  return Math.min(base,duration-1)+Math.min(elapsed,remaining);
 }
 public static int[] fit(int width,int height){
  if(width<1||height<1)throw new IllegalArgumentException("video dimensions");
  double scale=Math.min(1,640d/Math.max(width,height));
  return new int[]{Math.max(1,(int)Math.round(width*scale)),Math.max(1,(int)Math.round(height*scale))};
 }
 public static void validate(long duration,int width,int height){
  if(duration<1||duration>MAX_DURATION_MS||width<1||height<1||width>8192||height>8192||(long)width*height>33554432L)throw new IllegalArgumentException("10분 이내의 동영상을 선택하세요");
 }
}

