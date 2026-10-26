package com.yonlab.vision;
/** Model-metadata normalization and coordinate inversion, independent of Android. */
final class OneTensorMath {
 private OneTensorMath(){}
 static float efficientNet(int pixel){if(pixel<0||pixel>255)throw new IllegalArgumentException("pixel");return (pixel-127f)/128f;}
 static int quantize(float value,float scale,int zero){if(!Float.isFinite(value)||!Float.isFinite(scale)||scale<=0)throw new IllegalArgumentException("quantization");return Math.max(0,Math.min(255,Math.round(value/scale)+zero));}
 static float coordinate(float normalized,int pad,int resized,int source){if(!Float.isFinite(normalized)||resized<=0||source<=0)throw new IllegalArgumentException("geometry");return (normalized*source-pad)/resized;}
 static int[] cocoToMlKit(){return new int[]{0,2,5,7,8,11,12,13,14,15,16,23,24,25,26,27,28};}
}
