package com.yonlab.vision;
import org.junit.Test;import static org.junit.Assert.*;
public class OneScoreLabelsTest {
 @Test public void actualReportedScoresRoundForRendering(){assertEquals("person 67%",OneScoreLabels.detection("person",.66796875f));assertEquals("person 63%",OneScoreLabels.detection("person",.62890625f));}
 @Test public void boundariesAndSmallNumbersRemainHonest(){assertEquals("0%",OneScoreLabels.percent(0));assertEquals("100%",OneScoreLabels.percent(1));assertEquals("40%",OneScoreLabels.percent(.4f));assertEquals("1%",OneScoreLabels.percent(.005f));assertEquals("0%",OneScoreLabels.percent(.004f));}
 @Test(expected=IllegalArgumentException.class)public void rejectNaN(){OneScoreLabels.percent(Float.NaN);}
 @Test(expected=IllegalArgumentException.class)public void rejectAboveOne(){OneScoreLabels.percent(1.01f);}
 @Test(expected=IllegalArgumentException.class)public void rejectNegative(){OneScoreLabels.percent(-.01f);}
}
