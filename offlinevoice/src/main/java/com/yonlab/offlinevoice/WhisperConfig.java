package com.yonlab.offlinevoice;
import com.k2fsa.sherpa.onnx.*;import java.io.File;
/** One multilingual weight set; empty native language selects automatic detection. */
final class WhisperConfig {
 private WhisperConfig(){}
 static OfflineRecognizerConfig create(File directory){return create(directory,2);}
 static OfflineRecognizerConfig create(File directory,int threads){return create(directory,threads,"tiny");}
 static OfflineRecognizerConfig create(File directory,int threads,String size){
  if(!"tiny".equals(size)&&!"base".equals(size)&&!"small".equals(size))throw new IllegalArgumentException("Whisper size");
  if(threads!=1&&threads!=2&&threads!=4)throw new IllegalArgumentException("CPU threads");
  OfflineWhisperModelConfig whisper=new OfflineWhisperModelConfig();whisper.setEncoder(new File(directory,size+"-encoder.int8.onnx").getAbsolutePath());whisper.setDecoder(new File(directory,size+"-decoder.int8.onnx").getAbsolutePath());whisper.setLanguage("");whisper.setTask("transcribe");whisper.setTailPaddings(300);whisper.setEnableTokenTimestamps(false);whisper.setEnableSegmentTimestamps(false);
  OfflineModelConfig model=new OfflineModelConfig();model.setWhisper(whisper);model.setTokens(new File(directory,size+"-tokens.txt").getAbsolutePath());model.setModelType("whisper");model.setNumThreads(threads);model.setProvider("cpu");model.setDebug(false);
  FeatureConfig feature=new FeatureConfig();feature.setSampleRate(16000);feature.setFeatureDim(80);OfflineRecognizerConfig config=new OfflineRecognizerConfig();config.setModelConfig(model);config.setFeatConfig(feature);config.setDecodingMethod("greedy_search");return config;
 }
}
