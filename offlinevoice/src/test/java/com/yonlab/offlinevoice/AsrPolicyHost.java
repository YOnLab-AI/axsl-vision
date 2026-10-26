package com.yonlab.offlinevoice;
import java.util.*;
public final class AsrPolicyHost {
 static int checks; static void check(boolean v,String n){if(!v)throw new AssertionError(n);checks++;}
 public static void main(String[] args)throws Exception{
 check(VoiceModelPolicy.defaultId("GO").equals("vosk-en-us-small-0.15"),"GO is English");
 check(VoiceModelPolicy.defaultId("KO").equals("vosk-ko-small-0.22"),"KO default follows final device trial selection");
 check(VoiceModelPolicy.defaultId("ALL").equals("vosk-ko-small-0.22"),"ALL initial model uses Korean Vosk default");
 check(VoiceModelPolicy.resolve("KO",null,"").equals(VoiceModelPolicy.KO),"new KO selection defaults to Vosk");
 check(VoiceModelPolicy.resolve("KO",VoiceModelPolicy.KO,VoiceModelPolicy.MOON).equals(VoiceModelPolicy.KO),"Vosk remains explicitly selectable");
 check(VoiceModelPolicy.resolve("KO",null,VoiceModelPolicy.KO).equals(VoiceModelPolicy.KO),"existing deliberate Vosk preference retained");
 check(VoiceModelPolicy.compatible("GO","vosk-en-us-small-0.15"),"English group compatible");
 check(!VoiceModelPolicy.compatible("GO","vosk-ko-small-0.22"),"KO not accidental GO default");
 check(!VoiceModelPolicy.compatible("KO","unknown"),"invalid model rejected");
 check(VoiceModelPolicy.resolve("GO",null,VoiceModelPolicy.KO).equals(VoiceModelPolicy.EN),"GO initial intent never inherits KO preference");
 check(VoiceModelPolicy.resolve("KO",VoiceModelPolicy.EN,VoiceModelPolicy.KO).equals(VoiceModelPolicy.EN),"explicit cross-language model retained");
 check(VoiceModelPolicy.resolve("KO","",VoiceModelPolicy.MOON).equals(VoiceModelPolicy.KO),"retired Korean model preference safely falls back");
 check(VoiceModelPolicy.resolve("GO",null,VoiceModelPolicy.MOON).equals(VoiceModelPolicy.EN),"retired model preference cannot override English default");
 check(!VoiceModelPolicy.known(VoiceModelPolicy.MOON)&&!VoiceModelPolicy.compatible("ALL",VoiceModelPolicy.MOON),"retired model is not publicly executable");
 boolean retired=false;try{VoiceModelPolicy.resolve("KO",VoiceModelPolicy.MOON,VoiceModelPolicy.KO);}catch(IllegalArgumentException e){retired=true;}check(retired,"explicit retired model is rejected before launch");
 boolean invalid=false;try{VoiceModelPolicy.resolve("GO","unknown",VoiceModelPolicy.EN);}catch(IllegalArgumentException e){invalid=true;}check(invalid,"invalid explicit model rejected before launch");
 check(AsrTextMetrics.cer("\ud55c \uae00","\u1112\u1161\u11ab\uae00",true)==0.0,"NFC and whitespace policy");
 check(AsrTextMetrics.cer("abcd","abxd",true)==0.25,"one substitution");
 check(AsrTextMetrics.cer("a","aaa",true)==2.0,"CER can exceed one");
 check(AsrTextMetrics.cer("\ud83d\ude00a","\ud83d\ude00b",true)==0.5,"Unicode codepoints");
 check(AsrTextMetrics.cer("", "abc",true)==null,"empty reference no CER");
 check(AsrTextMetrics.cer("abc","abc",false)==null,"unverified reference no CER");
 check(AsrTextMetrics.cer("A!","a",true)==1.0,"case punctuation preserved");
 check(VoiceReportText.html("<script>&\"'").equals("&lt;script&gt;&amp;&quot;&#39;"),"HTML escaped");
 check(VoiceReportText.csv("a,\"b\"\n").equals("\"a,\"\"b\"\"\n\""),"CSV quotes newline");
 check(VoiceReportText.csv("=1+2").equals("\"'=1+2\""),"CSV formula blocked");
 check(VoiceReportText.csv("\t+1").startsWith("\"'"),"CSV whitespace formula blocked");
 System.out.println("ASR policy PASS "+checks+" assertions");
 }
}
