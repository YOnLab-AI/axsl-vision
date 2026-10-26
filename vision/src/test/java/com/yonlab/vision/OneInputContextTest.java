package com.yonlab.vision;
import org.junit.Test;
import static org.junit.Assert.*;
/** Language-sensitive inputs must keep the same KO/GO context through launch and comparison. */
public final class OneInputContextTest {
 @Test public void englishOcrSampleSelectsEnglishContext(){assertEquals("GO",OneCatalog.comparisonGroup("ocr","GO","KO"));}
 @Test public void koreanDocumentSampleSelectsKoreanContext(){assertEquals("KO",OneCatalog.comparisonGroup("document","KO","GO"));}
 @Test public void commonSamplePreservesUserLanguage(){assertEquals("GO",OneCatalog.comparisonGroup("ocr","SHARED","GO"));assertEquals("KO",OneCatalog.comparisonGroup("speech","SHARED","KO"));}
 @Test public void languageNeutralVisionPreservesRegionFilter(){assertEquals("ALL",OneCatalog.comparisonGroup("detect","KO","ALL"));}
 @Test public void ownInputPreservesUserLanguage(){assertEquals("GO",OneCatalog.comparisonGroup("document",null,"GO"));}
}
