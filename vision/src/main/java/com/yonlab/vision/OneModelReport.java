package com.yonlab.vision;
import java.util.*;

/** Human-readable comparison report. Image strings must contain actual encoded output. */
public final class OneModelReport {
 private OneModelReport(){}
 public static final class Model {
  public String title="",state="",summary="",detail="",modelSha="",image="";
  public long loadMs,inferenceMs,elapsedMs;
  public final List<String[]> metrics=new ArrayList<>();
 }
 public static String state(String raw){if("COMPLETED".equals(raw))return "완료";if(raw!=null&&raw.contains("CANCEL"))return "취소";if(raw!=null&&(raw.contains("ERROR")||raw.contains("FAIL")))return "오류";return raw==null?"":raw;}
 public static String qualityLabel(String key){switch(key){case "CER":case "cer":return "문자 오류율 CER";case "fieldExact":return "필드 일치";case "top1":return "1순위 정답";case "top3":return "상위 3개 정답";case "mrr":return "정답 순위 MRR";case "PCK":case "pck":return "자세 일치 PCK";case "precision":return "정밀도";case "recall":return "재현율";case "f1":return "F1";case "TP":return "정답 검출 TP";case "FP":return "오탐 FP";case "FN":return "미탐 FN";default:return key;}}
 public static String qualityValue(String key,Object value){if(value instanceof Boolean)return (Boolean)value?"일치":"불일치";if(value instanceof Number){double n=((Number)value).doubleValue();if(!Double.isFinite(n))return "";if(Arrays.asList("CER","cer","PCK","pck","precision","recall","f1").contains(key))return String.format(Locale.KOREA,"%.1f%%",100*n);return n==Math.rint(n)?Long.toString((long)n):String.format(Locale.KOREA,"%.3f",n);}return value instanceof String?(String)value:"";}
 public static String html(String feature,String demo,String sha,List<Model> models,String logo){
  return html(feature,demo,sha,models,logo,"");
 }
 public static String html(String feature,String demo,String sha,List<Model> models,String logo,String attribution){
  int ok=0,errors=0;long max=1,fast=Long.MAX_VALUE;for(Model m:models){max=Math.max(max,m.inferenceMs);if("COMPLETED".equals(m.state)){ok++;fast=Math.min(fast,m.inferenceMs);}else if("오류".equals(state(m.state)))errors++;}
  StringBuilder b=OneExportCore.start(feature+" · 모델 비교",demo==null||demo.isEmpty()?"같은 입력으로 실행한 모델별 결과":demo,logo);
  b.append("<section class='overview'><div class='stat'><strong>모델 ").append(models.size()).append("개</strong><span>동일 입력 비교</span></div><div class='stat'><strong>성공 ").append(ok).append(" · 오류 ").append(errors).append("</strong><span>실제 실행 상태</span></div>");
  if(fast!=Long.MAX_VALUE)b.append("<div class='stat'><strong>최단 분석 ").append(fast).append(" ms</strong><span>성공한 모델 중 분석 시간</span></div>");
  b.append("</section><section class='report-grid").append(models.size()==1?" single":models.size()>2?" three":"").append("'>");
  for(Model m:models){String status=state(m.state);b.append("<article class='result-card'><div class='card-top'><h2>").append(OneExportCore.escape(m.title)).append("</h2><span class='badge ").append("완료".equals(status)?"done":"오류".equals(status)?"error":"").append("'>").append(OneExportCore.escape(status)).append("</span></div><p class='summary'>").append(OneExportCore.escape(m.summary)).append("</p>");
   boolean hasImage=OneExportCore.image(m.image,"jpeg")||OneExportCore.image(m.image,"png");b.append("<div class='model-body").append(models.size()==1&&hasImage?" with-image":"").append("'>");if(hasImage)b.append("<div class='model-media'><img class='result-image").append(OneExportCore.image(m.image,"png")?" transparent":"").append("' alt='실제 모델 출력' src='").append(m.image).append("'></div>");b.append("<div class='model-detail'>");
   b.append("<div class='timings'><div><strong>").append(m.loadMs).append(" ms</strong><span>모델 준비</span></div><div><strong>").append(m.inferenceMs).append(" ms</strong><span>분석</span></div><div><strong>").append(m.elapsedMs).append(" ms</strong><span>전체 처리</span></div></div><progress aria-label='분석 시간' max='").append(max).append("' value='").append(Math.max(0,m.inferenceMs)).append("'></progress>");
   if(!m.metrics.isEmpty()){b.append("<dl>");for(String[] metric:m.metrics)if(metric.length>=2)b.append("<dt>").append(OneExportCore.escape(metric[0])).append("</dt><dd>").append(OneExportCore.escape(metric[1])).append("</dd>");b.append("</dl>");}
   if(m.detail!=null&&!m.detail.isEmpty())b.append("<h3>출력 상세</h3><pre>").append(OneExportCore.escape(m.detail)).append("</pre>");
   if(m.modelSha!=null&&!m.modelSha.isEmpty())b.append("<details><summary>모델 근거</summary><small>모델 SHA256</small><pre>").append(OneExportCore.escape(m.modelSha)).append("</pre></details>");
   b.append("</div></div></article>");
  }
  b.append("</section>");if(attribution!=null&&!attribution.isEmpty())b.append("<details class='attribution'><summary>자료 출처·이용 조건</summary><pre>").append(OneExportCore.escape(attribution)).append("</pre></details>");return b.append("<details><summary>동일 입력 · 시험 근거</summary><small>입력 SHA256</small><pre>").append(OneExportCore.escape(sha)).append("</pre></details><footer>AXSL Vision · 시간 막대는 이 입력에서 기록한 분석 시간을 비교합니다.</footer></main></html>").toString();
 }
}
