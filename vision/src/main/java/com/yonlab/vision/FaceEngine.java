package com.yonlab.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Looper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Upright image observations and real YuNet five-point alignment/SFace features. */
public final class FaceEngine implements AutoCloseable {
    private static final int DETECT_SIDE=640;
    private static final String YUNET="face_detection_yunet_2023mar.onnx";
    private static final String SFACE="face_recognition_sface_2021dec.onnx";
    private static final String YUNET_SHA="8f2383e4dd3cfbb4553ea8718107fc0423210dc964f9f4280604804ed2552fa4";
    private static final String EXPRESSION="facial_expression_recognition_mobilefacenet_2022july.onnx";
    public static final String EXPRESSION_SHA256="4f61307602fc089ce20488a31d4e4614e3c9753a7d6c41578c854858b183e1a9";
    public static final int MAX_EXPRESSION_FACES=8;
    private static final String AGE_GENDER="age_gender_recognition_retail_0013_fp32.onnx";
    public static final String AGE_GENDER_SHA256="f4649ed528edcb0dbf3bb9474ffbd4fe32384f3c4b8e49493214464d0917e678";
    private static final Object MODELS_LOCK=new Object(),MLKIT_GATE=new Object();
    private static final Executor DIRECT=Runnable::run;
    private static Task<?> inflight;
    /** OpenCV's Java wrappers have no public release. Exactly one pair is cached per process. */
    private static FaceDetectorYN sharedDetector;
    private static FaceRecognizerSF sharedRecognizer;
    /** One lazy FER Net shares MODELS_LOCK; its native model remains cached for this process. */
    private static Net sharedExpression;
    private static Net sharedAgeGender;
    private final Context app;
    private final boolean observeMouth,useAlternate,useBlaze;
    private final String selectedModel;
    private volatile PlusBlazeDetector blazeDetector;
    private volatile PlusFaceLandmarks landmarkDetector;
    private volatile OneYuNet alternate;
    private final AtomicBoolean closed=new AtomicBoolean(),busy=new AtomicBoolean(),poisoned=new AtomicBoolean();
    private FaceDetector detector,contourDetector;
    private int pendingTasks;
    private volatile long expressionNs;
    private volatile int expressionFaces;
    private volatile long ageGenderNs;
    private volatile int ageGenderFaces;

    public FaceEngine(Context context){this(context,"face","mlkit-sface-2023");}
    public FaceEngine(Context context,String mode,String modelId){app=context.getApplicationContext();String feature=OneModelPolicy.canonicalFeature(mode);String selected=OneModels.resolve(app,feature,modelId);selectedModel=selected;observeMouth="drive".equals(feature);useAlternate="yunet-2026-sface".equals(selected);useBlaze="blazeface-short-sface".equals(selected)||"blazeface-full-sface".equals(selected);}
    private void checkOpen(){if(closed.get()||poisoned.get())throw new IllegalStateException("분석을 다시 시작해 주세요");if(Thread.currentThread().isInterrupted())throw new IllegalStateException("분석이 취소되었습니다");}
    public List<Observations.Face> analyze(Bitmap bitmap,boolean embeddings)throws Exception{
        return analyze(bitmap,embeddings,false);
    }
    public List<Observations.Face> analyze(Bitmap bitmap,boolean embeddings,boolean expressions)throws Exception{
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("사진 분석은 작업 스레드에서 실행하세요");
        if(!busy.compareAndSet(false,true))throw new IllegalStateException("이전 분석이 진행 중입니다");
        try{
            checkOpen();expressionNs=0;expressionFaces=0;ageGenderNs=0;ageGenderFaces=0;if(bitmap==null||bitmap.isRecycled()||bitmap.getWidth()<1||bitmap.getHeight()<1||(long)bitmap.getWidth()*bitmap.getHeight()>16000000L)throw new IllegalArgumentException("사진 크기와 형식을 확인해 주세요");
            if(PlusFaceLandmarks.supports(selectedModel)){if(landmarkDetector==null){PlusFaceLandmarks created=new PlusFaceLandmarks(app,selectedModel);if(closed.get()){created.close();throw new IOException("분석이 취소되었습니다");}landmarkDetector=created;}return landmarkDetector.analyze(bitmap);}
            List<PlusBlazeDetector.Box> primary=useBlaze?blaze().detect(bitmap):null;checkOpen();
            List<com.google.mlkit.vision.face.Face> source=detect(bitmap,false);checkOpen();
            List<Observations.Face> attributes=new ArrayList<>();
            for(com.google.mlkit.vision.face.Face f:source){
                Rect b=f.getBoundingBox();Observations.Face item=box(b.left,b.top,b.right,b.bottom,bitmap.getWidth(),bitmap.getHeight());if(item==null)continue;
                item.yaw=f.getHeadEulerAngleY();item.pitch=f.getHeadEulerAngleX();item.roll=f.getHeadEulerAngleZ();
                item.leftEye=probability(f.getLeftEyeOpenProbability());item.rightEye=probability(f.getRightEyeOpenProbability());item.smile=probability(f.getSmilingProbability());
                Integer tracking=f.getTrackingId();item.trackingId=tracking==null?-1:tracking;attributes.add(item);
            }
            if(!embeddings&&observeMouth&&!attributes.isEmpty()){
                List<com.google.mlkit.vision.face.Face> contours=detect(bitmap,true);checkOpen();
                for(com.google.mlkit.vision.face.Face f:contours){Rect b=f.getBoundingBox();Observations.Face bounds=box(b.left,b.top,b.right,b.bottom,bitmap.getWidth(),bitmap.getHeight());Observations.Face best=bestOverlap(bounds,attributes);if(best!=null)best.mouthOpen=mouthRatio(f);}
            }
            if(!embeddings&&!expressions)return primary==null?attributes:primaryFaces(primary,attributes);
            return embed(bitmap,attributes,embeddings,expressions,primary);
        }finally{busy.set(false);if(closed.get())closeAlternate();}
    }
    private PlusBlazeDetector blaze()throws Exception{
        if(blazeDetector==null){PlusBlazeDetector created=new PlusBlazeDetector(app,"blazeface-full-sface".equals(selectedModel)?"full":"short");if(closed.get()){created.close();throw new IOException("분석이 취소되었습니다");}blazeDetector=created;}return blazeDetector;
    }
    /** Fingerprint covers all neural weights used by the selected detection/alignment pipeline. */
    public String modelFingerprint(){
        if(PlusFaceLandmarks.supports(selectedModel))return ModelPlusAssets.fingerprint(PlusBlazeDetector.SHORT_SHA,"face-landmarker-478".equals(selectedModel)?PlusFaceLandmarks.V2_SHA:PlusFaceLandmarks.LEGACY_SHA);
        if(useBlaze)return ModelPlusAssets.fingerprint("blazeface-full-sface".equals(selectedModel)?PlusBlazeDetector.FULL_SHA:PlusBlazeDetector.SHORT_SHA,YUNET_SHA,Registry.SFACE_SHA256);
        return observeMouth?"":useAlternate?OneOnnx.expected(selectedModel):Registry.SFACE_SHA256;
    }
    private static List<Observations.Face> primaryFaces(List<PlusBlazeDetector.Box> boxes,List<Observations.Face> attributes){List<Observations.Face> result=new ArrayList<>();for(PlusBlazeDetector.Box detected:boxes){Observations.Face face=detected.face(),metadata=bestOverlap(face,attributes);if(metadata!=null)copyAttributes(metadata,face);result.add(face);}return result;}
    private List<com.google.mlkit.vision.face.Face> detect(Bitmap input,boolean contours)throws Exception{
        final Bitmap snapshot;final Task<List<com.google.mlkit.vision.face.Face>> task;
        synchronized(MLKIT_GATE){
            checkOpen();if(inflight!=null&&!inflight.isComplete())throw new IllegalStateException("이전 사진 분석의 종료를 기다려 주세요");
            FaceDetector selected;
            if(contours){
                if(contourDetector==null)contourDetector=FaceDetection.getClient(new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL).build());
                selected=contourDetector;
            }else{
                if(detector==null)detector=FaceDetection.getClient(new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).enableTracking().setMinFaceSize(.08f).build());
                selected=detector;
            }
            snapshot=input.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)throw new IOException("사진을 준비할 수 없습니다");
            pendingTasks++;
            try{task=selected.process(InputImage.fromBitmap(snapshot,0));inflight=task;}
            catch(Exception e){pendingTasks--;snapshot.recycle();throw e;}
        }
        task.addOnCompleteListener(DIRECT,finished->{
            snapshot.recycle();
            synchronized(MLKIT_GATE){pendingTasks--;if(inflight==finished)inflight=null;if((closed.get()||poisoned.get())&&pendingTasks==0)closeDetectors();}
        });
        try{return Tasks.await(task,15,TimeUnit.SECONDS);}
        catch(TimeoutException e){poisoned.set(true);throw new IOException("얼굴 분석 시간이 초과됐습니다. 다시 실행하세요",e);}
        catch(InterruptedException e){poisoned.set(true);Thread.currentThread().interrupt();throw e;}
    }
    private List<Observations.Face> embed(Bitmap bitmap,List<Observations.Face> attributes,boolean embeddings,boolean expressions,List<PlusBlazeDetector.Box> primary)throws Exception{
        synchronized(MODELS_LOCK){
            checkOpen();loadModels(app);if(expressions){loadExpression(app);loadAgeGender(app);}checkOpen();
            Mat rgba=new Mat(),bgr=new Mat(),small=new Mat(),padded=new Mat(),faces=new Mat();
            try{
                Utils.bitmapToMat(bitmap,rgba);Imgproc.cvtColor(rgba,bgr,Imgproc.COLOR_RGBA2BGR);
                double scale=Math.min(DETECT_SIDE/(double)bgr.cols(),DETECT_SIDE/(double)bgr.rows());
                int rw=Math.max(1,(int)Math.round(bgr.cols()*scale)),rh=Math.max(1,(int)Math.round(bgr.rows()*scale));
                int left=(DETECT_SIDE-rw)/2,top=(DETECT_SIDE-rh)/2;double scaleX=rw/(double)bgr.cols(),scaleY=rh/(double)bgr.rows();
                Imgproc.resize(bgr,small,new Size(rw,rh));Core.copyMakeBorder(small,padded,top,DETECT_SIDE-rh-top,left,DETECT_SIDE-rw-left,Core.BORDER_CONSTANT,new Scalar(0,0,0));
                if(useAlternate){if(alternate==null){OneYuNet created=new OneYuNet(app);if(closed.get()){created.close();throw new IOException("분석이 취소되었습니다");}alternate=created;}List<float[]> detected=alternate.detect(bitmap);if(!detected.isEmpty()){faces.create(detected.size(),15,CvType.CV_32F);for(int n=0;n<detected.size();n++)faces.put(n,0,detected.get(n));}}else sharedDetector.detect(padded,faces);List<Observations.Face> result=new ArrayList<>();
                if(faces.empty())return primary==null?result:primaryFaces(primary,attributes);
                boolean[] matched=primary==null?null:new boolean[primary.size()];
                if(faces.cols()!=15)throw new IOException("얼굴 검출 모델 출력을 확인해 주세요");
                for(int i=0;i<faces.rows();i++){
                    checkOpen();float[] detection=new float[15];faces.get(i,0,detection);boolean valid=true;
                    for(float f:detection)if(!finite(f)){valid=false;break;}if(!valid||detection[2]<=0||detection[3]<=0)continue;
                    detection[0]=(float)((detection[0]-left)/scaleX);detection[1]=(float)((detection[1]-top)/scaleY);detection[2]/=scaleX;detection[3]/=scaleY;
                    for(int k=4;k<14;k+=2){detection[k]=(float)((detection[k]-left)/scaleX);detection[k+1]=(float)((detection[k+1]-top)/scaleY);}
                    Observations.Face observation=box(detection[0],detection[1],detection[0]+detection[2],detection[1]+detection[3],bitmap.getWidth(),bitmap.getHeight());if(observation==null)continue;
                    // BlazeFace supplies the displayed detection; YuNet supplies actual five-point alignment for the unchanged SFace128D registry.
                    if(primary!=null){int best=-1;float overlap=.25f;for(int j=0;j<primary.size();j++)if(!matched[j]){float value=PlusBlazeDetector.overlap(observation,primary.get(j));if(value>overlap){overlap=value;best=j;}}if(best<0)continue;matched[best]=true;observation=primary.get(best).face();}
                    Mat row=new Mat(1,15,CvType.CV_32F),aligned=new Mat(),feature=new Mat(),floatFeature=new Mat();
                    try{
                        row.put(0,0,detection);sharedRecognizer.alignCrop(bgr,row,aligned);
                        if(embeddings){sharedRecognizer.feature(aligned,feature);
                            if(feature.total()*feature.channels()!=Registry.DIMENSION)throw new IOException("얼굴 특징 모델 차원이 다릅니다");
                            feature.convertTo(floatFeature,CvType.CV_32F);float[] vector=new float[Registry.DIMENSION];floatFeature.get(0,0,vector);observation.embedding=Registry.normalize(vector);
                        }
                        if(expressions&&expressionFaces<MAX_EXPRESSION_FACES){long started=System.nanoTime();try{expression(aligned,observation);expressionFaces++;}finally{expressionNs+=System.nanoTime()-started;}}
                        if(expressions&&ageGenderFaces<MAX_EXPRESSION_FACES){checkOpen();long started=System.nanoTime();try{if(ageGender(bgr,observation))ageGenderFaces++;}finally{ageGenderNs+=System.nanoTime()-started;}}
                    }finally{floatFeature.release();feature.release();aligned.release();row.release();}
                    Observations.Face metadata=bestOverlap(observation,attributes);if(metadata!=null)copyAttributes(metadata,observation);result.add(observation);
                }
                if(primary!=null)for(int j=0;j<primary.size();j++)if(!matched[j]){Observations.Face face=primary.get(j).face(),metadata=bestOverlap(face,attributes);if(metadata!=null)copyAttributes(metadata,face);result.add(face);}
                Collections.sort(result,new Comparator<Observations.Face>(){public int compare(Observations.Face a,Observations.Face b){int x=Float.compare(a.left,b.left);return x!=0?x:Float.compare(a.top,b.top);}});checkOpen();return result;
            }finally{faces.release();padded.release();small.release();bgr.release();rgba.release();}
        }
    }
    private static void loadExpression(Context context)throws Exception{
        if(sharedExpression!=null)return;
        File file=model(context,EXPRESSION,EXPRESSION_SHA256);Net net=Dnn.readNetFromONNX(file.getAbsolutePath());
        if(net.empty())throw new IOException("표정 모델을 불러올 수 없습니다");
        net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);net.setPreferableTarget(Dnn.DNN_TARGET_CPU);sharedExpression=net;
    }
    private static void expression(Mat aligned,Observations.Face observation)throws Exception{
        if(aligned.cols()!=112||aligned.rows()!=112||aligned.channels()!=3)throw new IOException("표정 입력 정렬을 확인해 주세요");
        Mat blob=Dnn.blobFromImage(aligned,1.0/127.5,new Size(112,112),new Scalar(127.5,127.5,127.5),true,false,CvType.CV_32F),output=null,flat=null;
        try{
            sharedExpression.setInput(blob,"data");output=sharedExpression.forward("label");
            if(output.total()*output.channels()!=ExpressionPolicy.CLASSES||output.depth()!=CvType.CV_32F)throw new IOException("표정 모델의 출력 차원을 확인해 주세요");
            flat=output.reshape(1,1);float[] logits=new float[ExpressionPolicy.CLASSES];flat.get(0,0,logits);
            observation.expressionProbabilities=ExpressionPolicy.softmax(logits);observation.expressionIndex=ExpressionPolicy.argmax(observation.expressionProbabilities);
            if(observation.expressionIndex<0)throw new IOException("표정 모델의 출력 값을 확인해 주세요");
            observation.expressionConfidence=observation.expressionProbabilities[observation.expressionIndex];
        }finally{if(flat!=null)flat.release();if(output!=null)output.release();blob.release();}
    }
    public double lastExpressionMs(){return expressionNs/1_000_000d;}
    public int lastExpressionFaces(){return expressionFaces;}
    private static void loadAgeGender(Context context)throws Exception{
        if(sharedAgeGender!=null)return;File file=model(context,AGE_GENDER,AGE_GENDER_SHA256);Net net=Dnn.readNetFromONNX(file.getAbsolutePath());
        if(net.empty())throw new IOException("연령 모델을 불러올 수 없습니다");net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);net.setPreferableTarget(Dnn.DNN_TARGET_CPU);sharedAgeGender=net;
    }
    private static boolean ageGender(Mat bgr,Observations.Face observation)throws Exception{
        int left=Math.max(0,(int)Math.floor(observation.left*bgr.cols())),top=Math.max(0,(int)Math.floor(observation.top*bgr.rows()));
        int right=Math.min(bgr.cols(),(int)Math.ceil(observation.right*bgr.cols())),bottom=Math.min(bgr.rows(),(int)Math.ceil(observation.bottom*bgr.rows()));
        // Official minimum object width is62px; smaller observations keep unknown age/gender.
        if(right-left<62||bottom-top<62)return false;
        Mat roi=new Mat(bgr,new org.opencv.core.Rect(left,top,right-left,bottom-top)),blob=null,ageFlat=null;List<Mat> outputs=new ArrayList<>();
        try{
            blob=Dnn.blobFromImage(roi,1,new Size(62,62),new Scalar(0,0,0),false,false,CvType.CV_32F);sharedAgeGender.setInput(blob,"data");sharedAgeGender.forward(outputs,java.util.Collections.singletonList("fc3_a"));
            if(outputs.size()!=1||outputs.get(0).total()*outputs.get(0).channels()!=1||outputs.get(0).depth()!=CvType.CV_32F)throw new IOException("연령 모델의 출력 차원을 확인해 주세요");
            ageFlat=outputs.get(0).reshape(1,1);float[] age=new float[1];ageFlat.get(0,0,age);AgeGenderPolicy.applyAge(age,observation);return true;
        }finally{if(ageFlat!=null)ageFlat.release();for(Mat output:outputs)output.release();if(blob!=null)blob.release();roi.release();}
    }
    public double lastAgeGenderMs(){return ageGenderNs/1_000_000d;}
    public int lastAgeGenderFaces(){return ageGenderFaces;}
    private static void loadModels(Context context)throws Exception{
        if(sharedDetector!=null&&sharedRecognizer!=null)return;
        if(!OpenCVLoader.initLocal())throw new IOException("분석 엔진을 불러올 수 없습니다");
        File detectorFile=model(context,YUNET,YUNET_SHA),recognizerFile=model(context,SFACE,Registry.SFACE_SHA256);
        // Model initialization occurs once behind MODELS_LOCK; re-entry shares this fixed-size pair.
        if(sharedDetector==null)sharedDetector=FaceDetectorYN.create(detectorFile.getAbsolutePath(),"",new Size(DETECT_SIDE,DETECT_SIDE),.8f,.3f,5000,Dnn.DNN_BACKEND_OPENCV,Dnn.DNN_TARGET_CPU);
        if(sharedRecognizer==null)sharedRecognizer=FaceRecognizerSF.create(recognizerFile.getAbsolutePath(),"",Dnn.DNN_BACKEND_OPENCV,Dnn.DNN_TARGET_CPU);
    }
    private static File model(Context context,String name,String expected)throws Exception{
        File folder=new File(context.getNoBackupFilesDir(),"vision-models");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("모델 저장 공간을 확인해 주세요");
        File target=new File(folder,name);if(target.isFile()&&expected.equals(sha(target)))return target;
        File pending=new File(folder,name+"."+UUID.randomUUID()+".pending");
        try{
            try(InputStream in=context.getAssets().open("models/"+name);FileOutputStream out=new FileOutputStream(pending)){
                byte[] block=new byte[16384];int count,total=0;while((count=in.read(block))!=-1){total+=count;if(total>40*1024*1024)throw new IOException("모델 크기를 확인해 주세요");out.write(block,0,count);}out.getFD().sync();
            }
            if(!expected.equals(sha(pending)))throw new IOException("모델 해시가 일치하지 않습니다");
            if(!pending.renameTo(target))throw new IOException("모델을 준비할 수 없습니다");return target;
        }finally{if(pending.exists())pending.delete();}
    }
    private static String sha(File file)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] block=new byte[16384];int n;while((n=in.read(block))!=-1)digest.update(block,0,n);}
        StringBuilder text=new StringBuilder();for(byte b:digest.digest())text.append(String.format(Locale.ROOT,"%02x",b&255));return text.toString();
    }
    private static boolean finite(float value){return !Float.isNaN(value)&&!Float.isInfinite(value);}
    private static float probability(Float value){return value!=null&&finite(value)&&value>=0&&value<=1?value:-1;}
    private static float clamp(float value){return Math.max(0,Math.min(1,value));}
    private static Observations.Face box(float l,float t,float r,float b,int width,int height){
        if(!finite(l)||!finite(t)||!finite(r)||!finite(b)||width<1||height<1)return null;
        l=clamp(l/width);t=clamp(t/height);r=clamp(r/width);b=clamp(b/height);return l<r&&t<b?new Observations.Face(l,t,r,b):null;
    }
    private static Observations.Face bestOverlap(Observations.Face target,List<Observations.Face> candidates){
        if(target==null)return null;Observations.Face best=null;float bestIou=.25f;
        for(Observations.Face c:candidates){float w=Math.max(0,Math.min(target.right,c.right)-Math.max(target.left,c.left)),h=Math.max(0,Math.min(target.bottom,c.bottom)-Math.max(target.top,c.top));float intersection=w*h,union=(target.right-target.left)*(target.bottom-target.top)+(c.right-c.left)*(c.bottom-c.top)-intersection;float iou=union>0?intersection/union:0;if(iou>bestIou){bestIou=iou;best=c;}}
        return best;
    }
    private static void copyAttributes(Observations.Face from,Observations.Face to){to.yaw=from.yaw;to.pitch=from.pitch;to.roll=from.roll;to.leftEye=from.leftEye;to.rightEye=from.rightEye;to.smile=from.smile;to.mouthOpen=from.mouthOpen;to.trackingId=from.trackingId;}
    /** Inner lip gap / mouth width: a geometric ratio, not a yawn probability. */
    private static float mouthRatio(com.google.mlkit.vision.face.Face face){
        FaceContour upper=face.getContour(FaceContour.UPPER_LIP_BOTTOM),lower=face.getContour(FaceContour.LOWER_LIP_TOP);
        if(upper==null||lower==null)return -1;List<PointF> u=upper.getPoints(),l=lower.getPoints();if(u.size()<3||l.size()<3)return -1;
        float left=Float.MAX_VALUE,right=-Float.MAX_VALUE,upperY=0,lowerY=0;int un=0,ln=0;
        for(PointF p:u){left=Math.min(left,p.x);right=Math.max(right,p.x);}for(PointF p:l){left=Math.min(left,p.x);right=Math.max(right,p.x);}float width=right-left;if(!(width>1))return -1;
        for(PointF p:u)if(p.x>left+width*.3f&&p.x<right-width*.3f){upperY+=p.y;un++;}
        for(PointF p:l)if(p.x>left+width*.3f&&p.x<right-width*.3f){lowerY+=p.y;ln++;}
        if(un==0||ln==0)return -1;float ratio=Math.abs(lowerY/ln-upperY/un)/width;return finite(ratio)?clamp(ratio):-1;
    }
    /** Nonblocking cancellation; unfinished ML Kit input remains owned until its completion callback. */
    public void close(){if(!closed.compareAndSet(false,true))return;synchronized(MLKIT_GATE){if(pendingTasks==0)closeDetectors();}}
    private void closeDetectors(){FaceDetector first=detector,second=contourDetector;detector=null;contourDetector=null;try{if(first!=null)first.close();}finally{if(second!=null)second.close();if(!busy.get())closeAlternate();}}
    private synchronized void closeAlternate(){OneYuNet owned=alternate;PlusBlazeDetector blaze=blazeDetector;PlusFaceLandmarks landmarks=landmarkDetector;alternate=null;blazeDetector=null;landmarkDetector=null;try{if(owned!=null)owned.close();}finally{try{if(blaze!=null)blaze.close();}finally{if(landmarks!=null)landmarks.close();}}}
}
