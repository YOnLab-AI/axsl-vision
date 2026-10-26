package com.yonlab.vision;
import java.text.Normalizer;import java.util.*;
final class ExpansionQualityMath {
 private ExpansionQualityMath(){}
 static String text(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[\\p{Z}\\p{P}\\s]+","");}
 static double cer(String reference,String hypothesis){int[] a=text(reference).codePoints().toArray(),b=text(hypothesis).codePoints().toArray();if(a.length==0||a.length>16000||b.length>16000)throw new IllegalArgumentException("reference size");int[] previous=new int[b.length+1],next=new int[b.length+1];for(int j=0;j<=b.length;j++)previous[j]=j;for(int i=1;i<=a.length;i++){next[0]=i;for(int j=1;j<=b.length;j++)next[j]=Math.min(previous[j]+1,Math.min(next[j-1]+1,previous[j-1]+(a[i-1]==b[j-1]?0:1)));int[] swap=previous;previous=next;next=swap;}return previous[b.length]/(double)a.length;}
 static double reciprocalRank(List<String> ranked,Set<String> expected){for(int i=0;i<ranked.size();i++)if(expected.contains(ranked.get(i)))return 1d/(i+1);return 0;}
 static boolean sameField(String key,String actual,String expected){if(key.equals("total"))return actual.replaceAll("[^0-9.]","").equals(expected.replaceAll("[^0-9.]",""));return text(actual).equals(text(expected));}
}
