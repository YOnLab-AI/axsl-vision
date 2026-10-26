package com.yonlab.vision;
/** Pure compatibility policy. IDs denote actual models/engines, never performance presets. */
public final class OneModelPolicy {
 private OneModelPolicy(){}
 public static String[] models(String feature,int sdk){return ModelPlusPolicy.append(canonicalFeature(feature),sdk,baseModels(feature,sdk));}
 private static String[] baseModels(String feature,int sdk){
  if(feature==null)throw new IllegalArgumentException("기능을 선택하세요");
  switch(feature){
   case "detect":return sdk>=24?new String[]{"efficientdet-lite0","ssd-mobilenet-v1","yolox-nano"}:new String[]{"ssd-mobilenet-v1"};
   case "flow":return sdk>=24?new String[]{"ssd-mobilenet-v1","efficientdet-lite0","yolox-nano"}:new String[]{"ssd-mobilenet-v1"};
   case "classify":return sdk>=24?new String[]{"mobilenet-v4-fp32","mobilenet-v2-2022","efficientnet-lite0-f32","efficientnet-lite0-i8"}:new String[0];
   case "segment":return sdk>=24?new String[]{"pphumanseg-2023","selfie-segmentation","deeplab-v3"}:new String[]{"pphumanseg-2023"};
   case "faces":case "face":case "access":case "compare":case "vis":return sdk>=24?new String[]{"mlkit-sface-2023","yunet-2026-sface"}:new String[]{"mlkit-sface-2023"};
   case "drive":return new String[]{"mlkit-face-contours"};
   case "pose":return new String[]{"mlkit-pose","mlkit-pose-accurate","movenet-lightning","movenet-thunder"};
   case "ocr":return new String[]{"mlkit-ocr-ko","mlkit-ocr-latin"};
   case "qr":return new String[]{"zxing-qr","mlkit-barcode"};
   case "document":return new String[]{"doc-ocr-ko","doc-ocr-latin"};
   case "studio":return new String[]{"studio-pphuman","studio-selfie"};
   case "search":return new String[]{"search-tinyclip-q8","search-mobilenet-v3-small","search-mobilenet-v3-large"};
   case "sound":return new String[]{"yamnet-classifier","yamnet-original"};
   case "coach":return new String[]{"coach-mlkit","coach-mlkit-accurate","coach-movenet-lightning","coach-movenet-thunder"};
   case "signage":return new String[]{"android-media"};
   // Speech IDs are owned by the separate offlinevoice module.
   case "speech":return new String[0];
   default:throw new IllegalArgumentException("지원하는 기능을 선택하세요");
  }
 }
 public static String defaultId(String feature,int sdk){String[] ids=models(feature,sdk);return ids.length==0?"":ids[0];}
 public static String defaultId(String feature,int sdk,String group){return "document".equals(feature)?"GO".equals(group)?"doc-ocr-latin":"doc-ocr-ko":"ocr".equals(feature)?"GO".equals(group)?"mlkit-ocr-latin":"mlkit-ocr-ko":defaultId(feature,sdk);}
 public static void validate(String feature,String id,int sdk){for(String allowed:models(feature,sdk))if(allowed.equals(id))return;throw new IllegalArgumentException("이 기능에 맞는 모델을 선택하세요");}
 public static String canonicalFeature(String value){return "face".equals(value)?"faces":"lab".equals(value)?"faces":value;}
}
