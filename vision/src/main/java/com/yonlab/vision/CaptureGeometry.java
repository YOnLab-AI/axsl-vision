package com.yonlab.vision;
public final class CaptureGeometry {
    public static final class Frame {public final int width,height;public final int[] argb;public final boolean letterboxed;Frame(int w,int h,int[] a,boolean l){width=w;height=h;argb=a;letterboxed=l;}}
    public static Frame normalize(byte[] bgr,int w,int h,boolean mirror){
        if(bgr==null||w<1||h<1||w>8192||h>8192||(long)w*h>16000000L||(long)w*h*3!=bgr.length)throw new IllegalArgumentException("invalid frame");
        // CameraPipe already supplies upright pixels for the current display rotation.
        // Keep their real aspect ratio; portrait photos must not become landscape pads.
        int[] output=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int at=(y*w+x)*3,target=y*w+(mirror?w-1-x:x);
            output[target]=0xff000000|((bgr[at+2]&255)<<16)|((bgr[at+1]&255)<<8)|(bgr[at]&255);
        }
        return new Frame(w,h,output,false);
    }
}
