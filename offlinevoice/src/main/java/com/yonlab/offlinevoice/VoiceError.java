package com.yonlab.offlinevoice;
final class VoiceError {
 private VoiceError(){}
 static String message(String code){switch(code==null?"":code){
  case "TOO_LONG":return "30초 이하 음성 구간을 선택하세요";
  case "TOO_LARGE":return "32MB 이하 음성 파일을 선택하세요";
  case "EMPTY_AUDIO":case "NO_AUDIO_TRACK":return "소리가 담긴 음성 파일을 선택하세요";
  case "TRUNCATED":case "MALFORMED":case "INVALID_PCM":return "손상된 음성 파일입니다. 원본을 다시 선택하세요";
  case "SOURCE_UNAVAILABLE":case "SOURCE_PERMISSION":return "음성 파일을 다시 선택해 접근 권한을 연결하세요";
  case "SOURCE_TIMEOUT":case "SOURCE_READ":return "음성 읽기를 마치지 못했습니다. 내려받은 파일을 선택하세요";
  case "SOURCE_BUSY":return "파일 제공자가 응답하지 않습니다. 다른 파일을 선택하세요";
  case "UNSUPPORTED_CONTAINER":case "UNSUPPORTED_FORMAT":case "CODEC_UNAVAILABLE":return "음성 형식을 변환할 수 없습니다. WAV 파일을 선택하세요";
  case "INPUT_CLEANUP":case "MODEL_CLEANUP":return "자원 정리를 마친 뒤 다시 실행하세요";
  case "OutOfMemoryError":return "메모리를 확보한 뒤 다시 실행하세요";
  case "UnsatisfiedLinkError":case "NoClassDefFoundError":case "IllegalArgumentException":return "음성 엔진을 준비하지 못했습니다. 다른 모델을 선택하세요";
  default:return "음성 실행을 마치지 못했습니다. 다른 입력이나 모델로 다시 실행하세요";
 }}
}
