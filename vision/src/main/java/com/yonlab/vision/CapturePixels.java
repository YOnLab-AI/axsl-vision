package com.yonlab.vision;

/** Raw input is already upright in the board's fixed landscape mounting. */
final class CapturePixels {
    private CapturePixels(){}
    static int[] argb(byte[] bgr,int width,int height,boolean mirrored){
        long pixels=(long)width*height;
        if(width<=height||height<1||pixels>Integer.MAX_VALUE/3||bgr==null||bgr.length!=pixels*3)
            throw new IllegalArgumentException("Invalid landscape BGR frame");
        int[] result=new int[(int)pixels];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int source=(y*width+x)*3,target=y*width+(mirrored?width-1-x:x);
            result[target]=0xff000000|((bgr[source+2]&255)<<16)|((bgr[source+1]&255)<<8)|(bgr[source]&255);
        }
        return result;
    }
}
