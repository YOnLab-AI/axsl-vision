package com.yonlab.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import ai.onnxruntime.*;
import org.json.JSONArray;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

/** Pinned Paddle DB detection + perspective line crops + greedy CTC recognition. */
final class PaddleOcrEngine implements AutoCloseable {
 private static final String ROOT="one/modelplus/ocr/",V5DET="a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d",V6DET="193bab7a04fca699a6c82e6abb5b81bdb28177f0abd4062552b04908dafb19f8",KOREC="92f0b7785e64fc9090106a241cf4c1eb97472824558272751b88a2a4476d3a08",TINYREC="9ef676d6ed3c88256a2d92c640c44f25b0c40947e111b14b8be8f594091563e6";
 final String modelSha256;private final OrtEnvironment env;private OrtSession detector,recognizer;private final List<String> alphabet;
 private final boolean tiny,v5;private final int lineLimit,widthLimit;private final float bitmapThreshold,boxThreshold;private final double unclip;
 private boolean closed;private final OneModelRunner.Cancel cancel;

 PaddleOcrEngine(Context c,String id)throws Exception{this(c,id,()->false);}
 PaddleOcrEngine(Context c,String id,OneModelRunner.Cancel cancellation)throws Exception{
  cancel=cancellation;
  worker();if(!Arrays.asList("paddle-ocr-v5-ko","paddle-ocr-v6-ko","paddle-ocr-v6-tiny").contains(id))throw new IllegalArgumentException("문자 모델을 선택하세요");
  v5="paddle-ocr-v5-ko".equals(id);tiny="paddle-ocr-v6-tiny".equals(id);lineLimit=tiny?64:96;widthLimit=tiny?3200:1600;bitmapThreshold=v5?.3f:.2f;boxThreshold=v5?.6f:.4f;unclip=v5?1.5:1.4;
  String detSha=v5?V5DET:V6DET,recSha=tiny?TINYREC:KOREC,dictSha=tiny?"b9bf03b8b02c9c22f6af847769269d5c1f877cfc8914bbaecac6a44d7a6061de":"e600744eabcf1db6513518f7e09105cdb746ee0504ef110b25478147d9f0aa81";
  modelSha256=ModelPlusAssets.fingerprint(detSha,recSha,dictSha);alphabet=loadAlphabet(c,tiny?"paddle-v6-tiny-rec-dict.json":"paddle-v5-ko-rec-dict.json",dictSha,tiny?6904:11945);env=OrtEnvironment.getEnvironment();
  if(!OpenCVLoader.initLocal())throw new IOException("문자 이미지 엔진을 준비하세요");
  try{detector=open(c,v5?"paddle-v5-det.onnx":"paddle-v6-det.onnx",detSha);recognizer=open(c,tiny?"paddle-v6-tiny-rec.onnx":"paddle-v5-ko-rec.onnx",recSha);}catch(Exception|LinkageError|OutOfMemoryError e){closeAfterFailure(e);throw e;}
 }
 private OrtSession open(Context c,String filename,String sha)throws Exception{
  File f=ModelPlusAssets.file(c,ROOT+filename,sha,32L*1024*1024);OrtSession initialized=null;
  try(OrtSession.SessionOptions o=new OrtSession.SessionOptions()){o.setIntraOpNumThreads(OneExecutionSettings.threads(c));o.setInterOpNumThreads(1);initialized=env.createSession(f.getAbsolutePath(),o);if(initialized.getNumInputs()!=1||!initialized.getInputNames().contains("x")||!initialized.getOutputNames().contains("fetch_name_0"))throw new IOException("문자 모델 입출력을 확인하세요");return initialized;}catch(Exception|LinkageError|OutOfMemoryError e){if(initialized!=null)try{initialized.close();}catch(Exception cleanup){e.addSuppressed(cleanup);}throw e;}
 }
 synchronized PhotoVisionEngine.Ocr ocr(Bitmap bitmap)throws Exception{
  worker();check();if(bitmap==null||bitmap.isRecycled()||(long)bitmap.getWidth()*bitmap.getHeight()>16000000)throw new IOException("문서 사진을 확인하세요");
  Mat rgba=new Mat(),bgr=new Mat();
  try{Utils.bitmapToMat(bitmap,rgba);Imgproc.cvtColor(rgba,bgr,Imgproc.COLOR_RGBA2BGR);List<Region> regions=detect(bgr);List<Recognized> recognized=new ArrayList<>();
   for(Region region:regions){check();Mat crop=perspective(bgr,region.points);try{String text=recognize(crop);if(!text.isEmpty())recognized.add(new Recognized(region,text));}finally{crop.release();}}
   recognized.sort(Comparator.comparingDouble((Recognized r)->r.region.top).thenComparingDouble(r->r.region.left));
   // Group adjacent text pieces on a shared visual line without a non-transitive comparator.
   for(int i=0;i<recognized.size();){int end=i+1;double center=recognized.get(i).region.centerY(),height=recognized.get(i).region.height();while(end<recognized.size()){Region next=recognized.get(end).region;if(Math.abs(next.centerY()-center)>Math.min(height,next.height())*.45)break;end++;}recognized.subList(i,end).sort(Comparator.comparingDouble(r->r.region.left));i=end;}
   StringBuilder text=new StringBuilder();List<PhotoVisionEngine.TextBox> boxes=new ArrayList<>();for(Recognized line:recognized){check();if(text.length()>0)text.append('\n');text.append(line.text);Region r=line.region;android.graphics.Rect bounds=new android.graphics.Rect(Math.max(0,(int)Math.floor(r.left)),Math.max(0,(int)Math.floor(r.top)),Math.min(bitmap.getWidth(),(int)Math.ceil(r.right)),Math.min(bitmap.getHeight(),(int)Math.ceil(r.bottom)));if(!bounds.isEmpty())boxes.add(new PhotoVisionEngine.TextBox(line.text,bounds,bitmap.getWidth(),bitmap.getHeight()));}
   return new PhotoVisionEngine.Ocr(text.toString(),boxes);
  }finally{rgba.release();bgr.release();}
 }
 private List<Region> detect(Mat source)throws Exception{
  double scale=960d/Math.max(source.cols(),source.rows());int w=Math.max(32,(int)Math.round(source.cols()*scale/32)*32),h=Math.max(32,(int)Math.round(source.rows()*scale/32)*32);Mat resized=new Mat();float[][] probability;
  try{Imgproc.resize(source,resized,new Size(w,h),0,0,Imgproc.INTER_LINEAR);float[] values=planar(resized,true);try(OnnxTensor tensor=OnnxTensor.createTensor(env,FloatBuffer.wrap(values),new long[]{1,3,h,w});OrtSession.Result result=detector.run(Collections.singletonMap("x",tensor),Collections.singleton("fetch_name_0"))){Object raw=result.get("fetch_name_0").orElseThrow(()->new IOException("문자 영역 출력")).getValue();if(!(raw instanceof float[][][][]))throw new IOException("문자 영역 출력 차원");float[][][][] a=(float[][][][])raw;if(a.length!=1||a[0].length!=1||a[0][0].length<1||a[0][0].length>1024)throw new IOException("문자 영역 출력 크기");probability=a[0][0];}}
  finally{resized.release();}
  check();int ph=probability.length,pw=probability[0].length;if(pw<1||pw>1024||(long)pw*ph>1048576)throw new IOException("문자 영역 맵 크기");float[] flat=new float[pw*ph];byte[] binary=new byte[flat.length];for(int y=0;y<ph;y++){if(probability[y].length!=pw)throw new IOException("문자 영역 맵 행 크기");for(int x=0;x<pw;x++){float p=probability[y][x];if(!Float.isFinite(p)||p<-.001f||p>1.001f)throw new IOException("문자 영역 맵 수치");flat[y*pw+x]=p;binary[y*pw+x]=(byte)(p>bitmapThreshold?255:0);}}
  Mat map=new Mat(ph,pw,CvType.CV_32FC1),mask=new Mat(ph,pw,CvType.CV_8UC1),hierarchy=new Mat();List<MatOfPoint> contours=new ArrayList<>();List<Region> candidates=new ArrayList<>();
  try{map.put(0,0,flat);mask.put(0,0,binary);Imgproc.findContours(mask,contours,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);contours.sort((a,b)->Double.compare(Math.abs(Imgproc.contourArea(b)),Math.abs(Imgproc.contourArea(a))));
   for(int i=0;i<Math.min(1000,contours.size());i++){check();MatOfPoint contour=contours.get(i);if(contour.rows()<3)continue;MatOfPoint2f points=new MatOfPoint2f(contour.toArray());try{RotatedRect rectangle=Imgproc.minAreaRect(points);if(Math.min(rectangle.size.width,rectangle.size.height)<3)continue;Point[] quadrilateral=new Point[4];rectangle.points(quadrilateral);double score=boxScore(map,quadrilateral);if(!Double.isFinite(score)||score<boxThreshold)continue;double area=Math.abs(Imgproc.contourArea(points)),perimeter=Imgproc.arcLength(points,true);if(perimeter<=0||area<=0)continue;double padding=area*unclip/perimeter;RotatedRect expanded=new RotatedRect(rectangle.center,new Size(rectangle.size.width+2*padding,rectangle.size.height+2*padding),rectangle.angle);expanded.points(quadrilateral);for(Point p:quadrilateral){p.x=Math.max(0,Math.min(source.cols()-1,p.x*source.cols()/pw));p.y=Math.max(0,Math.min(source.rows()-1,p.y*source.rows()/ph));}Region region=new Region(order(quadrilateral),score);if(region.right-region.left>=3&&region.bottom-region.top>=3)candidates.add(region);}finally{points.release();}}
   candidates.sort((a,b)->Double.compare(b.score,a.score));if(candidates.size()>lineLimit)candidates=new ArrayList<>(candidates.subList(0,lineLimit));return candidates;
  }finally{for(MatOfPoint contour:contours)contour.release();map.release();mask.release();hierarchy.release();}
 }
 private static double boxScore(Mat probability,Point[] quad){
  double minX=Double.MAX_VALUE,minY=Double.MAX_VALUE,maxX=-1,maxY=-1;for(Point p:quad){minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);}int l=Math.max(0,(int)Math.floor(minX)),t=Math.max(0,(int)Math.floor(minY)),r=Math.min(probability.cols()-1,(int)Math.ceil(maxX)),b=Math.min(probability.rows()-1,(int)Math.ceil(maxY));if(l>r||t>b)return 0;
  Mat mask=Mat.zeros(b-t+1,r-l+1,CvType.CV_8UC1),roi=probability.submat(new org.opencv.core.Rect(l,t,r-l+1,b-t+1));Point[] relative=new Point[4];for(int i=0;i<4;i++)relative[i]=new Point(quad[i].x-l,quad[i].y-t);MatOfPoint polygon=new MatOfPoint(relative);try{Imgproc.fillConvexPoly(mask,polygon,new Scalar(255));return Core.mean(roi,mask).val[0];}finally{polygon.release();roi.release();mask.release();}
 }
 private Mat perspective(Mat source,Point[] quad)throws Exception{
  double width=Math.max(distance(quad[0],quad[1]),distance(quad[2],quad[3])),height=Math.max(distance(quad[0],quad[3]),distance(quad[1],quad[2]));double ratio=Math.min(1,2048d/Math.max(width,height));int w=Math.max(2,(int)Math.round(width*ratio)),h=Math.max(2,(int)Math.round(height*ratio));MatOfPoint2f from=new MatOfPoint2f(quad),to=new MatOfPoint2f(new Point(0,0),new Point(w-1,0),new Point(w-1,h-1),new Point(0,h-1));Mat transform=null,crop=new Mat();boolean success=false;
  try{transform=Imgproc.getPerspectiveTransform(from,to);Imgproc.warpPerspective(source,crop,transform,new Size(w,h),Imgproc.INTER_CUBIC,Core.BORDER_REPLICATE,new Scalar(0));if(h>=w*1.5){Mat rotated=new Mat();try{Core.rotate(crop,rotated,Core.ROTATE_90_COUNTERCLOCKWISE);crop.release();crop=rotated;rotated=null;}finally{if(rotated!=null)rotated.release();}}success=true;return crop;}finally{from.release();to.release();if(transform!=null)transform.release();if(!success)crop.release();}
 }
 private String recognize(Mat crop)throws Exception{
  check();int contentWidth=Math.max(1,(int)Math.ceil(48d*crop.cols()/crop.rows())),w=Math.min(widthLimit,Math.max(320,contentWidth)),resizedWidth=Math.min(w,contentWidth);Mat resized=new Mat();float[] values;
  try{Imgproc.resize(crop,resized,new Size(resizedWidth,48),0,0,Imgproc.INTER_LINEAR);float[] narrow=planar(resized,false);values=new float[3*48*w];for(int c=0;c<3;c++)for(int y=0;y<48;y++)System.arraycopy(narrow,c*48*resizedWidth+y*resizedWidth,values,c*48*w+y*w,resizedWidth);}finally{resized.release();}
  try(OnnxTensor tensor=OnnxTensor.createTensor(env,FloatBuffer.wrap(values),new long[]{1,3,48,w});OrtSession.Result result=recognizer.run(Collections.singletonMap("x",tensor),Collections.singleton("fetch_name_0"))){Object raw=result.get("fetch_name_0").orElseThrow(()->new IOException("문자 인식 출력")).getValue();if(!(raw instanceof float[][][])||((float[][][])raw).length!=1)throw new IOException("문자 인식 출력 차원");float[][] steps=((float[][][])raw)[0];if(steps.length<1||steps.length>1600)throw new IOException("문자 인식 시퀀스 크기");int previous=0,count=0;double confidence=0;StringBuilder text=new StringBuilder();for(float[] scores:steps){check();if(scores.length!=alphabet.size()+2)throw new IOException("문자 사전과 출력 크기가 다릅니다");int index=0;float best=-1;for(int k=0;k<scores.length;k++){float p=scores[k];if(!Float.isFinite(p)||p<-.001f||p>1.001f)throw new IOException("문자 인식 수치");if(p>best){best=p;index=k;}}if(index!=0&&index!=previous){text.append(index<=alphabet.size()?alphabet.get(index-1):" ");confidence+=best;count++;}previous=index;}
   if(count==0||confidence/count<.5)return "";return Normalizer.normalize(text.toString(),Normalizer.Form.NFC).trim();}
 }
 private static float[] planar(Mat bgr,boolean detection)throws IOException{
  if(bgr.type()!=CvType.CV_8UC3)throw new IOException("문자 사진 채널");int area=bgr.rows()*bgr.cols();byte[] bytes=new byte[area*3];bgr.get(0,0,bytes);float[] values=new float[area*3],mean={.485f,.456f,.406f},std={.229f,.224f,.225f};for(int i=0;i<area;i++){if((i&16383)==0&&Thread.currentThread().isInterrupted())throw new InterruptedIOException("문자 분석 취소");for(int c=0;c<3;c++){float v=bytes[i*3+c]&255;values[c*area+i]=detection?(v/255f-mean[c])/std[c]:v/127.5f-1;}}return values;
 }
 private static List<String> loadAlphabet(Context c,String filename,String sha,int expected)throws Exception{
  File file=ModelPlusAssets.file(c,ROOT+filename,sha,256*1024);ByteArrayOutputStream data=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(file)){byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){if(data.size()+n>256*1024)throw new IOException("문자 사전 크기");data.write(block,0,n);}}JSONArray array=new JSONArray(new String(data.toByteArray(),StandardCharsets.UTF_8));if(array.length()!=expected)throw new IOException("문자 사전 길이");List<String> chars=new ArrayList<>();for(int i=0;i<array.length();i++){String value=array.getString(i);if(value.isEmpty()||value.length()>16)throw new IOException("문자 사전 항목");chars.add(value);}return Collections.unmodifiableList(chars);
 }
 private static Point[] order(Point[] points){Point[] ordered=points.clone();double cx=0,cy=0;for(Point p:points){cx+=p.x/4;cy+=p.y/4;}final double x=cx,y=cy;Arrays.sort(ordered,Comparator.comparingDouble(p->Math.atan2(p.y-y,p.x-x)));int first=0;for(int i=1;i<4;i++)if(ordered[i].x+ordered[i].y<ordered[first].x+ordered[first].y)first=i;return new Point[]{ordered[first],ordered[(first+1)%4],ordered[(first+2)%4],ordered[(first+3)%4]};}
 private static double distance(Point a,Point b){return Math.hypot(a.x-b.x,a.y-b.y);}
 private static void worker(){if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("문자 분석은 작업 스레드에서 실행하세요");}
 private void check()throws IOException{if(closed)throw new IOException("문자 모델이 종료되었습니다");if(cancel.canceled()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("문자 분석 취소");}
 private static final class Region{final Point[] points;final double score,left,top,right,bottom;Region(Point[] p,double s){points=p;score=s;double l=Double.MAX_VALUE,t=Double.MAX_VALUE,r=-1,b=-1;for(Point point:p){l=Math.min(l,point.x);t=Math.min(t,point.y);r=Math.max(r,point.x);b=Math.max(b,point.y);}left=l;top=t;right=r;bottom=b;}double centerY(){return (top+bottom)/2;}double height(){return bottom-top;}}
 private static final class Recognized{final Region region;final String text;Recognized(Region region,String text){this.region=region;this.text=text;}}
 private void closeAfterFailure(Throwable failure){for(OrtSession session:new OrtSession[]{recognizer,detector})if(session!=null)try{session.close();}catch(Exception e){failure.addSuppressed(e);}recognizer=null;detector=null;closed=true;}
 public synchronized void close(){if(closed)return;closed=true;Throwable failure=null;for(OrtSession session:new OrtSession[]{recognizer,detector})if(session!=null)try{session.close();}catch(Exception e){if(failure==null)failure=e;else failure.addSuppressed(e);}recognizer=null;detector=null;if(failure!=null)throw new IllegalStateException("문자 모델 종료",failure);}
}
