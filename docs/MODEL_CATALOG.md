# 모델·엔진 카탈로그

AXSL Vision **0.1 / code21**의 실행 레지스트리를 설명합니다. 화면의 **기본 선택**은 실행 기본값이며 측정된 우열을 뜻하지 않습니다. 각 항목의 가중치·SDK·코드 엔진과 전후 처리를 분리해 기록합니다.

## 무엇을 세었는가

| 항목 | 값 | 해석 |
|---|---:|---|
| 기능 | 19 | 공개 작업 종류 |
| 기능별 모델·엔진 슬롯 | 87 | 기능과 선택 ID의 관계 수 |
| 고유 실행 ID | 68 | 문서·코치 등의 기능별 별칭 포함 |
| 역사적 bundled ONNX/TFLite/ZIP | 47 | 파일·아카이브 수, 독립 모델 수가 아님 |
| 코드 엔진 ID | 4 | android-media, zxing-qr, opencv-qr, opencv-qr-aruco |

한 ID가 여러 기능에 나타나며 한 파이프라인이 여러 가중치 파일을 씁니다. `doc-*`, `studio-*`, `coach-*`, 검색 별칭은 같은 파일을 재사용하기도 합니다. SDK 모델은 SDK 배포물에 포함되며 개별 모델 SHA를 추출하거나 만들어 넣지 않습니다. 68에서 코드 엔진을 뺀 수 역시 독립 가중치 수가 아닙니다.

기계가 읽을 수 있는 전체 관계는 [model-catalog.json](model-catalog.json), 파일/아카이브 SHA 목록은 [model-artifacts.json](model-artifacts.json)에 있습니다. 자산은 Release pack을 검증해 저장소의 원래 module path로 복원합니다. 아래 asset path는 Android AssetManager의 논리 경로입니다. Release pack의 lock/검증 절차는 [한국어 시작 안내](../README.ko.md)를 따릅니다.

## 런타임과 해석

- ONNX Runtime Android 1.24.3, LiteRT 1.4.2 Interpreter, OpenCV 4.13.0, ML Kit의 고정 AAR, Vosk 0.3.75 및 sherpa-onnx 2.0.0 local AAR를 사용합니다. 음성 AAR 내부 native 구성요소의 버전·고지는 일반 vision ORT와 따로 취급합니다.
- 설정의 auto/cpu1/cpu2/cpu4는 실행 프로필이며 모델 수에 포함하지 않습니다. SDK 내부 스케줄링은 앱이 요청한 CPU thread 수와 다를 수 있습니다.
- `modelSha256`는 구현이 보고하는 파일 또는 조합 fingerprint입니다. ML Kit SDK 내부 가중치와 선택적 속성 head를 항상 전부 덮는 해시라고 해석하지 않습니다. 개별 포함 파일은 아래 asset binding을 기준으로 추적합니다.
- PANNs는 가중치 CC-BY-4.0, 원본 코드 MIT입니다. ML Kit는 [별도 Google 약관](https://developers.google.com/ml-kit/terms) 대상입니다. 앱의 독자 코드 Apache-2.0을 모델·자료 전체로 확대하지 않습니다.
- 현재 **0.1/code21**은 [검증 요약](validation-0.1-build21.json)과 [BUILD21_STATUS](BUILD21_STATUS.md)에 기록한8개 ID의24회 실행만 완료했습니다. 나머지60개 ID는 이번 기록에서 기기 미시험입니다. 아래 **이전 공개0.1/code20**·**과거4.4.0/code19** 근거는 각각 원래 버전의 관측으로 유지합니다.

## 현재 code21 실행 범위

이번 기록의 실제 실행을 ID별로 나눈 수입니다. 같은 입력의 반복 호출과 보고서 회귀도 실행 수에 포함하며 독립 정확도 표본 수로 해석하지 않습니다. 이 ID를 쓰는 다른 기능까지 시험한 것으로 확대하지 않습니다.

| 실행 ID | 완료 실행 | 고유 encoded 입력 | 실제 시험 기능 |
|---|---:|---:|---|
| [efficientdet-lite0](#model-efficientdet-lite0) | 3 | 3 | `detect` |
| [mlkit-ocr-ko](#model-mlkit-ocr-ko) | 6 | 5 | `ocr` |
| [mobilenet-v2-2022](#model-mobilenet-v2-2022) | 1 | 1 | `classify` |
| [mobilenet-v4-fp32](#model-mobilenet-v4-fp32) | 4 | 4 | `classify` |
| [vosk-ko-small-0.22](#model-vosk-ko-small-0.22) | 5 | 4 | `speech` |
| [whisper-tiny-multilingual-int8](#model-whisper-tiny-multilingual-int8) | 2 | 2 | `speech` |
| [yamnet-classifier](#model-yamnet-classifier) | 1 | 1 | `sound` |
| [zxing-qr](#model-zxing-qr) | 2 | 2 | `qr` |

나머지60개 ID의 현재 상태는 **이번 code21 기록에서 미시험**입니다. 코드·자산의 존재와 실제 실행, 정확도 평가는 별개이며 각 카드에서 같은 기준으로 표시합니다. 이전 버전의 `historicalCase`와 `publicValidationCase`는 해당 versionCode를 유지합니다.

## 기능별 슬롯

| 기능 ID | 슬롯 | 실행 ID |
|---|---:|---|
| `detect` | 5 | [efficientdet-lite0](#model-efficientdet-lite0), [ssd-mobilenet-v1](#model-ssd-mobilenet-v1), [yolox-nano](#model-yolox-nano), [nanodet-plus](#model-nanodet-plus), [efficientdet-lite1](#model-efficientdet-lite1) |
| `classify` | 6 | [mobilenet-v4-fp32](#model-mobilenet-v4-fp32), [mobilenet-v2-2022](#model-mobilenet-v2-2022), [efficientnet-lite0-f32](#model-efficientnet-lite0-f32), [efficientnet-lite0-i8](#model-efficientnet-lite0-i8), [resnet18-v1](#model-resnet18-v1), [squeezenet1-1](#model-squeezenet1-1) |
| `segment` | 5 | [pphumanseg-2023](#model-pphumanseg-2023), [selfie-segmentation](#model-selfie-segmentation), [deeplab-v3](#model-deeplab-v3), [segment-modnet](#model-segment-modnet), [segment-selfie-landscape](#model-segment-selfie-landscape) |
| `faces` | 4 | [mlkit-sface-2023](#model-mlkit-sface-2023), [yunet-2026-sface](#model-yunet-2026-sface), [blazeface-short-sface](#model-blazeface-short-sface), [blazeface-full-sface](#model-blazeface-full-sface) |
| `compare` | 4 | [mlkit-sface-2023](#model-mlkit-sface-2023), [yunet-2026-sface](#model-yunet-2026-sface), [blazeface-short-sface](#model-blazeface-short-sface), [blazeface-full-sface](#model-blazeface-full-sface) |
| `access` | 4 | [mlkit-sface-2023](#model-mlkit-sface-2023), [yunet-2026-sface](#model-yunet-2026-sface), [blazeface-short-sface](#model-blazeface-short-sface), [blazeface-full-sface](#model-blazeface-full-sface) |
| `drive` | 3 | [mlkit-face-contours](#model-mlkit-face-contours), [face-mesh-468](#model-face-mesh-468), [face-landmarker-478](#model-face-landmarker-478) |
| `flow` | 5 | [ssd-mobilenet-v1](#model-ssd-mobilenet-v1), [efficientdet-lite0](#model-efficientdet-lite0), [yolox-nano](#model-yolox-nano), [nanodet-plus](#model-nanodet-plus), [efficientdet-lite1](#model-efficientdet-lite1) |
| `vis` | 4 | [mlkit-sface-2023](#model-mlkit-sface-2023), [yunet-2026-sface](#model-yunet-2026-sface), [blazeface-short-sface](#model-blazeface-short-sface), [blazeface-full-sface](#model-blazeface-full-sface) |
| `pose` | 6 | [mlkit-pose](#model-mlkit-pose), [mlkit-pose-accurate](#model-mlkit-pose-accurate), [movenet-lightning](#model-movenet-lightning), [movenet-thunder](#model-movenet-thunder), [posenet-mobilenet](#model-posenet-mobilenet), [blazepose-full](#model-blazepose-full) |
| `ocr` | 5 | [mlkit-ocr-ko](#model-mlkit-ocr-ko), [mlkit-ocr-latin](#model-mlkit-ocr-latin), [paddle-ocr-v5-ko](#model-paddle-ocr-v5-ko), [paddle-ocr-v6-ko](#model-paddle-ocr-v6-ko), [paddle-ocr-v6-tiny](#model-paddle-ocr-v6-tiny) |
| `qr` | 4 | [zxing-qr](#model-zxing-qr), [mlkit-barcode](#model-mlkit-barcode), [opencv-qr](#model-opencv-qr), [opencv-qr-aruco](#model-opencv-qr-aruco) |
| `speech` | 5 | [vosk-ko-small-0.22](#model-vosk-ko-small-0.22), [vosk-en-us-small-0.15](#model-vosk-en-us-small-0.15), [whisper-tiny-multilingual-int8](#model-whisper-tiny-multilingual-int8), [whisper-base-multilingual-int8](#model-whisper-base-multilingual-int8), [whisper-small-multilingual-int8](#model-whisper-small-multilingual-int8) |
| `signage` | 3 | [android-media](#model-android-media), [blazeface-short-sface](#model-blazeface-short-sface), [blazeface-full-sface](#model-blazeface-full-sface) |
| `document` | 5 | [doc-ocr-ko](#model-doc-ocr-ko), [doc-ocr-latin](#model-doc-ocr-latin), [doc-paddle-v5-ko](#model-doc-paddle-v5-ko), [doc-paddle-v6-ko](#model-doc-paddle-v6-ko), [doc-paddle-v6-tiny](#model-doc-paddle-v6-tiny) |
| `studio` | 4 | [studio-pphuman](#model-studio-pphuman), [studio-selfie](#model-studio-selfie), [studio-modnet](#model-studio-modnet), [studio-selfie-landscape](#model-studio-selfie-landscape) |
| `search` | 5 | [search-tinyclip-q8](#model-search-tinyclip-q8), [search-mobilenet-v3-small](#model-search-mobilenet-v3-small), [search-mobilenet-v3-large](#model-search-mobilenet-v3-large), [search-resnet18](#model-search-resnet18), [search-squeezenet](#model-search-squeezenet) |
| `sound` | 4 | [yamnet-classifier](#model-yamnet-classifier), [yamnet-original](#model-yamnet-original), [panns-cnn6](#model-panns-cnn6), [panns-cnn10](#model-panns-cnn10) |
| `coach` | 6 | [coach-mlkit](#model-coach-mlkit), [coach-mlkit-accurate](#model-coach-mlkit-accurate), [coach-movenet-lightning](#model-coach-movenet-lightning), [coach-movenet-thunder](#model-coach-movenet-thunder), [coach-posenet](#model-coach-posenet), [coach-blazepose](#model-coach-blazepose) |

## 실행 ID별 상세

<a id="model-android-media"></a>
### `android-media` — Android Media

| 항목 | 계약 |
|---|---|
| 종류 | 코드 엔진 |
| 사용 기능 | `signage` |
| 런타임 | Android ImageView / VideoView |
| 전처리 | 명시적으로 추가한 사진·영상의 content URI 및 앱 소유 사본 |
| 후처리·출력 | 순서·사진 간격·일시정지/재개·전체 화면·수명주기 제어 |
| 이용 조건 | 앱 코드 Apache-2.0; Android SDK/platform terms 및 입력 자료 조건 별도 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** AI 추론 모델 또는 맞춤 광고 모델이 아님

**별도 asset 파일:** 없음. 플랫폼/코드 엔진입니다.

**구현:** [SignageActivity.java](../vision/src/main/java/com/yonlab/vision/SignageActivity.java), [SignagePlaybackPolicy.java](../vision/src/main/java/com/yonlab/vision/SignagePlaybackPolicy.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://developer.android.com/reference/android/widget/VideoView).

<a id="model-blazeface-full-sface"></a>
### `blazeface-full-sface` — BlazeFace Full · SFace

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `faces`, `compare`, `access`, `vis`, `signage` |
| 런타임 | Google ML Kit / SDK-managed + OpenCV 4.13.0 + LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 선택 검출기와 ML Kit 관측 → YuNet 5점 정렬 → SFace 입력; 속성 heads는 기능별 선택 실행 |
| 후처리·출력 | SFace128D; compare/access/vis/선택적 signage에 서로 다른 후처리; faces/vis는 별도 expression/age 관측 가능 |
| 이용 조건 | YuNet MIT; SFace·속성·MediaPipe Apache-2.0; ML Kit는 별도 Google 약관 |
| 공유 관계 | faces/compare/access/vis가 동일 파이프라인 ID를 재사용; BlazeFace 두 ID는 signage에도 등록 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 모든 asset이 매 실행에 무조건 사용된다는 뜻이 아님. SDK 가중치 SHA는 별도 노출하지 않음. modelSha256 하나가 모든 SDK/선택적 head를 덮는다고 해석하지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `models/face_detection_yunet_2023mar.onnx` | model / 232589 | `8f2383e4dd3cfbb4553ea8718107fc0423210dc964f9f4280604804ed2552fa4` |
| `models/face_recognition_sface_2021dec.onnx` | model / 38696353 | `0ba9fbfa01b5270c96627c4ef784da859931e02f04419c829e83484087c34e79` |
| `one/modelplus/facepose/blazeface-full-sparse.tflite` | model / 676746 | `2c3728e6da56f21e21a320433396fb06d40d9088f2247c05e5635a688d45dfe1` |
| `models/facial_expression_recognition_mobilefacenet_2022july.onnx` | optional: faces/vis expression / 4791892 | `4f61307602fc089ce20488a31d4e4614e3c9753a7d6c41578c854858b183e1a9` |
| `models/age_gender_recognition_retail_0013_fp32.onnx` | optional: faces/vis age-head observation / 8558439 | `f4649ed528edcb0dbf3bb9474ffbd4fe32384f3c4b8e49493214464d0917e678` |

복원 경로: `vision/src/main/assets/models/face_detection_yunet_2023mar.onnx`, `vision/src/main/assets/models/face_recognition_sface_2021dec.onnx`, `vision/src/main/assets/one/modelplus/facepose/blazeface-full-sparse.tflite`, `vision/src/main/assets/models/facial_expression_recognition_mobilefacenet_2022july.onnx`, `vision/src/main/assets/models/age_gender_recognition_retail_0013_fp32.onnx`.

**구현:** [FaceEngine.java](../vision/src/main/java/com/yonlab/vision/FaceEngine.java), [Registry.java](../vision/src/main/java/com/yonlab/vision/Registry.java), [OneYuNet.java](../vision/src/main/java/com/yonlab/vision/OneYuNet.java), [PlusBlazeDetector.java](../vision/src/main/java/com/yonlab/vision/PlusBlazeDetector.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/facepose/catalog.json), [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt), [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://github.com/opencv/opencv_zoo/tree/main/models/face_detection_yunet), [source 2](https://github.com/opencv/opencv_zoo/tree/main/models/face_recognition_sface), [source 3](https://developers.google.com/ml-kit/terms), [source 4](https://storage.googleapis.com/mediapipe-assets/face_detection_full_range_sparse.tflite).

<a id="model-blazeface-short-sface"></a>
### `blazeface-short-sface` — BlazeFace Short · SFace

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `faces`, `compare`, `access`, `vis`, `signage` |
| 런타임 | Google ML Kit / SDK-managed + OpenCV 4.13.0 + LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 선택 검출기와 ML Kit 관측 → YuNet 5점 정렬 → SFace 입력; 속성 heads는 기능별 선택 실행 |
| 후처리·출력 | SFace128D; compare/access/vis/선택적 signage에 서로 다른 후처리; faces/vis는 별도 expression/age 관측 가능 |
| 이용 조건 | YuNet MIT; SFace·속성·MediaPipe Apache-2.0; ML Kit는 별도 Google 약관 |
| 공유 관계 | faces/compare/access/vis가 동일 파이프라인 ID를 재사용; BlazeFace 두 ID는 signage에도 등록 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 모든 asset이 매 실행에 무조건 사용된다는 뜻이 아님. SDK 가중치 SHA는 별도 노출하지 않음. modelSha256 하나가 모든 SDK/선택적 head를 덮는다고 해석하지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `models/face_detection_yunet_2023mar.onnx` | model / 232589 | `8f2383e4dd3cfbb4553ea8718107fc0423210dc964f9f4280604804ed2552fa4` |
| `models/face_recognition_sface_2021dec.onnx` | model / 38696353 | `0ba9fbfa01b5270c96627c4ef784da859931e02f04419c829e83484087c34e79` |
| `one/modelplus/facepose/blazeface-short.tflite` | model / 229032 | `3bc182eb9f33925d9e58b5c8d59308a760f4adea8f282370e428c51212c26633` |
| `models/facial_expression_recognition_mobilefacenet_2022july.onnx` | optional: faces/vis expression / 4791892 | `4f61307602fc089ce20488a31d4e4614e3c9753a7d6c41578c854858b183e1a9` |
| `models/age_gender_recognition_retail_0013_fp32.onnx` | optional: faces/vis age-head observation / 8558439 | `f4649ed528edcb0dbf3bb9474ffbd4fe32384f3c4b8e49493214464d0917e678` |

복원 경로: `vision/src/main/assets/models/face_detection_yunet_2023mar.onnx`, `vision/src/main/assets/models/face_recognition_sface_2021dec.onnx`, `vision/src/main/assets/one/modelplus/facepose/blazeface-short.tflite`, `vision/src/main/assets/models/facial_expression_recognition_mobilefacenet_2022july.onnx`, `vision/src/main/assets/models/age_gender_recognition_retail_0013_fp32.onnx`.

**구현:** [FaceEngine.java](../vision/src/main/java/com/yonlab/vision/FaceEngine.java), [Registry.java](../vision/src/main/java/com/yonlab/vision/Registry.java), [OneYuNet.java](../vision/src/main/java/com/yonlab/vision/OneYuNet.java), [PlusBlazeDetector.java](../vision/src/main/java/com/yonlab/vision/PlusBlazeDetector.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/facepose/catalog.json), [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt), [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://github.com/opencv/opencv_zoo/tree/main/models/face_detection_yunet), [source 2](https://github.com/opencv/opencv_zoo/tree/main/models/face_recognition_sface), [source 3](https://developers.google.com/ml-kit/terms), [source 4](https://storage.googleapis.com/mediapipe-assets/face_detection_short_range.tflite).

<a id="model-blazepose-full"></a>
### `blazepose-full` — BlazePose Full

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `pose` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | pose detector → hip-centered upright 256 ROI → RGB/255 |
| 후처리·출력 | 33점 출력 → COCO17 공통 관절 연결 → overlay/coach 기하 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | pose/coach 동일 detector+landmark 파일 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/facepose/blazepose-detector.tflite` | model / 2959046 | `9ba9dd3d42efaaba86b4ff0122b06f29c4122e756b329d89dca1e297fd8f866c` |
| `one/modelplus/facepose/blazepose-full.tflite` | model / 6440512 | `e9a5c5cb17f736fafd4c2ec1da3b3d331d6edbe8a0d32395855aeb2cdfd64b9f` |

복원 경로: `vision/src/main/assets/one/modelplus/facepose/blazepose-detector.tflite`, `vision/src/main/assets/one/modelplus/facepose/blazepose-full.tflite`.

**구현:** [PlusPoseEngine.java](../vision/src/main/java/com/yonlab/vision/PlusPoseEngine.java), [PlusBlazeDetector.java](../vision/src/main/java/com/yonlab/vision/PlusBlazeDetector.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/facepose/catalog.json).

**공식 upstream:** [source 1](https://github.com/google-ai-edge/mediapipe), [source 2](https://storage.googleapis.com/mediapipe-assets/pose_landmark_full.tflite), [source 3](https://storage.googleapis.com/mediapipe-assets/pose_detection.tflite).

<a id="model-coach-blazepose"></a>
### `coach-blazepose` — BlazePose Full

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `coach` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | pose detector → hip-centered upright 256 ROI → RGB/255 |
| 후처리·출력 | 33점 출력 → COCO17 공통 관절 연결 → overlay/coach 기하 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | pose/coach 동일 detector+landmark 파일 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/facepose/blazepose-detector.tflite` | model / 2959046 | `9ba9dd3d42efaaba86b4ff0122b06f29c4122e756b329d89dca1e297fd8f866c` |
| `one/modelplus/facepose/blazepose-full.tflite` | model / 6440512 | `e9a5c5cb17f736fafd4c2ec1da3b3d331d6edbe8a0d32395855aeb2cdfd64b9f` |

복원 경로: `vision/src/main/assets/one/modelplus/facepose/blazepose-detector.tflite`, `vision/src/main/assets/one/modelplus/facepose/blazepose-full.tflite`.

**구현:** [PlusPoseEngine.java](../vision/src/main/java/com/yonlab/vision/PlusPoseEngine.java), [PlusBlazeDetector.java](../vision/src/main/java/com/yonlab/vision/PlusBlazeDetector.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/facepose/catalog.json).

**공식 upstream:** [source 1](https://github.com/google-ai-edge/mediapipe), [source 2](https://storage.googleapis.com/mediapipe-assets/pose_landmark_full.tflite), [source 3](https://storage.googleapis.com/mediapipe-assets/pose_detection.tflite).

<a id="model-coach-mlkit"></a>
### `coach-mlkit` — ML Kit Pose

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `coach` |
| 런타임 | Google ML Kit / SDK-managed pose-detection:18.0.0-beta5 |
| 전처리 | InputImage bitmap → SDK single-image pose |
| 후처리·출력 | 33점/likelihood → overlay; coach 별칭은 각도·좌우 기하 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | pose/coach가 같은 SDK 모델; Accurate는 별도 모델 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [OnePoseEngine.java](../vision/src/main/java/com/yonlab/vision/OnePoseEngine.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/pose-detection), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-coach-mlkit-accurate"></a>
### `coach-mlkit-accurate` — ML Kit Pose Accurate

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `coach` |
| 런타임 | Google ML Kit / SDK-managed pose-detection-accurate:18.0.0-beta5 |
| 전처리 | InputImage bitmap → SDK single-image pose |
| 후처리·출력 | 33점/likelihood → overlay; coach 별칭은 각도·좌우 기하 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | pose/coach가 같은 SDK 모델; Accurate는 별도 모델 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [OnePoseEngine.java](../vision/src/main/java/com/yonlab/vision/OnePoseEngine.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/pose-detection), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-coach-movenet-lightning"></a>
### `coach-movenet-lightning` — MoveNet Lightning

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `coach` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 192 square black letterbox, RGB UINT8 NHWC |
| 후처리·출력 | 1×1×17×3 → 원본 좌표 → COCO17/공통 관절 ID → coach 기하 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | pose/coach 동일 파일 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/movenet-lightning.tflite` | model / 4758512 | `0fac2226112d0371903ca86e3853cec24ef603a0b2f96f589b180f0ebdd135ab` |

복원 경로: `vision/src/main/assets/one/models/movenet-lightning.tflite`.

**구현:** [OneMoveNetEngine.java](../vision/src/main/java/com/yonlab/vision/OneMoveNetEngine.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://www.kaggle.com/models/google/movenet).

<a id="model-coach-movenet-thunder"></a>
### `coach-movenet-thunder` — MoveNet Thunder

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `coach` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 256 square black letterbox, RGB UINT8 NHWC |
| 후처리·출력 | 1×1×17×3 → 원본 좌표 → COCO17/공통 관절 ID → coach 기하 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | pose/coach 동일 파일 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/movenet-thunder.tflite` | model / 12584128 | `41641538679ec79b07d4101e591dda47d098c09af29607674b2a40b8a3798dd3` |

복원 경로: `vision/src/main/assets/one/models/movenet-thunder.tflite`.

**구현:** [OneMoveNetEngine.java](../vision/src/main/java/com/yonlab/vision/OneMoveNetEngine.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://www.kaggle.com/models/google/movenet).

<a id="model-coach-posenet"></a>
### `coach-posenet` — PoseNet MobileNet

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `coach` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | RGB square input, (value−128)/128; PlusTensorModel 실제 shape 검증 |
| 후처리·출력 | heatmap/offset → COCO17 → 공통 관절; coach 각도 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | pose/coach 동일 파일 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/facepose/posenet-mobile.tflite` | model / 13269068 | `7bcc1efa3010de45640406a4ce0acc2ef89a742021f11adacdca2fd6e494fa07` |

복원 경로: `vision/src/main/assets/one/modelplus/facepose/posenet-mobile.tflite`.

**구현:** [PlusPoseEngine.java](../vision/src/main/java/com/yonlab/vision/PlusPoseEngine.java), [PlusTensorModel.java](../vision/src/main/java/com/yonlab/vision/PlusTensorModel.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/facepose/catalog.json).

**공식 upstream:** [source 1](https://github.com/tensorflow/examples/tree/master/lite/examples/posenet), [source 2](https://storage.googleapis.com/download.tensorflow.org/models/tflite/posenet_mobilenet_v1_100_257x257_multi_kpt_stripped.tflite).

<a id="model-deeplab-v3"></a>
### `deeplab-v3` — DeepLabV3 MobileNet

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `segment` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | OneDeepLabEngine이 입력 dtype/shape를 검증하고 RGB tensor 구성 |
| 후처리·출력 | PASCAL21 per-pixel argmax/영역 overlay |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/deeplab-v3.tflite` | model / 2780176 | `ff36e24d40547fe9e645e2f4e8745d1876d6e38b332d39a82f0bf0f5d1d561b3` |

복원 경로: `vision/src/main/assets/one/models/deeplab-v3.tflite`.

**구현:** [OneDeepLabEngine.java](../vision/src/main/java/com/yonlab/vision/OneDeepLabEngine.java), [SupplementalImageRunner.java](../vision/src/main/java/com/yonlab/vision/SupplementalImageRunner.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://www.kaggle.com/models/tensorflow/deeplabv3/tfLite/default/1).

<a id="model-doc-ocr-ko"></a>
### `doc-ocr-ko` — ML Kit Korean OCR

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `document` |
| 런타임 | Google ML Kit / SDK-managed text-recognition-korean:16.0.1 |
| 전처리 | InputImage bitmap → 선택 script recognizer; document는 보정된 이미지 |
| 후처리·출력 | text/block/bounds → 원문; doc 별칭은 날짜·금액 추출 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | ocr/document 동일 SDK 모델 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** KO/GO는 자료·언어 맥락이며 사람의 국적 추정이 아님

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [PhotoVisionEngine.java](../vision/src/main/java/com/yonlab/vision/PhotoVisionEngine.java), [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/text-recognition/v2), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-doc-ocr-latin"></a>
### `doc-ocr-latin` — ML Kit Latin OCR

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `document` |
| 런타임 | Google ML Kit / SDK-managed text-recognition:16.0.1 |
| 전처리 | InputImage bitmap → 선택 script recognizer; document는 보정된 이미지 |
| 후처리·출력 | text/block/bounds → 원문; doc 별칭은 날짜·금액 추출 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | ocr/document 동일 SDK 모델 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** KO/GO는 자료·언어 맥락이며 사람의 국적 추정이 아님

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [PhotoVisionEngine.java](../vision/src/main/java/com/yonlab/vision/PhotoVisionEngine.java), [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/text-recognition/v2), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-doc-paddle-v5-ko"></a>
### `doc-paddle-v5-ko` — PaddleOCR v5-ko

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `document` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU + OpenCV 4.13.0 |
| 전처리 | DB 검출 정규화 → 다각형 line crop/perspective → 인식기 입력 |
| 후처리·출력 | greedy CTC·사전 decode → line 순서 정리; document는 날짜/금액 후처리 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | ocr/document 동일 detector+recognizer+alphabet; v6-ko와 v5-ko는 한국어 v5 인식기 공유 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** v6-ko는 v6 detector + 한국어 v5 recognizer. v6-tiny는 영문·중문 비교용이며 한국어 전용 인식기 아님

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/ocr/paddle-v5-det.onnx` | model / 4826518 | `a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d` |
| `one/modelplus/ocr/paddle-v5-ko-rec.onnx` | model / 13418787 | `92f0b7785e64fc9090106a241cf4c1eb97472824558272751b88a2a4476d3a08` |
| `one/modelplus/ocr/paddle-v5-ko-rec-dict.json` | recognition alphabet / 83288 | `e600744eabcf1db6513518f7e09105cdb746ee0504ef110b25478147d9f0aa81` |

복원 경로: `vision/src/main/assets/one/modelplus/ocr/paddle-v5-det.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v5-ko-rec.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v5-ko-rec-dict.json`.

**구현:** [PaddleOcrEngine.java](../vision/src/main/java/com/yonlab/vision/PaddleOcrEngine.java), [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java).

**출처·고지 원장:** [source-models.json](../vision/src/main/assets/one/modelplus/ocr/source-models.json).

**공식 upstream:** [source 1](https://github.com/PaddlePaddle/PaddleOCR), [source 2](https://huggingface.co/PaddlePaddle), [source 3](https://huggingface.co/PaddlePaddle/korean_PP-OCRv5_mobile_rec_onnx/tree/5c6f574b8e2230adf4287b33e736d71b9fabd28e), [source 4](https://huggingface.co/PaddlePaddle/PP-OCRv5_mobile_det_onnx/tree/e6f4fa85f00e168c862bc462aebca69eef9b3d3d).

<a id="model-doc-paddle-v6-ko"></a>
### `doc-paddle-v6-ko` — PaddleOCR v6-ko

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `document` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU + OpenCV 4.13.0 |
| 전처리 | DB 검출 정규화 → 다각형 line crop/perspective → 인식기 입력 |
| 후처리·출력 | greedy CTC·사전 decode → line 순서 정리; document는 날짜/금액 후처리 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | ocr/document 동일 detector+recognizer+alphabet; v6-ko와 v5-ko는 한국어 v5 인식기 공유 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** v6-ko는 v6 detector + 한국어 v5 recognizer. v6-tiny는 영문·중문 비교용이며 한국어 전용 인식기 아님

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/ocr/paddle-v6-det.onnx` | model / 1780590 | `193bab7a04fca699a6c82e6abb5b81bdb28177f0abd4062552b04908dafb19f8` |
| `one/modelplus/ocr/paddle-v5-ko-rec.onnx` | model / 13418787 | `92f0b7785e64fc9090106a241cf4c1eb97472824558272751b88a2a4476d3a08` |
| `one/modelplus/ocr/paddle-v5-ko-rec-dict.json` | recognition alphabet / 83288 | `e600744eabcf1db6513518f7e09105cdb746ee0504ef110b25478147d9f0aa81` |

복원 경로: `vision/src/main/assets/one/modelplus/ocr/paddle-v6-det.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v5-ko-rec.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v5-ko-rec-dict.json`.

**구현:** [PaddleOcrEngine.java](../vision/src/main/java/com/yonlab/vision/PaddleOcrEngine.java), [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java).

**출처·고지 원장:** [source-models.json](../vision/src/main/assets/one/modelplus/ocr/source-models.json).

**공식 upstream:** [source 1](https://github.com/PaddlePaddle/PaddleOCR), [source 2](https://huggingface.co/PaddlePaddle), [source 3](https://huggingface.co/PaddlePaddle/PP-OCRv6_tiny_det_onnx/tree/2ba1506c0380b8f0b03dd142459aac66d4421f6c), [source 4](https://huggingface.co/PaddlePaddle/korean_PP-OCRv5_mobile_rec_onnx/tree/5c6f574b8e2230adf4287b33e736d71b9fabd28e).

<a id="model-doc-paddle-v6-tiny"></a>
### `doc-paddle-v6-tiny` — PaddleOCR v6-tiny

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `document` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU + OpenCV 4.13.0 |
| 전처리 | DB 검출 정규화 → 다각형 line crop/perspective → 인식기 입력 |
| 후처리·출력 | greedy CTC·사전 decode → line 순서 정리; document는 날짜/금액 후처리 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | ocr/document 동일 detector+recognizer+alphabet; v6-ko와 v5-ko는 한국어 v5 인식기 공유 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** v6-ko는 v6 detector + 한국어 v5 recognizer. v6-tiny는 영문·중문 비교용이며 한국어 전용 인식기 아님

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/ocr/paddle-v6-det.onnx` | model / 1780590 | `193bab7a04fca699a6c82e6abb5b81bdb28177f0abd4062552b04908dafb19f8` |
| `one/modelplus/ocr/paddle-v6-tiny-rec.onnx` | model / 4462639 | `9ef676d6ed3c88256a2d92c640c44f25b0c40947e111b14b8be8f594091563e6` |
| `one/modelplus/ocr/paddle-v6-tiny-rec-dict.json` | recognition alphabet / 47870 | `b9bf03b8b02c9c22f6af847769269d5c1f877cfc8914bbaecac6a44d7a6061de` |

복원 경로: `vision/src/main/assets/one/modelplus/ocr/paddle-v6-det.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v6-tiny-rec.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v6-tiny-rec-dict.json`.

**구현:** [PaddleOcrEngine.java](../vision/src/main/java/com/yonlab/vision/PaddleOcrEngine.java), [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java).

**출처·고지 원장:** [source-models.json](../vision/src/main/assets/one/modelplus/ocr/source-models.json).

**공식 upstream:** [source 1](https://github.com/PaddlePaddle/PaddleOCR), [source 2](https://huggingface.co/PaddlePaddle), [source 3](https://huggingface.co/PaddlePaddle/PP-OCRv6_tiny_det_onnx/tree/2ba1506c0380b8f0b03dd142459aac66d4421f6c), [source 4](https://huggingface.co/PaddlePaddle/PP-OCRv6_tiny_rec_onnx/tree/2612ab37152ae0a677521bae4e1e3d4fb4cf7c30).

<a id="model-efficientdet-lite0"></a>
### `efficientdet-lite0` — EfficientDet Lite0 COCO

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `detect`, `flow` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 원본 RGB를 모델이 요구하는 정사각 UINT8 텐서로 resize; 실제 tensor shape/type 검증 |
| 후처리·출력 | boxes/classes/scores/count → sparse COCO label 매핑 → 임계값/정규화 좌표 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | detect/flow가 같은 파일을 사용 |
| 검증 범위 | 0.1/code21: 실제 release controller 실행3건 완료; 시험 기능 detect; 고유 encoded 입력3개. 요청 ID·입력 SHA·정리 확인이며 전체 기능·정확도 수락과 구분; [현재 검증 요약](validation-0.1-build21.json) |

**과거 실행 근거:** 4.4.0/code19, `detect/ko-scene-04`, SM-F956N/API37. 현재0.1/code21의 실행 증거로 대체하지 않습니다.

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `models/ball-efficientdet.tflite` | model / 4563519 | `2e04c53bfeac0ac2a30c057c7e2a777594ce39baaac35a92f74fb1e8c4fc4e0b` |

복원 경로: `app/src/main/assets/models/ball-efficientdet.tflite`.

**이전 공개0.1/code20 실행 근거:** `ko-scene-04` / `detect`의 한 입력 실행. [공개 검증 요약](validation-0.1.json)에 hash·시간·평가 범위를 기록했습니다.

**구현:** [ObjectEngine.java](../vision/src/main/java/com/yonlab/vision/ObjectEngine.java), [OneObjectEngine.java](../vision/src/main/java/com/yonlab/vision/OneObjectEngine.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://www.kaggle.com/models/tensorflow/efficientdet).

<a id="model-efficientdet-lite1"></a>
### `efficientdet-lite1` — EfficientDet-Lite1 384

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `detect`, `flow` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | RGB direct resize384; uint8 values0..255; normalized [ymin,xmin,ymax,xmax] boxes; sparse COCO90 IDs |
| 후처리·출력 | 모델별 tensor decode/NMS → COCO 검출 box와 score |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 동일 asset을 사용하는 기능별 별칭을 따로 모델 가중치로 세지 않음 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/vision/efficientdet-lite1.tflite` | model / 6077535 | `183afa66aad648b0dc16519ae1f55646d9cd38d2b544b917a9ab1da320ebed75` |

복원 경로: `vision/src/main/assets/one/modelplus/vision/efficientdet-lite1.tflite`.

**구현:** [AddedVisionModels.java](../vision/src/main/java/com/yonlab/vision/AddedVisionModels.java), [AddedObjectEngine.java](../vision/src/main/java/com/yonlab/vision/AddedObjectEngine.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/vision/catalog.json).

**공식 upstream:** [source 1](https://www.kaggle.com/models/tensorflow/efficientdet/tfLite/lite1-detection-default/1), [source 2](https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/object_detection/android/lite-model_efficientdet_lite1_detection_metadata_1.tflite).

<a id="model-efficientnet-lite0-f32"></a>
### `efficientnet-lite0-f32` — EfficientNet Lite0 FP32

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `classify` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 정사각 center crop224; (RGB−127)/128; INT8 선택은 모델의 scale/zero-point로 UINT8 변환 |
| 후처리·출력 | 1000개 score; 양자화 출력은 dequantize 후 Top5 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** FP32/INT8는 서로 다른 파일이며 CPU thread preset을 모델로 센 것이 아님

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/efficientnet-lite0-f32.tflite` | model / 18582189 | `6c7ab0a6e5dcbf38a8c33b960996a55a3b4300b36a018c4545801de3a3c8bde0` |

복원 경로: `vision/src/main/assets/one/models/efficientnet-lite0-f32.tflite`.

**구현:** [OneEfficientNet.java](../vision/src/main/java/com/yonlab/vision/OneEfficientNet.java), [SupplementalImageRunner.java](../vision/src/main/java/com/yonlab/vision/SupplementalImageRunner.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://developers.google.com/edge/mediapipe/solutions/vision/image_classifier).

<a id="model-efficientnet-lite0-i8"></a>
### `efficientnet-lite0-i8` — EfficientNet Lite0 INT8

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `classify` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 정사각 center crop224; (RGB−127)/128; INT8 선택은 모델의 scale/zero-point로 UINT8 변환 |
| 후처리·출력 | 1000개 score; 양자화 출력은 dequantize 후 Top5 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** FP32/INT8는 서로 다른 파일이며 CPU thread preset을 모델로 센 것이 아님

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/efficientnet-lite0-i8.tflite` | model / 5434517 | `bc2ffe19c1118de0c0c2a9088992da5589722656e0fba81421385300a4a34b16` |

복원 경로: `vision/src/main/assets/one/models/efficientnet-lite0-i8.tflite`.

**구현:** [OneEfficientNet.java](../vision/src/main/java/com/yonlab/vision/OneEfficientNet.java), [SupplementalImageRunner.java](../vision/src/main/java/com/yonlab/vision/SupplementalImageRunner.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://developers.google.com/edge/mediapipe/solutions/vision/image_classifier).

<a id="model-face-landmarker-478"></a>
### `face-landmarker-478` — Face landmarks 478

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `drive` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU + OpenCV 4.13.0 |
| 전처리 | BlazeFace short → 가장 큰 얼굴의 눈 정렬 ROI, 1.5× margin → RGB/255 |
| 후처리·출력 | 실제 478점 → EAR·입 열림 비율·solvePnP 자세 → Drive 시간 상태 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 두 모델 모두 같은 BlazeFace short detector 사용 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 478점은 blendshape 출력이나 감정 분류 478종을 뜻하지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/facepose/blazeface-short.tflite` | model / 229032 | `3bc182eb9f33925d9e58b5c8d59308a760f4adea8f282370e428c51212c26633` |
| `one/modelplus/facepose/face-landmarker-478.tflite` | model / 2553590 | `c7d54204ce0448474c7f3fa9af494787c0965cbdd6f20fc72867e43046bd43d5` |

복원 경로: `vision/src/main/assets/one/modelplus/facepose/blazeface-short.tflite`, `vision/src/main/assets/one/modelplus/facepose/face-landmarker-478.tflite`.

**구현:** [PlusFaceLandmarks.java](../vision/src/main/java/com/yonlab/vision/PlusFaceLandmarks.java), [PlusTensorModel.java](../vision/src/main/java/com/yonlab/vision/PlusTensorModel.java), [Analytics.java](../vision/src/main/java/com/yonlab/vision/Analytics.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/facepose/catalog.json).

**공식 upstream:** [source 1](https://github.com/google-ai-edge/mediapipe), [source 2](https://storage.googleapis.com/mediapipe-assets/face_detection_short_range.tflite), [source 3](https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task).

<a id="model-face-mesh-468"></a>
### `face-mesh-468` — Face landmarks 468

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `drive` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU + OpenCV 4.13.0 |
| 전처리 | BlazeFace short → 가장 큰 얼굴의 눈 정렬 ROI, 1.5× margin → RGB/255 |
| 후처리·출력 | 실제 468점 → EAR·입 열림 비율·solvePnP 자세 → Drive 시간 상태 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 두 모델 모두 같은 BlazeFace short detector 사용 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 478점은 blendshape 출력이나 감정 분류 478종을 뜻하지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/facepose/blazeface-short.tflite` | model / 229032 | `3bc182eb9f33925d9e58b5c8d59308a760f4adea8f282370e428c51212c26633` |
| `one/modelplus/facepose/face-mesh-468.tflite` | model / 1242398 | `1055cb9d4a9ca8b8c688902a3a5194311138ba256bcc94e336d8373a5f30c814` |

복원 경로: `vision/src/main/assets/one/modelplus/facepose/blazeface-short.tflite`, `vision/src/main/assets/one/modelplus/facepose/face-mesh-468.tflite`.

**구현:** [PlusFaceLandmarks.java](../vision/src/main/java/com/yonlab/vision/PlusFaceLandmarks.java), [PlusTensorModel.java](../vision/src/main/java/com/yonlab/vision/PlusTensorModel.java), [Analytics.java](../vision/src/main/java/com/yonlab/vision/Analytics.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/facepose/catalog.json).

**공식 upstream:** [source 1](https://github.com/google-ai-edge/mediapipe), [source 2](https://storage.googleapis.com/mediapipe-assets/face_detection_short_range.tflite), [source 3](https://storage.googleapis.com/mediapipe-assets/face_landmark.tflite).

<a id="model-mlkit-barcode"></a>
### `mlkit-barcode` — ML Kit Barcode

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `qr` |
| 런타임 | Google ML Kit / SDK-managed barcode-scanning:17.3.0 |
| 전처리 | InputImage → SDK barcode scanner |
| 후처리·출력 | 여러 코드의 payload/format/bounds; QR 정답은 실제 문자열/format과 비교 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [OneBarcodeEngine.java](../vision/src/main/java/com/yonlab/vision/OneBarcodeEngine.java), [SupplementalImageRunner.java](../vision/src/main/java/com/yonlab/vision/SupplementalImageRunner.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/barcode-scanning), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-mlkit-face-contours"></a>
### `mlkit-face-contours` — ML Kit Face Contours

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `drive` |
| 런타임 | Google ML Kit / SDK-managed face-detection:16.1.7 |
| 전처리 | InputImage → ML Kit 얼굴/contour |
| 후처리·출력 | 눈 열림 확률·머리 각도·입 기하 → Analytics.Drive 시간 상태 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 눈 열림 확률과 EAR 기하 비율은 다른 값

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [FaceEngine.java](../vision/src/main/java/com/yonlab/vision/FaceEngine.java), [Analytics.java](../vision/src/main/java/com/yonlab/vision/Analytics.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/face-detection), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-mlkit-ocr-ko"></a>
### `mlkit-ocr-ko` — ML Kit Korean OCR

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `ocr` |
| 런타임 | Google ML Kit / SDK-managed text-recognition-korean:16.0.1 |
| 전처리 | InputImage bitmap → 선택 script recognizer; document는 보정된 이미지 |
| 후처리·출력 | text/block/bounds → 원문; doc 별칭은 날짜·금액 추출 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | ocr/document 동일 SDK 모델 |
| 검증 범위 | 0.1/code21: 실제 release controller 실행6건 완료; 시험 기능 ocr; 고유 encoded 입력5개. 요청 ID·입력 SHA·정리 확인이며 전체 기능·정확도 수락과 구분; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** KO/GO는 자료·언어 맥락이며 사람의 국적 추정이 아님

**과거 실행 근거:** 4.4.0/code19, `ocr/ko-ocr-1-normal`, SM-F956N/API37. 현재0.1/code21의 실행 증거로 대체하지 않습니다.

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**이전 공개0.1/code20 실행 근거:** `ko-ocr-1-normal` / `ocr`의 한 입력 실행. [공개 검증 요약](validation-0.1.json)에 hash·시간·평가 범위를 기록했습니다.

**구현:** [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [PhotoVisionEngine.java](../vision/src/main/java/com/yonlab/vision/PhotoVisionEngine.java), [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/text-recognition/v2), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-mlkit-ocr-latin"></a>
### `mlkit-ocr-latin` — ML Kit Latin OCR

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `ocr` |
| 런타임 | Google ML Kit / SDK-managed text-recognition:16.0.1 |
| 전처리 | InputImage bitmap → 선택 script recognizer; document는 보정된 이미지 |
| 후처리·출력 | text/block/bounds → 원문; doc 별칭은 날짜·금액 추출 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | ocr/document 동일 SDK 모델 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** KO/GO는 자료·언어 맥락이며 사람의 국적 추정이 아님

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [PhotoVisionEngine.java](../vision/src/main/java/com/yonlab/vision/PhotoVisionEngine.java), [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/text-recognition/v2), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-mlkit-pose"></a>
### `mlkit-pose` — ML Kit Pose

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `pose` |
| 런타임 | Google ML Kit / SDK-managed pose-detection:18.0.0-beta5 |
| 전처리 | InputImage bitmap → SDK single-image pose |
| 후처리·출력 | 33점/likelihood → overlay; coach 별칭은 각도·좌우 기하 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | pose/coach가 같은 SDK 모델; Accurate는 별도 모델 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [OnePoseEngine.java](../vision/src/main/java/com/yonlab/vision/OnePoseEngine.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/pose-detection), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-mlkit-pose-accurate"></a>
### `mlkit-pose-accurate` — ML Kit Pose Accurate

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `pose` |
| 런타임 | Google ML Kit / SDK-managed pose-detection-accurate:18.0.0-beta5 |
| 전처리 | InputImage bitmap → SDK single-image pose |
| 후처리·출력 | 33점/likelihood → overlay; coach 별칭은 각도·좌우 기하 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | pose/coach가 같은 SDK 모델; Accurate는 별도 모델 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [OnePoseEngine.java](../vision/src/main/java/com/yonlab/vision/OnePoseEngine.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/pose-detection), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-mlkit-sface-2023"></a>
### `mlkit-sface-2023` — ML Kit · YuNet · SFace

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `faces`, `compare`, `access`, `vis` |
| 런타임 | Google ML Kit / SDK-managed + OpenCV 4.13.0 |
| 전처리 | 선택 검출기와 ML Kit 관측 → YuNet 5점 정렬 → SFace 입력; 속성 heads는 기능별 선택 실행 |
| 후처리·출력 | SFace128D; compare/access/vis/선택적 signage에 서로 다른 후처리; faces/vis는 별도 expression/age 관측 가능 |
| 이용 조건 | YuNet MIT; SFace·속성·MediaPipe Apache-2.0; ML Kit는 별도 Google 약관 |
| 공유 관계 | faces/compare/access/vis가 동일 파이프라인 ID를 재사용; BlazeFace 두 ID는 signage에도 등록 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 모든 asset이 매 실행에 무조건 사용된다는 뜻이 아님. SDK 가중치 SHA는 별도 노출하지 않음. modelSha256 하나가 모든 SDK/선택적 head를 덮는다고 해석하지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `models/face_detection_yunet_2023mar.onnx` | model / 232589 | `8f2383e4dd3cfbb4553ea8718107fc0423210dc964f9f4280604804ed2552fa4` |
| `models/face_recognition_sface_2021dec.onnx` | model / 38696353 | `0ba9fbfa01b5270c96627c4ef784da859931e02f04419c829e83484087c34e79` |
| `models/facial_expression_recognition_mobilefacenet_2022july.onnx` | optional: faces/vis expression / 4791892 | `4f61307602fc089ce20488a31d4e4614e3c9753a7d6c41578c854858b183e1a9` |
| `models/age_gender_recognition_retail_0013_fp32.onnx` | optional: faces/vis age-head observation / 8558439 | `f4649ed528edcb0dbf3bb9474ffbd4fe32384f3c4b8e49493214464d0917e678` |

복원 경로: `vision/src/main/assets/models/face_detection_yunet_2023mar.onnx`, `vision/src/main/assets/models/face_recognition_sface_2021dec.onnx`, `vision/src/main/assets/models/facial_expression_recognition_mobilefacenet_2022july.onnx`, `vision/src/main/assets/models/age_gender_recognition_retail_0013_fp32.onnx`.

**구현:** [FaceEngine.java](../vision/src/main/java/com/yonlab/vision/FaceEngine.java), [Registry.java](../vision/src/main/java/com/yonlab/vision/Registry.java), [OneYuNet.java](../vision/src/main/java/com/yonlab/vision/OneYuNet.java), [PlusBlazeDetector.java](../vision/src/main/java/com/yonlab/vision/PlusBlazeDetector.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt), [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://github.com/opencv/opencv_zoo/tree/main/models/face_detection_yunet), [source 2](https://github.com/opencv/opencv_zoo/tree/main/models/face_recognition_sface), [source 3](https://developers.google.com/ml-kit/terms).

<a id="model-mobilenet-v2-2022"></a>
### `mobilenet-v2-2022` — MobileNetV2 FP32

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `classify` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU + OpenCV 4.13.0 |
| 전처리 | RGB 256×256 linear resize → center224 → ImageNet mean/std NCHW |
| 후처리·출력 | 1×1000 logits softmax → Top5 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 실제 release controller 실행1건 완료; 시험 기능 classify; 고유 encoded 입력1개. 요청 ID·입력 SHA·정리 확인이며 전체 기능·정확도 수락과 구분; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/mobilenet-v2-2022.onnx` | model / 13964571 | `c0c3f76d93fa3fd6580652a45618618a220fced18babf65774ed169de0432ad5` |

복원 경로: `vision/src/main/assets/one/models/mobilenet-v2-2022.onnx`.

**구현:** [OneOnnx.java](../vision/src/main/java/com/yonlab/vision/OneOnnx.java), [OneModelRunner.java](../vision/src/main/java/com/yonlab/vision/OneModelRunner.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://github.com/opencv/opencv_zoo/tree/main/models/image_classification_mobilenet).

<a id="model-mobilenet-v4-fp32"></a>
### `mobilenet-v4-fp32` — MobileNetV4 Small FP32

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `classify` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU + OpenCV 4.13.0 |
| 전처리 | RGB shortest-side256 bicubic → center224 → ImageNet mean/std NCHW |
| 후처리·출력 | 1×1000 logits softmax → Top5 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 실제 release controller 실행4건 완료; 시험 기능 classify; 고유 encoded 입력4개. 요청 ID·입력 SHA·정리 확인이며 전체 기능·정확도 수락과 구분; [현재 검증 요약](validation-0.1-build21.json) |

**과거 실행 근거:** 4.4.0/code19, `classify/ko-scene-08`, SM-F956N/API37. 현재0.1/code21의 실행 증거로 대체하지 않습니다.

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/mobilenet-v4-fp32.onnx` | model / 15069033 | `7a003770e4f863bd8e19e20a8389c5212552e256e0dacd833afaf8d0b26cdc02` |

복원 경로: `vision/src/main/assets/one/models/mobilenet-v4-fp32.onnx`.

**이전 공개0.1/code20 실행 근거:** `ko-scene-08` / `classify`의 한 입력 실행. [공개 검증 요약](validation-0.1.json)에 hash·시간·평가 범위를 기록했습니다.

**구현:** [OneOnnx.java](../vision/src/main/java/com/yonlab/vision/OneOnnx.java), [OneModelRunner.java](../vision/src/main/java/com/yonlab/vision/OneModelRunner.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://huggingface.co/timm/mobilenetv4_conv_small.e2400_r224_in1k).

<a id="model-movenet-lightning"></a>
### `movenet-lightning` — MoveNet Lightning

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `pose` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 192 square black letterbox, RGB UINT8 NHWC |
| 후처리·출력 | 1×1×17×3 → 원본 좌표 → COCO17/공통 관절 ID → coach 기하 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | pose/coach 동일 파일 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/movenet-lightning.tflite` | model / 4758512 | `0fac2226112d0371903ca86e3853cec24ef603a0b2f96f589b180f0ebdd135ab` |

복원 경로: `vision/src/main/assets/one/models/movenet-lightning.tflite`.

**구현:** [OneMoveNetEngine.java](../vision/src/main/java/com/yonlab/vision/OneMoveNetEngine.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://www.kaggle.com/models/google/movenet).

<a id="model-movenet-thunder"></a>
### `movenet-thunder` — MoveNet Thunder

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `pose` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 256 square black letterbox, RGB UINT8 NHWC |
| 후처리·출력 | 1×1×17×3 → 원본 좌표 → COCO17/공통 관절 ID → coach 기하 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | pose/coach 동일 파일 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/movenet-thunder.tflite` | model / 12584128 | `41641538679ec79b07d4101e591dda47d098c09af29607674b2a40b8a3798dd3` |

복원 경로: `vision/src/main/assets/one/models/movenet-thunder.tflite`.

**구현:** [OneMoveNetEngine.java](../vision/src/main/java/com/yonlab/vision/OneMoveNetEngine.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://www.kaggle.com/models/google/movenet).

<a id="model-nanodet-plus"></a>
### `nanodet-plus` — NanoDet-Plus m1.5x 416

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `detect`, `flow` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | RGB letterbox416 black; mean103.53/116.28/123.675, std57.375/57.12/58.395; DFL8 bins, strides8/16/32, per-class NMS IoU0.6, maximum100 detections |
| 후처리·출력 | 모델별 tensor decode/NMS → COCO 검출 box와 score |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 동일 asset을 사용하는 기능별 별칭을 따로 모델 가중치로 세지 않음 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/vision/nanodet.onnx` | model / 3800954 | `4b82da9944b88577175ee23a459dce2e26e6e4be573def65b1055dc2d9720186` |

복원 경로: `vision/src/main/assets/one/modelplus/vision/nanodet.onnx`.

**구현:** [AddedVisionModels.java](../vision/src/main/java/com/yonlab/vision/AddedVisionModels.java), [AddedObjectEngine.java](../vision/src/main/java/com/yonlab/vision/AddedObjectEngine.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/vision/catalog.json).

**공식 upstream:** [source 1](https://github.com/opencv/opencv_zoo/tree/main/models/object_detection_nanodet), [source 2](https://huggingface.co/opencv/object_detection_nanodet/resolve/5bfd47077350a726ad440dd7bd1e1e35e8ebcfb2/object_detection_nanodet_2022nov.onnx).

<a id="model-opencv-qr"></a>
### `opencv-qr` — OpenCV QR

| 항목 | 계약 |
|---|---|
| 종류 | 코드 엔진 |
| 사용 기능 | `qr` |
| 런타임 | OpenCV 4.13.0 |
| 전처리 | QR 전용 image → QRCodeDetector |
| 후처리·출력 | 검출된 QR의 공통 OpenCV 해독; payload/format |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 서로 다른 검출 경로와 공통 QR 해독; 두 가중치 모델이 아님 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**별도 asset 파일:** 없음. 플랫폼/코드 엔진입니다.

**구현:** [AddedQrRunner.java](../vision/src/main/java/com/yonlab/vision/AddedQrRunner.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://docs.opencv.org/4.x/de/dc3/classcv_1_1QRCodeDetector.html), [source 2](https://docs.opencv.org/4.x/d2/d8c/classcv_1_1QRCodeDetectorAruco.html).

<a id="model-opencv-qr-aruco"></a>
### `opencv-qr-aruco` — OpenCV QR ArUco

| 항목 | 계약 |
|---|---|
| 종류 | 코드 엔진 |
| 사용 기능 | `qr` |
| 런타임 | OpenCV 4.13.0 |
| 전처리 | QR 전용 image → QRCodeDetectorAruco |
| 후처리·출력 | 검출된 QR의 공통 OpenCV 해독; payload/format |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 서로 다른 검출 경로와 공통 QR 해독; 두 가중치 모델이 아님 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**별도 asset 파일:** 없음. 플랫폼/코드 엔진입니다.

**구현:** [AddedQrRunner.java](../vision/src/main/java/com/yonlab/vision/AddedQrRunner.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://docs.opencv.org/4.x/de/dc3/classcv_1_1QRCodeDetector.html), [source 2](https://docs.opencv.org/4.x/d2/d8c/classcv_1_1QRCodeDetectorAruco.html).

<a id="model-paddle-ocr-v5-ko"></a>
### `paddle-ocr-v5-ko` — PaddleOCR v5-ko

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `ocr` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU + OpenCV 4.13.0 |
| 전처리 | DB 검출 정규화 → 다각형 line crop/perspective → 인식기 입력 |
| 후처리·출력 | greedy CTC·사전 decode → line 순서 정리; document는 날짜/금액 후처리 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | ocr/document 동일 detector+recognizer+alphabet; v6-ko와 v5-ko는 한국어 v5 인식기 공유 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** v6-ko는 v6 detector + 한국어 v5 recognizer. v6-tiny는 영문·중문 비교용이며 한국어 전용 인식기 아님

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/ocr/paddle-v5-det.onnx` | model / 4826518 | `a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d` |
| `one/modelplus/ocr/paddle-v5-ko-rec.onnx` | model / 13418787 | `92f0b7785e64fc9090106a241cf4c1eb97472824558272751b88a2a4476d3a08` |
| `one/modelplus/ocr/paddle-v5-ko-rec-dict.json` | recognition alphabet / 83288 | `e600744eabcf1db6513518f7e09105cdb746ee0504ef110b25478147d9f0aa81` |

복원 경로: `vision/src/main/assets/one/modelplus/ocr/paddle-v5-det.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v5-ko-rec.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v5-ko-rec-dict.json`.

**구현:** [PaddleOcrEngine.java](../vision/src/main/java/com/yonlab/vision/PaddleOcrEngine.java), [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java).

**출처·고지 원장:** [source-models.json](../vision/src/main/assets/one/modelplus/ocr/source-models.json).

**공식 upstream:** [source 1](https://github.com/PaddlePaddle/PaddleOCR), [source 2](https://huggingface.co/PaddlePaddle), [source 3](https://huggingface.co/PaddlePaddle/korean_PP-OCRv5_mobile_rec_onnx/tree/5c6f574b8e2230adf4287b33e736d71b9fabd28e), [source 4](https://huggingface.co/PaddlePaddle/PP-OCRv5_mobile_det_onnx/tree/e6f4fa85f00e168c862bc462aebca69eef9b3d3d).

<a id="model-paddle-ocr-v6-ko"></a>
### `paddle-ocr-v6-ko` — PaddleOCR v6-ko

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `ocr` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU + OpenCV 4.13.0 |
| 전처리 | DB 검출 정규화 → 다각형 line crop/perspective → 인식기 입력 |
| 후처리·출력 | greedy CTC·사전 decode → line 순서 정리; document는 날짜/금액 후처리 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | ocr/document 동일 detector+recognizer+alphabet; v6-ko와 v5-ko는 한국어 v5 인식기 공유 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** v6-ko는 v6 detector + 한국어 v5 recognizer. v6-tiny는 영문·중문 비교용이며 한국어 전용 인식기 아님

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/ocr/paddle-v6-det.onnx` | model / 1780590 | `193bab7a04fca699a6c82e6abb5b81bdb28177f0abd4062552b04908dafb19f8` |
| `one/modelplus/ocr/paddle-v5-ko-rec.onnx` | model / 13418787 | `92f0b7785e64fc9090106a241cf4c1eb97472824558272751b88a2a4476d3a08` |
| `one/modelplus/ocr/paddle-v5-ko-rec-dict.json` | recognition alphabet / 83288 | `e600744eabcf1db6513518f7e09105cdb746ee0504ef110b25478147d9f0aa81` |

복원 경로: `vision/src/main/assets/one/modelplus/ocr/paddle-v6-det.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v5-ko-rec.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v5-ko-rec-dict.json`.

**구현:** [PaddleOcrEngine.java](../vision/src/main/java/com/yonlab/vision/PaddleOcrEngine.java), [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java).

**출처·고지 원장:** [source-models.json](../vision/src/main/assets/one/modelplus/ocr/source-models.json).

**공식 upstream:** [source 1](https://github.com/PaddlePaddle/PaddleOCR), [source 2](https://huggingface.co/PaddlePaddle), [source 3](https://huggingface.co/PaddlePaddle/PP-OCRv6_tiny_det_onnx/tree/2ba1506c0380b8f0b03dd142459aac66d4421f6c), [source 4](https://huggingface.co/PaddlePaddle/korean_PP-OCRv5_mobile_rec_onnx/tree/5c6f574b8e2230adf4287b33e736d71b9fabd28e).

<a id="model-paddle-ocr-v6-tiny"></a>
### `paddle-ocr-v6-tiny` — PaddleOCR v6-tiny

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `ocr` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU + OpenCV 4.13.0 |
| 전처리 | DB 검출 정규화 → 다각형 line crop/perspective → 인식기 입력 |
| 후처리·출력 | greedy CTC·사전 decode → line 순서 정리; document는 날짜/금액 후처리 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | ocr/document 동일 detector+recognizer+alphabet; v6-ko와 v5-ko는 한국어 v5 인식기 공유 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** v6-ko는 v6 detector + 한국어 v5 recognizer. v6-tiny는 영문·중문 비교용이며 한국어 전용 인식기 아님

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/ocr/paddle-v6-det.onnx` | model / 1780590 | `193bab7a04fca699a6c82e6abb5b81bdb28177f0abd4062552b04908dafb19f8` |
| `one/modelplus/ocr/paddle-v6-tiny-rec.onnx` | model / 4462639 | `9ef676d6ed3c88256a2d92c640c44f25b0c40947e111b14b8be8f594091563e6` |
| `one/modelplus/ocr/paddle-v6-tiny-rec-dict.json` | recognition alphabet / 47870 | `b9bf03b8b02c9c22f6af847769269d5c1f877cfc8914bbaecac6a44d7a6061de` |

복원 경로: `vision/src/main/assets/one/modelplus/ocr/paddle-v6-det.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v6-tiny-rec.onnx`, `vision/src/main/assets/one/modelplus/ocr/paddle-v6-tiny-rec-dict.json`.

**구현:** [PaddleOcrEngine.java](../vision/src/main/java/com/yonlab/vision/PaddleOcrEngine.java), [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java).

**출처·고지 원장:** [source-models.json](../vision/src/main/assets/one/modelplus/ocr/source-models.json).

**공식 upstream:** [source 1](https://github.com/PaddlePaddle/PaddleOCR), [source 2](https://huggingface.co/PaddlePaddle), [source 3](https://huggingface.co/PaddlePaddle/PP-OCRv6_tiny_det_onnx/tree/2ba1506c0380b8f0b03dd142459aac66d4421f6c), [source 4](https://huggingface.co/PaddlePaddle/PP-OCRv6_tiny_rec_onnx/tree/2612ab37152ae0a677521bae4e1e3d4fb4cf7c30).

<a id="model-panns-cnn10"></a>
### `panns-cnn10` — PANNs CNN10

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `sound` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | 32kHz native PCM → checkpoint Hann/mel tensors → log-mel 98×64; 0.975s window/0.480s hop |
| 후처리·출력 | 527종 AudioSet → window/mean/peak ranking; source/native PCM SHA 구분 |
| 이용 조건 | 가중치 CC-BY-4.0; 원본 코드 MIT; 앱 통합 코드 Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** source catalog의 runtimeInferenceTestPerformed=false/benchmarkPerformed=false를 전체 검증 PASS로 바꾸지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/audio/panns-cnn10.onnx` | model / 20875311 | `b6caad166df74fa25e1f1983b3e85a21ce6324a03458c123374a5f2703b21318` |
| `one/modelplus/audio/panns-cnn10-frontend.bin` | checkpoint-derived frontend tensors / 135424 | `59ee4583ee2fdd42a0738fa35c408aad80f65e85a43dcf1dfb61778260ce5a0d` |

복원 경로: `vision/src/main/assets/one/modelplus/audio/panns-cnn10.onnx`, `vision/src/main/assets/one/modelplus/audio/panns-cnn10-frontend.bin`.

**구현:** [PannSoundEngine.java](../vision/src/main/java/com/yonlab/vision/PannSoundEngine.java), [PannFrontend.java](../vision/src/main/java/com/yonlab/vision/PannFrontend.java), [PannModels.java](../vision/src/main/java/com/yonlab/vision/PannModels.java).

**출처·고지 원장:** [audio-models.json](../vision/src/main/assets/one/modelplus/audio/audio-models.json).

**공식 upstream:** [source 1](https://zenodo.org/records/3987831), [source 2](https://github.com/qiuqiangkong/audioset_tagging_cnn).

<a id="model-panns-cnn6"></a>
### `panns-cnn6` — PANNs CNN6

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `sound` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | 32kHz native PCM → checkpoint Hann/mel tensors → log-mel 98×64; 0.975s window/0.480s hop |
| 후처리·출력 | 527종 AudioSet → window/mean/peak ranking; source/native PCM SHA 구분 |
| 이용 조건 | 가중치 CC-BY-4.0; 원본 코드 MIT; 앱 통합 코드 Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** source catalog의 runtimeInferenceTestPerformed=false/benchmarkPerformed=false를 전체 검증 PASS로 바꾸지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/audio/panns-cnn6.onnx` | model / 19350324 | `00fd20f5fae17b5d5a1dadcc86f8fc394be5c55366e85455a835031c5f1731b3` |
| `one/modelplus/audio/panns-cnn6-frontend.bin` | checkpoint-derived frontend tensors / 135424 | `59ee4583ee2fdd42a0738fa35c408aad80f65e85a43dcf1dfb61778260ce5a0d` |

복원 경로: `vision/src/main/assets/one/modelplus/audio/panns-cnn6.onnx`, `vision/src/main/assets/one/modelplus/audio/panns-cnn6-frontend.bin`.

**구현:** [PannSoundEngine.java](../vision/src/main/java/com/yonlab/vision/PannSoundEngine.java), [PannFrontend.java](../vision/src/main/java/com/yonlab/vision/PannFrontend.java), [PannModels.java](../vision/src/main/java/com/yonlab/vision/PannModels.java).

**출처·고지 원장:** [audio-models.json](../vision/src/main/assets/one/modelplus/audio/audio-models.json).

**공식 upstream:** [source 1](https://zenodo.org/records/3987831), [source 2](https://github.com/qiuqiangkong/audioset_tagging_cnn).

<a id="model-posenet-mobilenet"></a>
### `posenet-mobilenet` — PoseNet MobileNet

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `pose` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | RGB square input, (value−128)/128; PlusTensorModel 실제 shape 검증 |
| 후처리·출력 | heatmap/offset → COCO17 → 공통 관절; coach 각도 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | pose/coach 동일 파일 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/facepose/posenet-mobile.tflite` | model / 13269068 | `7bcc1efa3010de45640406a4ce0acc2ef89a742021f11adacdca2fd6e494fa07` |

복원 경로: `vision/src/main/assets/one/modelplus/facepose/posenet-mobile.tflite`.

**구현:** [PlusPoseEngine.java](../vision/src/main/java/com/yonlab/vision/PlusPoseEngine.java), [PlusTensorModel.java](../vision/src/main/java/com/yonlab/vision/PlusTensorModel.java), [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/facepose/catalog.json).

**공식 upstream:** [source 1](https://github.com/tensorflow/examples/tree/master/lite/examples/posenet), [source 2](https://storage.googleapis.com/download.tensorflow.org/models/tflite/posenet_mobilenet_v1_100_257x257_multi_kpt_stripped.tflite).

<a id="model-pphumanseg-2023"></a>
### `pphumanseg-2023` — PPHumanSeg

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `segment` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | RGB192 → (value/255−0.5)/0.5 → NCHW |
| 후처리·출력 | 192×192 foreground/background → 원본 크기 마스크; studio는 합성 효과 후처리 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | segment와 studio가 같은 가중치 사용 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/pphumanseg-2023.onnx` | model / 6163938 | `552d8a984054e59b5d773d24b9b12022b22046ceb2bbc4c9aaeaceb36a9ddf24` |

복원 경로: `vision/src/main/assets/one/models/pphumanseg-2023.onnx`.

**구현:** [OneOnnx.java](../vision/src/main/java/com/yonlab/vision/OneOnnx.java), [BackgroundStudio.java](../vision/src/main/java/com/yonlab/vision/BackgroundStudio.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://github.com/opencv/opencv_zoo/tree/main/models/human_segmentation_pphumanseg).

<a id="model-resnet18-v1"></a>
### `resnet18-v1` — ResNet-18 v1

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `classify` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | ImageNet RGB shortest-side256, center224, /255, mean.485/.456/.406, std.229/.224/.225; retrieval L2-normalizes512 features |
| 후처리·출력 | 원래 classifier logits → Top5; search ID는 추가 pooled yon_embedding 512D → L2 정규화 → cosine 검색 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 동일 asset을 사용하는 기능별 별칭을 따로 모델 가중치로 세지 않음 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/vision/resnet18-v1.onnx` | model / 46820951 | `a587e02c452cfa2caa579b00d4354ef21190b2be691039b179b3f1412ae9b210` |

복원 경로: `vision/src/main/assets/one/modelplus/vision/resnet18-v1.onnx`.

**구현:** [AddedVisionModels.java](../vision/src/main/java/com/yonlab/vision/AddedVisionModels.java), [AddedVisionOnnx.java](../vision/src/main/java/com/yonlab/vision/AddedVisionOnnx.java), [AddedVisionImageRunner.java](../vision/src/main/java/com/yonlab/vision/AddedVisionImageRunner.java), [ImageRetrieval.java](../vision/src/main/java/com/yonlab/vision/ImageRetrieval.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/vision/catalog.json).

**공식 upstream:** [source 1](https://github.com/onnx/models/tree/c5cae0f1942c8d7727372c7b1f6b485aa39e4421/validated/vision/classification/resnet), [source 2](https://media.githubusercontent.com/media/onnx/models/c5cae0f1942c8d7727372c7b1f6b485aa39e4421/validated/vision/classification/resnet/model/resnet18-v1-7.onnx).

<a id="model-search-mobilenet-v3-large"></a>
### `search-mobilenet-v3-large` — MobileNetV3 Large Embedder

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `search` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | RGB resize224 /255; 모델 metadata mean0/std255 |
| 후처리·출력 | 실제 embedding → L2 → 동일 갤러리 cosine, 동일 픽셀 자기사본 제외 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 검색 순위는 정답률과 다름; 모델별 embedding cache 분리

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/search-mobilenet-v3-large.tflite` | model / 10889458 | `11af3c560dfeed7737cb4c03c23bf52a8403020784192d4dea0b74862a12828d` |

복원 경로: `vision/src/main/assets/one/models/search-mobilenet-v3-large.tflite`.

**구현:** [ImageRetrieval.java](../vision/src/main/java/com/yonlab/vision/ImageRetrieval.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://developers.google.com/edge/mediapipe/solutions/vision/image_embedder).

<a id="model-search-mobilenet-v3-small"></a>
### `search-mobilenet-v3-small` — MobileNetV3 Small Embedder

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `search` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | RGB resize224 /255; 모델 metadata mean0/std255 |
| 후처리·출력 | 실제 embedding → L2 → 동일 갤러리 cosine, 동일 픽셀 자기사본 제외 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 검색 순위는 정답률과 다름; 모델별 embedding cache 분리

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/search-mobilenet-v3-small.tflite` | model / 4117670 | `bbbb4c51a55a53905af1daec995ca1aae355046f8839bb8c9f5ce9271394bc40` |

복원 경로: `vision/src/main/assets/one/models/search-mobilenet-v3-small.tflite`.

**구현:** [ImageRetrieval.java](../vision/src/main/java/com/yonlab/vision/ImageRetrieval.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://developers.google.com/edge/mediapipe/solutions/vision/image_embedder).

<a id="model-search-resnet18"></a>
### `search-resnet18` — ResNet-18 v1

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `search` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | ImageNet RGB shortest-side256, center224, /255, mean.485/.456/.406, std.229/.224/.225; retrieval L2-normalizes512 features |
| 후처리·출력 | 원래 classifier logits → Top5; search ID는 추가 pooled yon_embedding 512D → L2 정규화 → cosine 검색 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 동일 asset을 사용하는 기능별 별칭을 따로 모델 가중치로 세지 않음 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/vision/resnet18-v1.onnx` | model / 46820951 | `a587e02c452cfa2caa579b00d4354ef21190b2be691039b179b3f1412ae9b210` |

복원 경로: `vision/src/main/assets/one/modelplus/vision/resnet18-v1.onnx`.

**구현:** [AddedVisionModels.java](../vision/src/main/java/com/yonlab/vision/AddedVisionModels.java), [AddedVisionOnnx.java](../vision/src/main/java/com/yonlab/vision/AddedVisionOnnx.java), [AddedVisionImageRunner.java](../vision/src/main/java/com/yonlab/vision/AddedVisionImageRunner.java), [ImageRetrieval.java](../vision/src/main/java/com/yonlab/vision/ImageRetrieval.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/vision/catalog.json).

**공식 upstream:** [source 1](https://github.com/onnx/models/tree/c5cae0f1942c8d7727372c7b1f6b485aa39e4421/validated/vision/classification/resnet), [source 2](https://media.githubusercontent.com/media/onnx/models/c5cae0f1942c8d7727372c7b1f6b485aa39e4421/validated/vision/classification/resnet/model/resnet18-v1-7.onnx).

<a id="model-search-squeezenet"></a>
### `search-squeezenet` — SqueezeNet1.1

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `search` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | ImageNet RGB shortest-side256, center224, /255, mean.485/.456/.406, std.229/.224/.225; retrieval L2-normalizes512 features |
| 후처리·출력 | 원래 classifier logits → Top5; search ID는 추가 pooled yon_embedding 512D → L2 정규화 → cosine 검색 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 동일 asset을 사용하는 기능별 별칭을 따로 모델 가중치로 세지 않음 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/vision/squeezenet1-1.onnx` | model / 4956520 | `69fed662717dec53395aff9a072b59be71acd7a666ea45fa5ec2e8a200d27e11` |

복원 경로: `vision/src/main/assets/one/modelplus/vision/squeezenet1-1.onnx`.

**구현:** [AddedVisionModels.java](../vision/src/main/java/com/yonlab/vision/AddedVisionModels.java), [AddedVisionOnnx.java](../vision/src/main/java/com/yonlab/vision/AddedVisionOnnx.java), [AddedVisionImageRunner.java](../vision/src/main/java/com/yonlab/vision/AddedVisionImageRunner.java), [ImageRetrieval.java](../vision/src/main/java/com/yonlab/vision/ImageRetrieval.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/vision/catalog.json).

**공식 upstream:** [source 1](https://github.com/onnx/models/tree/c5cae0f1942c8d7727372c7b1f6b485aa39e4421/validated/vision/classification/squeezenet), [source 2](https://media.githubusercontent.com/media/onnx/models/c5cae0f1942c8d7727372c7b1f6b485aa39e4421/validated/vision/classification/squeezenet/model/squeezenet1.1-7.onnx).

<a id="model-search-tinyclip-q8"></a>
### `search-tinyclip-q8` — TinyCLIP ViT 8M INT8

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `search` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | CLIP shortest-side224 bicubic/center224 RGB; published CLIP mean/std |
| 후처리·출력 | image_embeds512 → L2 → 동일 갤러리 cosine |
| 이용 조건 | MIT |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 텍스트 생성/대화 모델이 아니며 이미지 인코더만 사용

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/search-tinyclip-vision-q8.onnx` | model / 8957217 | `bbf8426b0e548881dcfd9257030dd6139fceeeb4808994968b882ecc3ada291f` |

복원 경로: `vision/src/main/assets/one/models/search-tinyclip-vision-q8.onnx`.

**구현:** [ImageRetrieval.java](../vision/src/main/java/com/yonlab/vision/ImageRetrieval.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://github.com/microsoft/Cream/tree/main/TinyCLIP).

<a id="model-segment-modnet"></a>
### `segment-modnet` — MODNet portrait matting

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `segment` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | RGB /255, mean0.5 std0.5; aspect resize short-side512, bounded longest-side768, multiples32. Soft alpha bilinear-resized to source image. |
| 후처리·출력 | 실제 alpha 출력 → 원본 크기 마스크 → overlay/배경 합성 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 동일 asset을 사용하는 기능별 별칭을 따로 모델 가중치로 세지 않음 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** Xenova/modnet Transformers.js ONNX conversion

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/vision/modnet.onnx` | model / 25888640 | `07c308cf0fc7e6e8b2065a12ed7fc07e1de8febb7dc7839d7b7f15dd66584df9` |

복원 경로: `vision/src/main/assets/one/modelplus/vision/modnet.onnx`.

**구현:** [AddedVisionModels.java](../vision/src/main/java/com/yonlab/vision/AddedVisionModels.java), [AddedPortraitEngine.java](../vision/src/main/java/com/yonlab/vision/AddedPortraitEngine.java), [BackgroundStudio.java](../vision/src/main/java/com/yonlab/vision/BackgroundStudio.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/vision/catalog.json).

**공식 upstream:** [source 1](https://github.com/ZHKKKe/MODNet), [source 2](https://huggingface.co/Xenova/modnet/resolve/fa2fa546052fba4c08921230a26cc69a333fca12/onnx/model.onnx).

<a id="model-segment-selfie-landscape"></a>
### `segment-selfie-landscape` — MediaPipe Selfie Segmentation Landscape

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `segment` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | RGB direct resize256x144; /255; soft alpha bilinear-resized to source image. |
| 후처리·출력 | 실제 alpha 출력 → 원본 크기 마스크 → overlay/배경 합성 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 동일 asset을 사용하는 기능별 별칭을 따로 모델 가중치로 세지 않음 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** ONNX Community / Xenova conversion of Google MediaPipe landscape model

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/vision/selfie-landscape.onnx` | model / 462338 | `7a0adcfdb1715d3b0ff0f61486d8a181c33f4a208343a5abfccbc6540e872d24` |

복원 경로: `vision/src/main/assets/one/modelplus/vision/selfie-landscape.onnx`.

**구현:** [AddedVisionModels.java](../vision/src/main/java/com/yonlab/vision/AddedVisionModels.java), [AddedPortraitEngine.java](../vision/src/main/java/com/yonlab/vision/AddedPortraitEngine.java), [BackgroundStudio.java](../vision/src/main/java/com/yonlab/vision/BackgroundStudio.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/vision/catalog.json).

**공식 upstream:** [source 1](https://github.com/google-ai-edge/mediapipe/blob/master/docs/solutions/selfie_segmentation.md), [source 2](https://huggingface.co/onnx-community/mediapipe_selfie_segmentation_landscape/resolve/2497d5bec26c626c7b3c4edc6e1fefc21b64f6c3/onnx/model.onnx).

<a id="model-selfie-segmentation"></a>
### `selfie-segmentation` — ML Kit Selfie Segmentation

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `segment` |
| 런타임 | Google ML Kit / SDK-managed segmentation-selfie:16.0.0-beta6 |
| 전처리 | InputImage bitmap → SDK SINGLE_IMAGE_MODE soft mask |
| 후처리·출력 | bilinear 원본 크기 alpha → segment overlay 또는 studio 교체/흐림/제거 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | segment와 studio의 동일 SDK 모델 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** SDK 내부 가중치를 추출하지 않으며 개별 SHA를 임의로 만들지 않음

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [BackgroundStudio.java](../vision/src/main/java/com/yonlab/vision/BackgroundStudio.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/selfie-segmentation), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-squeezenet1-1"></a>
### `squeezenet1-1` — SqueezeNet1.1

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `classify` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | ImageNet RGB shortest-side256, center224, /255, mean.485/.456/.406, std.229/.224/.225; retrieval L2-normalizes512 features |
| 후처리·출력 | 원래 classifier logits → Top5; search ID는 추가 pooled yon_embedding 512D → L2 정규화 → cosine 검색 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 동일 asset을 사용하는 기능별 별칭을 따로 모델 가중치로 세지 않음 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/vision/squeezenet1-1.onnx` | model / 4956520 | `69fed662717dec53395aff9a072b59be71acd7a666ea45fa5ec2e8a200d27e11` |

복원 경로: `vision/src/main/assets/one/modelplus/vision/squeezenet1-1.onnx`.

**구현:** [AddedVisionModels.java](../vision/src/main/java/com/yonlab/vision/AddedVisionModels.java), [AddedVisionOnnx.java](../vision/src/main/java/com/yonlab/vision/AddedVisionOnnx.java), [AddedVisionImageRunner.java](../vision/src/main/java/com/yonlab/vision/AddedVisionImageRunner.java), [ImageRetrieval.java](../vision/src/main/java/com/yonlab/vision/ImageRetrieval.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/vision/catalog.json).

**공식 upstream:** [source 1](https://github.com/onnx/models/tree/c5cae0f1942c8d7727372c7b1f6b485aa39e4421/validated/vision/classification/squeezenet), [source 2](https://media.githubusercontent.com/media/onnx/models/c5cae0f1942c8d7727372c7b1f6b485aa39e4421/validated/vision/classification/squeezenet/model/squeezenet1.1-7.onnx).

<a id="model-ssd-mobilenet-v1"></a>
### `ssd-mobilenet-v1` — SSD MobileNet COCO

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `detect`, `flow` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | RGB resize → UINT8 NHWC; 출력 4개와 shape 검증 |
| 후처리·출력 | 검출 box와 sparse COCO label; score는 정확도 지표가 아님 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | detect/flow 공통 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `models/detect.tflite` | model / 4183312 | `e4b118e5e4531945de2e659742c7c590f7536f8d0ed26d135abcfe83b4779d13` |

복원 경로: `vision/src/main/assets/models/detect.tflite`.

**구현:** [ObjectEngine.java](../vision/src/main/java/com/yonlab/vision/ObjectEngine.java), [OneObjectEngine.java](../vision/src/main/java/com/yonlab/vision/OneObjectEngine.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://www.kaggle.com/models/tensorflow/ssd-mobilenet-v1).

<a id="model-studio-modnet"></a>
### `studio-modnet` — MODNet portrait matting

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `studio` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | RGB /255, mean0.5 std0.5; aspect resize short-side512, bounded longest-side768, multiples32. Soft alpha bilinear-resized to source image. |
| 후처리·출력 | 실제 alpha 출력 → 원본 크기 마스크 → overlay/배경 합성 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 동일 asset을 사용하는 기능별 별칭을 따로 모델 가중치로 세지 않음 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** Xenova/modnet Transformers.js ONNX conversion

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/vision/modnet.onnx` | model / 25888640 | `07c308cf0fc7e6e8b2065a12ed7fc07e1de8febb7dc7839d7b7f15dd66584df9` |

복원 경로: `vision/src/main/assets/one/modelplus/vision/modnet.onnx`.

**구현:** [AddedVisionModels.java](../vision/src/main/java/com/yonlab/vision/AddedVisionModels.java), [AddedPortraitEngine.java](../vision/src/main/java/com/yonlab/vision/AddedPortraitEngine.java), [BackgroundStudio.java](../vision/src/main/java/com/yonlab/vision/BackgroundStudio.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/vision/catalog.json).

**공식 upstream:** [source 1](https://github.com/ZHKKKe/MODNet), [source 2](https://huggingface.co/Xenova/modnet/resolve/fa2fa546052fba4c08921230a26cc69a333fca12/onnx/model.onnx).

<a id="model-studio-pphuman"></a>
### `studio-pphuman` — PPHumanSeg

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `studio` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | RGB192 → (value/255−0.5)/0.5 → NCHW |
| 후처리·출력 | 192×192 foreground/background → 원본 크기 마스크; studio는 합성 효과 후처리 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | segment와 studio가 같은 가중치 사용 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/pphumanseg-2023.onnx` | model / 6163938 | `552d8a984054e59b5d773d24b9b12022b22046ceb2bbc4c9aaeaceb36a9ddf24` |

복원 경로: `vision/src/main/assets/one/models/pphumanseg-2023.onnx`.

**구현:** [OneOnnx.java](../vision/src/main/java/com/yonlab/vision/OneOnnx.java), [BackgroundStudio.java](../vision/src/main/java/com/yonlab/vision/BackgroundStudio.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://github.com/opencv/opencv_zoo/tree/main/models/human_segmentation_pphumanseg).

<a id="model-studio-selfie"></a>
### `studio-selfie` — ML Kit Selfie Segmentation

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `studio` |
| 런타임 | Google ML Kit / SDK-managed segmentation-selfie:16.0.0-beta6 |
| 전처리 | InputImage bitmap → SDK SINGLE_IMAGE_MODE soft mask |
| 후처리·출력 | bilinear 원본 크기 alpha → segment overlay 또는 studio 교체/흐림/제거 |
| 이용 조건 | Google ML Kit/API terms; 별도 모델 파일 라이선스로 Apache-2.0을 추정하지 않음 |
| 공유 관계 | segment와 studio의 동일 SDK 모델 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** SDK 내부 가중치를 추출하지 않으며 개별 SHA를 임의로 만들지 않음

**별도 asset 파일:** 없음. SDK가 모델을 소유하며 별도 추출 파일을 배포하지 않습니다.

**구현:** [BackgroundStudio.java](../vision/src/main/java/com/yonlab/vision/BackgroundStudio.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt).

**공식 upstream:** [source 1](https://developers.google.com/ml-kit/vision/selfie-segmentation), [source 2](https://developers.google.com/ml-kit/terms).

<a id="model-studio-selfie-landscape"></a>
### `studio-selfie-landscape` — MediaPipe Selfie Segmentation Landscape

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `studio` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | RGB direct resize256x144; /255; soft alpha bilinear-resized to source image. |
| 후처리·출력 | 실제 alpha 출력 → 원본 크기 마스크 → overlay/배경 합성 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 동일 asset을 사용하는 기능별 별칭을 따로 모델 가중치로 세지 않음 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** ONNX Community / Xenova conversion of Google MediaPipe landscape model

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/modelplus/vision/selfie-landscape.onnx` | model / 462338 | `7a0adcfdb1715d3b0ff0f61486d8a181c33f4a208343a5abfccbc6540e872d24` |

복원 경로: `vision/src/main/assets/one/modelplus/vision/selfie-landscape.onnx`.

**구현:** [AddedVisionModels.java](../vision/src/main/java/com/yonlab/vision/AddedVisionModels.java), [AddedPortraitEngine.java](../vision/src/main/java/com/yonlab/vision/AddedPortraitEngine.java), [BackgroundStudio.java](../vision/src/main/java/com/yonlab/vision/BackgroundStudio.java).

**출처·고지 원장:** [catalog.json](../vision/src/main/assets/one/modelplus/vision/catalog.json).

**공식 upstream:** [source 1](https://github.com/google-ai-edge/mediapipe/blob/master/docs/solutions/selfie_segmentation.md), [source 2](https://huggingface.co/onnx-community/mediapipe_selfie_segmentation_landscape/resolve/2497d5bec26c626c7b3c4edc6e1fefc21b64f6c3/onnx/model.onnx).

<a id="model-vosk-en-us-small-0.15"></a>
### `vosk-en-us-small-0.15` — Vosk English

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `speech` |
| 런타임 | Vosk Android 0.3.75 + JNA 5.18.1 |
| 전처리 | 허용된 모델 manifest에 따라 검증·해제; mono16k PCM |
| 후처리·출력 | streaming partial/final text; 정답이 검증된 입력에서만 CER/WER 사용 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 언어별 서로 다른 모델 집합; upstream WER을 이 앱의 실측으로 옮기지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `offlinevoice/vosk-model-small-en-us-0.15.zip` | archive of model weight/config files / 41205931 | `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498` |

복원 경로: `offlinevoice/src/main/assets/offlinevoice/vosk-model-small-en-us-0.15.zip`.

**구현:** [VoiceModels.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceModels.java), [VoiceModelPolicy.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceModelPolicy.java), [OfflineVoiceActivity.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/OfflineVoiceActivity.java).

**출처·고지 원장:** [ASR-NOTICES.md](../offlinevoice/src/main/assets/offlinevoice/notices/ASR-NOTICES.md), [english-model-files.json](../offlinevoice/src/main/assets/offlinevoice/english-model-files.json).

**공식 upstream:** [source 1](https://alphacephei.com/vosk/models), [source 2](https://github.com/alphacep/vosk-api).

<a id="model-vosk-ko-small-0.22"></a>
### `vosk-ko-small-0.22` — Vosk 한국어

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `speech` |
| 런타임 | Vosk Android 0.3.75 + JNA 5.18.1 |
| 전처리 | 허용된 모델 manifest에 따라 검증·해제; mono16k PCM |
| 후처리·출력 | streaming partial/final text; 정답이 검증된 입력에서만 CER/WER 사용 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 실제 release controller 실행5건 완료; 시험 기능 speech; 고유 encoded 입력4개. 요청 ID·입력 SHA·정리 확인이며 전체 기능·정확도 수락과 구분; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 언어별 서로 다른 모델 집합; upstream WER을 이 앱의 실측으로 옮기지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `offlinevoice/vosk-model-small-ko-0.22.zip` | archive of model weight/config files / 86914329 | `eea36124087fed26c59996a4761519458e3bd185e8ea9d9865ad8760c4a1d989` |

복원 경로: `offlinevoice/src/main/assets/offlinevoice/vosk-model-small-ko-0.22.zip`.

**이전 공개0.1/code20 실행 근거:** `ko-speech-fleurs-045` / `speech`의 한 입력 실행. [공개 검증 요약](validation-0.1.json)에 hash·시간·평가 범위를 기록했습니다.

**구현:** [VoiceModels.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceModels.java), [VoiceModelPolicy.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceModelPolicy.java), [OfflineVoiceActivity.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/OfflineVoiceActivity.java).

**출처·고지 원장:** [ASR-NOTICES.md](../offlinevoice/src/main/assets/offlinevoice/notices/ASR-NOTICES.md), [model-files.json](../offlinevoice/src/main/assets/offlinevoice/model-files.json).

**공식 upstream:** [source 1](https://alphacephei.com/vosk/models), [source 2](https://github.com/alphacep/vosk-api).

<a id="model-whisper-base-multilingual-int8"></a>
### `whisper-base-multilingual-int8` — Whisper Base Multilingual INT8

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `speech` |
| 런타임 | sherpa-onnx 2.0.0 local AAR / CPU |
| 전처리 | mono16k PCM → sherpa feature80; encoder/decoder/token manifest 검증 |
| 후처리·출력 | greedy transcribe; language 빈값 auto; 언어 검출과 KO/GO 태그는 별개 |
| 이용 조건 | Whisper 가중치/코드 MIT; sherpa Apache-2.0; 포함 native 구성요소 고지 별도 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 다국어 공통 가중치이며 KO/GO 별도 모델로 중복 계산하지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `offlinevoice/whisper-base-int8/base-encoder.int8.onnx` | encoder / 29120534 | `0b8fb1304b6109976038efff5ace81720e00386f3ff6b54ee8c75291ca0a1e11` |
| `offlinevoice/whisper-base-int8/base-decoder.int8.onnx` | decoder / 130672026 | `9759d217388a01b3a4c7c15533201067b48ae819c4daafc8624e64b9409dc02d` |
| `offlinevoice/whisper-base-int8/base-tokens.txt` | tokens / 816730 | `b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126` |

복원 경로: `offlinevoice/src/main/assets/offlinevoice/whisper-base-int8/base-encoder.int8.onnx`, `offlinevoice/src/main/assets/offlinevoice/whisper-base-int8/base-decoder.int8.onnx`, `offlinevoice/src/main/assets/offlinevoice/whisper-base-int8/base-tokens.txt`.

**구현:** [WhisperConfig.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/WhisperConfig.java), [PlusWhisperModels.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/PlusWhisperModels.java), [VoiceModels.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceModels.java).

**출처·고지 원장:** [ASR-NOTICES.md](../offlinevoice/src/main/assets/offlinevoice/notices/ASR-NOTICES.md), [whisper-modelplus.json](../offlinevoice/src/main/assets/offlinevoice/whisper-modelplus.json).

**공식 upstream:** [source 1](https://github.com/openai/whisper), [source 2](https://github.com/k2-fsa/sherpa-onnx), [source 3](https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base).

<a id="model-whisper-small-multilingual-int8"></a>
### `whisper-small-multilingual-int8` — Whisper Small Multilingual INT8

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `speech` |
| 런타임 | sherpa-onnx 2.0.0 local AAR / CPU |
| 전처리 | mono16k PCM → sherpa feature80; encoder/decoder/token manifest 검증 |
| 후처리·출력 | greedy transcribe; language 빈값 auto; 언어 검출과 KO/GO 태그는 별개 |
| 이용 조건 | Whisper 가중치/코드 MIT; sherpa Apache-2.0; 포함 native 구성요소 고지 별도 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 다국어 공통 가중치이며 KO/GO 별도 모델로 중복 계산하지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `offlinevoice/whisper-small-int8/small-encoder.int8.onnx` | encoder / 112442483 | `4cbe7b22fa9026b843b60a68640c747de05bafb1a11b57edc0e66c232d9f33a9` |
| `offlinevoice/whisper-small-int8/small-decoder.int8.onnx` | decoder / 262226114 | `acad50b5c782696e91b55914cc5ab4f756f1532f76e22aa6fc615f39fb69a8ee` |
| `offlinevoice/whisper-small-int8/small-tokens.txt` | tokens / 816730 | `b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126` |

복원 경로: `offlinevoice/src/main/assets/offlinevoice/whisper-small-int8/small-encoder.int8.onnx`, `offlinevoice/src/main/assets/offlinevoice/whisper-small-int8/small-decoder.int8.onnx`, `offlinevoice/src/main/assets/offlinevoice/whisper-small-int8/small-tokens.txt`.

**구현:** [WhisperConfig.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/WhisperConfig.java), [PlusWhisperModels.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/PlusWhisperModels.java), [VoiceModels.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceModels.java).

**출처·고지 원장:** [ASR-NOTICES.md](../offlinevoice/src/main/assets/offlinevoice/notices/ASR-NOTICES.md), [whisper-modelplus.json](../offlinevoice/src/main/assets/offlinevoice/whisper-modelplus.json).

**공식 upstream:** [source 1](https://github.com/openai/whisper), [source 2](https://github.com/k2-fsa/sherpa-onnx), [source 3](https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small).

<a id="model-whisper-tiny-multilingual-int8"></a>
### `whisper-tiny-multilingual-int8` — Whisper Tiny Multilingual INT8

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `speech` |
| 런타임 | sherpa-onnx 2.0.0 local AAR / CPU |
| 전처리 | mono16k PCM → sherpa feature80; encoder/decoder/token manifest 검증 |
| 후처리·출력 | greedy transcribe; language 빈값 auto; 언어 검출과 KO/GO 태그는 별개 |
| 이용 조건 | Whisper 가중치/코드 MIT; sherpa Apache-2.0; 포함 native 구성요소 고지 별도 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 실제 release controller 실행2건 완료; 시험 기능 speech; 고유 encoded 입력2개. 요청 ID·입력 SHA·정리 확인이며 전체 기능·정확도 수락과 구분; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 다국어 공통 가중치이며 KO/GO 별도 모델로 중복 계산하지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `offlinevoice/whisper-tiny-multilingual-int8.zip` | archive containing encoder+decoder+tokens+license / 59484816 | `5001b4a0e90a068c78f079d613e91e3e58fc36be0350b19ce3dc651bdd599849` |

복원 경로: `offlinevoice/src/main/assets/offlinevoice/whisper-tiny-multilingual-int8.zip`.

**구현:** [WhisperConfig.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/WhisperConfig.java), [WhisperModelStore.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/WhisperModelStore.java), [VoiceModels.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceModels.java).

**출처·고지 원장:** [ASR-NOTICES.md](../offlinevoice/src/main/assets/offlinevoice/notices/ASR-NOTICES.md), [whisper-model-files.json](../offlinevoice/src/main/assets/offlinevoice/whisper-model-files.json).

**공식 upstream:** [source 1](https://github.com/openai/whisper), [source 2](https://github.com/k2-fsa/sherpa-onnx), [source 3](https://huggingface.co/csukuangfj/sherpa-onnx-whisper-tiny).

<a id="model-yamnet-classifier"></a>
### `yamnet-classifier` — YAMNet Mobile INT8

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `sound` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 16kHz mono float PCM; 15,600 samples window/7,680 hop; 마지막 창 zero pad |
| 후처리·출력 | 521종 window score → 전체 평균/peak/Top5; 시간축 표시 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 실제 release controller 실행1건 완료; 시험 기능 sound; 고유 encoded 입력1개. 요청 ID·입력 SHA·정리 확인이며 전체 기능·정확도 수락과 구분; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 파일명 INT8는 내부 가중치 모델 특성; 실행 입력/출력은 FLOAT32. PANNs와 클래스 체계가 다름

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/yamnet-classifier.tflite` | model / 4126810 | `10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de` |

복원 경로: `vision/src/main/assets/one/models/yamnet-classifier.tflite`.

**구현:** [SoundEngine.java](../vision/src/main/java/com/yonlab/vision/SoundEngine.java), [SoundMath.java](../vision/src/main/java/com/yonlab/vision/SoundMath.java), [SoundBenchActivity.java](../vision/src/main/java/com/yonlab/vision/SoundBenchActivity.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://www.kaggle.com/models/google/yamnet).

<a id="model-yamnet-original"></a>
### `yamnet-original` — YAMNet Original FP32

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `sound` |
| 런타임 | LiteRT 1.4.2 Interpreter / CPU |
| 전처리 | 16kHz mono float PCM; 15,600 samples window/7,680 hop; 마지막 창 zero pad |
| 후처리·출력 | 521종 window score → 전체 평균/peak/Top5; 시간축 표시 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 파일명 INT8는 내부 가중치 모델 특성; 실행 입력/출력은 FLOAT32. PANNs와 클래스 체계가 다름

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/yamnet-original.tflite` | model / 16096668 | `141fba1cdaae842c816f28edc4937e8b4f0af4c8df21862ccc6b52dc567993c3` |

복원 경로: `vision/src/main/assets/one/models/yamnet-original.tflite`.

**구현:** [SoundEngine.java](../vision/src/main/java/com/yonlab/vision/SoundEngine.java), [SoundMath.java](../vision/src/main/java/com/yonlab/vision/SoundMath.java), [SoundBenchActivity.java](../vision/src/main/java/com/yonlab/vision/SoundBenchActivity.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://www.kaggle.com/models/google/yamnet).

<a id="model-yolox-nano"></a>
### `yolox-nano` — YOLOX Nano

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `detect`, `flow` |
| 런타임 | ONNX Runtime Android 1.24.3 / CPU + OpenCV 4.13.0 |
| 전처리 | 416×416 좌상단 letterbox, padding114, raw BGR CHW |
| 후처리·출력 | raw head decode/NMS → 원본 좌표 복원 → COCO80과 sparse label 연결 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | detect/flow 공통 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `one/models/yolox-nano.onnx` | model / 3659407 | `c789161ed43c8269fcd4e67c67eeeb4e80c622da2eb296a20bc6007bd18a0b7d` |

복원 경로: `vision/src/main/assets/one/models/yolox-nano.onnx`.

**구현:** [OneYoloXEngine.java](../vision/src/main/java/com/yonlab/vision/OneYoloXEngine.java), [OneYoloXMath.java](../vision/src/main/java/com/yonlab/vision/OneYoloXMath.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://github.com/Megvii-BaseDetection/YOLOX/releases/tag/0.1.1rc0).

<a id="model-yunet-2026-sface"></a>
### `yunet-2026-sface` — YuNet 2026 · SFace

| 항목 | 계약 |
|---|---|
| 종류 | 가중치 또는 SDK 기반 파이프라인 |
| 사용 기능 | `faces`, `compare`, `access`, `vis` |
| 런타임 | Google ML Kit / SDK-managed + OpenCV 4.13.0 + ONNX Runtime Android 1.24.3 / CPU |
| 전처리 | 선택 검출기와 ML Kit 관측 → YuNet 5점 정렬 → SFace 입력; 속성 heads는 기능별 선택 실행 |
| 후처리·출력 | SFace128D; compare/access/vis/선택적 signage에 서로 다른 후처리; faces/vis는 별도 expression/age 관측 가능 |
| 이용 조건 | YuNet MIT; SFace·속성·MediaPipe Apache-2.0; ML Kit는 별도 Google 약관 |
| 공유 관계 | faces/compare/access/vis가 동일 파이프라인 ID를 재사용; BlazeFace 두 ID는 signage에도 등록 |
| 검증 범위 | 0.1/code21: 현재 검증 기록에서 이 ID의 기기 실행은 미시험. 과거 버전의 근거가 있으면 해당 버전으로 별도 해석하며 미실행을 실패로 표시하지 않음; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 모든 asset이 매 실행에 무조건 사용된다는 뜻이 아님. SDK 가중치 SHA는 별도 노출하지 않음. modelSha256 하나가 모든 SDK/선택적 head를 덮는다고 해석하지 않음

| Android asset path | 역할 / 바이트 | SHA-256 |
|---|---|---|
| `models/face_detection_yunet_2023mar.onnx` | model / 232589 | `8f2383e4dd3cfbb4553ea8718107fc0423210dc964f9f4280604804ed2552fa4` |
| `models/face_recognition_sface_2021dec.onnx` | model / 38696353 | `0ba9fbfa01b5270c96627c4ef784da859931e02f04419c829e83484087c34e79` |
| `one/models/yunet-2026-sface.onnx` | model / 229738 | `ebafce4e3c118d6554634be5c27ab333b4c047a9a8c3faf1d7cf93101c22f0f0` |
| `models/facial_expression_recognition_mobilefacenet_2022july.onnx` | optional: faces/vis expression / 4791892 | `4f61307602fc089ce20488a31d4e4614e3c9753a7d6c41578c854858b183e1a9` |
| `models/age_gender_recognition_retail_0013_fp32.onnx` | optional: faces/vis age-head observation / 8558439 | `f4649ed528edcb0dbf3bb9474ffbd4fe32384f3c4b8e49493214464d0917e678` |

복원 경로: `vision/src/main/assets/models/face_detection_yunet_2023mar.onnx`, `vision/src/main/assets/models/face_recognition_sface_2021dec.onnx`, `vision/src/main/assets/one/models/yunet-2026-sface.onnx`, `vision/src/main/assets/models/facial_expression_recognition_mobilefacenet_2022july.onnx`, `vision/src/main/assets/models/age_gender_recognition_retail_0013_fp32.onnx`.

**구현:** [FaceEngine.java](../vision/src/main/java/com/yonlab/vision/FaceEngine.java), [Registry.java](../vision/src/main/java/com/yonlab/vision/Registry.java), [OneYuNet.java](../vision/src/main/java/com/yonlab/vision/OneYuNet.java), [PlusBlazeDetector.java](../vision/src/main/java/com/yonlab/vision/PlusBlazeDetector.java).

**출처·고지 원장:** [Google-MLKit-Terms-Reference.txt](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt), [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://github.com/opencv/opencv_zoo/tree/main/models/face_detection_yunet), [source 2](https://github.com/opencv/opencv_zoo/tree/main/models/face_recognition_sface), [source 3](https://developers.google.com/ml-kit/terms).

<a id="model-zxing-qr"></a>
### `zxing-qr` — ZXing QR / Barcode

| 항목 | 계약 |
|---|---|
| 종류 | 코드 엔진 |
| 사용 기능 | `qr` |
| 런타임 | ZXing 3.5.3 |
| 전처리 | RGB pixels → luminance/binarizer |
| 후처리·출력 | symbol format/payload; QR 생성은 별도 encoder 코드 |
| 이용 조건 | Apache-2.0 |
| 공유 관계 | 이 항목의 asset binding을 기준으로 식별 |
| 검증 범위 | 0.1/code21: 실제 release controller 실행2건 완료; 시험 기능 qr; 고유 encoded 입력2개. 요청 ID·입력 SHA·정리 확인이며 전체 기능·정확도 수락과 구분; [현재 검증 요약](validation-0.1-build21.json) |

**해석:** 학습 가중치가 없는 해독 엔진

**과거 실행 근거:** 4.4.0/code19, `qr/qr-demo-1`, SM-F956N/API37. 현재0.1/code21의 실행 증거로 대체하지 않습니다.

**별도 asset 파일:** 없음. 플랫폼/코드 엔진입니다.

**이전 공개0.1/code20 실행 근거:** `qr-demo-1` / `qr`의 한 입력 실행. [공개 검증 요약](validation-0.1.json)에 hash·시간·평가 범위를 기록했습니다.

**구현:** [BarcodeHelper.java](../vision/src/main/java/com/yonlab/vision/BarcodeHelper.java), [OneModelRunner.java](../vision/src/main/java/com/yonlab/vision/OneModelRunner.java).

**출처·고지 원장:** [01-MODEL-FILE-NOTICES.txt](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt).

**공식 upstream:** [source 1](https://github.com/zxing/zxing).

## 재현과 변경 규칙

입력 encoded SHA, 모델 파일 SHA 또는 조합 fingerprint, 선택 ID, KO/GO 맥락, 앱 버전, CPU 요청 설정, 기기/API, 오류·종료 상태를 함께 기록합니다. 전처리·사전·label·decoder를 바꾸면 같은 가중치 SHA라도 다른 파이프라인입니다. 모델 비교 결과는 같은 입력과 같은 평가 범위를 확인한 뒤 해석합니다.

기본 레지스트리는 [OneModelPolicy.java](../vision/src/main/java/com/yonlab/vision/OneModelPolicy.java), 추가 선택은 [ModelPlusPolicy.java](../vision/src/main/java/com/yonlab/vision/ModelPlusPolicy.java), 음성 선택은 [VoiceModels.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceModels.java)가 기준입니다. 레지스트리에 없는 이전 ID 또는 label 문자열을 새 실행 모델로 문서화하지 않습니다.
