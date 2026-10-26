package com.yonlab.vision;
import java.math.BigDecimal;import java.math.RoundingMode;
/** Derive the rendered label once from the actual probability; preserve it beside the raw score. */
public final class OneScoreLabels {
 private OneScoreLabels(){}
 public static String percent(float score){if(!Float.isFinite(score)||score<0||score>1)throw new IllegalArgumentException("probability");return new BigDecimal(Float.toString(score)).movePointRight(2).setScale(0,RoundingMode.HALF_UP).toPlainString()+"%";}
 public static String detection(String label,float score){if(label==null||label.trim().isEmpty())throw new IllegalArgumentException("label");return label+" "+percent(score);}
}
