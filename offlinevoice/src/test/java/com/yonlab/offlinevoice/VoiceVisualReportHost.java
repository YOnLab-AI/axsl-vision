package com.yonlab.offlinevoice;
import java.util.*;
public final class VoiceVisualReportHost {
 private static int count;private static void check(boolean yes,String why){count++;if(!yes)throw new AssertionError(why);}
 public static void main(String[] args){
  VoiceVisualReport.Row row=new VoiceVisualReport.Row();row.title="Model <A>";row.modelId="model-a";row.state="COMPLETED";row.transcript="<script>evil()</script> 원문";row.hasEdited=true;row.edited="편집한 문장";row.reference="정답 비공개";row.referenceVerified=false;row.cer=.9;row.inputSha="a".repeat(64);row.durationMs=2400;row.loadMs=10;row.inferenceMs=240;row.elapsedMs=260;row.rtf=.1;
  String html=VoiceVisualReport.html(Collections.singletonList(row),"javascript:alert(1)");
  check(html.contains("&lt;script&gt;")&&!html.contains("<script>"),"transcript must escape executable markup");
  check(!html.contains("javascript:"),"logo URI allowlist");
  check(!html.contains("정답 비공개")&&!html.contains("90.0%"),"unverified reference cannot enter HTML or CER");
  check(html.contains("모델 인식 원문")&&html.contains("편집한 문장")&&html.contains("사용자 편집본"),"editing never replaces original output");
  check(html.contains("240 ms")&&html.contains("2.40초"),"captured times and audio length stay visible");
  check(html.contains("a".repeat(64)),"PCM identity retained");
  check(html.contains("connect-src 'none'")&&html.contains("grid-template-columns"),"offline responsive report");
  row.referenceVerified=true;row.cer=.25;html=VoiceVisualReport.html(Collections.singletonList(row),"");check(html.contains("25.0%")&&html.contains("정답 비공개"),"verified reference and CER appear together");
  row.cer=Double.NaN;html=VoiceVisualReport.html(Collections.singletonList(row),"");check(!html.contains("NaN"),"nonfinite CER is not displayed");
  VoiceVisualReport.Row fail=new VoiceVisualReport.Row();fail.state="FAILED";fail.inferenceMs=0;html=VoiceVisualReport.html(Arrays.asList(row,fail),"");check(html.contains("최단 인식 240 ms")&&!html.contains("최단 인식 0 ms"),"failed zero timing does not win");
  row.modelLanguage="auto";row.datasetGroup="GO";row.detectedLanguage="";html=VoiceVisualReport.html(Collections.singletonList(row),"");check(html.contains("자동 · 다국어")&&html.contains("입력 데이터")&&html.contains("GO")&&!html.contains("감지 언어"),"dataset and model language do not fabricate native detection");
  row.detectedLanguage="ko";html=VoiceVisualReport.html(Collections.singletonList(row),"");check(html.contains("감지 언어")&&html.contains("<dd>ko</dd>"),"actual SDK language remains distinct from GO dataset tag");
  row.attribution="Author <script>x</script> · CC BY 4.0";html=VoiceVisualReport.html(Collections.singletonList(row),"");check(html.contains("자료 출처·이용 조건")&&html.contains("CC BY 4.0")&&!html.contains("<script>x"),"voice standalone report retains escaped dataset attribution");
  System.out.println("Voice visual report PASS "+count+" checks");
 }
}
