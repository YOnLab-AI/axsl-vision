package com.yonlab.vision;
import android.content.Context;
import android.graphics.Bitmap;

/** One text-result contract for bundled ML Kit and the distinct Paddle weight pipelines. */
final class OneTextEngine implements AutoCloseable {
 private final PhotoVisionEngine mlkit;private final PaddleOcrEngine paddle;final String modelSha256,modelId;
 OneTextEngine(Context context,String requested)throws Exception{this(context,requested,()->false);}
 OneTextEngine(Context context,String requested,OneModelRunner.Cancel cancel)throws Exception{modelId=ModelPlusPolicy.ocrId(requested);if(ModelPlusPolicy.paddle(modelId)){paddle=new PaddleOcrEngine(context,modelId,cancel);mlkit=null;modelSha256=paddle.modelSha256;}else{paddle=null;mlkit=new PhotoVisionEngine("ocr",!"mlkit-ocr-latin".equals(modelId));modelSha256="";}}
 PhotoVisionEngine.Ocr ocr(Bitmap image)throws Exception{return paddle==null?mlkit.ocr(image):paddle.ocr(image);}
 public void close()throws Exception{if(paddle!=null)paddle.close();if(mlkit!=null)mlkit.close();}
}
