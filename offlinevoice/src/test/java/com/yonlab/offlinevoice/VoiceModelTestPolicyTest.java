package com.yonlab.offlinevoice;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class VoiceModelTestPolicyTest {
 @Test public void deliberateCrossLanguageTestsDoNotCollapseAllToKorean(){
  assertEquals("ALL",VoiceComparisonPolicy.navigationGroup("ALL",true));
  assertEquals("KO",VoiceComparisonPolicy.navigationGroup("ALL",false));
  assertEquals("GO",VoiceComparisonPolicy.navigationGroup("GO",false));
 }
 @Test public void aDifferentLanguageInputDoesNotChangeTheTestModelAvailability(){
  assertEquals("ALL",VoiceComparisonPolicy.inputGroup("ALL","GO",true));
  assertEquals("ALL",VoiceComparisonPolicy.inputGroup("ALL","KO",true));
  assertEquals("GO",VoiceComparisonPolicy.inputGroup("KO","GO",false));
 }
 @Test public void modelTestsNeverFallBackToAnotherEngineWhenASelectionDisappears(){
  List<String> available=Arrays.asList("vosk-ko","whisper-tiny");
  assertTrue(VoiceComparisonPolicy.selection(available,Collections.singletonList("removed"),"vosk-ko",true).isEmpty());
  assertEquals(Collections.singletonList("vosk-ko"),VoiceComparisonPolicy.selection(available,Collections.singletonList("removed"),"vosk-ko",false));
  assertEquals(Collections.singletonList("whisper-tiny"),VoiceComparisonPolicy.selection(available,Collections.singletonList("whisper-tiny"),"vosk-ko",true));
 }
}
