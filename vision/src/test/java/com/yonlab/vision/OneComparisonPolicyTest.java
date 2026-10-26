package com.yonlab.vision;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class OneComparisonPolicyTest {
 @Test public void languageContextDoesNotBecomeTheModelOrDemoLanguage(){
  assertEquals("GO",OneComparisonPolicy.group("GO"));
  assertEquals("KO",OneComparisonPolicy.group("KO"));
  assertEquals("ALL",OneComparisonPolicy.group("SHARED"));
  assertEquals("ALL",OneComparisonPolicy.group(null));
  assertEquals("글로벌 (GO)",OneComparisonPolicy.groupLabel("GO"));
 }
 @Test public void missingModelUsesTheRegionPreferenceAndExplicitChoiceWins(){
  List<String> available=Arrays.asList("mlkit-ocr-ko","mlkit-ocr-latin","paddle-ocr-v5-ko");
  assertEquals("mlkit-ocr-latin",OneComparisonPolicy.model(null,OneModelPolicy.defaultId("ocr",27,"GO"),available));
  assertEquals("mlkit-ocr-ko",OneComparisonPolicy.model("mlkit-ocr-ko","mlkit-ocr-latin",available));
  assertEquals("mlkit-ocr-latin",OneComparisonPolicy.model("","mlkit-ocr-latin",available));
 }
 @Test public void obsoleteExplicitModelIsRejectedRatherThanSilentlyChanged(){
  try{OneComparisonPolicy.model("removed-model","latin",Arrays.asList("ko","latin"));fail("Invalid explicit model was accepted");}catch(IllegalArgumentException expected){}
 }
 @Test public void restoringASelectionDoesNotSelectAllAgain(){
  List<String> available=Arrays.asList("ko","latin","paddle");
  assertEquals(Arrays.asList("ko","latin","paddle"),OneComparisonPolicy.selection(available,null,"latin",true));
  assertEquals(Collections.singletonList("latin"),OneComparisonPolicy.selection(available,null,"latin",false));
  assertEquals(Collections.singletonList("latin"),OneComparisonPolicy.selection(available,Arrays.asList("removed","latin","latin"),"ko",true));
  assertTrue(OneComparisonPolicy.selection(available,Collections.emptyList(),"latin",true).isEmpty());
 }
 @Test public void manualEntryAndPickerChangesNeverRunOnResume(){
  OneComparisonPolicy.StartGate manual=new OneComparisonPolicy.StartGate(false,false,true);
  assertFalse(manual.take());assertFalse(manual.take());
  OneComparisonPolicy.StartGate picker=new OneComparisonPolicy.StartGate(true,false,false);
  assertFalse(picker.take());
  OneComparisonPolicy.StartGate changed=new OneComparisonPolicy.StartGate(true,false,true);
  changed.cancel();assertFalse(changed.take());
 }
 @Test public void explicitAutomationRunsOnceAndNeverRestartsAfterRecreation(){
  OneComparisonPolicy.StartGate fresh=new OneComparisonPolicy.StartGate(true,false,true);
  assertTrue(fresh.take());assertFalse(fresh.take());
  assertFalse(new OneComparisonPolicy.StartGate(true,true,true).take());
 }
 @Test public void aSingleAnalysisPickerRunsOnceButComparisonPickerWaitsForSelection(){
  OneComparisonPolicy.StartGate gate=new OneComparisonPolicy.StartGate(false,true,false);
  gate.inputSelected(false);assertTrue(gate.take());assertFalse(gate.take());
  gate.inputSelected(true);assertFalse(gate.take());
  gate.inputSelected(false);gate.cancel();assertFalse(gate.take());
 }
 @Test public void completedRestoreRequiresTheSameRunSourceGroupAndTerminalWorker(){
  String sha=String.join("",Collections.nCopies(64,"a"));
  assertTrue(OneComparisonPolicy.canRestore("run-1",sha,"GO","run-1",sha,"GO","COMPLETED",true));
  assertTrue(OneComparisonPolicy.canRestore("run-1",sha,"GO","run-1",sha,"GO","PARTIAL_ERROR",true));
  assertFalse(OneComparisonPolicy.canRestore("run-1",sha,"GO","other",sha,"GO","COMPLETED",true));
  assertFalse(OneComparisonPolicy.canRestore("run-1",sha,"GO","run-1",sha,"KO","COMPLETED",true));
  assertFalse(OneComparisonPolicy.canRestore("run-1",sha,"GO","run-1",sha.replace('a','b'),"GO","COMPLETED",true));
  assertFalse(OneComparisonPolicy.canRestore("run-1",sha,"GO","run-1",sha,"GO","RUNNING",false));
  assertFalse(OneComparisonPolicy.canRestore("run-1",sha,"GO","run-1",sha,"GO","CANCELED",true));
  assertFalse(OneComparisonPolicy.canRestore("run-1","","GO","run-1","","GO","COMPLETED",true));
 }
 @Test public void aSavedRunningJobBecomesAnExplicitInterruptedState(){
  assertEquals("INTERRUPTED",OneComparisonPolicy.restoredPhase(true,false));
  assertEquals("COMPLETED",OneComparisonPolicy.restoredPhase(false,true));
  assertEquals("READY",OneComparisonPolicy.restoredPhase(false,false));
 }
}
