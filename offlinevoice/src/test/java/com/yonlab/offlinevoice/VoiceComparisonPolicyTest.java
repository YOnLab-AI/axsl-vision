package com.yonlab.offlinevoice;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class VoiceComparisonPolicyTest {
 @Test public void manualEntryAndPickerAreNotConsentToRun(){VoiceComparisonPolicy.StartGate gate=new VoiceComparisonPolicy.StartGate(false,false,true);assertFalse(gate.take());gate=new VoiceComparisonPolicy.StartGate(true,false,true);gate.cancel();assertFalse(gate.take());}
 @Test public void explicitAutomationRunsOnlyOnTheFirstFreshResume(){VoiceComparisonPolicy.StartGate gate=new VoiceComparisonPolicy.StartGate(true,false,true);assertTrue(gate.take());assertFalse(gate.take());assertFalse(new VoiceComparisonPolicy.StartGate(true,true,true).take());assertFalse(new VoiceComparisonPolicy.StartGate(true,false,false).take());}
 @Test public void switchingInputKeepsTheUsersCompatibleSubsetAndAnEmptySelection(){List<String> available=Arrays.asList("vosk-ko","whisper-tiny","whisper-small");assertEquals(Arrays.asList("whisper-tiny","whisper-small"),VoiceComparisonPolicy.selection(available,Arrays.asList("vosk-en","whisper-tiny","whisper-small"),"vosk-ko"));assertEquals(Collections.singletonList("vosk-ko"),VoiceComparisonPolicy.selection(available,Collections.singletonList("vosk-en"),"vosk-ko"));assertTrue(VoiceComparisonPolicy.selection(available,Collections.emptyList(),"vosk-ko").isEmpty());}
 @Test public void singleModelIntentDoesNotSelectAllMultilingualModels(){List<String> available=Arrays.asList("vosk-ko","whisper-tiny","whisper-base","whisper-small");assertEquals(Collections.singletonList("whisper-base"),VoiceComparisonPolicy.initialSelection(available,"whisper-base","vosk-ko",false));assertEquals(Collections.singletonList("vosk-ko"),VoiceComparisonPolicy.initialSelection(available,null,"vosk-ko",false));assertEquals(available,VoiceComparisonPolicy.initialSelection(available,"whisper-base","vosk-ko",true));assertTrue(VoiceComparisonPolicy.initialSelection(available,"vosk-en","vosk-ko",false).isEmpty());}
}
