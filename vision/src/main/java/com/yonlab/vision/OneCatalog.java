package com.yonlab.vision;
import java.util.*;
/** User-facing task registry. Models and bundled media remain separate typed registries. */
public final class OneCatalog {
 private OneCatalog(){}
 public static final class Feature {public final String id,title,summary,icon,category;public final String[] media;Feature(String i,String t,String s,String n,String c,String...m){id=i;title=t;summary=s;icon=n;category=c;media=m.clone();}}
 public static List<Feature> all(){List<Feature> r=new ArrayList<>();
 r.add(new Feature("detect","객체 찾기","장면 속 대상을 찾고 위치를 확인합니다","search","vision","image","video"));
 r.add(new Feature("classify","이미지 분류","사진 전체가 어떤 종류의 장면·대상인지 분류합니다","image","vision","image"));
 r.add(new Feature("segment","영역 분할","사람·배경 또는 학습된 물체 종류별 영역을 나눕니다","image","vision","image"));
 r.add(new Feature("faces","얼굴 분석","얼굴 위치·특징과 모델이 관측한 표정·상태를 확인합니다","scan_face","people","image","video"));
 r.add(new Feature("compare","얼굴 비교","두 얼굴을 골라 특징을 비교합니다","users","people","image"));
 r.add(new Feature("access","출입 인식","등록된 얼굴 특징과 입력 얼굴의 유사도를 대조합니다","shield_check","people","image","video"));
 r.add(new Feature("drive","드라이버 관측","눈·고개·입의 상태와 시간을 확인합니다","pulse","people","image","video"));
 r.add(new Feature("flow","사람 흐름","기준선을 지나는 움직임을 셉니다","arrow","people","video"));
 r.add(new Feature("vis","방문자 집계","고유 관측과 방문 세션을 구분합니다","users","people","video"));
 r.add(new Feature("pose","자세 보기","사진에서 관절 위치를 찾아 자세를 표시합니다","pulse","people","image"));
 r.add(new Feature("ocr","문자 읽기","사진 속 글자를 읽습니다","image","vision","image"));
 r.add(new Feature("qr","QR 읽기·만들기","코드를 읽거나 새 QR을 만듭니다","credit_card","vision","image"));
 r.add(new Feature("speech","말을 글로","음성을 글로 변환하고 모델별 인식 결과를 비교합니다","volume_2","media","audio"));
 r.add(new Feature("signage","매장·행사 안내판","방문 안내·행사 공지·제품 소개를 사진과 영상으로 반복 재생합니다","image","media","image","video"));
 r.add(new Feature("document","문서·영수증 정리","문서를 바로잡고 날짜·금액과 원문을 정리합니다","credit_card","vision","image"));
 r.add(new Feature("studio","배경 스튜디오","인물의 배경을 바꾸고 흐림 효과를 만듭니다","image","vision","image"));
 r.add(new Feature("search","비슷한 사진 찾기","같은 갤러리에서 기준 사진과 특징이 가까운 장면을 찾습니다","search","vision","image"));
 r.add(new Feature("sound","생활 소리 분석","주변 소리를 시간별로 살펴보고 종류를 비교합니다","volume_2","media","audio"));
 r.add(new Feature("coach","운동 자세 코치","관절 각도와 좌우 자세를 눈으로 확인합니다","pulse","people","image"));
 return Collections.unmodifiableList(r);}
 public static Feature find(String id){if(id==null)return null;for(Feature f:all())if(f.id.equals(id))return f;return null;}
 public static String category(String id){return "vision".equals(id)?"비전 AI":"people".equals(id)?"사람·공간":"음성·미디어";}
 /** ModelBench decodes still images; speech delegates to the audio comparison. */
 public static String[] inputs(Feature f,boolean comparison){if(f==null)return new String[0];if(!comparison)return f.media.clone();if("signage".equals(f.id))return new String[0];return new String[]{("speech".equals(f.id)||"sound".equals(f.id))?"audio":"image"};}
 public static boolean accepts(Feature f,String type){return accepts(f,type,false);}
 public static boolean accepts(Feature f,String type,boolean comparison){String[] kinds=inputs(f,comparison);if(kinds.length==0)return false;if(type==null||"any".equals(type))return true;String k=type.contains("/")?type.substring(0,type.indexOf('/')):type;for(String t:kinds)if(t.equals(k))return true;return false;}
 public static String comparisonGroup(String feature,String demoGroup,String userGroup){return ("speech".equals(feature)||"ocr".equals(feature)||"document".equals(feature))&&("KO".equals(demoGroup)||"GO".equals(demoGroup))?demoGroup:userGroup;}
}

