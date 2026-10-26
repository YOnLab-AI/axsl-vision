package com.yonlab.offlinevoice;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Uses existing public source WAVs and their independently made PCM16 references. */
public final class AudioFixtureHost {
 public static void main(String[] args)throws Exception{
  Path media=Paths.get(args[0]);int checks=0;
  for(int i=1;i<=3;i++){
   Path source=media.resolve(String.format(Locale.ROOT,"01_media/audio/fleurs_ko_%03d.wav",i)),reference=media.resolve(String.format(Locale.ROOT,"01_media/derived/voice_korean_%02d.wav",i));
   ByteArrayOutputStream converted=new ByteArrayOutputStream();PcmAudio converter;
   try(InputStream in=Files.newInputStream(source)){converter=WavePcm.decode(in,Files.size(source),(b,n)->converted.write(b,0,n),()->false);}
   byte[] expected;try(InputStream in=Files.newInputStream(reference)){PcmWave.Header h=PcmWave.read(in,Files.size(reference),()->false);expected=in.readNBytes(h.dataBytes);}
   byte[] actual=converted.toByteArray();if(actual.length!=expected.length)throw new AssertionError("sample count "+i);checks++;
   int max=0;for(int at=0;at<actual.length;at+=2){int a=(short)((actual[at]&255)|((actual[at+1]&255)<<8)),b=(short)((expected[at]&255)|((expected[at+1]&255)<<8));max=Math.max(max,Math.abs(a-b));}
   if(max>1)throw new AssertionError("quantization difference "+max);checks++;
   ByteArrayOutputStream identity=new ByteArrayOutputStream();try(InputStream in=Files.newInputStream(reference)){WavePcm.decode(in,Files.size(reference),(b,n)->identity.write(b,0,n),()->false);}
   if(!Arrays.equals(expected,identity.toByteArray()))throw new AssertionError("PCM16 reference identity "+i);checks++;
   System.out.println("fixture="+i+" sourceFrames="+converter.inputFrames()+" outputSamples="+converter.outputSamples()+" pcmBytes="+actual.length+" maxReferenceDifferenceLsb="+max+" pcm16Identity=true");
  }
  System.out.println("Public audio fixture conversion PASS "+checks+" checks; no ASR or device run");
 }
}
