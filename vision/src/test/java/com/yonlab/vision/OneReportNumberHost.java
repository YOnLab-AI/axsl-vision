package com.yonlab.vision;
public final class OneReportNumberHost {
 private static int checks;
 private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 public static void main(String[] args){
  check("-0.25".equals(OneReportRows.numberValue(-.25,"similarity")),"negative cosine must survive report projection");
  check("-1".equals(OneReportRows.numberValue(-1,"similarity")),"cosine lower bound is a valid result");
  check("0".equals(OneReportRows.numberValue(0,"similarity")),"zero cosine");
  check("0.25".equals(OneReportRows.numberValue(.25,"similarity")),"positive cosine");
  check("".equals(OneReportRows.numberValue(-1,"elapsedMs")),"negative time sentinel stays absent");
  check("".equals(OneReportRows.numberValue(-1,"faces")),"negative count sentinel stays absent");
  check("".equals(OneReportRows.numberValue(Double.NaN,"similarity")),"nonfinite cosine stays absent");
  check("".equals(OneReportRows.numberValue(Double.NEGATIVE_INFINITY,"similarity")),"infinite cosine stays absent");
  check("".equals(OneReportRows.numberValue("-0.25","similarity")),"string does not become a numeric result");
  System.out.println("One report signed metrics PASS "+checks+" checks");
 }
}
