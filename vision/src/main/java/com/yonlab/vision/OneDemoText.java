package com.yonlab.vision;
import java.util.Locale;
/** Sample condition labels describe the input, never an inferred success rate. */
final class OneDemoText {
 private OneDemoText(){}
 static String difficulty(String value){switch(value==null?"":value.toLowerCase(Locale.ROOT)){case "easy":return "쉬움";case "medium":return "보통";case "hard":return "어려움";case "negative":return "대조군";default:return "";}}
 static String condition(String value){if(value==null)return "";switch(value.toLowerCase(Locale.ROOT)){case "blur":case "blurred":return "흐림";case "low-light":case "dark":return "어두움";case "glare":return "반사";case "noise":case "noisy":return "노이즈";case "occlusion":case "occluded":return "가림";case "small-text":return "작은 글자";case "rotated":case "rotation":return "회전";case "clean":return "선명함";default:return value.replace('-',' ');}}
 static String summary(String difficulty,String[] conditions){StringBuilder b=new StringBuilder(difficulty(difficulty));if(conditions!=null)for(int n=0;n<Math.min(2,conditions.length);n++){String label=condition(conditions[n]);if(!label.isEmpty()){if(b.length()>0)b.append(" · ");b.append(label);}}return b.toString();}
}
