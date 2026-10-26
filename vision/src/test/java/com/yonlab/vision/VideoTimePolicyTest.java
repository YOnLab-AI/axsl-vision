package com.yonlab.vision;
import org.junit.Test;
import static org.junit.Assert.*;
public final class VideoTimePolicyTest {
 @Test public void requestedMediaClockUsesElapsedTime(){assertEquals(1200,VideoTimePolicy.sourceAt(1500,1000,700,2000));}
 @Test public void backwardWallClockCannotReverseMedia(){assertEquals(700,VideoTimePolicy.sourceAt(900,1000,700,2000));}
 @Test public void hugeElapsedCannotOverflowOrPassEnd(){assertEquals(1999,VideoTimePolicy.sourceAt(Long.MAX_VALUE,0,700,2000));}
 @Test public void resumedBaseIsCappedAtLastMillisecond(){assertEquals(1999,VideoTimePolicy.sourceAt(2000,1000,1900,2000));}
 @Test public void landscapeAndPortraitFitWithoutUpscaling(){assertArrayEquals(new int[]{640,360},VideoTimePolicy.fit(1920,1080));assertArrayEquals(new int[]{360,640},VideoTimePolicy.fit(1080,1920));assertArrayEquals(new int[]{320,240},VideoTimePolicy.fit(320,240));}
 @Test(expected=IllegalArgumentException.class)public void zeroDurationRejected(){VideoTimePolicy.validate(0,1920,1080);}
 @Test(expected=IllegalArgumentException.class)public void overTenMinutesRejected(){VideoTimePolicy.validate(600001,1920,1080);}
 @Test(expected=IllegalArgumentException.class)public void missingVideoDimensionsRejected(){VideoTimePolicy.validate(1000,0,1080);}
 @Test(expected=IllegalArgumentException.class)public void unboundedSourcePixelsRejected(){VideoTimePolicy.validate(1000,8192,8192);}
 @Test public void valid4KAndRateBound(){VideoTimePolicy.validate(600000,3840,2160);assertTrue(VideoTimePolicy.INTERVAL_MS>=334);}
 @Test public void nullOnlyAtMetadataTailCanVerifyPreviousFrame(){assertEquals(39920,VideoTimePolicy.terminalRetryAt(40301,40300,39920,81));}
 @Test public void middleAndFirstNullNeverGrantRetry(){assertEquals(-1,VideoTimePolicy.terminalRetryAt(40301,20000,19900,40));assertEquals(-1,VideoTimePolicy.terminalRetryAt(40301,40300,-1,0));assertEquals(-1,VideoTimePolicy.terminalRetryAt(40301,40300,39920,0));}
 @Test public void terminalWindowIsBoundedTo500Milliseconds(){assertEquals(39700,VideoTimePolicy.terminalRetryAt(40301,39801,39700,81));assertEquals(-1,VideoTimePolicy.terminalRetryAt(40301,39800,39700,81));}
 @Test public void previousFrameMustBeRecentAndMonotonic(){assertEquals(38801,VideoTimePolicy.terminalRetryAt(40301,40300,38801,81));assertEquals(-1,VideoTimePolicy.terminalRetryAt(40301,40300,38800,81));assertEquals(-1,VideoTimePolicy.terminalRetryAt(40301,39801,39900,81));}
 @Test public void metadataEndAndInvalidTimestampNeverGrantRetry(){assertEquals(-1,VideoTimePolicy.terminalRetryAt(40301,40301,39920,81));assertEquals(-1,VideoTimePolicy.terminalRetryAt(40301,40300,40301,81));assertEquals(-1,VideoTimePolicy.terminalRetryAt(0,0,0,1));}
 @Test public void shortVideoStillNeedsAtLeastOneVerifiedFrame(){assertEquals(50,VideoTimePolicy.terminalRetryAt(100,99,50,1));assertEquals(-1,VideoTimePolicy.terminalRetryAt(100,99,-1,0));}
}
