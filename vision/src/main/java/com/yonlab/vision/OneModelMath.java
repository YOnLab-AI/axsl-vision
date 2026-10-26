package com.yonlab.vision;
/** Bounded numerical/report helpers shared by real runners and plain JVM tests. */
public final class OneModelMath {
 private OneModelMath(){}
 public static boolean finite(float v){return !Float.isNaN(v)&&!Float.isInfinite(v);}
 public static float[] softmax(float[] logits){if(logits==null||logits.length==0||logits.length>10000)throw new IllegalArgumentException("logits");float max=-Float.MAX_VALUE;for(float f:logits){if(!finite(f))throw new IllegalArgumentException("logits");max=Math.max(max,f);}double total=0;float[] out=new float[logits.length];for(int i=0;i<out.length;i++){out[i]=(float)Math.exp(logits[i]-max);total+=out[i];}for(int i=0;i<out.length;i++)out[i]/=total;return out;}
 public static float[] unletterbox(float l,float t,float r,float b,int width,int height,int side){if(width<1||height<1||side<1||!finite(l)||!finite(t)||!finite(r)||!finite(b)||r<=l||b<=t)throw new IllegalArgumentException("geometry");double scale=Math.min(side/(double)width,side/(double)height);int rw=Math.max(1,(int)Math.round(width*scale)),rh=Math.max(1,(int)Math.round(height*scale));int px=(side-rw)/2,py=(side-rh)/2;return new float[]{clip((l-px)/rw),clip((t-py)/rh),clip((r-px)/rw),clip((b-py)/rh)};}
 public static float clip(float f){return Math.max(0,Math.min(1,f));}
 public static float sigmoid(float f){if(!finite(f))throw new IllegalArgumentException("mask");return (float)(f>=0?1/(1+Math.exp(-f)):Math.exp(f)/(1+Math.exp(f)));}
 public static String html(String s){return (s==null?"":s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
 public static String csv(String s){String v=s==null?"":s;int n=0;while(n<v.length()&&Character.isWhitespace(v.charAt(n)))n++;if(n<v.length()&&"=+@-".indexOf(v.charAt(n))>=0)v="'"+v;return "\""+v.replace("\"","\"\"")+"\"";}
}
