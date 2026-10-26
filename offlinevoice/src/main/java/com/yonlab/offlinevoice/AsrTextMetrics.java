package com.yonlab.offlinevoice;
import java.text.Normalizer;

/** CER is a verified-reference edit distance, never an inferred confidence or accuracy. */
public final class AsrTextMetrics {
 public static final String POLICY="NFC_REMOVE_UNICODE_WHITESPACE_PRESERVE_CASE_PUNCTUATION_CODEPOINTS_V1";
 private AsrTextMetrics(){}
 public static String normalize(String text){String s=Normalizer.normalize(text==null?"":text,Normalizer.Form.NFC);StringBuilder b=new StringBuilder();s.codePoints().filter(c->!Character.isWhitespace(c)&&!Character.isSpaceChar(c)).forEach(b::appendCodePoint);return b.toString();}
 public static Double cer(String reference,String hypothesis,boolean verified){if(!verified)return null;int[] a=normalize(reference).codePoints().toArray(),b=normalize(hypothesis).codePoints().toArray();if(a.length==0)return null;if(a.length>8192||b.length>8192)throw new IllegalArgumentException("Text comparison limit");int[] prev=new int[b.length+1],next=new int[b.length+1];for(int j=0;j<=b.length;j++)prev[j]=j;for(int i=1;i<=a.length;i++){next[0]=i;for(int j=1;j<=b.length;j++)next[j]=Math.min(Math.min(prev[j]+1,next[j-1]+1),prev[j-1]+(a[i-1]==b[j-1]?0:1));int[] swap=prev;prev=next;next=swap;}return (double)prev[b.length]/a.length;}
}
