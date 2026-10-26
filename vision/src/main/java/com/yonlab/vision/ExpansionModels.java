package com.yonlab.vision;
/** Names describe actual shipped weights; shared pipelines do not create fictitious models. */
final class ExpansionModels {
 private ExpansionModels(){}
 static String label(String id){switch(id){
 case "yolox-nano":return "YOLOX Nano";case "selfie-segmentation":return "ML Kit Selfie Segmentation";case "deeplab-v3":return "DeepLabV3 MobileNet";case "efficientdet-lite0":return "EfficientDet Lite0 INT8";
 case "efficientnet-lite0-f32":return "EfficientNet Lite0 FP32";
 case "efficientnet-lite0-i8":return "EfficientNet Lite0 INT8";
 case "movenet-lightning":case "coach-movenet-lightning":return "MoveNet Lightning";
 case "movenet-thunder":case "coach-movenet-thunder":return "MoveNet Thunder";
 case "mlkit-barcode":return "ML Kit Barcode";
 case "doc-ocr-ko":return "ML Kit Korean OCR";
 case "doc-ocr-latin":return "ML Kit Latin OCR";
 case "studio-pphuman":return "PPHumanSeg";
 case "studio-selfie":return "ML Kit Selfie Segmentation";
 case "search-tinyclip-q8":return "TinyCLIP ViT 8M INT8";
 case "search-mobilenet-v3-small":return "MobileNetV3 Small Embedding";
 case "search-mobilenet-v3-large":return "MobileNetV3 Large Embedding";
 case "yamnet-classifier":return "YAMNet Mobile INT8";
 case "yamnet-original":return "YAMNet Original FP32";
 case "coach-mlkit":return "ML Kit Pose";
 case "coach-mlkit-accurate":return "ML Kit Pose Accurate";
 default:return null;}}
 static String description(String id){switch(id){
 case "yolox-nano":return "COCO 80종 · 가벼운 객체 탐지";case "selfie-segmentation":return "인물·배경 분리 · 부드러운 인물 경계";case "deeplab-v3":return "21개 장면 영역 · 사람·동물·물체 분할";case "efficientdet-lite0":return "COCO 객체 탐지 · SSD·YOLO와 같은 입력 비교";
 case "efficientnet-lite0-f32":case "efficientnet-lite0-i8":return "ImageNet 1,000종 · 실제 정밀도별 결과와 시간 비교";
 case "movenet-lightning":case "coach-movenet-lightning":return "17개 관절 · 빠른 자세 관측";
 case "movenet-thunder":case "coach-movenet-thunder":return "17개 관절 · 더 큰 자세 모델";
 case "mlkit-barcode":return "QR·바코드 · ZXing과 해석 결과 비교";
 case "doc-ocr-ko":return "한국어 문서 · 보정과 날짜·금액 정리";
 case "doc-ocr-latin":return "글로벌 문서 · 보정과 날짜·금액 정리";
 case "studio-pphuman":case "studio-selfie":return "인물 마스크 · 배경 교체·흐림·배경 제거";
 case "search-tinyclip-q8":return "시각 임베딩 · 같은 갤러리에서 가까운 사진 찾기";
 case "search-mobilenet-v3-small":return "작은 임베딩 모델 · 빠른 사진 유사도 검색";
 case "search-mobilenet-v3-large":return "큰 임베딩 모델 · 검색 순위와 응답 비교";
 case "yamnet-classifier":return "521종 생활 소리 · 모바일용 재학습·양자화 모델";
 case "yamnet-original":return "521종 생활 소리 · 원본 FP32 모델";
 case "coach-mlkit":case "coach-mlkit-accurate":return "33개 관절 · 각도·좌우 상태를 함께 관측";
 default:return null;}}
}
