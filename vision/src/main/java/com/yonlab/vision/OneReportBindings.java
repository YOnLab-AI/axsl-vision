package com.yonlab.vision;

import java.util.*;

/** A shared export belongs to each model actually represented in the report. */
public final class OneReportBindings {
 private OneReportBindings(){}
 public static final class Binding {
  public final String modelId,reportPath,state="SAVED";
  private Binding(String modelId,String reportPath){this.modelId=modelId;this.reportPath=reportPath;}
 }
 public static List<Binding> savedModels(Collection<String> executedModels,String reportPath){
  LinkedHashSet<String> ids=new LinkedHashSet<>();
  for(String id:executedModels)if(id!=null&&!id.isEmpty())ids.add(id);
  List<Binding> bindings=new ArrayList<>();for(String id:ids)bindings.add(new Binding(id,reportPath));
  return Collections.unmodifiableList(bindings);
 }
}
