package com.yonlab.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import java.io.IOException;

/** Actual MODNet and MediaPipe Landscape soft alpha masks, with source-size composition elsewhere. */
final class AddedPortraitEngine {
 private AddedPortraitEngine(){}
 static BackgroundStudio.Mask infer(Context context,String id,Bitmap image,OneModelRunner.Cancel cancel,OneModelRunner.Result result)throws Exception{
  long start=SystemClock.elapsedRealtime();AddedVisionOnnx model=new AddedVisionOnnx(context,id);result.engineCreated=true;result.modelSha256=model.sha256;result.loadMs=SystemClock.elapsedRealtime()-start;
  try{long infer=SystemClock.elapsedRealtime();BackgroundStudio.Mask mask=model.run(image,output->{Object value=AddedVisionOnnx.output(output,id.endsWith("modnet")?"output":"alphas");if(!(value instanceof float[][][][]))throw new IOException("배경 모델 출력 형식");float[][][][] a=(float[][][][])value;if(a.length!=1||a[0].length!=1||a[0][0].length<1||a[0][0].length>1024)throw new IOException("배경 모델 출력 크기");int h=a[0][0].length,w=a[0][0][0].length;if(w<1||w>1024)throw new IOException("배경 모델 너비");float[] values=new float[w*h];for(int y=0;y<h;y++){ExpansionImageRunner.check(cancel);if(a[0][0][y].length!=w)throw new IOException("배경 모델 행 크기");for(int x=0;x<w;x++){float f=a[0][0][y][x];if(!Float.isFinite(f))throw new IOException("배경 모델 수치");values[y*w+x]=Math.max(0,Math.min(1,f));}}return new BackgroundStudio.Mask(values,w,h);});result.inferenceMs=SystemClock.elapsedRealtime()-infer;result.metrics.put("maskProvider",id.endsWith("modnet")?"MODNet / Xenova ONNX":"MediaPipe Selfie Landscape / ONNX Community").put("preprocessingContract",id.endsWith("modnet")?"RGB/255; mean0.5/std0.5; aspect-sized multiples32, longest side<=768":"RGB/255, direct256x144 resize; alpha144x256");return mask;}finally{model.close();result.engineCloseReturned=true;}
 }
}
