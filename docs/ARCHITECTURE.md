# 구조와 데이터 흐름

AXSL Vision 0.1은 하나의 Android application과 두 Android library 모듈로 구성됩니다. 기능별로 별도 APK를 실행하지 않고 입력·모델·자료 맥락을 내부 Activity와 공통 결과 계약에 전달합니다.

## 모듈과 책임

| 공개 경로 | Gradle 모듈 | 책임 |
|---|---|---|
| `app/` | `:app` | applicationId·버전·권한 병합·런처·서명·최종 패키징 |
| `vision/` | `:vision` | 기능·모델·자료 레지스트리, 사진/영상/실시간 처리, QR, 소리, 안내판, 기록·공유 |
| `offlinevoice/` | `:offlinevoice` | Vosk/Whisper ASR, 음성 입력 변환, 언어 그룹·비교·리포트 |
| `tools/` | 호스트 도구 | 고정 asset lock에 따른 runtime·모델·미디어 복원 |

모듈의 실제 연결은 [settings.gradle](../settings.gradle), 의존성은 [app/build.gradle](../app/build.gradle), [vision/build.gradle](../vision/build.gradle), [offlinevoice/build.gradle](../offlinevoice/build.gradle)이 기준입니다. 공개 버전의 디렉터리는 `app/vision/offlinevoice`이며 이전 작업 폴더의 staging 경로를 요구하지 않습니다.

## 작업 시작과 입력

code21의 모델 중심 진입점은 [ModelTestActivity](../vision/src/main/java/com/yonlab/vision/ModelTestActivity.java)입니다. 현재 기기의 실행 ID를 중복 없이 모으고 ID를 지원하는 기능만 제시합니다. [OneTestSamples](../vision/src/main/java/com/yonlab/vision/OneTestSamples.java)는 자료의 난도·그룹·형식·조건 태그로 필터와 다양성 선택을 수행합니다. 이는 모델 정확도 예측이 아닙니다. 사용자 흐름은 [MODEL_TESTING](MODEL_TESTING.md)에 있습니다.

[OneGuidance](../vision/src/main/java/com/yonlab/vision/OneGuidance.java)는 현재 기능·모델 ID·runtime·입력 조건·결과 해석을 공통 native panel로 표시합니다. 작은 [model-guide.json](../vision/src/main/assets/one/model-guide.json)은 문서 카탈로그의 실행 메타데이터에서 [sync_model_guide.py](../tools/sync_model_guide.py)로 생성하며, 과거 시험 결과나 성능 순위를 포함하지 않습니다.

1. [OneCatalog](../vision/src/main/java/com/yonlab/vision/OneCatalog.java)가 19개 기능의 ID·입력 형식을 정의합니다.
2. [OneModels](../vision/src/main/java/com/yonlab/vision/OneModels.java)는 기능·API·KO/GO 맥락에 맞는 실행 ID와 저장 선택을 구합니다. base 정책은 [OneModelPolicy](../vision/src/main/java/com/yonlab/vision/OneModelPolicy.java), 추가 선택은 [ModelPlusPolicy](../vision/src/main/java/com/yonlab/vision/ModelPlusPolicy.java), 음성은 [VoiceModels](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceModels.java)가 소유합니다.
3. [OneInputs](../vision/src/main/java/com/yonlab/vision/OneInputs.java)는 내부 자료 라이브러리를 엽니다. 내 파일 가져오기는 명시적인 별도 입력입니다.
4. [DemoStore](../vision/src/main/java/com/yonlab/vision/DemoStore.java)는 데모 ID·encoded SHA·bytes·자료 형식·언어 그룹·출처·정답 정보를 읽고 실제 자료 사본의 URI를 제공합니다.
5. [OneLaunch](../vision/src/main/java/com/yonlab/vision/OneLaunch.java)가 기능·model ID·content URI·demo ID·group을 적절한 controller에 전달합니다. 실행 여부는 진입점이 명시적으로 전달합니다. 사용자의 **이 자료로 시험** 동작, controller의 시작 버튼, 비공개 검증 도구의 명시적 auto-start를 서로 구분합니다.

모델 시험의 `OneLaunch.openTest`는 사용자가 고른 실제 ID와 `keep_model` 맥락을 전달합니다. [OneModelTestPolicy](../vision/src/main/java/com/yonlab/vision/OneModelTestPolicy.java)는 입력 변경 시 ID·선택 집합을 유지하고, 자료 언어로 모델을 대신 고르지 않게 합니다. 정지 사진은 `ModelBenchActivity`, 음성은 `VoiceCompareActivity`, 소리는 `SoundBenchActivity`, 영상은 영상 controller를 사용합니다. 단일 사진의 모델 구성 시험과 전용 두 얼굴 대조·시간 집계 등 전체 기능 흐름은 구분합니다.

모델 시험의 기록 진입점은 `OneReportsActivity`에 `model_id`를 전달합니다. 같은 ID의 행을 먼저 모은 뒤 화면 필터·HTML/CSV 공유에 적용하므로 다른 모델의 기록을 같은 모델의 입력별 결과로 섞지 않습니다.

content URI 권한을 전달하고 원본 입력 식별을 유지합니다. 같은 원본으로 다른 기능을 열 때 encoded SHA를 보존합니다. 가공 결과 PNG는 별도 결과물이며 입력 원본의 hash로 바꾸지 않습니다. `FileProvider` 설정은 [vision manifest](../vision/src/main/AndroidManifest.xml)에 있습니다.

## controller와 엔진

| 작업 | controller·핵심 구현 | 결과 |
|---|---|---|
| 정지 사진·같은 입력 비교 | [ModelBenchActivity](../vision/src/main/java/com/yonlab/vision/ModelBenchActivity.java), [OneModelRunner](../vision/src/main/java/com/yonlab/vision/OneModelRunner.java) | 모델별 상태·출력·시간·해시 |
| 영상·시간 상태 | [VideoAnalysisActivity](../vision/src/main/java/com/yonlab/vision/VideoAnalysisActivity.java), [Analytics](../vision/src/main/java/com/yonlab/vision/Analytics.java) | 검출·track·line crossing·Drive/Visitor 상태 |
| 실시간 카메라 | [LiveActivity](../vision/src/main/java/com/yonlab/vision/LiveActivity.java) | 프레임 관측·수명주기 상태 |
| 두 얼굴 비교·등록 | [CompareActivity](../vision/src/main/java/com/yonlab/vision/CompareActivity.java), [Registry](../vision/src/main/java/com/yonlab/vision/Registry.java) | 선택한 얼굴의 특징 비교·로컬 등록 대조 |
| 문서·배경·검색·코치 | [ExpansionImageRunner](../vision/src/main/java/com/yonlab/vision/ExpansionImageRunner.java) | OCR 필드·합성 이미지·검색 순위·관절 기하 |
| 생활 소리 | [SoundBenchActivity](../vision/src/main/java/com/yonlab/vision/SoundBenchActivity.java), [SoundEngine](../vision/src/main/java/com/yonlab/vision/SoundEngine.java) | PCM·시간 창·분류 후보·입력 식별 |
| 말→글 | [OfflineVoiceActivity](../offlinevoice/src/main/java/com/yonlab/offlinevoice/OfflineVoiceActivity.java), [VoiceCompareActivity](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceCompareActivity.java) | 인식 원문·편집본·언어/모델 맥락·리포트 |
| 안내판 | [SignageActivity](../vision/src/main/java/com/yonlab/vision/SignageActivity.java), [SignagePlaybackPolicy](../vision/src/main/java/com/yonlab/vision/SignagePlaybackPolicy.java) | 사진·영상 순환·pause/resume·전체 화면 |

각 기능의 전체 흐름은 [FEATURES](FEATURES.md), model ID별 정확한 자산 binding은 [MODEL_CATALOG](MODEL_CATALOG.md)에 있습니다. flow의 정지 사진 비교는 검출 결과이며 line crossing은 시간 controller에서 계산합니다. 같은 방식으로 drive의 사진 관측에 눈 감김 지속시간을 붙이지 않습니다.

## runtime·모델을 구분하는 경계

ONNX Runtime Android 1.24.3, LiteRT 1.4.2 Interpreter, OpenCV 4.13.0이 공개 Java API를 통해 실행됩니다. ML Kit는 고정 AAR의 API와 SDK 관리 모델을 사용합니다. 음성은 Vosk 0.3.75 및 sherpa-onnx 2.0.0 local AAR를 사용하며 포함 native 구성요소 고지는 별도로 보존합니다. 일반 vision ORT와 음성 AAR 내부 ORT의 버전·license scope를 합치지 않습니다.

[OneExecutionSettings](../vision/src/main/java/com/yonlab/vision/OneExecutionSettings.java)의 auto/cpu1/cpu2/cpu4는 CPU 요청 설정입니다. 모델 종류가 아니며 SDK가 내부 스케줄링을 모두 이 값으로 맞춘다고 가정하지 않습니다. 보드 설정은 지원 보드 식별과 사용자의 선택을 따릅니다. 이 설정이 있다는 사실은 NPU/DSP/NNAPI 가속을 검증했다는 뜻이 아닙니다.

## 자산 복원과 파일 신뢰

큰 binary는 Release pack으로 분리하고 `python tools/fetch_assets.py --all`로 복원합니다. fetcher는 저장소의 고정 lock을 기준으로 파일 경로·크기·SHA를 확인합니다. 확인되지 않은 archive entry나 임의의 최신 모델로 대체하지 않습니다. 앱 내부에서도 [ModelPlusAssets](../vision/src/main/java/com/yonlab/vision/ModelPlusAssets.java), [OneOnnx](../vision/src/main/java/com/yonlab/vision/OneOnnx.java), [WhisperModelStore](../offlinevoice/src/main/java/com/yonlab/offlinevoice/WhisperModelStore.java), [PlusWhisperModels](../offlinevoice/src/main/java/com/yonlab/offlinevoice/PlusWhisperModels.java)가 고정 manifest와 파일을 확인합니다.

모델 hash가 같아도 전처리·label·사전·출력 decode를 바꾸면 다른 pipeline입니다. ResNet/SqueezeNet의 검색은 원래 학습 tensor를 유지한 파일의 pooled feature 출력을 사용합니다. Paddle OCR의 detector·recognizer·alphabet은 함께 식별해야 합니다. Vosk/Whisper ZIP은 여러 모델 구성 파일을 가진 archive입니다.

## 수명주기와 저장

작업은 worker에서 수행하고 취소·화면 소유권·결과 세대를 확인합니다. Activity 재배치가 입력이나 완료 결과를 다른 실행으로 바꾸지 않도록 run/input/group 상태를 함께 확인합니다. 취소·오류·완료를 구분하고 생성한 runtime 입력·출력·세션은 소유한 worker 경로에서 정리합니다. 일부 OpenCV wrapper/공유 모델은 process cache를 사용하므로 모든 native 모델이 매 화면 종료 시 해제된다고 설명하지 않습니다.

안내판의 전체 화면은 재생 View와 현재 영상 위치를 유지하면서 조작부를 전환합니다. 재생 중에만 화면 켜짐 flag를 적용하고 pause·백그라운드 전환에서 해제합니다. 새 목록으로 바꾸거나 자료를 삭제할 때는 앱 소유 사본과 외부 원본을 구분합니다. 관객 관측 설정은 사용자의 명시적 선택을 유지합니다.

## 결과·정답·공유

[OneRunLog](../vision/src/main/java/com/yonlab/vision/OneRunLog.java)는 실제 실행 메타를 기록합니다. `loadMs`, `inferenceMs`, `elapsedMs`, state, input/model SHA는 서로 다른 필드입니다. 원본 decode·모델 준비·SDK lazy initialization이 어느 범위에 포함됐는지 결과의 `timingScope`를 함께 읽습니다.

[ExpansionTruth](../vision/src/main/java/com/yonlab/vision/ExpansionTruth.java)는 demo ID와 실제 encoded SHA가 맞는 정답만 연결합니다. 문서 fieldExact·검색 MRR·소리 Top1/Top3·관절 PCK·음성 CER/WER 등의 계산 지원과 실제 평가 완료는 구분합니다. 정답이 없으면 관측값만 표시합니다.

[OneInputAttribution](../vision/src/main/java/com/yonlab/vision/OneInputAttribution.java), [OnePngAttribution](../vision/src/main/java/com/yonlab/vision/OnePngAttribution.java), [OneModelReport](../vision/src/main/java/com/yonlab/vision/OneModelReport.java), [OneExportCore](../vision/src/main/java/com/yonlab/vision/OneExportCore.java)가 결과·입력 출처·텍스트 escaping·CSV 수식을 다룹니다. HTML/CSV/JSON와 결과 PNG는 사용자가 명시적으로 내보냅니다. 원본·파생 미디어의 조건을 독립 숫자 관측값 전체의 라이선스로 바꾸지 않습니다.

## 권한·배포

app manifest는 INTERNET permission을 병합에서 제거합니다. CAMERA와 RECORD_AUDIO는 해당 입력에 사용합니다. 외부 공유는 Android의 명시적 전달 동작입니다. 최종 permission은 해당 0.1 APK의 패키지 검사 결과를 [VALIDATION](VALIDATION.md)에서 확인합니다. source의 이전 네트워크 helper 클래스 존재와 최종 application의 permission을 구분합니다.

public repository에 개인 서명키·로컬 SDK 경로·private 검증 helper를 포함하지 않습니다. debug는 로컬 debug 인증서를 사용하고 release 서명은 외부 환경에서 제공합니다. reproducible input/model 식별과 APK의 bit-for-bit 재현은 다른 요구사항입니다. 공개 빌드 결과의 APK SHA는 실제 생성 후 기록합니다.
