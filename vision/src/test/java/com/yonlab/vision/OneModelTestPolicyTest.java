package com.yonlab.vision;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class OneModelTestPolicyTest {
 @Test public void aNormalWorkspaceExplicitChoiceIsAlsoPreserved(){
  assertTrue(OneModelTestPolicy.keepModel("mlkit-ocr-ko",false));
  assertTrue(OneModelTestPolicy.keepModel("vosk-ko-small-0.22",true));
  assertFalse(OneModelTestPolicy.keepModel(null,false));
  assertFalse(OneModelTestPolicy.keepModel("",false));
 }
 @Test public void choosingAnotherInputCannotReplaceTheTestedModelWithItsLanguageDefault(){
  List<String> models=Arrays.asList("mlkit-ocr-ko","mlkit-ocr-latin");
  assertEquals("mlkit-ocr-ko",OneModelTestPolicy.modelAfterInput("mlkit-ocr-ko","mlkit-ocr-latin",models,true));
  assertEquals("mlkit-ocr-latin",OneModelTestPolicy.modelAfterInput("mlkit-ocr-ko","mlkit-ocr-latin",models,false));
 }
 @Test public void anUnavailableTestedModelIsRejectedInsteadOfSubstituted(){
  try{OneModelTestPolicy.modelAfterInput("removed","default",Collections.singletonList("default"),true);fail("A tested model was silently replaced");}catch(IllegalArgumentException expected){}
 }
 @Test public void speechTestsKeepAllModelAvailabilitySeparateFromTheInputLanguage(){
  assertEquals("ALL",OneModelTestPolicy.launchGroup("speech","GO","KO",true));
  assertEquals("GO",OneModelTestPolicy.launchGroup("speech","GO","KO",false));
  assertEquals("GO",OneModelTestPolicy.launchGroup("ocr","GO","KO",true));
  assertEquals("KO",OneModelTestPolicy.launchGroup("classify","GO","KO",true));
 }
 @Test public void changingInputKeepsOnlyTheDeliberatelySelectedModelsInStableOrder(){
  List<String> available=Arrays.asList("first","second","third");
  assertEquals(Arrays.asList("third","first"),OneModelTestPolicy.selectionAfterInput(available,Arrays.asList("third","removed","first","third")));
  assertTrue(OneModelTestPolicy.selectionAfterInput(available,Collections.emptyList()).isEmpty());
 }
}
