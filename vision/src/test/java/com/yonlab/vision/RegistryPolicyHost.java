package com.yonlab.vision;

import java.lang.reflect.*;
import java.util.*;

/** Plain JVM contract checks; exercises production vector/match policy without Android DB mocks. */
public final class RegistryPolicyHost {
    private static Class<?> registry,entry;
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static Object invoke(String name,Class<?>[] types,Object... values)throws Exception{
        try{return registry.getMethod(name,types).invoke(null,values);}
        catch(InvocationTargetException e){if(e.getCause() instanceof Exception)throw (Exception)e.getCause();throw e;}
    }
    private static float[] normalize(float[] v)throws Exception{return (float[])invoke("normalize",new Class<?>[]{float[].class},(Object)v);}
    private static float cosine(float[] a,float[] b)throws Exception{return (Float)invoke("cosine",new Class<?>[]{float[].class,float[].class},a,b);}
    private static void invalid(float[] v)throws Exception{try{normalize(v);throw new AssertionError("invalid vector accepted");}catch(IllegalArgumentException expected){checks++;}}
    private static float[] vector(float a,float b){float[] v=new float[128];v[0]=a;v[1]=b;return v;}
    private static Object row(long id,String name,boolean archived,String signature,float[] embedding)throws Exception{
        return entry.getConstructor(long.class,String.class,boolean.class,long.class,long.class,String.class,float[].class).newInstance(id,name,archived,1L,1L,signature,embedding);
    }
    private static Object choose(List<Object> rows,float[] v,float threshold)throws Exception{return invoke("choose",new Class<?>[]{List.class,float[].class,float.class},rows,v,threshold);}
    private static long id(Object m)throws Exception{return m.getClass().getField("id").getLong(m);}
    private static boolean matched(Object m)throws Exception{return m.getClass().getField("matched").getBoolean(m);}
    public static void main(String[] args)throws Exception{
        try{registry=Class.forName("com.yonlab.vision.Registry");entry=Class.forName("com.yonlab.vision.Registry$Entry");}
        catch(ClassNotFoundException e){throw new AssertionError("Registry contract has not been implemented",e);}
        String signature=(String)registry.getField("MODEL_SIGNATURE").get(null);
        float[] source=vector(3,4),n=normalize(source);
        check(n!=source&&source[0]==3&&source[1]==4,"normalization must copy caller vector");
        check(Math.abs(n[0]-.6f)<1e-6&&Math.abs(n[1]-.8f)<1e-6,"L2 normalization");
        check(Math.abs(cosine(source,source)-1)<1e-6,"same feature cosine");
        check(Math.abs(cosine(vector(1,0),vector(0,1)))<1e-6,"orthogonal feature cosine");
        check(Math.abs(cosine(vector(1,0),vector(-1,0))+1)<1e-6,"opposite feature cosine");
        invalid(null);invalid(new float[127]);invalid(new float[129]);invalid(new float[128]);
        float[] bad=vector(1,0);bad[3]=Float.NaN;invalid(bad);bad[3]=Float.POSITIVE_INFINITY;invalid(bad);
        check(Float.isFinite(normalize(vector(Float.MAX_VALUE,Float.MAX_VALUE))[0]),"large finite values must not overflow norm");
        check(normalize(vector(Float.MIN_VALUE,0))[0]==1f,"small nonzero values keep a valid norm");
        Object low=row(8,"first",false,signature,vector(1,0)),high=row(2,"best",false,signature,vector(0,1));
        check(id(choose(Arrays.asList(low,high),vector(.6f,.8f),.75f))==2,"highest matching candidate wins");
        check(id(choose(Arrays.asList(high,low),vector(.6f,.8f),.75f))==2,"candidate order must not affect match");
        Object tieHigh=row(7,"tie-high",false,signature,vector(1,0)),tieLow=row(1,"tie-low",false,signature,vector(1,0));
        check(id(choose(Arrays.asList(tieHigh,tieLow),vector(1,0),1f))==1,"ties use stable lowest id; threshold is inclusive");
        Object archived=row(3,"archived",true,signature,vector(0,1));
        check(!matched(choose(Arrays.asList(archived),vector(0,1),.3f)),"archived rows excluded");
        Object old=row(4,"other-model",false,"different-model",vector(0,1));
        check(!matched(choose(Arrays.asList(old),vector(0,1),.3f)),"different model features excluded");
        check(!matched(choose(Arrays.asList(low),vector(0,1),.4f)),"below threshold stays unknown");
        check(!matched(choose(Collections.emptyList(),vector(0,1),.4f)),"empty registry stays unknown");
        try{choose(Arrays.asList(low),vector(1,0),Float.NaN);throw new AssertionError("NaN threshold accepted");}catch(IllegalArgumentException expected){checks++;}
        try{choose(Arrays.asList(low),vector(1,0),1.1f);throw new AssertionError("out of range threshold accepted");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("PASS "+checks+" production registry policy checks; Android SQLite and model inference not executed");
    }
}
