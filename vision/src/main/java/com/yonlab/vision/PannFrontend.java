package com.yonlab.vision;

import java.io.*;
import java.nio.*;

/** PANNs 32 kHz frontend. The published checkpoint supplies the Hann and mel weights. */
final class PannFrontend {
 static final int RATE=32000,WINDOW=31200,HOP=15360,FFT=1024,FFT_HOP=320,BINS=64,FRAMES=98;
 static final int WEIGHT_BYTES=(FFT+(FFT/2+1)*BINS)*4;
 private final float[] hann=new float[FFT],mel=new float[(FFT/2+1)*BINS];
 private final int[][] melBins=new int[BINS][];
 private final float[][] melWeights=new float[BINS][];
 private final int[] reverse=new int[FFT];
 private final double[] cosine=new double[FFT/2],sine=new double[FFT/2];
 PannFrontend(File weights)throws IOException{
  if(weights.length()!=WEIGHT_BYTES)throw new IOException("소리 전처리 크기를 확인하세요");
  ByteBuffer bytes=ByteBuffer.allocate(WEIGHT_BYTES).order(ByteOrder.LITTLE_ENDIAN);
  try(DataInputStream in=new DataInputStream(new FileInputStream(weights))){in.readFully(bytes.array());if(in.read()!=-1)throw new IOException("소리 전처리 형식");}
  for(int i=0;i<hann.length;i++){hann[i]=bytes.getFloat();if(!Float.isFinite(hann[i])||hann[i]<0||hann[i]>1.00001f)throw new IOException("소리 창 함수");}
  for(int i=0;i<mel.length;i++){mel[i]=bytes.getFloat();if(!Float.isFinite(mel[i])||mel[i]<0)throw new IOException("소리 멜 필터");}
  // Triangular mel filters are sparse. Retain every nonzero publisher coefficient.
  for(int band=0;band<BINS;band++){int count=0;for(int k=0;k<=FFT/2;k++)if(mel[k*BINS+band]!=0)count++;if(count==0)throw new IOException("소리 멜 필터 대역");melBins[band]=new int[count];melWeights[band]=new float[count];int index=0;for(int k=0;k<=FFT/2;k++)if(mel[k*BINS+band]!=0){melBins[band][index]=k;melWeights[band][index++]=mel[k*BINS+band];}}
  for(int i=0;i<FFT;i++)reverse[i]=Integer.reverse(i)>>>22;
  for(int i=0;i<FFT/2;i++){cosine[i]=Math.cos(-2*Math.PI*i/FFT);sine[i]=Math.sin(-2*Math.PI*i/FFT);}
 }
 float[] transform(float[] pcm,int start,SoundMath.Cancel cancel)throws IOException{
  if(pcm==null||start<0||start>=pcm.length)throw new IOException("소리 입력을 확인하세요");
  // Pad the same 0.975 s analysis interval as YAMNet before applying centered reflection.
  float[] window=new float[WINDOW];System.arraycopy(pcm,start,window,0,Math.min(WINDOW,pcm.length-start));
  float[] features=new float[FRAMES*BINS];double[] real=new double[FFT],imaginary=new double[FFT],power=new double[FFT/2+1];
  for(int frame=0;frame<FRAMES;frame++){
   SoundMath.check(cancel);int base=frame*FFT_HOP-FFT/2;
   for(int i=0;i<FFT;i++){int sample=base+i;if(sample<0)sample=-sample;else if(sample>=WINDOW)sample=2*WINDOW-2-sample;float value=window[sample];if(!Float.isFinite(value))throw new IOException("소리 PCM 값");real[reverse[i]]=value*hann[i];imaginary[reverse[i]]=0;}
   for(int size=2;size<=FFT;size<<=1){int half=size/2,step=FFT/size;for(int block=0;block<FFT;block+=size)for(int j=0;j<half;j++){int lo=block+j,hi=lo+half,k=j*step;double r=real[hi]*cosine[k]-imaginary[hi]*sine[k],im=real[hi]*sine[k]+imaginary[hi]*cosine[k];real[hi]=real[lo]-r;imaginary[hi]=imaginary[lo]-im;real[lo]+=r;imaginary[lo]+=im;}}
   for(int k=0;k<power.length;k++)power[k]=real[k]*real[k]+imaginary[k]*imaginary[k];
   for(int band=0;band<BINS;band++){double energy=0;for(int k=0;k<melBins[band].length;k++)energy+=power[melBins[band][k]]*melWeights[band][k];features[frame*BINS+band]=(float)(10*Math.log10(Math.max(1e-10,energy)));}
  }
  return features;
 }
}
