package com.yonlab.vision;

import com.google.zxing.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.*;

/** Offline optical code parsing; decoded content is data and is never executed. */
final class BarcodeHelper {
    static final class Decoded {final String text,format;final int rotation;Decoded(Result r,int rotation){text=r.getText();format=r.getBarcodeFormat().name();this.rotation=rotation;}}
    static Decoded decode(int[] pixels,int width,int height)throws NotFoundException {
        if(pixels==null||width<=0||height<=0||(long)width*height>4000000L||pixels.length!=(long)width*height)throw new IllegalArgumentException("QR image dimensions");
        Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);hints.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);hints.put(DecodeHintType.CHARACTER_SET,"UTF-8");
        int[] current=pixels;int w=width,h=height;
        for(int turn=0;turn<4;turn++){
            MultiFormatReader reader=new MultiFormatReader();
            try{return new Decoded(reader.decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(w,h,current))),hints),turn*90);}
            catch(NotFoundException absent){if(turn==3)throw absent;}
            finally{reader.reset();}
            int[] next=new int[current.length];for(int y=0;y<h;y++)for(int x=0;x<w;x++)next[x*h+(h-1-y)]=current[y*w+x];current=next;int old=w;w=h;h=old;
        }
        throw NotFoundException.getNotFoundInstance();
    }
    static int[] qr(String text,int size)throws WriterException {
        if(text==null||text.length()==0||size<64||size>1024)throw new IllegalArgumentException("QR input");
        Map<EncodeHintType,Object> hints=new EnumMap<>(EncodeHintType.class);hints.put(EncodeHintType.CHARACTER_SET,"UTF-8");hints.put(EncodeHintType.MARGIN,4);
        BitMatrix matrix=new QRCodeWriter().encode(text,BarcodeFormat.QR_CODE,size,size,hints);int[] out=new int[size*size];for(int y=0;y<size;y++)for(int x=0;x<size;x++)out[y*size+x]=matrix.get(x,y)?0xff000000:0xffffffff;return out;
    }
}
