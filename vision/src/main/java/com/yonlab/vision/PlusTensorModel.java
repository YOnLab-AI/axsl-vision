package com.yonlab.vision;

import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import org.tensorflow.lite.*;

/** One CPU LiteRT interpreter per owner, float NHWC input and bounded owned output buffers. */
final class PlusTensorModel implements AutoCloseable {
    final String sha;final int width,height;final int[][] shapes;
    private final Interpreter interpreter;private final ByteBuffer input;private final ByteBuffer[] outputs;private final float[][] values;private final int[] pixels;private boolean closed;
    PlusTensorModel(Context context,String name,String expected)throws Exception{
        sha=expected;Interpreter created=new Interpreter(ModelPlusAssets.map(context,"one/modelplus/facepose/"+name,expected,64L*1024*1024),new Interpreter.Options().setNumThreads(OneExecutionSettings.threads(context)));
        try{created.allocateTensors();int[] shape=created.getInputTensor(0).shape();if(created.getInputTensorCount()!=1||created.getInputTensor(0).dataType()!=DataType.FLOAT32||shape.length!=4||shape[0]!=1||shape[3]!=3||shape[1]<32||shape[1]>512||shape[2]<32||shape[2]>512)throw new IOException("추가 모델 입력 규격을 확인하세요");height=shape[1];width=shape[2];pixels=new int[width*height];input=ByteBuffer.allocateDirect(created.getInputTensor(0).numBytes()).order(ByteOrder.nativeOrder());outputs=new ByteBuffer[created.getOutputTensorCount()];shapes=new int[outputs.length][];values=new float[outputs.length][];long total=0;for(int i=0;i<outputs.length;i++){Tensor tensor=created.getOutputTensor(i);if(tensor.dataType()!=DataType.FLOAT32)throw new IOException("추가 모델 출력 형식을 확인하세요");total+=tensor.numBytes();if(total>16*1024*1024)throw new IOException("추가 모델 출력 크기를 확인하세요");outputs[i]=ByteBuffer.allocateDirect(tensor.numBytes()).order(ByteOrder.nativeOrder());shapes[i]=tensor.shape();values[i]=new float[tensor.numBytes()/4];}interpreter=created;}catch(Exception|Error failure){created.close();throw failure;}
    }
    synchronized float[][] run(Bitmap source,Roi roi,float mean,float divisor)throws Exception{
        if(closed||Thread.currentThread().isInterrupted())throw new InterruptedIOException("분석 취소");if(source==null||source.isRecycled())throw new IOException("사진을 확인하세요");
        Bitmap scaled=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);try{
            Canvas canvas=new Canvas(scaled);canvas.drawColor(Color.BLACK);float[] from=roi.corners(),to={0,0,width,0,0,height};Matrix transform=new Matrix();if(!transform.setPolyToPoly(from,0,to,0,3))throw new IOException("모델 입력 영역을 확인하세요");canvas.drawBitmap(source,transform,new Paint(Paint.FILTER_BITMAP_FLAG));scaled.getPixels(pixels,0,width,0,0,width,height);input.clear();for(int pixel:pixels){input.putFloat((((pixel>>>16)&255)-mean)/divisor);input.putFloat((((pixel>>>8)&255)-mean)/divisor);input.putFloat(((pixel&255)-mean)/divisor);}input.rewind();Map<Integer,Object> buffers=new HashMap<>();for(int i=0;i<outputs.length;i++){outputs[i].clear();buffers.put(i,outputs[i]);}interpreter.runForMultipleInputsOutputs(new Object[]{input},buffers);if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("분석 취소");for(int i=0;i<outputs.length;i++){outputs[i].rewind();outputs[i].asFloatBuffer().get(values[i]);}return values;
        }finally{scaled.recycle();}
    }
    public synchronized void close(){if(!closed){closed=true;interpreter.close();}}
    static float sigmoid(float value){if(!Float.isFinite(value))return 0;return(float)(1/(1+Math.exp(-Math.max(-100,Math.min(100,value)))));}
    static final class Roi {
        final float cx,cy,size,angle;final int imageWidth,imageHeight;
        Roi(float x,float y,float side,float radians,int w,int h){cx=x;cy=y;size=side;angle=radians;imageWidth=w;imageHeight=h;if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(side)||!Float.isFinite(radians)||side<2||side>Math.max(w,h)*4f)throw new IllegalArgumentException("입력 영역");}
        static Roi whole(Bitmap image){return new Roi(image.getWidth()/2f,image.getHeight()/2f,Math.max(image.getWidth(),image.getHeight()),0,image.getWidth(),image.getHeight());}
        float[] pixel(float u,float v){double c=Math.cos(angle),s=Math.sin(angle);float x=(u-.5f)*size,y=(v-.5f)*size;return new float[]{cx+(float)(c*x-s*y),cy+(float)(s*x+c*y)};}
        float[] normalized(float u,float v){float[] point=pixel(u,v);point[0]/=imageWidth;point[1]/=imageHeight;return point;}
        float[] corners(){float[] a=pixel(0,0),b=pixel(1,0),c=pixel(0,1);return new float[]{a[0],a[1],b[0],b[1],c[0],c[1]};}
    }
}
