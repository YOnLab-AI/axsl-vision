package com.yonlab.vision;
import java.lang.reflect.*;
import java.util.*;
/** Plain-JVM contract/numerical regressions; no Android/model execution claimed. */
public final class OneModelContractHost {
 private static int passed,failed;
 interface Check {void run() throws Exception;}
 static Object call(String type,String name,Class<?>[] sig,Object... args)throws Exception {try{return Class.forName("com.yonlab.vision."+type).getMethod(name,sig).invoke(null,args);}catch(ClassNotFoundException e){throw new AssertionError(type+" contract is not implemented");}catch(InvocationTargetException e){if(e.getCause() instanceof IllegalArgumentException)throw (IllegalArgumentException)e.getCause();throw e;}}
 static String[] models(String f,int sdk)throws Exception{return (String[])call("OneModelPolicy","models",new Class[]{String.class,int.class},f,sdk);}
 static void expect(boolean b){if(!b)throw new AssertionError();}
 static void bad(Check c)throws Exception{try{c.run();throw new AssertionError("invalid value accepted");}catch(IllegalArgumentException expected){}}
 static void check(String n,Check c){try{c.run();passed++;System.out.println("PASS "+n);}catch(Throwable e){failed++;System.out.println("FAIL "+n+" "+e.getClass().getSimpleName()+": "+e.getMessage());}}
 public static void main(String[] a)throws Exception {
  check("measured photo detection and temporal defaults are separate",()->expect(Arrays.equals(models("detect",27),new String[]{"efficientdet-lite0","ssd-mobilenet-v1","yolox-nano"})&&Arrays.equals(models("flow",27),new String[]{"ssd-mobilenet-v1","efficientdet-lite0","yolox-nano"})));
  check("API23 excludes ORT models",()->expect(Arrays.equals(models("detect",23),new String[]{"ssd-mobilenet-v1"})&&models("classify",23).length==0));
  check("verified V4 FP32 default and independent EfficientNet alternatives exclude invalid V4 INT8",()->{expect(Arrays.equals(models("classify",27),new String[]{"mobilenet-v4-fp32","mobilenet-v2-2022","efficientnet-lite0-f32","efficientnet-lite0-i8"})&&Arrays.equals(models("segment",27),new String[]{"pphumanseg-2023","selfie-segmentation","deeplab-v3"}));bad(()->call("OneModelPolicy","validate",new Class[]{String.class,String.class,int.class},"classify","mobilenet-v4-int8",27));});
  check("no research face512 replacement",()->{for(String s:models("compare",27))expect(!s.contains("mbf")&&!s.contains("edgeface"));});
  check("invalid feature is rejected",()->bad(()->models("../detect",27)));
  check("incompatible model rejected",()->bad(()->call("OneModelPolicy","validate",new Class[]{String.class,String.class,int.class},"drive","yolo26n",27)));
  check("stable baseline selected",()->expect("ssd-mobilenet-v1".equals(call("OneModelPolicy","defaultId",new Class[]{String.class,int.class},"flow",27))));
  check("same model cannot be repeated by settings",()->expect(Arrays.equals(models("pose",27),new String[]{"mlkit-pose","mlkit-pose-accurate","movenet-lightning","movenet-thunder"})&&models("qr",27).length==2));
  check("stable softmax handles huge logits",()->{float[] v=(float[])call("OneModelMath","softmax",new Class[]{float[].class},(Object)new float[]{1000,1001});expect(Math.abs(v[1]-.7310586)<1e-5&&Math.abs(v[0]+v[1]-1)<1e-6);});
  check("NaN logits rejected",()->bad(()->call("OneModelMath","softmax",new Class[]{float[].class},(Object)new float[]{0,Float.NaN})));
  check("letterbox reverse retains original geometry",()->{float[] b=(float[])call("OneModelMath","unletterbox",new Class[]{float.class,float.class,float.class,float.class,int.class,int.class,int.class},0f,140f,640f,500f,1280,720,640);expect(Math.abs(b[0])<1e-6&&Math.abs(b[1])<1e-6&&Math.abs(b[2]-1)<1e-6&&Math.abs(b[3]-1)<1e-6);});
  check("invalid geometry rejected",()->bad(()->call("OneModelMath","unletterbox",new Class[]{float.class,float.class,float.class,float.class,int.class,int.class,int.class},0f,0f,Float.NaN,1f,100,100,640)));
  check("HTML result escaped",()->expect("&lt;img &amp; &quot;x&quot;&gt;".equals(call("OneModelMath","html",new Class[]{String.class},"<img & \"x\">"))));
  check("CSV formulas and quotes escaped",()->expect("\"'=SUM(1,2)\"".equals(call("OneModelMath","csv",new Class[]{String.class},"=SUM(1,2)"))));
  check("duplicate boxes count one TP and one FP",()->{long[] n=(long[])call("OneDetectionEval","evaluate",new Class[]{float[][].class,float[][].class,float.class},(Object)new float[][]{{0,0,1,1,.9f,0},{0,0,1,1,.8f,0}},(Object)new float[][]{{0,0,1,1,0}},.5f);expect(Arrays.equals(n,new long[]{1,1,0}));});
  check("class mismatch is FP plus FN",()->{long[] n=(long[])call("OneDetectionEval","evaluate",new Class[]{float[][].class,float[][].class,float.class},(Object)new float[][]{{0,0,1,1,.9f,1}},(Object)new float[][]{{0,0,1,1,0}},.5f);expect(Arrays.equals(n,new long[]{0,1,1}));});
  check("missing predictions retain FN",()->{long[] n=(long[])call("OneDetectionEval","evaluate",new Class[]{float[][].class,float[][].class,float.class},(Object)new float[0][],(Object)new float[][]{{0,0,1,1,0}},.5f);expect(Arrays.equals(n,new long[]{0,0,1}));});
  check("nonfinite ground truth is rejected",()->bad(()->call("OneDetectionEval","evaluate",new Class[]{float[][].class,float[][].class,float.class},(Object)new float[0][],(Object)new float[][]{{0,0,Float.NaN,1,0}},.5f)));
  check("multiline CSV formula remains inert",()->expect(((String)call("OneModelMath","csv",new Class[]{String.class},"=1\nx")).startsWith("\"'")));
  System.out.println("RESULT passed="+passed+" failed="+failed);if(failed>0)System.exit(1);
 }
}
