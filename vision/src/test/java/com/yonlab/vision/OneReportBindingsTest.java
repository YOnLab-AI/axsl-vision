package com.yonlab.vision;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class OneReportBindingsTest {
 @Test public void aSharedReportRemainsReachableFromEveryExecutedModelsHistory(){
  String path="exports/AXSL-Vision-models-run-1.html";
  List<OneReportBindings.Binding> bindings=OneReportBindings.savedModels(Arrays.asList("mobilenet-v4-fp32","mobilenet-v2-2022"),path);
  assertEquals(2,bindings.size());
  assertEquals("mobilenet-v4-fp32",bindings.get(0).modelId);
  assertEquals("mobilenet-v2-2022",bindings.get(1).modelId);
  for(OneReportBindings.Binding b:bindings){assertEquals(path,b.reportPath);assertEquals("SAVED",b.state);}
 }
 @Test public void repeatedExecutionRowsDoNotCreateDuplicateSavedEntriesForOneModel(){
  List<OneReportBindings.Binding> bindings=OneReportBindings.savedModels(Arrays.asList("yamnet-original","yamnet-classifier","yamnet-original"),"exports/sound.html");
  assertEquals(2,bindings.size());
  assertEquals("yamnet-original",bindings.get(0).modelId);
  assertEquals("yamnet-classifier",bindings.get(1).modelId);
 }
 @Test public void noExecutionCannotInventAnAnonymousOrComparisonModelHistoryEntry(){
  assertTrue(OneReportBindings.savedModels(Collections.emptyList(),"exports/report.html").isEmpty());
  assertTrue(OneReportBindings.savedModels(Arrays.asList("",null),"exports/report.html").isEmpty());
 }
}
