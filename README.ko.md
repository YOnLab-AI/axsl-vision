# AXSL Vision 0.1

사진·영상·음성으로 19개 온디바이스 작업과 모델별 처리 과정을 살펴보는 Android 레퍼런스 앱입니다. 시험할 모델을 먼저 고른 뒤 다양한 입력으로 실제 출력·처리 시간·입력과 모델의 식별 정보를 확인합니다. 같은 입력에 여러 모델을 비교하는 흐름도 제공합니다.

이 저장소는 앱의 독자 코드를 Apache-2.0으로 공개합니다. 모델·SDK·데모 자료는 각각의 원래 조건을 유지합니다. 특히 작성 예제 33개는 **이 무료 앱 배포에 함께 포함하는 범위**로 제공되며 Apache-2.0 대상이 아닙니다. [자료와 이용 조건](docs/DATA_AND_LICENSES.md)을 먼저 확인하세요.

[English](README.md) · [모델 시험 안내](docs/MODEL_TESTING.md) · [기능별 설명](docs/FEATURES.md) · [모델 카탈로그](docs/MODEL_CATALOG.md) · [구조](docs/ARCHITECTURE.md) · [검증 범위](docs/VALIDATION.md)

모델 시험 업데이트는 표시 버전 **0.1**, 내부 **code21**입니다. 새 빌드의 실제 시험 기록은 [버전별 검증](docs/VALIDATION.md)에서 확인합니다. 아래 code20 수치는 이전 공개 APK의 근거로 유지합니다.

## 모델을 중심으로 시험하기

1. 홈에서 **모델 시험**을 열고 표시명과 고유 실행 ID를 확인합니다.
2. 선택 모델이 지원하는 **시험 기능**과 입력 형식을 고릅니다.
3. **조건별 6개** 또는 **전체 목록**에서 그룹·난도·조건이 다른 자료를 시험합니다. 전체 목록은 12개씩 더 볼 수 있습니다.
4. 자료를 바꾸어도 모델을 유지하고, **이 모델의 시험 기록**에서 같은 ID의 입력별 결과를 확인합니다.

현재 기능·모델 안내는 목적, 모델·엔진 ID, 실제 runtime, 입력 조건, 결과 해석 순서로 통일했습니다. **기본 선택**은 성능 순위가 아니며 검증 참조값이 없는 자료의 점수·개수를 정확도로 표시하지 않습니다. [상세 사용·개발 안내](docs/MODEL_TESTING.md)에 모델 구성 시험과 전체 기능 시험의 차이를 정리했습니다.

## 무엇을 할 수 있나요?

| 분야 | 기능 |
|---|---|
| 이미지·문서 | 객체 찾기, 이미지 분류, 영역 분할, 문자 읽기, QR 읽기·만들기, 문서·영수증 정리, 배경 스튜디오, 비슷한 사진 찾기 |
| 사람·공간 | 얼굴 분석, 얼굴 비교, 출입 인식, 드라이버 관측, 사람 흐름, 방문자 집계, 자세 보기, 운동 자세 코치 |
| 음성·미디어 | 말을 글로, 생활 소리 분석, 매장·행사 안내판 |

레지스트리에는 **87개 기능별 모델·엔진 슬롯, 68개 고유 실행 ID**가 있습니다. 기능별 별칭과 공유 가중치, SDK 기반 모델, 코드 엔진을 포함한 수입니다. Android Media·ZXing·OpenCV QR 경로는 학습 모델과 구분합니다. 전체 목록은 [모델 카탈로그](docs/MODEL_CATALOG.md)에 있습니다.

공개 기본 자료는 312개입니다. 이미지 235개·음성 66개·영상 11개, KO 105개·GO 131개·SHARED 76개로 구성됩니다. KO/GO는 자료·언어 그룹입니다. 출처가 불명확한 `go-coco-86`은 기본 공개 목록에서 제외했습니다. 안내판의 방문·행사·제품 문구는 가상의 예시입니다.

## 준비와 빌드

JDK21, Android SDK Platform36·Build Tools36.0.0, Python3.10 이상을 준비하고 `JAVA_HOME`과 `ANDROID_HOME`을 설정합니다. Android 최소 버전은 API24이며 compile/target은 36입니다. Java 소스 호환성은 17, Gradle은 9.1.0, Android Gradle Plugin은 9.0.1로 고정합니다.

큰 모델·미디어·로컬 음성 runtime은 Git 본문과 분리한 Release pack으로 제공합니다. [자산 복원 안내](assets/README.md)와 [fetcher 사용법](tools/README.md)을 확인할 수 있습니다. 저장소의 고정 lock을 기준으로 허용된 파일·크기·SHA-256을 검증하여 복원합니다. 자산을 임의의 최신 파일로 바꾸면 원래 전처리·출력 계약과 달라질 수 있습니다.

```sh
python tools/fetch_assets.py --all
./gradlew :vision:testDebugUnitTest :offlinevoice:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

PowerShell에서는 다음 명령을 사용합니다.

```powershell
python tools/fetch_assets.py --all
.\gradlew.bat :vision:testDebugUnitTest :offlinevoice:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

출력은 `app/build/outputs/apk/debug/app-debug.apk`입니다. 앱을 실행한 뒤 모델 시험→모델→지원 기능→기본 자료 또는 내 파일→시험 순서로 확인합니다. 기존 기능 탐색과 같은 입력의 모델 비교도 사용할 수 있습니다. 카메라·마이크 입력은 해당 기능에서 사용자가 시작하고 권한을 허용한 경우에 사용합니다. HTML/CSV/JSON 및 결과 이미지의 저장·공유는 명시적 행동으로 실행합니다.

공개 소스에 개인 서명키를 포함하지 않습니다. 기본 debug 빌드는 로컬 debug 서명을 사용합니다. release 서명은 외부 환경의 `AXSL_SIGNING_STORE_FILE`, `AXSL_SIGNING_STORE_PASSWORD`, `AXSL_SIGNING_KEY_ALIAS`, `AXSL_SIGNING_KEY_PASSWORD`로 제공합니다. 동일 applicationId의 기존 설치본을 업데이트하려면 인증서가 일치해야 합니다. 저장소를 복제했다는 사실만으로 이전 배포본의 서명 권한이 생기지 않습니다.

## 검증 결과와 범위

공개 **0.1/code20 release**에서 Android 단위시험244개와 자산 bootstrap16개, 총 **호스트260개**가 통과했습니다. release 빌드 성공, lint 오류0·MonochromeLauncherIcon 경고2입니다. SM-F956N/API37·글자 배율1.5에서 **실제 engine 실행5건**과 **UI 관측5건**을 각각 통과했습니다. 기기 관측은 호스트260개와 별도로 셉니다.

대표 실행은 MobileNetV4 분류, EfficientDet Lite0 탐지, ML Kit Korean OCR, ZXing QR, Vosk 한국어의 각 입력 한 개입니다. 분류·탐지·OCR은 출력 관측이고 QR은 해당 raster의 원문·형식 일치입니다. Vosk의 **9.9초 한 클립 CER0.15789**는 NFC·Unicode 공백 제거·대소문자/문장부호 유지·codepoint 편집거리 정책의 결과입니다. corpus 정확도나 반복 benchmark를 뜻하지 않습니다. 이미 준비된 PCM engine 시간과 입력 준비/decode 시간은 분리했습니다.

APK 크기: **1,583,002,073바이트**. SHA-256:

`cde2536127ea067c0c4b2ff143e3113f7f051565b2338043747f144d7a7721f7`

[검증 상세](docs/VALIDATION.md)와 [개인 정보 없는 0.1 요약](docs/validation-0.1.json)에 입력·모델 hash와 시간 범위를 남겼습니다. 이전 **4.4.0/code19**의 물리22건·호스트244건·대표4실행은 역사적 근거로 분리했습니다. 전체19기능·68개ID의 정확도, 장기 무인 안내판 운영, 다른 Android/기기·보드, 기계적 접힘/펼침은 이번 수락 범위에 포함하지 않습니다.

## 문서와 출처

| 문서 | 내용 |
|---|---|
| [MODEL_TESTING](docs/MODEL_TESTING.md) | 모델 먼저 선택, 다양한 입력 조건, 동일 ID의 시험 기록, 결과 해석 |
| [FEATURES](docs/FEATURES.md) | 19기능의 입력·출력·모델·처리·활용·한계·구현 위치 |
| [MODEL_CATALOG](docs/MODEL_CATALOG.md) | 68개 ID의 runtime·파일 binding·전후 처리·공유 관계·이용 조건 |
| [ARCHITECTURE](docs/ARCHITECTURE.md) | 세 모듈, 입력 전달, controller, 수명주기, 기록·공유 |
| [VALIDATION](docs/VALIDATION.md) | 버전별 검증과 과거 단일 입력 관측의 범위 |
| [DATA_AND_LICENSES](docs/DATA_AND_LICENSES.md) | 코드·모델·SDK·자료·작성 예제의 별도 조건 |
| [LIMITATIONS](docs/LIMITATIONS.md) | 모델·평가·시간·기기·배포의 해석 범위 |

공식 runtime 문서: [ONNX Runtime Java](https://onnxruntime.ai/docs/get-started/with-java.html), [LiteRT](https://developers.google.com/edge/litert), [ML Kit 약관](https://developers.google.com/ml-kit/terms), [Vosk 모델 목록](https://alphacephei.com/vosk/models), [Whisper](https://github.com/openai/whisper).
