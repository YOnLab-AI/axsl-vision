package com.yonlab.vision;
/** Vectors must belong to the same checked encoder and preprocessing contract. */
final class RetrievalMath {
 static float[] normalized(float[] input){if(input==null||input.length<2)throw new IllegalArgumentException("embedding dimensions");double norm=0;for(float v:input){if(Float.isNaN(v)||Float.isInfinite(v))throw new IllegalArgumentException("embedding numeric");norm+=(double)v*v;}if(norm<1e-20)throw new IllegalArgumentException("embedding norm");float[] out=input.clone();double inverse=1/Math.sqrt(norm);for(int i=0;i<out.length;i++)out[i]=(float)(out[i]*inverse);return out;}
 static double cosine(float[] a,float[] b){if(a==null||b==null||a.length!=b.length||a.length<2)throw new IllegalArgumentException("same encoder dimensions");double dot=0,aa=0,bb=0;for(int n=0;n<a.length;n++){if(Float.isNaN(a[n])||Float.isInfinite(a[n])||Float.isNaN(b[n])||Float.isInfinite(b[n]))throw new IllegalArgumentException("embedding numeric");dot+=(double)a[n]*b[n];aa+=(double)a[n]*a[n];bb+=(double)b[n]*b[n];}if(aa<1e-20||bb<1e-20)throw new IllegalArgumentException("embedding norm");return Math.max(-1,Math.min(1,dot/Math.sqrt(aa*bb)));}
}
