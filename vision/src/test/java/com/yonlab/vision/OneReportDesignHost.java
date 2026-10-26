package com.yonlab.vision;
import java.util.*;
public final class OneReportDesignHost {
 private static int count;private static void check(boolean yes,String why){count++;if(!yes)throw new AssertionError(why);}
 public static void main(String[] args){
  OneModelReport.Model first=new OneModelReport.Model();first.title="모델 <A>";first.state="COMPLETED";first.summary="사람 2명";first.detail="<script>bad()</script>";first.modelSha="a".repeat(64);first.loadMs=12;first.inferenceMs=35;first.elapsedMs=52;first.image="data:image/jpeg;base64,YWJj";first.metrics.add(new String[]{"정답 F1","0.75"});
  OneModelReport.Model second=new OneModelReport.Model();second.title="모델 B";second.state="ERROR";second.summary="모델을 읽지 못함";second.inferenceMs=0;second.image="javascript:alert(1)";
  String html=OneModelReport.html("객체 찾기","<sample>","b".repeat(64),Arrays.asList(first,second),"");
  check(html.contains("모델 &lt;A&gt;")&&!html.contains("<script>"),"model text and details escaped");
  check(html.contains("사람 2명")&&html.contains("정답 F1")&&html.contains("0.75"),"actual output and quality retained");
  check(html.contains("data:image/jpeg;base64,YWJj")&&!html.contains("javascript:"),"only encoded actual image allowed");
  check(html.contains("35 ms")&&html.contains("12 ms"),"actual inference and configuration timing visible");
  check(html.contains("동일 입력")&&html.contains("b".repeat(64)),"encoded input identity retained");
  check(html.contains("grid-template-columns")&&html.contains("@media"),"responsive report grid");
  check(html.contains("connect-src 'none'")&&html.contains("form-action 'none'"),"offline CSP retained");
  check(html.contains("성공한 모델 중 분석 시간")&&!html.contains("정확도 순위"),"timing is not promoted to accuracy");
  check(html.contains("성공 1")&&html.contains("오류 1"),"failed model counted separately");
  check(!html.contains("최단 분석 0 ms"),"failed zero duration cannot win");
  String[] headers={"시각","기능","모델","상태","준비 ms","추론 ms","전체 ms","입력 SHA256","내용"};
  String report=OneExportCore.html("분석 리포트",headers,Collections.singletonList(new String[]{"오늘","사진 분류","V4","완료","10","40","55","c".repeat(64),"실제 요약"}),"");
  check(report.contains("report-grid")&&report.contains("실제 요약")&&report.contains("표로 보기"),"summary cards precede optional technical table");
  check(report.contains("기록 1")&&report.contains("완료 1"),"record metrics described as records");
  check("25.0%".equals(OneModelReport.qualityValue("CER",.25)),"CER uses an explicit percentage");
  check("150.0%".equals(OneModelReport.qualityValue("CER",1.5)),"insertion errors are not clipped at100 percent");
  check("불일치".equals(OneModelReport.qualityValue("top1",false)),"negative control remains a miss");
  check("".equals(OneModelReport.qualityValue("PCK",Double.NaN)),"nonfinite quality remains unmeasured");
  check("어려움 · 흐림".equals(OneDemoText.summary("hard",new String[]{"blur"})),"sample conditions do not claim recognition success");
  first.image="data:image/png;base64,YWJj";html=OneModelReport.html("배경 스튜디오","sample","b".repeat(64),Collections.singletonList(first),"");check(html.contains("result-image transparent")&&html.contains("data:image/png;base64,YWJj"),"PNG output retains transparent preview and its actual bytes");
  html=OneModelReport.html("사진 비교","sample","b".repeat(64),Collections.singletonList(first),"","Author: <script>bad</script> · CC BY-SA 4.0 · https://example.org/source");check(html.contains("자료 출처·이용 조건")&&html.contains("CC BY-SA 4.0")&&html.contains("&lt;script&gt;")&&!html.contains("<script>bad"),"standalone HTML carries safely escaped attribution");
  System.out.println("One report design PASS "+count+" checks");
 }
}
