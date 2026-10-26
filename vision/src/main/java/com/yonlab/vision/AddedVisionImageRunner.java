package com.yonlab.vision;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import org.json.*;
import java.io.*;
import java.util.*;

/** Additional real weights within the same same-input comparison contract. */
public final class AddedVisionImageRunner {
 private AddedVisionImageRunner(){}
 public static boolean handles(String feature,String id){return ("detect".equals(feature)||"flow".equals(feature))&&AddedVisionModels.object(id)||"classify".equals(feature)&&AddedVisionModels.classifier(id)||"segment".equals(feature)&&id.startsWith("segment-")&&AddedVisionModels.portrait(id)||"studio".equals(feature)&&id.startsWith("studio-")&&AddedVisionModels.portrait(id)||"search".equals(feature)&&AddedVisionModels.retrieval(id);}
 public static OneModelRunner.Result run(Context context,String feature,String id,Bitmap image,OneModelRunner.Cancel cancel){
  OneModelRunner.Result result=new OneModelRunner.Result(feature,id);long start=SystemClock.elapsedRealtime();AutoCloseable engine=null;
  try{ExpansionImageRunner.check(cancel);if(!handles(feature,id))throw new IllegalArgumentException("사진 모델을 선택하세요");
   if("studio".equals(feature))BackgroundStudio.run(context,id,image,cancel,result);
   else if("segment".equals(feature))BackgroundStudio.segment(context,id,image,cancel,result);
   else if("search".equals(feature))ImageRetrieval.run(context,id,image,cancel,result);
   else if("classify".equals(feature)){
    long load=SystemClock.elapsedRealtime();AddedVisionOnnx model=new AddedVisionOnnx(context,id);engine=model;result.engineCreated=true;result.modelSha256=model.sha256;result.loadMs=SystemClock.elapsedRealtime()-load;long infer=SystemClock.elapsedRealtime();float[] logits=model.run(image,r->{Object value=r.get(0).getValue();if(!(value instanceof float[][])||((float[][])value).length!=1||((float[][])value)[0].length!=1000)throw new IOException("분류 모델 출력 크기");return ((float[][])value)[0].clone();});for(float value:logits)if(!Float.isFinite(value))throw new IOException("분류 모델 수치");float[] scores=OneModelMath.softmax(logits);result.inferenceMs=SystemClock.elapsedRealtime()-infer;ExpansionImageRunner.check(cancel);List<String> labels=new ArrayList<>();OneObjectEngine.read(context,"one/models/imagenet-labels.txt",labels);if(labels.size()!=1000)throw new IOException("ImageNet 분류표");List<Integer> order=new ArrayList<>();for(int i=0;i<1000;i++)order.add(i);order.sort((a,b)->Float.compare(scores[b],scores[a]));JSONArray top=new JSONArray();StringBuilder detail=new StringBuilder();for(int i=0;i<5;i++){int index=order.get(i);top.put(new JSONObject().put("classId",index).put("label",labels.get(index)).put("score",scores[index]));detail.append(labels.get(index)).append(" · ").append(String.format(Locale.ROOT,"%.1f%%",scores[index]*100)).append('\n');}result.metrics.put("top5",top).put("preprocessingContract","RGB shortest-side256 / center224; ImageNet mean/std");result.summary=labels.get(order.get(0));result.detail=detail.toString();result.image=image.copy(Bitmap.Config.ARGB_8888,false);
   }else{
    long load=SystemClock.elapsedRealtime();OneObjectEngine model=new OneObjectEngine(context,id);engine=model;result.engineCreated=true;result.modelSha256=AddedVisionModels.sha(id);result.loadMs=SystemClock.elapsedRealtime()-load;long infer=SystemClock.elapsedRealtime();List<Observations.Detection> objects=model.analyze(image,.4f);result.inferenceMs=SystemClock.elapsedRealtime()-infer;ExpansionImageRunner.check(cancel);result.image=image.copy(Bitmap.Config.ARGB_8888,true);if(result.image==null)throw new IOException("사진 복사");Canvas canvas=new Canvas(result.image);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xff0068f8);paint.setStrokeWidth(Math.max(2,image.getWidth()/300f));paint.setTextSize(Math.max(15,image.getWidth()/35f));JSONArray detections=new JSONArray();for(Observations.Detection object:objects){paint.setStyle(Paint.Style.STROKE);canvas.drawRect(object.left*image.getWidth(),object.top*image.getHeight(),object.right*image.getWidth(),object.bottom*image.getHeight(),paint);String label=OneScoreLabels.detection(object.label,object.score);paint.setStyle(Paint.Style.FILL);canvas.drawText(label,object.left*image.getWidth()+3,Math.max(paint.getTextSize(),object.top*image.getHeight()-4),paint);detections.put(new JSONObject().put("label",object.label).put("score",object.score).put("renderedLabel",label).put("left",object.left).put("top",object.top).put("right",object.right).put("bottom",object.bottom));}result.metrics.put("detections",detections).put("outputClasses","COCO sparse90 IDs; person=0");result.summary="객체 "+objects.size()+"개";if("flow".equals(feature))result.detail="사진의 사람 검출 비교입니다. 이동 집계는 영상·카메라에서 실행하세요.";
   }
   ExpansionImageRunner.check(cancel);result.state="COMPLETED";
  }catch(Exception|LinkageError|OutOfMemoryError e){result.state=cancel.canceled()||Thread.currentThread().isInterrupted()?"CANCELED":"ERROR";result.errorType=e.getClass().getSimpleName();result.summary="CANCELED".equals(result.state)?"분석 취소":"다른 모델로 다시 분석하세요";android.util.Log.w("YOn","added_vision_"+feature,e);}
  finally{if(!result.engineCreated)result.engineCloseReturned=true;try{if(engine!=null){engine.close();result.engineCloseReturned=true;}}catch(Exception|LinkageError e){result.state="ERROR";result.errorType=e.getClass().getSimpleName();}result.elapsedMs=SystemClock.elapsedRealtime()-start;if(result.image==null&&image!=null&&!image.isRecycled())try{result.image=image.copy(Bitmap.Config.ARGB_8888,false);}catch(OutOfMemoryError ignored){}}
  return result;
 }
}
