package com.yonlab.vision;
import java.util.*;
/** Visible observations from the current input; absent values are omitted. */
public final class OneFacePresentation {
 private OneFacePresentation(){}
 public static String details(List<Observations.Face> faces){StringBuilder out=new StringBuilder();int index=0;for(Observations.Face f:faces){if(out.length()>0)out.append("\n\n");out.append("얼굴 ").append(++index);if(f.expressionIndex>=0)out.append(" · ").append(ExpressionPolicy.korean(f.expressionIndex)).append(' ').append(Math.round(f.expressionConfidence*100)).append('%');if(Float.isFinite(f.ageYears))out.append(" · 약 ").append(Math.round(f.ageYears)).append("세");angle(out,"고개 좌우",f.yaw);angle(out,"고개 위아래",f.pitch);angle(out,"기울기",f.roll);probability(out,"왼쪽 눈",f.leftEye);probability(out,"오른쪽 눈",f.rightEye);probability(out,"미소",f.smile);if(Float.isFinite(f.leftEyeAspect)&&Float.isFinite(f.rightEyeAspect))out.append("\n눈 비율(EAR) · 왼쪽 ").append(String.format(Locale.ROOT,"%.3f",f.leftEyeAspect)).append(" · 오른쪽 ").append(String.format(Locale.ROOT,"%.3f",f.rightEyeAspect));if(f.landmarkCount>0)out.append("\n관측점 ").append(f.landmarkCount).append("개");if(Float.isFinite(f.mouthOpen)&&f.mouthOpen>=0)out.append("\n입 벌림 ").append(String.format(Locale.ROOT,"%.2f",f.mouthOpen));}return out.toString();}
 private static void angle(StringBuilder text,String label,float value){if(Float.isFinite(value))text.append('\n').append(label).append(' ').append(Math.round(value)).append('°');}
 private static void probability(StringBuilder text,String label,float value){if(Float.isFinite(value)&&value>=0&&value<=1)text.append('\n').append(label).append(' ').append(Math.round(value*100)).append('%');}
}
