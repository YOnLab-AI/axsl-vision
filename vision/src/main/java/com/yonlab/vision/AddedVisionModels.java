package com.yonlab.vision;

/** Exact bundled assets. Added pooled outputs do not replace any trained tensor. */
final class AddedVisionModels {
 private AddedVisionModels(){}
 static final String ROOT="one/modelplus/vision/";
 static String asset(String id){switch(id){
  case "nanodet-plus":return ROOT+"nanodet.onnx";
  case "efficientdet-lite1":return ROOT+"efficientdet-lite1.tflite";
  case "resnet18-v1":case "search-resnet18":return ROOT+"resnet18-v1.onnx";
  case "squeezenet1-1":case "search-squeezenet":return ROOT+"squeezenet1-1.onnx";
  case "segment-modnet":case "studio-modnet":return ROOT+"modnet.onnx";
  case "segment-selfie-landscape":case "studio-selfie-landscape":return ROOT+"selfie-landscape.onnx";
  default:throw new IllegalArgumentException("비교 모델을 선택하세요");}}
 static String sha(String id){switch(id){
  case "nanodet-plus":return "4b82da9944b88577175ee23a459dce2e26e6e4be573def65b1055dc2d9720186";
  case "efficientdet-lite1":return "183afa66aad648b0dc16519ae1f55646d9cd38d2b544b917a9ab1da320ebed75";
  case "resnet18-v1":case "search-resnet18":return "a587e02c452cfa2caa579b00d4354ef21190b2be691039b179b3f1412ae9b210";
  case "squeezenet1-1":case "search-squeezenet":return "69fed662717dec53395aff9a072b59be71acd7a666ea45fa5ec2e8a200d27e11";
  case "segment-modnet":case "studio-modnet":return "07c308cf0fc7e6e8b2065a12ed7fc07e1de8febb7dc7839d7b7f15dd66584df9";
  case "segment-selfie-landscape":case "studio-selfie-landscape":return "7a0adcfdb1715d3b0ff0f61486d8a181c33f4a208343a5abfccbc6540e872d24";
  default:throw new IllegalArgumentException("모델 파일을 확인하세요");}}
 static boolean object(String id){return "nanodet-plus".equals(id)||"efficientdet-lite1".equals(id);}
 static boolean classifier(String id){return "resnet18-v1".equals(id)||"squeezenet1-1".equals(id);}
 static boolean retrieval(String id){return "search-resnet18".equals(id)||"search-squeezenet".equals(id);}
 static boolean portrait(String id){return "segment-modnet".equals(id)||"studio-modnet".equals(id)||"segment-selfie-landscape".equals(id)||"studio-selfie-landscape".equals(id);}
}
