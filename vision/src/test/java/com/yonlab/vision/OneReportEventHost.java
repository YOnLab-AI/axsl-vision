package com.yonlab.vision;
public final class OneReportEventHost {
 private static int checks;
 private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 public static void main(String[] args){
  check("객체 찾기".equals(OneReportRows.eventFeature("detect","live-detect")),"explicit canonical live feature must survive metadata projection");
  check("QR 읽기·만들기".equals(OneReportRows.eventFeature("qr","tool-qr")),"QR generation feature title survives report projection");
  check("이미지 분류".equals(OneReportRows.eventFeature("classify","unknown-event")),"known declared feature is authoritative");
  check("사람 흐름".equals(OneReportRows.eventFeature("","live-flow")),"legacy live prefix fallback");
  check("QR 읽기·만들기".equals(OneReportRows.eventFeature("bad","tool-qr")),"unknown declaration uses valid legacy event type");
  check("얼굴 분석".equals(OneReportRows.eventFeature("face","unknown-event")),"legacy face declaration maps to canonical faces");
  check("얼굴 분석".equals(OneReportRows.eventFeature("","video-face")),"existing video prefix remains supported");
  check("분석 기록".equals(OneReportRows.eventFeature("bad","unknown-event")),"unknown event remains generic");
  System.out.println("One report event feature PASS "+checks+" checks");
 }
}
