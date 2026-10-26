package com.yonlab.offlinevoice;
public final class VoiceReportText {
 private VoiceReportText(){}
 public static String html(String s){return (s==null?"":s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
 public static String csv(String s){String value=s==null?"":s;int first=0;while(first<value.length()&&(Character.isWhitespace(value.charAt(first))||Character.isSpaceChar(value.charAt(first))))first++;if(first<value.length()&&"=+-@".indexOf(value.charAt(first))>=0)value="'"+value;return "\""+value.replace("\"","\"\"")+"\"";}
}
