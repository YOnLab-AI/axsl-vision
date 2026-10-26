package com.yonlab.vision;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression scenarios from the current-date feature audit; no Android or wall-clock dependency. */
public class CurrentDateAnalyticsTest {
 private static Observations.Face face(int id,float eyes,float yaw,float mouth){
  Observations.Face f=new Observations.Face(.1f,.1f,.8f,.9f);f.trackingId=id;f.leftEye=f.rightEye=eyes;f.yaw=yaw;f.pitch=0;f.mouthOpen=mouth;return f;
 }
 private static Object value(Object snapshot,String name){try{return snapshot.getClass().getField(name).get(snapshot);}catch(ReflectiveOperationException error){throw new AssertionError("Missing observable field: "+name,error);}}
 @Test public void longUnobservedGapCannotCreateDrowsinessOrLookAway(){
  Analytics.Drive drive=new Analytics.Drive();drive.update(List.of(face(1,0,40,.8f)),100);
  Analytics.DriveSnapshot next=drive.update(List.of(face(1,0,40,.8f)),5100);
  assertFalse("A five-second gap is not continuous eye evidence",next.drowsy);assertFalse(next.lookingAway);assertEquals(0,next.closedMs);assertEquals(0,next.lookAwayMs);
 }
 @Test public void knownNewDriverCannotInheritPriorNeutral(){
  Analytics.Drive drive=new Analytics.Drive();drive.update(List.of(face(1,1,35,-1)),100);assertTrue(drive.calibrate());
  assertEquals(5,drive.update(List.of(face(1,1,40,-1)),200).yaw,.001);
  Analytics.DriveSnapshot next=drive.update(List.of(face(2,1,0,-1)),300);
  assertFalse("The neutral belongs to the prior tracked driver",next.calibrated);assertEquals(0,next.yaw,.001);assertEquals(0,next.baselineYaw,.001);
 }
 @Test public void explicitPauseKeepsNeutralAndBlinkCountButBreaksTimers(){
  Analytics.Drive drive=new Analytics.Drive();drive.update(List.of(face(1,1,20,-1)),0);drive.calibrate();drive.update(List.of(face(1,0,20,-1)),100);drive.update(List.of(face(1,1,20,-1)),200);
  drive.pause();Analytics.DriveSnapshot next=drive.update(List.of(face(99,0,20,-1)),5000);
  assertTrue(next.calibrated);assertEquals(0,next.yaw,.001);assertEquals(1,next.blinks);assertFalse(next.drowsy);
 }
 @Test public void mouthUnknownClosedAndOpenDurationAreDistinct(){
  Analytics.Drive drive=new Analytics.Drive();Analytics.DriveSnapshot unknown=drive.update(List.of(face(1,1,0,-1)),0);
  assertEquals(false,value(unknown,"mouthKnown"));assertTrue(Float.isNaN((Float)value(unknown,"mouthRatio")));
  Analytics.DriveSnapshot closed=drive.update(List.of(face(1,1,0,.1f)),100);assertEquals(true,value(closed,"mouthKnown"));assertFalse(closed.mouthOpen);
  drive.update(List.of(face(1,1,0,.7f)),200);Analytics.DriveSnapshot open=drive.update(List.of(face(1,1,0,.8f)),900);
  assertTrue(open.mouthOpen);assertEquals(700L,value(open,"mouthOpenMs"));assertEquals(.8f,(Float)value(open,"mouthRatio"),.0001);
  Analytics.DriveSnapshot gap=drive.update(List.of(face(1,1,0,.8f)),5000);assertEquals(0L,value(gap,"mouthOpenMs"));
  Analytics.DriveSnapshot missing=drive.update(List.of(face(1,1,0,-1)),5100);assertEquals(false,value(missing,"mouthKnown"));assertEquals(0L,value(missing,"mouthOpenMs"));
 }
 @Test public void distinctAnonymousIdsDifferFromRevisitSessions(){
  Analytics.Visitors visitors=new Analytics.Visitors(1000);List<Observations.Face> one=List.of(face(7,1,0,-1));
  visitors.update(one,new long[]{7},0);visitors.update(one,new long[]{7},100);Analytics.VisitorSnapshot repeat=visitors.update(one,new long[]{7},1200);
  assertEquals(2,repeat.unique);assertEquals(2,value(repeat,"sessions"));assertEquals(1,value(repeat,"distinct"));assertEquals(true,value(repeat,"distinctComplete"));
  Analytics.VisitorSnapshot two=visitors.update(List.of(face(7,1,0,-1),face(8,1,0,-1)),new long[]{7,8},1300);
  assertEquals(3,value(two,"sessions"));assertEquals(2,value(two,"distinct"));assertEquals(200,two.totalDwellMs);assertEquals(200f/3,two.meanDwellMs,.001);
 }
 @Test public void resetClearsBothVisitorScopesAndUnknownIdIsNotInvented(){
  Analytics.Visitors visitors=new Analytics.Visitors();Analytics.VisitorSnapshot unknown=visitors.update(List.of(face(-1,1,0,-1)),new long[]{-1},0);
  assertEquals(0,value(unknown,"distinct"));assertEquals(0,value(unknown,"sessions"));assertEquals(1,unknown.untracked);
  visitors.update(List.of(face(1,1,0,-1)),new long[]{1},100);visitors.reset();assertEquals(0,value(visitors.snapshot(),"distinct"));assertEquals(0,value(visitors.snapshot(),"sessions"));
 }
 @Test public void missingFaceOrLongGapDoesNotTransferKnownNeutralToNewTrack(){
  Analytics.Drive drive=new Analytics.Drive();drive.update(List.of(face(1,1,35,-1)),0);drive.calibrate();drive.update(List.of(),100);
  assertFalse(drive.update(List.of(face(2,1,0,-1)),200).calibrated);
  drive.update(List.of(face(2,1,20,-1)),300);drive.calibrate();assertFalse(drive.update(List.of(face(3,1,0,-1)),5000).calibrated);
 }
 @Test public void distinctTrackingIsBoundedAndMarksItsLowerBound(){
  Analytics.Visitors visitors=new Analytics.Visitors(1);List<Observations.Face> faces=List.of(face(1,1,0,-1));
  for(int i=0;i<=Analytics.Visitors.MAX_DISTINCT_IDS;i++)visitors.update(faces,new long[]{i+1},i*2L);
  Analytics.VisitorSnapshot result=visitors.snapshot();assertEquals(Analytics.Visitors.MAX_DISTINCT_IDS,value(result,"distinct"));assertEquals(false,value(result,"distinctComplete"));assertEquals(Analytics.Visitors.MAX_DISTINCT_IDS+1,value(result,"sessions"));
  visitors.reset();assertEquals(true,value(visitors.snapshot(),"distinctComplete"));
 }
}
