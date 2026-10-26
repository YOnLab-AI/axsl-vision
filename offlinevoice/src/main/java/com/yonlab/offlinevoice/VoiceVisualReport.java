package com.yonlab.offlinevoice;
import java.util.*;

/** Pure presentation of captured voice rows; no recognizer, network or implicit transcript collection. */
public final class VoiceVisualReport {
 private VoiceVisualReport(){}
 public static final class Row {
  public String title="",modelId="",language="",modelLanguage="",datasetGroup="",detectedLanguage="",state="",transcript="",edited="",reference="",inputSha="",sourceSha="",timingScope="",attribution="";
  public boolean referenceVerified,hasEdited;
  public long loadMs,inferenceMs,elapsedMs,durationMs;
  public Double cer,rtf;
 }
 private static String escape(String s){return VoiceReportText.html(s);}
 private static String state(String s){return "COMPLETED".equals(s)?"완료":s.contains("CANCEL")?"취소":s.contains("STOP")?"중지":s.contains("FAIL")||s.contains("ERROR")?"오류":s;}
 public static String html(List<Row> rows,String logo){
  int done=0,failed=0;long fast=Long.MAX_VALUE,max=1;for(Row row:rows){if("COMPLETED".equals(row.state)){done++;fast=Math.min(fast,row.inferenceMs);}else if("오류".equals(state(row.state)))failed++;max=Math.max(max,row.inferenceMs);}
  StringBuilder html=VoiceReportHtml.start(rows.size()>1?"음성 모델 비교":"음성 인식 결과","모델이 읽은 문장, 정답과의 차이, 처리 시간을 함께 확인하세요",logo);
  html.append("<section class='overview'><div class='stat'><strong>모델 ").append(rows.size()).append("개</strong><span>이 보고서의 실행 결과</span></div><div class='stat'><strong>완료 ").append(done).append(" · 오류 ").append(failed).append("</strong><span>실제 실행 상태</span></div>");if(fast!=Long.MAX_VALUE)html.append("<div class='stat'><strong>최단 인식 ").append(fast).append(" ms</strong><span>완료한 모델의 인식 구간</span></div>");html.append("</section><section class='report-grid").append(rows.size()>2?" three":"").append("'>");
  for(Row row:rows){String status=state(row.state);html.append("<article class='result-card'><div class='card-top'><h2>").append(escape(row.title)).append("</h2><span class='badge ").append("완료".equals(status)?"done":"오류".equals(status)?"error":"").append("'>").append(escape(status)).append("</span></div><small>").append(escape(row.language)).append("</small><div class='timings'><div><strong>").append(row.loadMs).append(" ms</strong><span>모델 준비</span></div><div><strong>").append(row.inferenceMs).append(" ms</strong><span>인식</span></div><div><strong>").append(row.elapsedMs).append(" ms</strong><span>전체 처리</span></div></div><progress aria-label='인식 시간' max='").append(max).append("' value='").append(Math.max(0,row.inferenceMs)).append("'></progress>");
   html.append("<dl><dt>음성 길이</dt><dd>").append(String.format(Locale.KOREA,"%.2f초",row.durationMs/1000d)).append("</dd>");if(!row.modelLanguage.isEmpty())html.append("<dt>모델 언어</dt><dd>").append(escape("auto".equals(row.modelLanguage)?"자동 · 다국어":row.modelLanguage)).append("</dd>");if(!row.datasetGroup.isEmpty())html.append("<dt>입력 데이터</dt><dd>").append(escape(row.datasetGroup)).append("</dd>");if(!row.detectedLanguage.isEmpty())html.append("<dt>감지 언어</dt><dd>").append(escape(row.detectedLanguage)).append("</dd>");if(row.rtf!=null&&Double.isFinite(row.rtf)&&row.rtf>=0)html.append("<dt>실시간 비율 RTF</dt><dd>").append(String.format(Locale.KOREA,"%.3f",row.rtf)).append("</dd>");if(row.referenceVerified&&row.cer!=null&&Double.isFinite(row.cer)&&row.cer>=0)html.append("<dt>이 입력의 CER</dt><dd>").append(String.format(Locale.KOREA,"%.1f%%",row.cer*100)).append("</dd>");html.append("</dl><h3>모델 인식 원문</h3><p class='summary'>").append(escape(row.transcript.isEmpty()?"인식된 내용 없음":row.transcript)).append("</p>");
   if(row.hasEdited)html.append("<h3>사용자 편집본</h3><p class='summary'>").append(escape(row.edited)).append("</p>");if(row.referenceVerified)html.append("<h3>공개 데이터 정답</h3><pre>").append(escape(row.reference)).append("</pre>");
   if(!row.attribution.isEmpty())html.append("<details class='attribution'><summary>자료 출처·이용 조건</summary><pre>").append(escape(row.attribution)).append("</pre></details>");html.append("<details><summary>입력·모델 근거</summary><dl><dt>모델</dt><dd>").append(escape(row.modelId)).append("</dd><dt>입력 PCM SHA256</dt><dd>").append(escape(row.inputSha)).append("</dd><dt>원본 파일 SHA256</dt><dd>").append(escape(row.sourceSha)).append("</dd><dt>측정 구간</dt><dd>").append(escape(row.timingScope)).append("</dd></dl></details></article>");
  }
  return html.append("</section><details><summary>측정 기준 보기</summary><p>CER은 정답에 비해 바뀌거나 빠지거나 추가된 문자의 비율입니다. 작은 값일수록 정답 문장과 가깝습니다.</p><p>NFC 정규화 · 공백 제거 · 대소문자와 문장부호 유지 · 문자 단위 편집 거리 / 정답 문자 수.</p><p>RTF는 기록된 인식 시간 / 음성 길이입니다. 모델 준비 시간은 별도 항목입니다.</p></details><footer>AXSL Vision · 음성과 결과를 연결하는 개발 기록</footer></main></html>").toString();
 }
}
