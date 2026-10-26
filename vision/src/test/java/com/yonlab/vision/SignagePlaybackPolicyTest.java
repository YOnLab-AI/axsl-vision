package com.yonlab.vision;

import org.junit.Test;
import static org.junit.Assert.*;

public class SignagePlaybackPolicyTest {
 @Test public void selectingAnItemResumesPausedPlaybackFromItsBeginning(){
  SignagePlaybackPolicy.Selection selected=SignagePlaybackPolicy.playSelection(2,3);
  assertNotNull(selected);assertEquals(2,selected.index);assertTrue(selected.playing);assertEquals(0,selected.videoPosition);
 }
 @Test public void removedOrEmptySelectionsCannotStartPlayback(){
  assertNull(SignagePlaybackPolicy.playSelection(0,0));assertNull(SignagePlaybackPolicy.playSelection(-1,3));assertNull(SignagePlaybackPolicy.playSelection(3,3));
 }
 @Test public void displayStaysAwakeOnlyForForegroundNonemptyPlayback(){
  assertTrue(SignagePlaybackPolicy.keepAwake(true,true,3));
  assertFalse(SignagePlaybackPolicy.keepAwake(false,true,3));
  assertFalse(SignagePlaybackPolicy.keepAwake(true,false,3));
  assertFalse(SignagePlaybackPolicy.keepAwake(true,true,0));
 }
 @Test public void savedAssetsMustBeBundledTemplatesOrLegacyPictures(){
  assertTrue(SignagePlaybackPolicy.bundledAsset("asset:one/signage/welcome.png"));
  assertTrue(SignagePlaybackPolicy.bundledAsset("asset:one/signage/event.png"));
  assertTrue(SignagePlaybackPolicy.bundledAsset("asset:one/signage/offer.png"));
  assertTrue(SignagePlaybackPolicy.bundledAsset("asset:one/media/KO/ko-scene-06.jpg"));
  assertFalse(SignagePlaybackPolicy.bundledAsset("asset:one/signage/../../secret.png"));
  assertFalse(SignagePlaybackPolicy.bundledAsset("asset:one/signage/unknown.png"));
  assertFalse(SignagePlaybackPolicy.bundledAsset(null));
 }
}
