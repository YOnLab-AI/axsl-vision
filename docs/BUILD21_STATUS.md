# Build21 verification results

표시 버전은 **0.1**, 내부 versionCode는 **21**입니다. 모델을 먼저 선택하고 다양한 입력 조건을 시험하는 흐름과 일관된 기능·모델 안내를 추가했습니다. 아래 결과는 같은 서명 release APK를 업데이트 설치한 실제 관측입니다. 이전 code20 기록과 `v0.1` 자산 pack은 보존합니다.

## 완료한 검증

| 범위 | 결과 | 집계 기준 |
|---|---|---|
| Android 호스트 시험 | 259 PASS / 43 suites | 실패·오류·skip0 |
| Python 호스트 시험 | 20 PASS | 자산 도구16 + 모델 안내 계약4 |
| 호스트 합계 | **279 PASS** | 기기 관측과 별도 |
| Release build / lint | 성공 / 오류0·기존 monochrome fallback 경고2 | 실제 themed launcher mode는 별도 미시험 |
| APK 서명 /16KB ZIP 정렬 | PASS | 개인 서명 키는 공개하지 않음 |
| 일반 UI 관측 | **6/6 PASS** | 글자 배율1.5, 가로·세로 전환과 실제 화면 복귀 포함 |
| 다양한 입력 행렬 | **17/17 실행 PASS** | 모델·입력 조합17건, 고유 ID6개, 서로 다른 encoded 입력13개 |
| 선택 유지·보고서 회귀 포함 controller 실행 전체 | **24건 완료** | 고유 ID8개, 서로 다른 encoded 입력16개; 반복 호출은 별도 실행으로 셈 |
| 저장 보고서 생성·모델 귀속 | 이미지1건·소리1건 PASS | 위24건에 포함; 실제 JSON과 모델별 SAVED 이벤트 집합 일치 |
| 저장 기록의 엄격한 모델 필터 | **2건 PASS** | 이미지·소리 각각의 실제 저장 카드·개수 확인; 모델 실행 건수에 추가하지 않음 |

기기는 **Galaxy Z Fold6 / SM-F956N / Android17(API37)**입니다. [개인 정보 없는 JSON](validation-0.1-build21.json)에 입력·모델 hash, 조건, 결과 상태와 시간 범위를 남겼습니다.

[원격 소스 CI](https://github.com/YOnLab-AI/axsl-vision/actions/runs/40323411899)도 구현 revision `02897725c629bed9ff3aaa61764a28625980fabd`에서 성공했습니다. 범위는 Python 시험, runtime 다운로드, Android 호스트 단위시험과 lint입니다. 기기 추론은 별도 검사이며 문서만 바꾼 후속 커밋과 구현 revision을 구분합니다.

## 모델을 고정하는 경로와 엔진 행렬

- 일반 UI6건은 실제 화면 조작으로 안내5필드, 조건별6개·전체 목록, 가로·세로 전환, 같은 모델의 기록과 복귀를 확인했습니다. 가로 화면은 실제 캡처 크기2376×968과 rotation1도 확인했습니다.
- 모델 시험 화면의 실제 Spinner·필터·자료 버튼에서 production `OneLaunch.openTest`로 이어지는 실행1건을 확인했습니다. 기본값과 다른 `mobilenet-v2-2022`가 결과까지 유지됐습니다.
- 같은 이미지 controller의 KO→GO 입력 변경2건과 같은 음성 controller의 KO→GO 입력 변경2건은 비공개 instrumentation에서 실제 자료 URI를 production 입력 반환 callback에 전달하고 실제 Run 버튼을 눌러 확인했습니다. 일반 라이브러리 picker를 처음부터 끝까지 터치한 시험은 아닙니다.
- 17건 행렬은 입력마다 production controller에 진입하고 실제 보이는 선택 control로 요청 ID를 고르는 방식입니다. `keep_model` 고정 모드의 연속 입력 시험으로 표시하지 않습니다. 사후 검사는 결과가 정확히1개이고 요청 ID·입력 SHA·리소스 정리 상태가 맞는지 확인했습니다.
- 비공개 helper와 raw 장치 로그는 배포하지 않습니다. 공개 요약만으로 같은 harness를 독립적으로 실행할 수는 없으며 공개 앱의 모델 시험 흐름으로 입력·ID를 확인할 수 있습니다.

## 관측된 차이

| 모델·기능 | 실제 관측 | 해석 범위 |
|---|---|---|
| MobileNetV4 분류 | 원본은 racket/racquet 0.74189, 어둡고 작은 변형은 croquet ball 0.54629. 빈 종이도 cleaver 후보 0.00522를 출력 | Top1 후보·점수 관측이며 정답률 아님. 분류기는 빈 입력에도 학습 분류 중 후보를 낼 수 있음 |
| EfficientDet Lite0 탐지 | 원본 3개, 저조도 2개, 빈 종이 0개 검출 | 독립적인 전체 정답 상자·개수 평가 없이 검출 정확도로 환산하지 않음 |
| ML Kit Korean OCR | 선명·흐림·기울어짐에서 각각 79자·8영역, 빈 종이 0자·0영역 | 같은 문자 수·영역 수가 같은 인식 정확도를 뜻하지 않음 |
| ZXing QR | 선명 코드와 원근 변형 코드 모두 해당 원문·형식 일치 관측 | 선명 코드의 정확 일치만 행렬 수락 기준. 변형 코드는 관측 결과 |
| Vosk 한국어 | 같은 발화의 clean CER0.68966, noise10 CER0.75862, noise20 CER0.65517 | 단일 발화별 CER. 잡음 표기는 기존 fixture의 명칭이며 새 SNR 실측 아님 |
| Whisper Tiny INT8 | 같은 한국어 clean 클립 CER0.41379, 별도 영어 클립 CER0.06250 | 해당 입력·정규화 정책의 값이며 언어 전체 또는 모델 전체의 순위 아님 |

CER은 `NFC_REMOVE_UNICODE_WHITESPACE_PRESERVE_CASE_PUNCTUATION_CODEPOINTS_V1` 정책을 사용한 해당 클립의 편집거리입니다. Vosk의 clean/noise10/noise20 값은 단조 관계를 보이지 않았습니다. 한 발화와 기존 잡음 명칭만으로 잡음 강도의 효과나 모델 성능 순위를 일반화하지 않습니다. 한국어 같은 clean 입력의 두 모델 관측도 corpus benchmark를 대신하지 않습니다.

<details><summary>17개 모델·입력 조합과 단일 실행 시간</summary>

| suite / model ID | 입력 ID | 실제 관측 | load / inference / elapsed ms |
|---|---|---|---|
| `classification` / `mobilenet-v4-fp32` | `exp-person-239537` | Top1 racket, racquet, score 0.74189 | 96 / 35 / 133 |
| `classification` / `mobilenet-v4-fp32` | `exp-person-239537-query-small-dim` | Top1 croquet ball, score 0.54629 | 43 / 14 / 58 |
| `classification` / `mobilenet-v4-fp32` | `exp-negative-blank` | Top1 cleaver, meat cleaver, chopper, score 0.00522 | 36 / 15 / 52 |
| `detection` / `efficientdet-lite0` | `exp-person-239537` | 3 detections; no accuracy gate | 27 / 26 / 56 |
| `detection` / `efficientdet-lite0` | `exp-person-239537-lowlight` | 2 detections; no accuracy gate | 16 / 40 / 59 |
| `detection` / `efficientdet-lite0` | `exp-negative-blank` | 0 detections; no accuracy gate | 20 / 31 / 54 |
| `korean-ocr` / `mlkit-ocr-ko` | `exp-doc-ko-01` | 79 chars / 8 blocks | 16 / 154 / 172 |
| `korean-ocr` / `mlkit-ocr-ko` | `exp-doc-ko-01-dim-blur` | 79 chars / 8 blocks | 0 / 129 / 131 |
| `korean-ocr` / `mlkit-ocr-ko` | `exp-doc-ko-01-tilted` | 79 chars / 8 blocks | 1 / 148 / 151 |
| `korean-ocr` / `mlkit-ocr-ko` | `exp-negative-blank` | 0 chars / 0 blocks | 0 / 124 / 125 |
| `qr-clear` / `zxing-qr` | `qr-demo-1` | QR payload/format matched: true | 0 / 75 / 76 |
| `qr-degraded` / `zxing-qr` | `exp-qr-ko-perspective` | QR payload/format matched: true | 0 / 23 / 24 |
| `vosk-noise` / `vosk-ko-small-0.22` | `ko-speech-2-clean` | CER 0.68966 (single clip) | 1539 / 1752 / 3469 |
| `vosk-noise` / `vosk-ko-small-0.22` | `ko-speech-2-noise10` | CER 0.75862 (single clip) | 1351 / 2129 / 3729 |
| `vosk-noise` / `vosk-ko-small-0.22` | `ko-speech-2-noise20` | CER 0.65517 (single clip) | 1716 / 2196 / 4201 |
| `whisper-languages` / `whisper-tiny-multilingual-int8` | `ko-speech-2-clean` | CER 0.41379 (single clip) | 553 / 496 / 1074 |
| `whisper-languages` / `whisper-tiny-multilingual-int8` | `go-speech-fleurs-001` | CER 0.06250 (single clip) | 521 / 390 / 944 |

</details>

정지 이미지의 시간은 source decode를 제외한 모델 준비·전처리·추론·후처리 범위입니다. 음성 `PREDECODED_SAME_PCM_ENGINE` 시간은 준비된 PCM의 engine 실행이며 입력 준비/decode 시간과 분리했습니다. JSON에서 encoded 입력 SHA와 PCM SHA를 다른 필드로 유지합니다. 한 번의 ms를 반복 성능·FPS·전력·NPU 가속으로 환산하지 않습니다.

## 시험 도구 진단과 재검증

초기 실패 원문은 비공개 진단으로 보존하고, 원인을 확인한 뒤 **동일 APK SHA**로 다시 시험했습니다.

- 일반 UI 도구의 swipe가 x2 좌표를 누락했고0ms tap도 무시됐습니다. 좌표를 바로잡고 display0에서220ms press를 사용한 재시험이6/6 통과했습니다.
- 이미지 입력 반환 회귀 도구는 교체된 입력이 자동으로 재실행되기를 기다렸습니다. production 모델 시험은 명시적 시작을 요구하므로, 모델 유지와 idle 상태를 확인한 뒤 실제 Run 버튼을 누르도록 도구를 고쳤습니다.
- 초기 음성 행렬 도구는 comparison flag를 전달하지 않은 상태에서 고정 모델의 숨겨진 선택 계약을 단정했습니다. controller 결과가 생성되기 전의 전제 검사였으며 엔진 추론 실패로 분류하지 않습니다. 수정한 행렬 방법과 선택 유지 회귀를 위와 같이 별도로 확인했습니다.

이 진단에서는 앱 소스를 바꾸지 않았습니다. 외부 기기 경합이라는 초기 원인 해석은 이 결과에 사용하지 않습니다.

## 설치본과 정리

- 파일: `AXSL-Vision-0.1-build21.apk`
- 크기: **1,583,020,342 bytes**
- SHA-256: `fe0fed606618084d6cd153d7925ccdd26d87ab1a2fb263258780ca2869a9067e`
- 실제 설치: **0.1/code21**, 기존 package·서명으로 업데이트.
- 추가 시험 helper 제거 확인, 임시 화면 켜짐 설정0·자동 회전1·사용자 회전0 복원 확인.
- 실행 영상은 제작하지 않았습니다.

## 공개 배포 단계

실기기 수락과 공개 검증 문서 준비를 완료했습니다. 최종 main 반영·태그·APK 게시는 additive release **`v0.1-build21`**로 진행합니다. 이전 `v0.1`과 자산 lock은 덮어쓰지 않습니다. 실제 배포 파일과 체크섬은 [해당 Release](https://github.com/YOnLab-AI/axsl-vision/releases/tag/v0.1-build21)에서 확인합니다.

전체19기능·68개 ID의 정확도, 모든 기기·보드, 장시간 운영, 기계적 접힘·펼침, 가속기·발열·전력은 이번 수락 범위가 아닙니다. [전체 검증 문서](VALIDATION.md)와 [모델 시험 안내](MODEL_TESTING.md)에 범위를 구분했습니다.
