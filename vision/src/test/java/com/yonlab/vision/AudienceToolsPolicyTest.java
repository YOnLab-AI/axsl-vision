package com.yonlab.vision;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
public final class AudienceToolsPolicyTest {
 private Observations.Face f(float age,float side){Observations.Face f=new Observations.Face(0,0,side,side);f.ageYears=age;return f;}
 @Test public void noFaceOrUnknownAgeDoesNotChooseContent(){assertEquals(-1,AudiencePolicy.select(Collections.emptyList()).group);AudiencePolicy.Summary s=AudiencePolicy.select(Arrays.asList(f(Float.NaN,.5f)));assertEquals(1,s.faceCount);assertEquals(0,s.knownAges);assertEquals(-1,s.group);assertEquals(-1,AudiencePolicy.assetIndex(-1));}
 @Test public void ageBoundariesChooseThreeActualAssetIndices(){assertEquals(2,AudiencePolicy.assetIndex(AudiencePolicy.select(Arrays.asList(f(29.99f,.3f))).group));assertEquals(1,AudiencePolicy.assetIndex(AudiencePolicy.select(Arrays.asList(f(30,.3f))).group));assertEquals(1,AudiencePolicy.assetIndex(AudiencePolicy.select(Arrays.asList(f(49.99f,.3f))).group));assertEquals(0,AudiencePolicy.assetIndex(AudiencePolicy.select(Arrays.asList(f(50,.3f))).group));}
 @Test public void actualMajorityWinsOverSingleLargerFace(){AudiencePolicy.Summary s=AudiencePolicy.select(Arrays.asList(f(60,.9f),f(25,.3f),f(20,.3f)));assertEquals(0,s.group);assertArrayEquals(new int[]{2,0,1},s.groups);assertEquals(3,s.knownAges);}
 @Test public void tiedCountsUseLargestFaceThenStableOrder(){assertEquals(1,AudiencePolicy.select(Arrays.asList(f(20,.3f),f(40,.8f))).group);assertEquals(2,AudiencePolicy.select(Arrays.asList(f(60,.5f),f(20,.5f))).group);}
 @Test public void corruptOrOutOfRangeEstimatesRemainUnknown(){Observations.Face outside=f(20,.3f);outside.left=-.1f;AudiencePolicy.Summary s=AudiencePolicy.select(Arrays.asList(outside,f(-1,.3f),f(121,.3f),f(Float.POSITIVE_INFINITY,.3f)));assertEquals(3,s.faceCount);assertEquals(0,s.knownAges);assertEquals(-1,s.group);}
 @Test public void zeroAgeIsNotReplacedByUnknown(){assertEquals(0,AudiencePolicy.select(Arrays.asList(f(0,.3f))).group);}
 @Test public void toolsAreExplicitAllowlist(){assertTrue(LabRoutes.photoTool("qr"));assertTrue(LabRoutes.photoTool("pose"));assertFalse(LabRoutes.photoTool("file:///sdcard/picture.jpg"));assertFalse(LabRoutes.photoTool(null));}
 @Test public void relatedAppsExcludeSelfAndDuplicates(){assertArrayEquals(new String[]{"face","vis"},LabRoutes.otherRoles(new String[]{"lab","face","face",null,"vis","lab"},"lab"));assertEquals(0,LabRoutes.otherRoles(null,"lab").length);}
}
