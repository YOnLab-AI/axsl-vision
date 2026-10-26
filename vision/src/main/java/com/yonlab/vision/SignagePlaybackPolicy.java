package com.yonlab.vision;

/** Playback transitions and asset admission shared by the real player and its regressions. */
final class SignagePlaybackPolicy {
 private SignagePlaybackPolicy(){}
 static final class Selection {
  final int index,videoPosition;final boolean playing;
  Selection(int selected){index=selected;videoPosition=0;playing=true;}
 }
 static Selection playSelection(int selected,int count){return selected>=0&&selected<count?new Selection(selected):null;}
 static boolean keepAwake(boolean active,boolean playing,int count){return active&&playing&&count>0;}
 static boolean bundledAsset(String uri){
  if(uri==null)return false;
  if(uri.equals("asset:one/signage/welcome.png")||uri.equals("asset:one/signage/event.png")||uri.equals("asset:one/signage/offer.png"))return true;
  return uri.matches("asset:one/media/KO/[A-Za-z0-9_-]+[.]jpg");
 }
}
