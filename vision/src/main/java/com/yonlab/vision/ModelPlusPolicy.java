package com.yonlab.vision;
import java.util.*;

/** Additional trained models or genuinely distinct code-recognition engines. */
public final class ModelPlusPolicy {
 private ModelPlusPolicy(){}
 public static String[] append(String feature,int sdk,String[] base){if(sdk<24)return base;String[] more=added(feature);String[] out=Arrays.copyOf(base,base.length+more.length);System.arraycopy(more,0,out,base.length,more.length);return out;}
 public static String[] added(String f){switch(f){
  case "detect":case "flow":return new String[]{"nanodet-plus","efficientdet-lite1"};
  case "classify":return new String[]{"resnet18-v1","squeezenet1-1"};
  case "segment":return new String[]{"segment-modnet","segment-selfie-landscape"};
  case "studio":return new String[]{"studio-modnet","studio-selfie-landscape"};
  case "search":return new String[]{"search-resnet18","search-squeezenet"};
  case "faces":case "face":case "access":case "compare":case "vis":case "signage":return new String[]{"blazeface-short-sface","blazeface-full-sface"};
  case "drive":return new String[]{"face-mesh-468","face-landmarker-478"};
  case "pose":return new String[]{"posenet-mobilenet","blazepose-full"};
  case "coach":return new String[]{"coach-posenet","coach-blazepose"};
  case "ocr":return new String[]{"paddle-ocr-v5-ko","paddle-ocr-v6-ko","paddle-ocr-v6-tiny"};
  case "document":return new String[]{"doc-paddle-v5-ko","doc-paddle-v6-ko","doc-paddle-v6-tiny"};
  case "qr":return new String[]{"opencv-qr","opencv-qr-aruco"};
  case "sound":return new String[]{"panns-cnn6","panns-cnn10"};
  default:return new String[0];
 }}
 public static String label(String id){switch(id){
  case "nanodet-plus":return "NanoDet Plus";case "efficientdet-lite1":return "EfficientDet Lite1";
  case "resnet18-v1":return "ResNet18";case "squeezenet1-1":return "SqueezeNet 1.1";
  case "segment-modnet":case "studio-modnet":return "MODNet Portrait Matting";
  case "segment-selfie-landscape":case "studio-selfie-landscape":return "MediaPipe Selfie Landscape";
  case "search-resnet18":return "ResNet18 · 512D";case "search-squeezenet":return "SqueezeNet · 512D";
  case "blazeface-short-sface":return "BlazeFace Short · SFace";case "blazeface-full-sface":return "BlazeFace Full · SFace";
  case "face-mesh-468":return "FaceMesh 468";case "face-landmarker-478":return "FaceLandmarker 478";
  case "posenet-mobilenet":case "coach-posenet":return "PoseNet MobileNet";case "blazepose-full":case "coach-blazepose":return "BlazePose Full";
  case "paddle-ocr-v5-ko":case "doc-paddle-v5-ko":return "PaddleOCR v5 · 한국어";
  case "paddle-ocr-v6-ko":case "doc-paddle-v6-ko":return "PaddleOCR v6 Detect · 한국어";
  case "paddle-ocr-v6-tiny":case "doc-paddle-v6-tiny":return "PaddleOCR v6 Tiny · 영문·중문";
  case "opencv-qr":return "OpenCV QR · 기본 검출";case "opencv-qr-aruco":return "OpenCV QR · ArUco 검출";
  case "panns-cnn6":return "PANNs CNN6";case "panns-cnn10":return "PANNs CNN10";default:return null;
 }}
 public static String description(String id){switch(id){
  case "nanodet-plus":return "COCO 80종 · 경량 anchor-free 검출 · ONNX";
  case "efficientdet-lite1":return "COCO 객체 탐지 · Lite1 별도 학습 가중치 · TFLite";
  case "resnet18-v1":return "ImageNet 1,000종 · residual CNN 비교";case "squeezenet1-1":return "ImageNet 1,000종 · 작은 Fire-module CNN 비교";
  case "segment-modnet":case "studio-modnet":return "인물 알파 매팅 · 연속 투명도 경계";
  case "segment-selfie-landscape":case "studio-selfie-landscape":return "144×256 인물 분리 · 가로 입력용 별도 가중치";
  case "search-resnet18":case "search-squeezenet":return "분류기 앞 실제 512차원 시각 특징 · 같은 앨범 코사인 검색";
  case "blazeface-short-sface":return "근거리 얼굴 검출 · YuNet 정렬 · 기존 SFace 등록과 연결";
  case "blazeface-full-sface":return "전 범위 얼굴 검출 · YuNet 정렬 · 기존 SFace 등록과 연결";
  case "face-mesh-468":return "실제 468점 · 눈 비율(EAR)·입·고개 기하 관측";
  case "face-landmarker-478":return "실제 478점 · 눈 비율(EAR)·입·고개 기하 관측";
  case "posenet-mobilenet":case "coach-posenet":return "MobileNet PoseNet · 실제 COCO 17관절";
  case "blazepose-full":case "coach-blazepose":return "BlazePose 33점 · COCO 17관절 연결";
  case "paddle-ocr-v5-ko":case "doc-paddle-v5-ko":return "PP-OCRv5 Mobile 검출 + 한국어 v5 인식";
  case "paddle-ocr-v6-ko":case "doc-paddle-v6-ko":return "PP-OCRv6 Tiny 검출 + 한국어 v5 인식 · 검출기 차이 비교";
  case "paddle-ocr-v6-tiny":case "doc-paddle-v6-tiny":return "PP-OCRv6 Tiny 검출·인식 · 영문·중문 비교";
  case "opencv-qr":return "QR 전용 · OpenCV 사각 패턴 검출·해독 엔진";
  case "opencv-qr-aruco":return "QR 전용 · 다른 ArUco 패턴 검출기와 공통 QR 해독";
  case "panns-cnn6":return "AudioSet 527종 · 6층 CNN · 32kHz log-mel";
  case "panns-cnn10":return "AudioSet 527종 · 10층 CNN · 32kHz log-mel";default:return null;
 }}
 static boolean paddle(String id){return id!=null&&(id.startsWith("paddle-ocr-")||id.startsWith("doc-paddle-"));}
 static String ocrId(String id){if(id==null)return "mlkit-ocr-ko";if(id.startsWith("doc-paddle-"))return "paddle-ocr-"+id.substring("doc-paddle-".length());return "doc-ocr-ko".equals(id)?"mlkit-ocr-ko":"doc-ocr-latin".equals(id)?"mlkit-ocr-latin":id;}
 static boolean addedQr(String id){return "opencv-qr".equals(id)||"opencv-qr-aruco".equals(id);}
}
