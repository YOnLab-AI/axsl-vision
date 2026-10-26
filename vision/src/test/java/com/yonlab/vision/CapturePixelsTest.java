package com.yonlab.vision;
import java.lang.reflect.*;import java.util.*;
public class CapturePixelsTest{
 static Method convert;static int passed;
 static int[] rgb(byte[] bytes,int w,int h,boolean mirror)throws Exception{try{return(int[])convert.invoke(null,bytes,w,h,mirror);}catch(InvocationTargetException e){throw(Exception)e.getCause();}}
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[]args)throws Exception{
  try{Class<?> c=Class.forName("com.yonlab.vision.CapturePixels");convert=c.getDeclaredMethod("argb",byte[].class,int.class,int.class,boolean.class);convert.setAccessible(true);}catch(ClassNotFoundException|NoSuchMethodException e){throw new AssertionError("Internal landscape capture conversion missing");}
  byte[] pixels={0,0,(byte)255,0,(byte)255,0,(byte)255,0,0,(byte)255,(byte)255,(byte)255,0,0,0,(byte)128,64,32};
  int[] expected={0xffff0000,0xff00ff00,0xff0000ff,0xffffffff,0xff000000,0xff204080};
  check(Arrays.equals(rgb(pixels,3,2,false),expected),"BGR channels or row orientation wrong");passed++;System.out.println("PASS landscape BGR to ARGB preserves rows/channels");
  int[] mirrored={expected[2],expected[1],expected[0],expected[5],expected[4],expected[3]};check(Arrays.equals(rgb(pixels,3,2,true),mirrored),"front mirror mismatch");passed++;System.out.println("PASS front camera exactly one horizontal mirror");
  byte[] before=pixels.clone();int[] independent=rgb(pixels,3,2,false);pixels[0]=11;check(independent[0]==expected[0],"output aliases input");passed++;System.out.println("PASS independent output ownership");
  try{rgb(new byte[18],2,3,false);throw new AssertionError("portrait accepted");}catch(IllegalArgumentException expectedError){}passed++;System.out.println("PASS portrait dimensions rejected");
  try{rgb(new byte[17],3,2,false);throw new AssertionError("truncated accepted");}catch(IllegalArgumentException expectedError){}passed++;System.out.println("PASS incomplete frame rejected");
  System.out.println("RESULT "+passed+" PASS / 0 FAIL (no Android Camera or JPEG executed)");
 }
}