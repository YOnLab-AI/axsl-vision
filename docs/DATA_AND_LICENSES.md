# 자료·모델·코드의 이용 조건

AXSL Vision의 독자 코드는 [Apache-2.0](../LICENSE)으로 공개합니다. 이 허락은 포함 모델·SDK·사진·음성·영상·작성 예제 전체를 다시 허가한다는 뜻이 아닙니다. 원본 고지와 자료별 출처·변형·이용 조건을 함께 보존합니다.

## 권리 범위를 분리합니다

| 범위 | 적용 기준 | 확인 위치 |
|---|---|---|
| 앱 독자 코드 | Apache-2.0 | 저장소 LICENSE와 각 파일의 기존 고지 |
| 오픈소스 runtime·native 구성요소 | 각 구성요소의 license/NOTICE | `vision/src/main/assets/one/licenses/`, `offlinevoice/src/main/assets/offlinevoice/notices/` |
| 모델 가중치·변환 파일 | 모델별 원래 조건·출처·변환 이력 | [MODEL_CATALOG](MODEL_CATALOG.md), 포함 catalog와 notices |
| ML Kit SDK·관리 모델 | Google ML Kit/API 약관 | [Google 약관 고지](../vision/src/main/assets/one/licenses/Google-MLKit-Terms-Reference.txt) |
| 외부 데모 사진·음성·영상 | 각 자료의 CC BY/CC BY-SA/CC0/public-domain 등 개별 조건 | [data-attribution.json](../vision/src/main/assets/one/data-attribution.json), [CREDITS](../vision/src/main/assets/one/CREDITS.md) |
| 직접 작성한 데모 fixture 33개 | 이 무료 앱 배포에 함께 포함하는 범위 | [authored-fixtures.json](authored-fixtures.json), 자료별 attribution 원문 |
| UI 도형·아이콘 | 해당 새 작성 자산 고지 및 고정 공식 Lucide ISC | 자산 provenance와 포함 license 원문 |

제3자 파일을 변경하지 않았다는 사실과 모든 용도에 재배포할 수 있다는 사실은 다릅니다. 모델 출처·해시 검증은 파일 동일성을 확인하며 이용 조건을 확대하지 않습니다. 원문 고지는 새로운 제품 이름에 맞춘다는 이유로 삭제하거나 다시 작성하지 않습니다.

### 원본 README의 링크 문맥

일부 원본 README는 원래 upstream의 폴더 구조를 기준으로 한 상대링크를 그대로 보존합니다. 이 통합 저장소의 새 탐색 링크와 구분합니다. `FER-UPSTREAM.md`, `Intel-AgeGender-UPSTREAM.md`, `MobileNetV2-README.md`의 상대링크 대상은 아래 공식 원본 문맥에서 확인할 수 있습니다. 원문 고지를 수정하여 별도 허락이나 구현 자료가 있는 것처럼 만들지 않습니다.

- [OpenCV Zoo Facial Expression 원본](https://github.com/opencv/opencv_zoo/tree/main/models/facial_expression_recognition)
- [Open Model Zoo Age/Gender 원본](https://github.com/openvinotoolkit/open_model_zoo/tree/master/models/intel/age-gender-recognition-retail-0013)
- [OpenCV Zoo MobileNet 원본](https://github.com/opencv/opencv_zoo/tree/main/models/image_classification_mobilenet)

## 공개 기본 자료 312개

| 분류 | 수량 |
|---|---:|
| 이미지 | 235 |
| 음성 | 66 |
| 영상 | 11 |
| KO | 105 |
| GO | 131 |
| SHARED | 76 |
| `referenceVerified=true` | 138 |

위 수량은 이전 313개 목록에서 `go-coco-86` 한 항목을 제외한 공개 기본 목록입니다. 그 항목의 이전 조건은 명확한 공개 이용 허락을 확인하기에 부족했으므로 기본 배포 자료에서 제외했습니다. 그 자료를 다른 경로에서 얻어 임의로 되살리지 않습니다. KO/GO는 언어·자료 그룹이며 사람의 국적·외모와 무관합니다.

`referenceVerified=true`는 해당 자료의 정답 출처 상태입니다. 이 숫자는 138개 입력에서 현재 앱의 모델 결과가 정확했다는 뜻이 아닙니다. 한 파일의 정답은 원래 encoded SHA와 연결하고 흐림·노이즈·가림 같은 파생 조건과 모델 출력을 정답으로 대체하지 않습니다.

### 작성 예제 33개의 별도 허락

원래 attribution에는 **“Project-authored demo fixture — permitted with this free app distribution”**가 기록돼 있습니다. 이 33개는 본 무료 앱 배포에 함께 포함하는 범위로 제공됩니다. 앱 코드의 Apache-2.0을 이 예제에 적용하거나 범용 공개 데이터셋·유료 앱 자산으로 다시 허가하지 않습니다. 그 밖의 사용 범위는 별도 권리 확인이 필요합니다.

[authored-fixtures.json](authored-fixtures.json)은 33개 ID·logical asset path·encoded SHA·원래 조건을 명시합니다. 해당 파일을 바꾼 뒤 원래 SHA·정답·허락 상태를 그대로 표시하지 않습니다. 안내판 예시·새 도형 자산과 이 33개 기존 fixture를 혼동하지 않습니다.

### 외부 자료의 원문·파생 조건

CC BY 자료는 출처·작성자·license·변형 고지를, CC BY-SA 자료는 해당 파생 미디어에 적용되는 조건을 보존합니다. NASA/public-domain 표시는 자료별 근거와 혼합 구성요소의 권리 상태를 함께 읽습니다. ESC-50 계열 자료는 원본 표기와 ESC-10 subset 조건을 자료별로 확인합니다. 한 데이터셋의 이름만으로 모든 파일에 같은 허락을 붙이지 않습니다.

과거 사진의 간판·가격·메뉴·행사·날짜는 기록 당시의 입력 내용입니다. 현재 현실의 안내로 사용하지 않습니다. 안내판 기본 문구는 가상의 예시이며 현장 정보로 교체하고 확인한 뒤 사용합니다.

## 모델·runtime 고지

| 모델/구성요소 | 확인한 원래 범위 |
|---|---|
| PANNs CNN6/CNN10 | 가중치 CC-BY-4.0, 원본 코드 MIT, checkpoint/ONNX/frontend SHA 별도 |
| Whisper Tiny/Base/Small | 원본 및 변환 가중치의 MIT 근거, encoder/decoder/tokens manifest, sherpa/native 고지 별도 |
| Vosk KO/EN | 각각 공식 모델 목록과 파일 manifest의 Apache-2.0 근거 |
| YuNet | MIT; SFace·얼굴 속성 head의 조건과 분리 |
| SFace·추가 MediaPipe/PoseNet·Paddle·기타 비전 가중치 | 각 catalog의 Apache-2.0 근거와 변환·모델 카드·파일 SHA 유지 |
| TinyCLIP | MIT 및 vision encoder 변환 근거 |
| ML Kit | SDK/API 약관. 예제 코드의 Apache 표시를 추출 모델의 license로 옮기지 않음 |
| OpenCV/LiteRT/ONNX Runtime/음성 native | 실제 배포 구성요소별 원문·NOTICE. 하나의 runtime license로 모든 bundled component를 덮지 않음 |

모델별 상세와 공식 원문 링크는 [MODEL_CATALOG](MODEL_CATALOG.md)에 있습니다. 특히 `modelplus/facepose/catalog.json`의 `inferenceExecuted=false`, PANNs catalog의 `runtimeInferenceTestPerformed=false`와 `benchmarkPerformed=false`는 검증 완료로 바꿔 읽지 않습니다.

주요 원장:

- [모델 파일별 고지](../vision/src/main/assets/one/licenses/01-MODEL-FILE-NOTICES.txt)
- [추가 비전 catalog](../vision/src/main/assets/one/modelplus/vision/catalog.json)
- [얼굴·자세 catalog](../vision/src/main/assets/one/modelplus/facepose/catalog.json)
- [Paddle 원본 모델](../vision/src/main/assets/one/modelplus/ocr/source-models.json)
- [PANNs 모델·frontend](../vision/src/main/assets/one/modelplus/audio/audio-models.json)
- [Whisper Base/Small manifest](../offlinevoice/src/main/assets/offlinevoice/whisper-modelplus.json)
- [음성 runtime 공식 고지 출처](../offlinevoice/src/main/assets/offlinevoice/notices/official-license-provenance.json)

공식 확인 링크: [ML Kit Terms](https://developers.google.com/ml-kit/terms), [Vosk 모델별 조건](https://alphacephei.com/vosk/models), [Whisper MIT](https://github.com/openai/whisper/blob/main/LICENSE), [PANNs 원본 가중치](https://zenodo.org/records/3987831).

## 큰 파일과 Release pack

Git 본문에는 모델·미디어·큰 native runtime을 모두 넣지 않습니다. 고정 lock과 secure fetcher가 허용된 파일·크기·SHA를 확인하여 모듈의 원래 경로로 복원합니다. Release pack을 받는 일은 그 안의 모든 자료를 Apache-2.0으로 받는 일이 아닙니다. 원래 catalog·license·attribution을 같은 버전으로 함께 배포합니다.

고정 파일·archive inventory는 [model-artifacts.json](model-artifacts.json)에 있습니다. 이는 역사적 기준의 ONNX25·TFLite19·모델 ZIP3 목록이며 SDK 내부 모델 수·독립 학습 가중치 수·추론 검증 수를 나타내지 않습니다.

기존 웹사이트 사진·아이콘의 좁은 이용 허락을 일반 오픈소스 허락으로 바꾸지 않습니다. 공개 UI는 새 작성 도형 이미지와 고정된 공식 Lucide ISC 자산으로 구성하며, 원본 회사 로고·웹사이트 전용 사진을 제품의 공개 브랜딩으로 사용하지 않습니다.

## 결과 내보내기와 개인 입력

앱은 demo ID와 실제 입력 encoded SHA로 attribution을 연결합니다. HTML/CSV/JSON 및 결과 PNG에 입력 자료의 출처와 해당 원문 조건을 유지합니다. [OneInputAttribution](../vision/src/main/java/com/yonlab/vision/OneInputAttribution.java)과 [OnePngAttribution](../vision/src/main/java/com/yonlab/vision/OnePngAttribution.java)가 이 연결을 구현합니다.

독립 숫자 관측과 포함된 원본·파생 미디어의 license scope는 구분합니다. 개인 파일은 사용자가 필요한 권리를 가진 경우에 선택합니다. 원본 파일을 가져왔다는 사실만으로 저장소나 앱에 재배포 권한이 생기지 않습니다. 얼굴 등록·음성 원문·리포트 공유의 범위를 확인한 뒤 명시적으로 전달합니다.
