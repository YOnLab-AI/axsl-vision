package com.yonlab.vision;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class ExpansionQualityMathTest {
 @Test public void cerIsUnicodeAndReferenceBound(){assertEquals(0,ExpansionQualityMath.cer("합계 7,500원","합계7500원"),0);assertEquals(.25,ExpansionQualityMath.cer("개발자용","기발자용"),0);assertEquals(2,ExpansionQualityMath.cer("가","가나다"),0);}
 @Test(expected=IllegalArgumentException.class)public void noScoreForEmptyReference(){ExpansionQualityMath.cer("", "model output");}
 @Test public void retrievalRanksOnlyMatchingIds(){assertEquals(.5,ExpansionQualityMath.reciprocalRank(Arrays.asList("wrong","right"),Set.of("right")),0);assertEquals(0,ExpansionQualityMath.reciprocalRank(List.of("wrong"),Set.of("right")),0);}
 @Test public void fieldChecksDoNotUseOcrOutputAsTruth(){assertTrue(ExpansionQualityMath.sameField("total","7,500원","7500"));assertFalse(ExpansionQualityMath.sameField("total","750","7500"));assertFalse(ExpansionQualityMath.sameField("merchant","다른 가게","욘마켓"));}
}
