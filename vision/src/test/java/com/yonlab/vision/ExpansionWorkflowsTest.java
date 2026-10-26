package com.yonlab.vision;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class ExpansionWorkflowsTest {
 @Test public void koreanReceiptKeepsLabeledTotalRatherThanLastTax(){DocumentFields.Fields f=DocumentFields.parse("YOn Mart\n2026-10-25\n소계 10,000원\n합계 ₩11,000\n세액 1,000원");assertEquals("11,000",f.total);assertEquals("KRW",f.currency);assertEquals("2026-10-25",f.date);assertEquals("YOn Mart",f.merchant);}
 @Test public void uppercaseReceiptHeadingIsNotTheMerchant(){DocumentFields.Fields f=DocumentFields.parse("RECEIPT\nBlue Coffee\n2026/10/25\nSUBTOTAL $9.50\nTOTAL $10.45");assertEquals("Blue Coffee",f.merchant);assertEquals("10.45",f.total);assertEquals("USD",f.currency);}
 @Test public void labeledFollowingLineAmountCanBeExtracted(){DocumentFields.Fields f=DocumentFields.parse("동네 가게\n합계\n15,000원\n감사합니다");assertEquals("15,000",f.total);assertTrue(f.totalSource.contains("합계"));}
 @Test public void pricesWithoutATotalLabelAreNeverInventedAsTotal(){DocumentFields.Fields f=DocumentFields.parse("가게 이름\n우유 2,000원\n빵 3,000원");assertEquals("",f.total);}
 @Test public void taxShownOnTheTotalLineIsNotMistakenForTheTotal(){DocumentFields.Fields f=DocumentFields.parse("가게 이름\n합계 12,000원 (세금 1,091원 포함)");assertEquals("12,000",f.total);}
 @Test public void impossibleCalendarDateRemainsAbsent(){assertEquals("",DocumentFields.parse("DATE 2026/02/30\nTOTAL 1200").date);}
 @Test public void cosineIsScaleInvariant(){assertEquals(1,RetrievalMath.cosine(new float[]{1,2,3},new float[]{2,4,6}),1e-7);assertEquals(-1,RetrievalMath.cosine(new float[]{1,0},new float[]{-3,0}),1e-7);}
 @Test(expected=IllegalArgumentException.class) public void incompatibleEncodersAreRejected(){RetrievalMath.cosine(new float[]{1,2},new float[]{1,2,3});}
 @Test(expected=IllegalArgumentException.class) public void zeroEmbeddingIsNotAValidMatch(){RetrievalMath.normalized(new float[]{0,0,0});}
 @Test(expected=IllegalArgumentException.class) public void nonfiniteEmbeddingIsRejected(){RetrievalMath.cosine(new float[]{Float.NaN,1},new float[]{1,2});}
 @Test public void portraitAndLandscapeRetainSamePhysicalJointAngle(){for(int[] size:new int[][]{{1920,1080},{1080,1920}}){int w=size[0],h=size[1];PoseGeometry.Point a=p(11,100f/w,100f/h),b=p(13,300f/w,100f/h),c=p(15,500f/w,300f/h);assertEquals(135,CoachGeometry.angle(a,b,c,w,h),1e-4);}}
 @Test public void missingJointIsNotZeroDegrees(){assertNull(CoachGeometry.angle(p(11,.1f,.2f),p(13,.2f,.2f),null,1920,1080));assertFalse(CoachGeometry.analyze(Arrays.asList(p(11,.1f,.2f),p(12,.8f,.2f)),1920,1080).containsKey("leftKnee"));}
 @Test public void shouldersFacingTheOtherWayAreNotTilted180(){Map<String,Double> a=CoachGeometry.analyze(Arrays.asList(p(11,.8f,.2f),p(12,.1f,.2f)),1920,1080);assertEquals(0,a.get("shoulderTilt"),1e-9);}
 @Test public void meanOfTwoMissingJointsIsAbsent(){assertNull(CoachGeometry.mean(Collections.emptyMap(),"leftHip","rightHip"));}
 @Test public void meanRetainsObservedLeftSide(){assertEquals(130,CoachGeometry.mean(Collections.singletonMap("leftHip",130d),"leftHip","rightHip"),0);}
 @Test public void meanRetainsObservedRightSide(){assertEquals(120,CoachGeometry.mean(Collections.singletonMap("rightHip",120d),"leftHip","rightHip"),0);}
 @Test public void meanOfBothObservedSidesIsTheirMean(){Map<String,Double>a=new HashMap<>();a.put("leftHip",120d);a.put("rightHip",140d);assertEquals(130,CoachGeometry.mean(a,"leftHip","rightHip"),0);}
 @Test public void anEmptyPoseIsAVisibleObservationState(){assertEquals("관측 지점 부족",CoachGeometry.observation(Collections.emptyMap()));}
 @Test public void missingHipAndKneeDoNotInvalidateVisibleUpperBody(){Map<String,Double>a=new HashMap<>();a.put("shoulderTilt",2d);a.put("trunkLean",6d);assertEquals("관절 움직임 관측",CoachGeometry.observation(a));}
 @Test public void lowConfidenceNegativePoseRemainsMissing(){Map<String,Double>a=CoachGeometry.analyze(Arrays.asList(new PoseGeometry.Point(11,.1f,.2f,.1f),new PoseGeometry.Point(12,.9f,.2f,.1f)),1920,1080);assertTrue(a.isEmpty());assertEquals("관측 지점 부족",CoachGeometry.observation(a));}
 private static PoseGeometry.Point p(int id,float x,float y){return new PoseGeometry.Point(id,x,y,.99f);}
}
