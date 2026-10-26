# 기능별 사용과 처리 구조

AXSL Vision **0.1/code21**은 하나의 Android 앱 안에서 19개 작업을 제공합니다. 기능 선택, 모델 선택, 입력 선택은 같은 작업 맥락입니다. 기본 자료는 검증된 SHA로 복원하는 로컬 데모이며 개인 파일은 명시적 가져오기에서 선택합니다. code21의 [모델 시험](MODEL_TESTING.md)은 실행 ID를 먼저 고른 뒤 지원 기능과 다양한 입력 조건을 선택하는 진입점입니다.

화면의 **기본 선택**은 최초 실행의 기본값이며 측정된 성능 순위가 아닙니다. 현재 기능·모델 안내는 시험 기능, 모델·엔진 ID, 실행 방식, 입력 조건, 결과 해석 순서로 표시합니다. 아래 기능 설명은 구현된 경로를 설명하며 전체 기기 수락·전체 모델 정확도를 뜻하지 않습니다. 실행 상태와 현재 검증 범위는 [VALIDATION](VALIDATION.md), 68개 ID의 상세 계약은 [MODEL_CATALOG](MODEL_CATALOG.md)을 확인합니다.

현재 code21 검증은 [BUILD21_STATUS](BUILD21_STATUS.md)와 [공개 요약](validation-0.1-build21.json)의8개 실행 ID·24회 controller 실행, 일반 UI6건, 호스트279개로 한정합니다. 아래19개 기능의 구현 설명을 모든 기능·모델의 기기 실행 완료로 해석하지 않습니다. 모델별 실제 횟수와 나머지 60개 ID의 미시험 상태는 [모델 카탈로그](MODEL_CATALOG.md)에서 확인합니다.

## 입력·비교의 공통 규칙

- 사진 모델 비교는 같은 정지 이미지를 사용합니다. 음성·생활 소리는 오디오 비교를 사용하며 안내판은 모델 비교 대상에서 제외됩니다.
- flow/drive/vis의 사진 비교는 검출·특징·기하 비교입니다. 시간 집계는 영상/실시간 controller에 남깁니다.
- OCR·문서·ASR의 KO/GO는 언어·자료 그룹입니다. SHARED 자료와 Whisper 공통 다국어 가중치를 별도 국적·언어 모델로 중복 계산하지 않습니다.
- 입력 encoded SHA와 모델/파이프라인 식별자를 기록합니다. 결과 PNG의 SHA와 입력 원본 SHA를 서로 바꾸지 않습니다.
- 데모의 difficulty/conditions는 흐림·노이즈·가림 등 입력 조건이며 인식 성공률이 아닙니다.
- 모델 시험에서는 입력의 KO/GO 그룹으로 선택 모델을 자동 교체하지 않습니다. 지원 언어와 다른 입력을 실행한 결과는 언어 조건 차이 관측으로 구분합니다.

## 기능 한눈에 보기

| ID | 기능 | 슬롯 | 입력 |
|---|---|---:|---|
| [detect](#feature-detect) | 객체 찾기 | 5 | 사진·영상; 지원 화면의 카메라 입력 |
| [classify](#feature-classify) | 이미지 분류 | 6 | 사진 1장 |
| [segment](#feature-segment) | 영역 분할 | 5 | 사진 1장 |
| [faces](#feature-faces) | 얼굴 분석 | 4 | 사진·영상; 지원 화면의 카메라 입력 |
| [compare](#feature-compare) | 얼굴 비교 | 4 | 두 사진에서 각각 비교할 얼굴 선택 |
| [access](#feature-access) | 출입 인식 | 4 | 사진·영상과 명시적으로 등록한 얼굴 특징 |
| [drive](#feature-drive) | 드라이버 관측 | 3 | 사진·영상; 지원 화면의 카메라 입력 |
| [flow](#feature-flow) | 사람 흐름 | 5 | 영상; 지원 화면의 실시간 관측 |
| [vis](#feature-vis) | 방문자 집계 | 4 | 영상과 얼굴 관측; 사진 비교는 얼굴 파이프라인만 실행 |
| [pose](#feature-pose) | 자세 보기 | 6 | 사진 1장 |
| [ocr](#feature-ocr) | 문자 읽기 | 5 | 사진 1장; KO/GO 언어 맥락 |
| [qr](#feature-qr) | QR 읽기·만들기 | 4 | 코드가 있는 사진; 생성은 사용자가 입력한 payload |
| [speech](#feature-speech) | 말을 글로 | 5 | 음성 파일 또는 사용자가 시작한 마이크 입력 |
| [signage](#feature-signage) | 매장·행사 안내판 | 3 | 사용자가 선택한 사진·영상 재생목록 |
| [document](#feature-document) | 문서·영수증 정리 | 5 | 문서 사진 1장; KO/GO 맥락 |
| [studio](#feature-studio) | 배경 스튜디오 | 4 | 인물 사진과 선택 배경/효과 |
| [search](#feature-search) | 비슷한 사진 찾기 | 5 | 사진 query와 명시적으로 구성한 갤러리 |
| [sound](#feature-sound) | 생활 소리 분석 | 4 | 음성 파일 또는 명시적으로 시작한 주변 마이크 입력 |
| [coach](#feature-coach) | 운동 자세 코치 | 6 | 자세가 보이는 사진 1장 |

<a id="feature-detect"></a>
## 객체 찾기 (`detect`)

| 항목 | 내용 |
|---|---|
| 입력 | 사진·영상; 지원 화면의 카메라 입력 |
| 출력 | 분류명·score·원본 좌표의 검출 상자 |
| 모델·엔진 선택 | [`efficientdet-lite0`](MODEL_CATALOG.md#model-efficientdet-lite0), [`ssd-mobilenet-v1`](MODEL_CATALOG.md#model-ssd-mobilenet-v1), [`yolox-nano`](MODEL_CATALOG.md#model-yolox-nano), [`nanodet-plus`](MODEL_CATALOG.md#model-nanodet-plus), [`efficientdet-lite1`](MODEL_CATALOG.md#model-efficientdet-lite1) |

**처리:** 선택 detector → 전처리/추론 → box decode와 label 매핑 → 원본 좌표 overlay.

**활용:** 같은 장면을 여러 검출기로 비교하거나 자료에서 대상 위치를 확인할 때.

**한계:** COCO 계열의 label 범위와 임계값을 따릅니다. 단일 장면의 score·상자 수로 정확도/mAP를 계산하지 않습니다. 영상의 전체 FPS는 decode·render와 별도 측정해야 합니다.

**구현 근거:** [OneObjectEngine.java](../vision/src/main/java/com/yonlab/vision/OneObjectEngine.java), [ObjectEngine.java](../vision/src/main/java/com/yonlab/vision/ObjectEngine.java), [OneYoloXEngine.java](../vision/src/main/java/com/yonlab/vision/OneYoloXEngine.java), [AddedObjectEngine.java](../vision/src/main/java/com/yonlab/vision/AddedObjectEngine.java), [VideoAnalysisActivity.java](../vision/src/main/java/com/yonlab/vision/VideoAnalysisActivity.java).

<a id="feature-classify"></a>
## 이미지 분류 (`classify`)

| 항목 | 내용 |
|---|---|
| 입력 | 사진 1장 |
| 출력 | ImageNet 1,000종의 Top5와 모델별 시간 |
| 모델·엔진 선택 | [`mobilenet-v4-fp32`](MODEL_CATALOG.md#model-mobilenet-v4-fp32), [`mobilenet-v2-2022`](MODEL_CATALOG.md#model-mobilenet-v2-2022), [`efficientnet-lite0-f32`](MODEL_CATALOG.md#model-efficientnet-lite0-f32), [`efficientnet-lite0-i8`](MODEL_CATALOG.md#model-efficientnet-lite0-i8), [`resnet18-v1`](MODEL_CATALOG.md#model-resnet18-v1), [`squeezenet1-1`](MODEL_CATALOG.md#model-squeezenet1-1) |

**처리:** 모델별 crop/정규화 → 1000개 출력 → softmax 또는 모델 score 정렬 → Top5.

**활용:** 양자화/FP32 모델의 응답과 후보가 같은 입력에서 어떻게 다른지 살펴볼 때.

**한계:** 전체 장면에 대한 분류입니다. Top1 score는 정답률이나 보정된 신뢰구간이 아닙니다. 정답 비교가 없는 사진에 정확도 수치를 만들지 않습니다.

**구현 근거:** [OneModelRunner.java](../vision/src/main/java/com/yonlab/vision/OneModelRunner.java), [OneOnnx.java](../vision/src/main/java/com/yonlab/vision/OneOnnx.java), [OneEfficientNet.java](../vision/src/main/java/com/yonlab/vision/OneEfficientNet.java), [AddedVisionImageRunner.java](../vision/src/main/java/com/yonlab/vision/AddedVisionImageRunner.java).

<a id="feature-segment"></a>
## 영역 분할 (`segment`)

| 항목 | 내용 |
|---|---|
| 입력 | 사진 1장 |
| 출력 | 영역 mask·인물 alpha·원본 크기 overlay |
| 모델·엔진 선택 | [`pphumanseg-2023`](MODEL_CATALOG.md#model-pphumanseg-2023), [`selfie-segmentation`](MODEL_CATALOG.md#model-selfie-segmentation), [`deeplab-v3`](MODEL_CATALOG.md#model-deeplab-v3), [`segment-modnet`](MODEL_CATALOG.md#model-segment-modnet), [`segment-selfie-landscape`](MODEL_CATALOG.md#model-segment-selfie-landscape) |

**처리:** 선택한 사람/다중 클래스 모델 → 실제 mask 출력 → 원본 크기 복원 → 색상 overlay.

**활용:** 인물 경계, 배경과 객체 영역을 시각적으로 확인할 때.

**한계:** PPHumanSeg/Selfie/MODNet은 인물 중심, DeepLabV3는 PASCAL21입니다. 출력 공간이 달라 원시 score를 하나의 순위로 합치지 않습니다. 얇은 경계·가림·배경 유사색은 결과를 바꿉니다.

**구현 근거:** [OneModelRunner.java](../vision/src/main/java/com/yonlab/vision/OneModelRunner.java), [SupplementalImageRunner.java](../vision/src/main/java/com/yonlab/vision/SupplementalImageRunner.java), [OneDeepLabEngine.java](../vision/src/main/java/com/yonlab/vision/OneDeepLabEngine.java), [BackgroundStudio.java](../vision/src/main/java/com/yonlab/vision/BackgroundStudio.java), [AddedPortraitEngine.java](../vision/src/main/java/com/yonlab/vision/AddedPortraitEngine.java).

<a id="feature-faces"></a>
## 얼굴 분석 (`faces`)

| 항목 | 내용 |
|---|---|
| 입력 | 사진·영상; 지원 화면의 카메라 입력 |
| 출력 | 얼굴 위치·관측 상태·128D 특징 및 선택적 표정/나이 head 결과 |
| 모델·엔진 선택 | [`mlkit-sface-2023`](MODEL_CATALOG.md#model-mlkit-sface-2023), [`yunet-2026-sface`](MODEL_CATALOG.md#model-yunet-2026-sface), [`blazeface-short-sface`](MODEL_CATALOG.md#model-blazeface-short-sface), [`blazeface-full-sface`](MODEL_CATALOG.md#model-blazeface-full-sface) |

**처리:** 선택 얼굴 detector와 ML Kit 관측 → YuNet 정렬/SFace → 기능별 속성 관측 → overlay.

**활용:** 얼굴 pipeline의 관측 내용과 모델별 차이를 이해할 때.

**한계:** 표정·나이 head는 이미지 기반 추정입니다. 가림·조명·각도·검출 실패를 함께 살펴봅니다. 민감한 사람의 실제 신원·성격·국적 등을 이 결과에서 추정하지 않습니다.

**구현 근거:** [FaceEngine.java](../vision/src/main/java/com/yonlab/vision/FaceEngine.java), [OneFacePresentation.java](../vision/src/main/java/com/yonlab/vision/OneFacePresentation.java), [PhotoActivity.java](../vision/src/main/java/com/yonlab/vision/PhotoActivity.java), [VideoAnalysisActivity.java](../vision/src/main/java/com/yonlab/vision/VideoAnalysisActivity.java).

<a id="feature-compare"></a>
## 얼굴 비교 (`compare`)

| 항목 | 내용 |
|---|---|
| 입력 | 두 사진에서 각각 비교할 얼굴 선택 |
| 출력 | 두 선택 얼굴의 SFace128D 특징 비교 값 |
| 모델·엔진 선택 | [`mlkit-sface-2023`](MODEL_CATALOG.md#model-mlkit-sface-2023), [`yunet-2026-sface`](MODEL_CATALOG.md#model-yunet-2026-sface), [`blazeface-short-sface`](MODEL_CATALOG.md#model-blazeface-short-sface), [`blazeface-full-sface`](MODEL_CATALOG.md#model-blazeface-full-sface) |

**처리:** 두 입력 decode → 각 얼굴 탐지/선택 → 5점 정렬 → SFace → 공통 유사도 계산.

**활용:** 동일 embedding 공간에서 두 입력을 비교하는 과정을 학습할 때.

**한계:** 사진 비교 화면은 두 얼굴을 명시적으로 골라 실행합니다. 모델 비교 화면의 단일 사진 결과는 검출/특징 추출 비교입니다. 유사도와 설정 threshold는 법적 신원 확인을 보장하지 않습니다.

**구현 근거:** [CompareActivity.java](../vision/src/main/java/com/yonlab/vision/CompareActivity.java), [FaceEngine.java](../vision/src/main/java/com/yonlab/vision/FaceEngine.java), [Registry.java](../vision/src/main/java/com/yonlab/vision/Registry.java).

<a id="feature-access"></a>
## 출입 인식 (`access`)

| 항목 | 내용 |
|---|---|
| 입력 | 사진·영상과 명시적으로 등록한 얼굴 특징 |
| 출력 | 등록 특징과의 일치 후보·유사도·관측 위치 |
| 모델·엔진 선택 | [`mlkit-sface-2023`](MODEL_CATALOG.md#model-mlkit-sface-2023), [`yunet-2026-sface`](MODEL_CATALOG.md#model-yunet-2026-sface), [`blazeface-short-sface`](MODEL_CATALOG.md#model-blazeface-short-sface), [`blazeface-full-sface`](MODEL_CATALOG.md#model-blazeface-full-sface) |

**처리:** 입력 얼굴 → 정렬/SFace128D → 로컬 Registry의 같은 모델 서명 특징과 대조.

**활용:** 로컬 얼굴 등록·대조 방식과 threshold 영향을 확인할 때.

**한계:** 등록자료의 소유·사용 권한을 확인해야 합니다. 레퍼런스 구현의 유사도 대조는 실제 출입통제의 보안 수락 시험을 대신하지 않습니다. 등록·삭제·공유는 별도 사용자 행동입니다.

**구현 근거:** [Registry.java](../vision/src/main/java/com/yonlab/vision/Registry.java), [FaceEngine.java](../vision/src/main/java/com/yonlab/vision/FaceEngine.java), [FaceGalleryActivity.java](../vision/src/main/java/com/yonlab/vision/FaceGalleryActivity.java), [LiveActivity.java](../vision/src/main/java/com/yonlab/vision/LiveActivity.java).

<a id="feature-drive"></a>
## 드라이버 관측 (`drive`)

| 항목 | 내용 |
|---|---|
| 입력 | 사진·영상; 지원 화면의 카메라 입력 |
| 출력 | 눈/입/고개 관측, blink 및 관측 지속시간 |
| 모델·엔진 선택 | [`mlkit-face-contours`](MODEL_CATALOG.md#model-mlkit-face-contours), [`face-mesh-468`](MODEL_CATALOG.md#model-face-mesh-468), [`face-landmarker-478`](MODEL_CATALOG.md#model-face-landmarker-478) |

**처리:** ML Kit contour 또는 BlazeFace+landmark → 눈/입 기하·고개 자세 → Analytics.Drive 시간 상태.

**활용:** 기하 관측과 시간 상태가 결합되는 방식을 이해할 때.

**한계:** 정지 사진에는 지속시간을 부여하지 않습니다. ML Kit 눈 열림 확률과 FaceMesh EAR는 서로 다른 값입니다. 관측 중단·대상 변경은 시간 상태에 영향을 줍니다. 실제 운전자 안전 성능은 별도 수락이 필요합니다.

**구현 근거:** [FaceEngine.java](../vision/src/main/java/com/yonlab/vision/FaceEngine.java), [PlusFaceLandmarks.java](../vision/src/main/java/com/yonlab/vision/PlusFaceLandmarks.java), [Analytics.java](../vision/src/main/java/com/yonlab/vision/Analytics.java), [VideoAnalysisActivity.java](../vision/src/main/java/com/yonlab/vision/VideoAnalysisActivity.java).

<a id="feature-flow"></a>
## 사람 흐름 (`flow`)

| 항목 | 내용 |
|---|---|
| 입력 | 영상; 지원 화면의 실시간 관측 |
| 출력 | person track와 기준선 양방향 통과 수 |
| 모델·엔진 선택 | [`ssd-mobilenet-v1`](MODEL_CATALOG.md#model-ssd-mobilenet-v1), [`efficientdet-lite0`](MODEL_CATALOG.md#model-efficientdet-lite0), [`yolox-nano`](MODEL_CATALOG.md#model-yolox-nano), [`nanodet-plus`](MODEL_CATALOG.md#model-nanodet-plus), [`efficientdet-lite1`](MODEL_CATALOG.md#model-efficientdet-lite1) |

**처리:** COCO person 검출 → 위치 기반 track 연결 → hysteresis/TTL → 기준선 통과.

**활용:** 영상에서 추적·라인 통과 집계의 기본 구조를 살펴볼 때.

**한계:** 모델 비교의 사진 입력은 person 검출 비교만 수행합니다. 사람 겹침·가림·프레임 간격·TTL에 따라 track가 달라지며 수가 실제 출입 인원과 항상 같지는 않습니다.

**구현 근거:** [OneObjectEngine.java](../vision/src/main/java/com/yonlab/vision/OneObjectEngine.java), [Analytics.java](../vision/src/main/java/com/yonlab/vision/Analytics.java), [VideoAnalysisActivity.java](../vision/src/main/java/com/yonlab/vision/VideoAnalysisActivity.java), [LiveActivity.java](../vision/src/main/java/com/yonlab/vision/LiveActivity.java).

<a id="feature-vis"></a>
## 방문자 집계 (`vis`)

| 항목 | 내용 |
|---|---|
| 입력 | 영상과 얼굴 관측; 사진 비교는 얼굴 파이프라인만 실행 |
| 출력 | 현재 관측 수·익명 ID·방문 세션·체류 상태 |
| 모델·엔진 선택 | [`mlkit-sface-2023`](MODEL_CATALOG.md#model-mlkit-sface-2023), [`yunet-2026-sface`](MODEL_CATALOG.md#model-yunet-2026-sface), [`blazeface-short-sface`](MODEL_CATALOG.md#model-blazeface-short-sface), [`blazeface-full-sface`](MODEL_CATALOG.md#model-blazeface-full-sface) |

**처리:** 얼굴/embedding → VisitorRegistry 익명 ID → Analytics.Visitors 세션·체류 집계.

**활용:** 고유 관측과 재방문 세션의 차이를 학습할 때.

**한계:** historical unique 필드는 세션 수 의미를 가집니다. distinct는 해당 counter 범위의 익명 ID이며 실제 세계의 영구 고유 인원과 동일하지 않습니다. 추적 범위·재등장·용량 제한을 함께 기록합니다.

**구현 근거:** [VisitorRegistry.java](../vision/src/main/java/com/yonlab/vision/VisitorRegistry.java), [VisitorPolicy.java](../vision/src/main/java/com/yonlab/vision/VisitorPolicy.java), [Analytics.java](../vision/src/main/java/com/yonlab/vision/Analytics.java), [VideoAnalysisActivity.java](../vision/src/main/java/com/yonlab/vision/VideoAnalysisActivity.java).

<a id="feature-pose"></a>
## 자세 보기 (`pose`)

| 항목 | 내용 |
|---|---|
| 입력 | 사진 1장 |
| 출력 | 관절 좌표·likelihood·skeleton overlay |
| 모델·엔진 선택 | [`mlkit-pose`](MODEL_CATALOG.md#model-mlkit-pose), [`mlkit-pose-accurate`](MODEL_CATALOG.md#model-mlkit-pose-accurate), [`movenet-lightning`](MODEL_CATALOG.md#model-movenet-lightning), [`movenet-thunder`](MODEL_CATALOG.md#model-movenet-thunder), [`posenet-mobilenet`](MODEL_CATALOG.md#model-posenet-mobilenet), [`blazepose-full`](MODEL_CATALOG.md#model-blazepose-full) |

**처리:** ML Kit33/MoveNet17/PoseNet17/BlazePose33 → 공통 관절 ID와 좌표 연결 → overlay.

**활용:** 다른 pose 출력의 좌표와 관절 대응을 비교할 때.

**한계:** 모델마다 관절 수와 score 정의가 다릅니다. COCO17 연결은 33점 모델의 전체 정보를 뜻하지 않습니다. 가림·화면 밖 관절은 결과를 제한합니다.

**구현 근거:** [OnePoseEngine.java](../vision/src/main/java/com/yonlab/vision/OnePoseEngine.java), [OneMoveNetEngine.java](../vision/src/main/java/com/yonlab/vision/OneMoveNetEngine.java), [PlusPoseEngine.java](../vision/src/main/java/com/yonlab/vision/PlusPoseEngine.java), [PoseGeometry.java](../vision/src/main/java/com/yonlab/vision/PoseGeometry.java).

<a id="feature-ocr"></a>
## 문자 읽기 (`ocr`)

| 항목 | 내용 |
|---|---|
| 입력 | 사진 1장; KO/GO 언어 맥락 |
| 출력 | 원문 문자열·텍스트 영역·문자 수 |
| 모델·엔진 선택 | [`mlkit-ocr-ko`](MODEL_CATALOG.md#model-mlkit-ocr-ko), [`mlkit-ocr-latin`](MODEL_CATALOG.md#model-mlkit-ocr-latin), [`paddle-ocr-v5-ko`](MODEL_CATALOG.md#model-paddle-ocr-v5-ko), [`paddle-ocr-v6-ko`](MODEL_CATALOG.md#model-paddle-ocr-v6-ko), [`paddle-ocr-v6-tiny`](MODEL_CATALOG.md#model-paddle-ocr-v6-tiny) |

**처리:** 선택 ML Kit 또는 Paddle detector → line crop/recognizer → 문자열·영역 표시.

**활용:** 간판·인쇄물·문서의 인식 결과를 같은 사진에서 비교할 때.

**한계:** Paddle v6-ko는 v6 detector와 한국어 v5 recognizer를 조합합니다. v6-tiny는 영문·중문 비교용입니다. 글자 수·영역 수는 정확도가 아니며 검증된 정답과 실제 출력이 있어야 CER을 표시합니다.

**구현 근거:** [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [PaddleOcrEngine.java](../vision/src/main/java/com/yonlab/vision/PaddleOcrEngine.java), [PhotoVisionEngine.java](../vision/src/main/java/com/yonlab/vision/PhotoVisionEngine.java), [ModelPlusPolicy.java](../vision/src/main/java/com/yonlab/vision/ModelPlusPolicy.java).

<a id="feature-qr"></a>
## QR 읽기·만들기 (`qr`)

| 항목 | 내용 |
|---|---|
| 입력 | 코드가 있는 사진; 생성은 사용자가 입력한 payload |
| 출력 | 해석 문자열·symbol format·영역; 생성 QR |
| 모델·엔진 선택 | [`zxing-qr`](MODEL_CATALOG.md#model-zxing-qr), [`mlkit-barcode`](MODEL_CATALOG.md#model-mlkit-barcode), [`opencv-qr`](MODEL_CATALOG.md#model-opencv-qr), [`opencv-qr-aruco`](MODEL_CATALOG.md#model-opencv-qr-aruco) |

**처리:** ZXing/ML Kit Barcode/OpenCV 기본 또는 ArUco 검출 → 해독; 별도 encoder로 QR 생성.

**활용:** 같은 코드의 해석 결과와 서로 다른 검출 경로를 비교할 때.

**한계:** OpenCV 두 경로는 QR 전용이며 같은 학습 가중치 모델 두 개가 아닙니다. 임의 payload는 실행 전에 내용을 확인합니다. 정답 일치는 해당 encoded raster와 원문 payload/format에만 적용합니다.

**구현 근거:** [BarcodeHelper.java](../vision/src/main/java/com/yonlab/vision/BarcodeHelper.java), [OneBarcodeEngine.java](../vision/src/main/java/com/yonlab/vision/OneBarcodeEngine.java), [AddedQrRunner.java](../vision/src/main/java/com/yonlab/vision/AddedQrRunner.java), [ToolPhotoActivity.java](../vision/src/main/java/com/yonlab/vision/ToolPhotoActivity.java).

<a id="feature-speech"></a>
## 말을 글로 (`speech`)

| 항목 | 내용 |
|---|---|
| 입력 | 음성 파일 또는 사용자가 시작한 마이크 입력 |
| 출력 | 인식 원문·편집본·선택 모델·실행 기록 |
| 모델·엔진 선택 | [`vosk-ko-small-0.22`](MODEL_CATALOG.md#model-vosk-ko-small-0.22), [`vosk-en-us-small-0.15`](MODEL_CATALOG.md#model-vosk-en-us-small-0.15), [`whisper-tiny-multilingual-int8`](MODEL_CATALOG.md#model-whisper-tiny-multilingual-int8), [`whisper-base-multilingual-int8`](MODEL_CATALOG.md#model-whisper-base-multilingual-int8), [`whisper-small-multilingual-int8`](MODEL_CATALOG.md#model-whisper-small-multilingual-int8) |

**처리:** AudioConversion → mono16k PCM → Vosk streaming 또는 sherpa Whisper → 문자열/리포트.

**활용:** 한국어/영어 및 공통 다국어 ASR의 결과를 비교할 때.

**한계:** KO/GO는 입력 자료/기본 모델 맥락입니다. Whisper auto detected language와 별개입니다. 편집 문장을 원래 모델 출력으로 바꾸지 않습니다. WER/CER은 검증된 비어 있지 않은 정답이 있는 입력만 계산합니다.

**구현 근거:** [OfflineVoiceActivity.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/OfflineVoiceActivity.java), [VoiceCompareActivity.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceCompareActivity.java), [VoiceModels.java](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceModels.java).

<a id="feature-signage"></a>
## 매장·행사 안내판 (`signage`)

| 항목 | 내용 |
|---|---|
| 입력 | 사용자가 선택한 사진·영상 재생목록 |
| 출력 | 순서·간격에 따른 반복 재생과 전체 화면 |
| 모델·엔진 선택 | [`android-media`](MODEL_CATALOG.md#model-android-media), [`blazeface-short-sface`](MODEL_CATALOG.md#model-blazeface-short-sface), [`blazeface-full-sface`](MODEL_CATALOG.md#model-blazeface-full-sface) |

**처리:** 자료 추가 → 목록 순서 선택 → 재생/일시정지 → 전체 화면; 사진 timer와 영상 lifecycle 제어.

**활용:** 방문 안내·행사 공지·제품 소개를 로컬 화면에 반복 표시할 때.

**한계:** Android Media는 AI 모델이 아닙니다. BlazeFace Short/Full은 사용자가 켠 관객 관측 경로이며 맞춤 광고 성능으로 소개하지 않습니다. 기본 예시는 가상 문구이고 명시적 확인 후 목록을 바꿉니다. 장기 무인 운영은 별도 시험 범위입니다.

**구현 근거:** [SignageActivity.java](../vision/src/main/java/com/yonlab/vision/SignageActivity.java), [SignagePlaybackPolicy.java](../vision/src/main/java/com/yonlab/vision/SignagePlaybackPolicy.java), [SignageImportPolicy.java](../vision/src/main/java/com/yonlab/vision/SignageImportPolicy.java).

<a id="feature-document"></a>
## 문서·영수증 정리 (`document`)

| 항목 | 내용 |
|---|---|
| 입력 | 문서 사진 1장; KO/GO 맥락 |
| 출력 | 보정 이미지·OCR 원문·날짜/금액 후보 |
| 모델·엔진 선택 | [`doc-ocr-ko`](MODEL_CATALOG.md#model-doc-ocr-ko), [`doc-ocr-latin`](MODEL_CATALOG.md#model-doc-ocr-latin), [`doc-paddle-v5-ko`](MODEL_CATALOG.md#model-doc-paddle-v5-ko), [`doc-paddle-v6-ko`](MODEL_CATALOG.md#model-doc-paddle-v6-ko), [`doc-paddle-v6-tiny`](MODEL_CATALOG.md#model-doc-paddle-v6-tiny) |

**처리:** 문서 경계/원근 보정 → ocr ID로 정규화 → OCR → 규칙 기반 필드 추출.

**활용:** 문서 보정과 인식·필드 정리의 연결을 이해할 때.

**한계:** 독립된 새 OCR 가중치 5개가 아닙니다. 원문을 보존하며 금액·날짜 후보는 원본과 대조합니다. 실제 청구/결제 판단은 사용자 확인이 필요합니다.

**구현 근거:** [DocumentWorkflow.java](../vision/src/main/java/com/yonlab/vision/DocumentWorkflow.java), [ModelPlusPolicy.java](../vision/src/main/java/com/yonlab/vision/ModelPlusPolicy.java), [OneTextEngine.java](../vision/src/main/java/com/yonlab/vision/OneTextEngine.java), [ExpansionTruth.java](../vision/src/main/java/com/yonlab/vision/ExpansionTruth.java).

<a id="feature-studio"></a>
## 배경 스튜디오 (`studio`)

| 항목 | 내용 |
|---|---|
| 입력 | 인물 사진과 선택 배경/효과 |
| 출력 | 교체·흐림·투명 배경 결과 PNG |
| 모델·엔진 선택 | [`studio-pphuman`](MODEL_CATALOG.md#model-studio-pphuman), [`studio-selfie`](MODEL_CATALOG.md#model-studio-selfie), [`studio-modnet`](MODEL_CATALOG.md#model-studio-modnet), [`studio-selfie-landscape`](MODEL_CATALOG.md#model-studio-selfie-landscape) |

**처리:** 실제 인물 alpha mask → 원본 크기 보간 → 선택 효과 합성.

**활용:** 같은 인물 mask가 다른 합성 효과로 이어지는 과정을 살펴볼 때.

**한계:** 영역 분할과 동일 가중치를 재사용합니다. 마스크의 가림·헤어 경계 오류가 합성에도 남습니다. 투명 결과는 PNG 형태와 공유된 자료 고지를 함께 유지합니다.

**구현 근거:** [BackgroundStudio.java](../vision/src/main/java/com/yonlab/vision/BackgroundStudio.java), [AddedPortraitEngine.java](../vision/src/main/java/com/yonlab/vision/AddedPortraitEngine.java), [OnePngAttribution.java](../vision/src/main/java/com/yonlab/vision/OnePngAttribution.java).

<a id="feature-search"></a>
## 비슷한 사진 찾기 (`search`)

| 항목 | 내용 |
|---|---|
| 입력 | 사진 query와 명시적으로 구성한 갤러리 |
| 출력 | cosine 순위·상위 장면·gallery/input SHA |
| 모델·엔진 선택 | [`search-tinyclip-q8`](MODEL_CATALOG.md#model-search-tinyclip-q8), [`search-mobilenet-v3-small`](MODEL_CATALOG.md#model-search-mobilenet-v3-small), [`search-mobilenet-v3-large`](MODEL_CATALOG.md#model-search-mobilenet-v3-large), [`search-resnet18`](MODEL_CATALOG.md#model-search-resnet18), [`search-squeezenet`](MODEL_CATALOG.md#model-search-squeezenet) |

**처리:** 실제 encoder → L2 embedding → 같은 갤러리 cosine → 동일 픽셀 자기 사본 제외.

**활용:** 인코더별 시각 검색과 캐시 분리 방식을 이해할 때.

**한계:** 갤러리는 코드에서 3장 이상/96장 이하로 제한합니다. 다른 encoder의 벡터를 섞지 않습니다. ResNet/SqueezeNet은 분류 파일의 pooled512 출력을 재사용하며 생성형 텍스트 검색이 아닙니다.

**구현 근거:** [ImageRetrieval.java](../vision/src/main/java/com/yonlab/vision/ImageRetrieval.java), [RetrievalMath.java](../vision/src/main/java/com/yonlab/vision/RetrievalMath.java), [AddedVisionOnnx.java](../vision/src/main/java/com/yonlab/vision/AddedVisionOnnx.java).

<a id="feature-sound"></a>
## 생활 소리 분석 (`sound`)

| 항목 | 내용 |
|---|---|
| 입력 | 음성 파일 또는 명시적으로 시작한 주변 마이크 입력 |
| 출력 | 파형·시간대별 후보·전체 평균/peak·입력/PCM SHA |
| 모델·엔진 선택 | [`yamnet-classifier`](MODEL_CATALOG.md#model-yamnet-classifier), [`yamnet-original`](MODEL_CATALOG.md#model-yamnet-original), [`panns-cnn6`](MODEL_CATALOG.md#model-panns-cnn6), [`panns-cnn10`](MODEL_CATALOG.md#model-panns-cnn10) |

**처리:** 공통 mono16k PCM; YAMNet은 waveform, PANNs는 native32k log-mel → 창별 추론 → ranking.

**활용:** 분류 ontology와 오디오 frontend가 결과에 미치는 영향을 비교할 때.

**한계:** YAMNet521종과 PANNs527종은 같은 출력 공간이 아닙니다. 마지막 window zero pad와 분석 창 범위를 기록합니다. 점수는 확정 사건·연속 감시 성능을 보장하지 않습니다.

**구현 근거:** [SoundBenchActivity.java](../vision/src/main/java/com/yonlab/vision/SoundBenchActivity.java), [SoundEngine.java](../vision/src/main/java/com/yonlab/vision/SoundEngine.java), [PannSoundEngine.java](../vision/src/main/java/com/yonlab/vision/PannSoundEngine.java), [PannFrontend.java](../vision/src/main/java/com/yonlab/vision/PannFrontend.java).

<a id="feature-coach"></a>
## 운동 자세 코치 (`coach`)

| 항목 | 내용 |
|---|---|
| 입력 | 자세가 보이는 사진 1장 |
| 출력 | 관절 각도·좌우 상태·skeleton 설명 |
| 모델·엔진 선택 | [`coach-mlkit`](MODEL_CATALOG.md#model-coach-mlkit), [`coach-mlkit-accurate`](MODEL_CATALOG.md#model-coach-mlkit-accurate), [`coach-movenet-lightning`](MODEL_CATALOG.md#model-coach-movenet-lightning), [`coach-movenet-thunder`](MODEL_CATALOG.md#model-coach-movenet-thunder), [`coach-posenet`](MODEL_CATALOG.md#model-coach-posenet), [`coach-blazepose`](MODEL_CATALOG.md#model-coach-blazepose) |

**처리:** pose ID로 정규화 → 실제 관절 → 기하 각도·좌우 비교.

**활용:** 관절 기반 규칙과 시각 설명의 구현을 학습할 때.

**한계:** 자세 보기와 같은 6개 가중치를 사용합니다. 단일 사진에서 반복 횟수·장기 운동 개선·건강 상태를 추정하지 않습니다. 부정확한 관절은 기하 결과에도 영향을 줍니다.

**구현 근거:** [FitnessCoach.java](../vision/src/main/java/com/yonlab/vision/FitnessCoach.java), [CoachGeometry.java](../vision/src/main/java/com/yonlab/vision/CoachGeometry.java), [PoseGeometry.java](../vision/src/main/java/com/yonlab/vision/PoseGeometry.java).

## 결과를 읽을 때

모델별 score, cosine, 눈 열림 확률, EAR, 관절 likelihood는 정의가 다릅니다. 서로 다른 종류의 값에 공통 정확도 순위를 붙이지 않습니다. 검증된 정답·입력 hash join이 있을 때만 해당 입력의 CER, fieldExact, Top1/Top3, MRR 또는 PCK를 보여 줍니다. 지원하는 지표가 있다는 사실과 이 버전에서 실제로 평가했다는 사실은 별개입니다.

**공통 진입점:** [OneCatalog](../vision/src/main/java/com/yonlab/vision/OneCatalog.java), [OneLaunch](../vision/src/main/java/com/yonlab/vision/OneLaunch.java), [OneModels](../vision/src/main/java/com/yonlab/vision/OneModels.java), [DemoStore](../vision/src/main/java/com/yonlab/vision/DemoStore.java).
