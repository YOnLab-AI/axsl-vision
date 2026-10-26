package com.yonlab.vision;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class OneTestSamplesTest {
 private OneTestSamples.Case sample(String id,String group,String media,String difficulty,String... conditions){return new OneTestSamples.Case(id,group,media,difficulty,conditions);}
 @Test public void diverseSetCoversDifficultyAndGroupsBeforeSimilarCopies(){
  List<OneTestSamples.Case> source=Arrays.asList(sample("a","KO","image","easy","clean"),sample("b","KO","image","easy","clean"),sample("c","GO","image","hard","blur"),sample("d","SHARED","image","negative","blank"));
  List<OneTestSamples.Case> selected=OneTestSamples.diverse(source,3);
  Set<String> ids=new HashSet<>();for(OneTestSamples.Case c:selected)ids.add(c.id);
  assertEquals(new HashSet<>(Arrays.asList("a","c","d")),ids);
 }
 @Test public void unspecifiedDifficultyRemainsDistinctAndIsNotEasy(){
  List<OneTestSamples.Case> source=Arrays.asList(sample("clean","KO","audio","","clean"),sample("noise","KO","audio","","noise"),sample("easy","GO","audio","easy"));
  assertFalse(OneTestSamples.matches(source.get(0),"ALL","any","easy",""));
  assertTrue(OneTestSamples.matches(source.get(0),"ALL","any","unspecified",""));
  assertTrue(OneTestSamples.matches(source.get(1),"KO","audio","ALL","noise"));
 }
 @Test public void explicitFiltersDoNotAdmitOtherMediaOrGroups(){
  OneTestSamples.Case c=sample("qr-hard","GO","image","hard","blur");
  assertFalse(OneTestSamples.matches(c,"KO","image","ALL",""));
  assertFalse(OneTestSamples.matches(c,"ALL","audio","ALL",""));
  assertTrue(OneTestSamples.matches(c,"GO","image","hard","qr"));
 }
 @Test public void selectionIsDeterministicUniqueAndBounded(){
  List<OneTestSamples.Case> source=Arrays.asList(sample("a","KO","image","easy"),sample("a","KO","image","easy"),sample("b","GO","video","hard"));
  assertEquals(2,OneTestSamples.diverse(source,6).size());
  assertEquals(OneTestSamples.diverse(source,1).get(0).id,OneTestSamples.diverse(source,1).get(0).id);
  assertTrue(OneTestSamples.diverse(source,0).isEmpty());
  assertTrue(OneTestSamples.diverse(Collections.emptyList(),6).isEmpty());
 }
}
