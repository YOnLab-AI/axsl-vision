package com.yonlab.vision;

/** Pure CPU policy. Native accelerators are not inferred from device names. */
public final class OneExecutionPolicy {
 private OneExecutionPolicy(){}
 public static int threads(String mode,boolean boardEnabled,int cores,boolean constrained){
  if(constrained)return 1;
  int requested="cpu1".equals(mode)?1:"cpu2".equals(mode)?2:"cpu4".equals(mode)?4:boardEnabled?4:2;
  int available=cores>0?cores:2;
  return requested>=4&&available>=4?4:requested>=2&&available>=2?2:1;
 }
}
