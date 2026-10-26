# 버전별 검증과 근거

검증 결과는 해당 APK·소스·입력·모델과 연결합니다. 이 문서는 공개 버전 **0.1/code20**과 이전 기준 **4.4.0/code19**를 구분합니다. 과거 결과를 새 버전 완료로 옮기지 않습니다.

## 공개 0.1/code20 결과

다음 결과는 공개 소스에서 만든 **0.1/code20 release APK**를 기준으로 합니다. [validation-0.1.json](validation-0.1.json)에 개인 경로·기기 serial·인식/정답 원문·run ID를 제외한 수치·입력/모델 hash·판정 범위를 공개했습니다.

- APK SHA-256: `cde2536127ea067c0c4b2ff143e3113f7f051565b2338043747f144d7a7721f7`
- APK 크기: **1,583,002,073바이트**. 설치된 실제 release와 SHA·서명이 일치하며 debuggable=false였습니다.

| 검증 단계 | 0.1 결과 | 범위 |
|---|---|---|
| Android 단위시험 | 244 PASS / 실패·오류·skip 0 | vision225, offlinevoice19; 39 suites |
| asset bootstrap 시험 | 16 PASS / 실패·skip 0 | 허용 archive·경로·SHA·staging 실패 보존 등의 호스트 시험 |
| 호스트 합계 | **260 PASS** | Android244 + Python16; 기기 시험과 별도 |
| release 조립 | PASS | 공개 모듈 :app의 실제 release APK |
| lint | 오류0·경고2 | 두 건 모두 MonochromeLauncherIcon; themed launcher mode 실기는 미시험 |
| 자산 복원 | 로컬 pack 설치661개·verify-only hash661개 PASS | runtime1 + models49 + demos611; 실제 HTTPS 전송 시험과 구분 |
| 실제 release engine/model 실행 | **5/5 PASS** | 아래 선택 ID 각각 입력 한 개 |
| 휴대폰 UI 관측 | **5/5 PASS** | 브랜드·버전·안내판 전체 화면·음성 화면 확인 |

기기 기록은 **SM-F956N/API37, 글자 배율1.5**입니다. 아래 실행의 CPU 요청은 1 thread, 보드 최적화는 OFF였습니다. SDK 내부 scheduling은 SDK 소유입니다. 이 문서의 호스트 결과는 로컬 실행이며 원격 GitHub Actions 성공을 주장하지 않습니다.

다섯 engine 실행은 실제 **non-debuggable release controller를 비공개 instrumentation으로 호출**한 검사입니다. helper 코드는 공개하지 않으므로 공개 요약만으로 동일 harness를 독립적으로 다시 실행할 수는 없습니다. UI 다섯 건은 일반 UI 조작을 관찰한 검사이며 engine controller 검사와 구분합니다.

### 0.1 실제 engine/model 실행 5건

| 기능 / model ID | 공개 입력 | 관측·평가 | load / inference / elapsed ms |
|---|---|---|---|
| `classify` / `mobilenet-v4-fp32` | `ko-scene-08` | Top1 grocery store, grocery, food market, market, score 0.33438 | 169 / 41 / 216 |
| `detect` / `efficientdet-lite0` | `ko-scene-04` | 검출 7개 | 46 / 34 / 91 |
| `ocr` / `mlkit-ocr-ko` | `ko-ocr-1-normal` | 82자·7영역 | 20 / 156 / 176 |
| `qr` / `zxing-qr` | `qr-demo-1` | QR_CODE, payload·format 정확 일치 | 0 / 85 / 86 |
| `speech` / `vosk-ko-small-0.22` | `ko-speech-fleurs-045` | 9.9초 단일 클립; CER 0.15789 | 1765 / 2118 / 4045 |

분류·탐지·OCR은 이 입력의 실제 출력 관측이며 정확도 평가가 없습니다. QR은 해당 encoded raster의 literal payload와 symbology가 일치했습니다. QR fixture의 이전 제품명이 포함된 원문은 보존된 reference data이며 앱 브랜딩 문구로 바꾸지 않았습니다. 입력을 바꾸어 기존 정답과 hash를 유지하지 않습니다.

**이미지 시간 범위:** 모델 준비와 전처리·추론·후처리 관측이며 source decode는 제외합니다. SDK lazy initialization이 inference에 포함될 수 있습니다. 단일 ms를 FPS·반복 benchmark·일반 정확도로 환산하지 않습니다.

### Vosk 단일 클립 CER과 시간 범위

`vosk-ko-small-0.22`가 공개 `ko-speech-fleurs-045` 한 클립을 완료했습니다. 음성 길이는 **9.9초**, mono16k PCM sampleCount는 **158,400**입니다. 검증된 원본 reference와 실제 source encoded SHA를 연결한 이 입력의 **CER은 0.15789473684210525(약 15.789%)**입니다. corpus 정확도·언어 전체 성능·다른 모델의 우열로 확대하지 않습니다.

정책 ID는 `NFC_REMOVE_UNICODE_WHITESPACE_PRESERVE_CASE_PUNCTUATION_CODEPOINTS_V1`입니다. [AsrTextMetrics](../offlinevoice/src/main/java/com/yonlab/offlinevoice/AsrTextMetrics.java)는 NFC 정규화 후 `Character.isWhitespace` 또는 `Character.isSpaceChar`인 codepoint를 제거하고 대소문자·문장부호를 유지합니다. Unicode codepoint의 Levenshtein 편집거리를 정규화된 reference 길이 **38**로 나눕니다. 인식 원문과 정답 원문은 공개 요약에 포함하지 않았습니다.

시간 계약은 `PREDECODED_SAME_PCM_ENGINE`입니다. 입력 준비/복사/decode/PCM hash는 **inputPrepareMs41**로 별도 기록됐습니다. 모델 준비 **1,765ms**, 인식 **2,118ms**, engine 구간 elapsed **4,045ms**는 이미 준비된 동일 PCM을 사용한 실행입니다. elapsed에는 engine 준비·인식·종료가 포함되며 입력 준비41ms를 포함한 end-to-end 시간으로 표시하지 않습니다. [VoiceCompareActivity](../offlinevoice/src/main/java/com/yonlab/offlinevoice/VoiceCompareActivity.java)의 기록 순서가 기준입니다.

공개 JSON은 source encoded SHA와 PCM SHA를 서로 다른 필드로 유지하고 Vosk model manifest/archive SHA를 보존합니다. 빈 개별 가중치 fingerprint를 임의 값으로 채우지 않았습니다.

### 0.1 휴대폰 UI 관측 5건

| 관측 | 판정 |
|---|---|
| AXSL Vision sole-brand home | PASS |
| Settings show version0.1 and no corporate logo | PASS |
| Guide player uses AXSL Vision branding | PASS |
| Full-screen entry and return | PASS |
| Voice module uses AXSL Vision branding | PASS |

이 다섯 UI 관측과 다섯 engine 실행은 호스트260개에 합산하지 않습니다. 전체 기능·모델 수락, 장기 안내판 운영, 다른 기기·보드, 기계적 접힘/펼침, 전력·발열·NPU 가속은 이번 결과 범위에 포함하지 않습니다.

재현 명령:

```sh
python tools/fetch_assets.py --all
python -m unittest discover -s tools/tests -v
./gradlew :vision:testDebugUnitTest :offlinevoice:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

위 명령은 공개 debug 빌드 재현용입니다. 검증된 배포 APK는 외부 release 서명을 사용한 별도 `:app:assembleRelease :app:lintRelease` 결과이며 인증서를 제공하지 않은 로컬 debug APK와 byte 동일성을 주장하지 않습니다. PowerShell에서는 `./gradlew` 대신 `.\gradlew.bat`를 사용합니다.

## 역사적 4.4.0/code19 근거

2026-10-26에 저장된 release APK 기록입니다. 기기는 **SM-F956N / API37**, 글자 배율 **1.5**입니다. 공개 요약은 [historical-validation.json](historical-validation.json)에 있으며 개인 경로·기기 serial·private 검증 helper를 포함하지 않습니다.

- APK SHA-256: `6826fbcbc3135c9fb652a5c89907bce093d0efdd5f624f3306141e830eb4088c`
- 물리 QA: **22/22 PASS**. 일반 UI 조작·시스템 앱 정보 확인·고정 실제 release controller 실행을 구분했습니다.
- 호스트: **39 suites / 244 tests**, failures/errors/skipped 모두 **0**. vision225개, offlinevoice19개입니다.
- 과거 lint: 오류0·경고3. SDK36 유지 경고와 API26 adaptive fallback의 monochrome 경고2건이며 API33 리소스에는 monochrome이 있었습니다. 실제 themed launcher mode는 미시험입니다.
- 기존 모델·자료·license 자산819개 SHA 일치·누락/변경0이었습니다. 이 수는 과거 자산 보존 검사 범위입니다.

### 대표 실제 release 실행 4건

각 모델에 **입력 한 개**를 실행한 관측입니다. 요청 CPU 설정은 1 thread, board optimization은 OFF였습니다. SDK 내부 scheduling은 SDK가 소유한다고 기록했습니다.

| 기능 / model ID | 입력 | 실제 관측 | load / inference / elapsed ms |
|---|---|---|---|
| `classify` / `mobilenet-v4-fp32` | `ko-scene-08` | Top1 grocery store, grocery, food market, market, score 0.33438 | 85 / 37 / 127 |
| `detect` / `efficientdet-lite0` | `ko-scene-04` | 검출 7개 | 30 / 33 / 68 |
| `ocr` / `mlkit-ocr-ko` | `ko-ocr-1-normal` | 82자·7영역 | 8 / 169 / 177 |
| `qr` / `zxing-qr` | `qr-demo-1` | QR_CODE, payload·format 정확 일치 | 0 / 78 / 79 |

분류·탐지·OCR에는 이 기록에서 별도 정확도 평가가 없습니다. QR은 해당 encoded raster의 원문 payload와 symbology 일치만 확인했습니다. QR 한 건의 성공을 전체 자료 성공률로 계산하지 않습니다. 각 입력 SHA와 파일형 모델 SHA는 요약 JSON에 있습니다. ML Kit와 코드 엔진의 빈 modelSha256을 임의 가중치 hash로 채우지 않았습니다.

**시간 범위:** 이미지 결과의 원문 timing scope는 모델 구성과 전처리·추론·후처리를 포함하며 source decode를 제외합니다. SDK lazy initialization이 inference에 포함될 수 있습니다. 표의 단일 ms를 반복 benchmark·FPS·정확도 우열로 바꾸지 않습니다.

### 과거 물리 QA 22건

| 시험 | 당시 판정 |
|---|---|
| 큰 글꼴에서 안내판 목적·시작 버튼 표시 | PASS |
| 업데이트 후 기존 재생목록 7개 보존 | PASS |
| 일시정지·화면 켜짐 플래그 해제 | PASS |
| 목록 선택 재개·실제 영상 프레임 진행·화면 켜짐 | PASS |
| 전체 화면에서 조작부 숨김·복귀 버튼 제공 | PASS |
| 가로 전환 후 전체 화면 유지 | PASS |
| 뒤로가기로 전체 화면 종료·조작부 복귀 | PASS |
| 예시 목록 교체 전 확인·취소 | PASS |
| 명시적 확인 후 안내판 예시 3종 불러오기 | PASS |
| 전체 화면 종료 버튼 동작 | PASS |
| 다음 안내판으로 수동 전환 | PASS |
| 사진 전환 간격 조절·자동 순환 | PASS |
| 사용 안내 열기·닫기 | PASS |
| 종료·재진입 후 예시 목록과 설정 유지 | PASS |
| 자료 추가 라이브러리 열기·취소 | PASS |
| 홈 이동 시 재생 중지·화면 켜짐 해제 | PASS |
| 재진입·백그라운드 후 관객 분석 OFF 유지 | PASS |
| 이미지 분류 실제 release 실행 | PASS |
| 객체 탐지 실제 release 실행 | PASS |
| 한글 OCR 실제 release 실행 | PASS |
| QR 정답 일치 실제 release 실행 | PASS |
| 설치된 앱 이름·Vision 아이콘 시각 확인 | PASS |

### 과거 시험 도구의 진단과 재검증

초기 진단 결과와 최종 결과는 구분했습니다. 화면 캡처 stdout에 안내 문자열이 섞인 경우 기기 파일을 직접 읽어 PNG를 검증했고, 큰 글꼴에서 화면 밖 요소는 scroll 또는 실제 상태로 판정했습니다. Android17 창 flag는 대상 player 창의 0x80 bit를 확인했습니다. RGBA 차영상의 alpha 차이0 문제는 RGB 픽셀 비교로 고쳤으며 별도 정지 캡처에서 미디어의 실제 변화를 확인했습니다. HOME 직후 전환 상태는 완료 상태와 player 원장을 기다린 뒤 판정했습니다.

초기 도구의 실패를 제품 PASS로 숨기지 않았으며 원인을 확인한 항목을 실제 상태로 다시 시험했습니다. 이 문서에는 개인 경로·장치 serial·private controller 실행 자료를 공개하지 않습니다. 공개 요약의 숫자 범위와 재현 조건을 유지합니다.

## 이 근거가 수락하지 않은 범위

- 전체 19기능·68개 ID·모든 가중치의 정확도/안정성 검증.
- 반복 benchmark, p50/p95, 장시간 FPS, 전력·발열·NPU/DSP 가속.
- 다른 휴대폰·구형 Android·AI Kit 보드의 수락.
- 장기 무인 안내판 운영·전원/저장공간/codec 고장 시나리오.
- 기계적 접힘·펼침, 실제 themed monochrome launcher 모드.
- 과거 4.4.0의 결과를 현재 0.1의 추가 기능·모델 수락으로 확대하는 해석.

## 새 검증을 추가할 때

앱 version/code와 APK SHA, source revision, 입력 encoded SHA, 모델 파일 SHA/조합 fingerprint, runtime, CPU 요청 profile, device model/API, 결과 state·오류·종료 상태와 timing scope를 기록합니다. 품질 평가에는 검증된 정답의 provenance와 input hash join을 함께 남깁니다. 원본 출력·사용자 편집·정답은 서로 다른 필드로 유지합니다.

공유 자료에는 필요한 항목만 남기고 개인 파일 경로·기기 serial·연락처·얼굴 등록 목록·private URL·인증 정보를 포함하지 않습니다. 실제 로그를 공개할 수 없다면 공개 요약이라고 명시하고 독립적으로 확인할 수 없는 범위를 적습니다.
