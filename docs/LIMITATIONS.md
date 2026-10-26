# 구현·검증·배포의 범위

AXSL Vision 0.1은 온디바이스 처리와 모델 비교 구조를 학습·재현하기 위한 레퍼런스 앱입니다. 소스에 기능이 있고 모델 파일이 존재하는 상태, 호스트 시험이 통과한 상태, 실제 기기에서 입력을 실행한 상태를 구분합니다. 버전별 완료 근거는 [VALIDATION](VALIDATION.md)이 기준입니다.

## 수량과 모델 의미

19개 기능의 87개 슬롯에는 68개 고유 실행 ID가 등장합니다. 기능별 별칭·공유 파일·복합 pipeline·SDK 모델·코드 엔진을 포함합니다. 87을 독립 학습 모델 수로, 47개 ONNX/TFLite/ZIP 파일을 47개 독립 모델로 바꾸지 않습니다.

Android Media는 재생 엔진, ZXing과 OpenCV QR 경로는 코드 인식 엔진입니다. OpenCV 기본/ArUco는 서로 다른 검출 경로와 공통 해독을 사용합니다. thread 수·CPU profile·입력 크기 preset은 추가 모델로 세지 않습니다. 같은 가중치로 문서·배경 효과·관절 기하·검색을 만드는 별칭도 새 학습 가중치가 아닙니다.

## 입력과 전후 처리

검출·분류·mask·embedding·landmark는 각 모델의 전처리·출력 label·정규화·후처리 계약을 따릅니다. crop, resize, padding, 채널 순서, alphabet 또는 decoder를 바꾸면 같은 모델 SHA라도 결과가 달라집니다. [MODEL_CATALOG](MODEL_CATALOG.md)의 ID별 계약과 원본 코드를 함께 확인합니다.

정지 사진은 한 장면의 관측입니다. flow의 line crossing, drive의 눈 감김 지속시간, vis의 체류/세션은 영상·실시간 controller의 시간 상태입니다. 사진 모델 비교에 시간 집계 성능을 붙이지 않습니다.

KO/GO/SHARED는 자료·언어 맥락입니다. 외모·국적 추정이 아니며 Whisper가 반환하는 detected language와 다른 정보입니다. Paddle v6-ko는 v6 detector와 v5 Korean recognizer를 조합합니다. v6-tiny를 한국어 전용 recognizer로 소개하지 않습니다.

## 점수와 품질 평가

분류 score, 객체 score, cosine similarity, 눈 열림 확률, EAR, pose likelihood는 서로 다른 관측입니다. 하나의 공통 정확도 순위로 합치지 않습니다. YAMNet521종과 PANNs527종의 class 공간도 다릅니다.

정답이 검증되고 실제 입력 hash와 연결된 경우에만 해당 입력의 CER/WER, fieldExact, Top1/Top3, MRR, PCK 등을 평가합니다. 지표 계산 코드가 있다는 사실은 현재 버전에서 평가가 완료됐다는 뜻이 아닙니다. reference가 없거나 `referenceVerified=false`인 자료에는 새 정답·점수를 만들지 않습니다. 모델 출력을 정답으로 재사용하지 않습니다.

과거 대표 실행 네 건 중 분류·탐지·OCR은 실제 출력 관측이며 정확도 평가가 없습니다. QR 한 건만 해당 raster의 payload와 symbol format 일치가 확인됐습니다. 이를 전체 QR 자료의 성공률로 확대하지 않습니다.

## 시간과 자원

단일 실행의 load/inference/elapsed는 반복 benchmark·p50/p95·실시간 FPS와 다릅니다. 이미지 결과의 timing scope는 원본 decode를 제외하고 전처리·추론·후처리를 포함할 수 있으며 SDK lazy initialization이 inference에 남을 수 있습니다. 음성/생활 소리는 decode·resample·frontend·window 처리 범위를 별도로 기록합니다.

요청 CPU thread 수는 앱이 제어하는 runtime의 설정입니다. ML Kit SDK 내부 스케줄링과 동일하다고 보장하지 않습니다. NPU/DSP/GPU/NNAPI 가속, 전력·발열·지속 처리 성능은 해당 기기에서 별도 측정해야 합니다. 보드 최적화 설정 또는 CPU 실행 성공을 보드 가속 검증으로 바꾸지 않습니다.

## 사람·공간 기능

얼굴·표정·나이 head는 이미지 기반 관측입니다. 얼굴 비교/등록 threshold는 실제 출입통제의 보안 수락을 대체하지 않습니다. 방문자의 distinct는 counter 범위의 익명 ID이며 실제 세계의 영구 고유 인원을 뜻하지 않습니다. 재등장·가림·TTL·track 단절·저장 한도가 집계에 영향을 줍니다.

드라이버 관측은 눈·입·고개 기하와 시간 상태를 설명합니다. 실제 운전자 안전 성능, 위험 검출 신뢰성, 규제 수락을 검증한 자료가 아닙니다. 운동 자세 코치는 단일 사진의 관절 기하이며 건강 상태·운동 효과·반복 횟수를 평가하지 않습니다.

## 안내판·미디어

전체 화면, pause/resume, 화면 켜짐 flag, 사진 간격, 목록 교체는 실제 구현과 시험 범위를 따릅니다. 예시 문구·시간·프로그램은 가상 자료입니다. 관객 관측은 사용자가 켠 경우에만 사용하며 자동 맞춤 광고 성능이나 연령 추정 정확도를 주장하지 않습니다.

장기간 무인 운영, 모든 codec·파일 손상·대용량 목록·저장 공간 고갈·전원 복구·열 제한·기계적 접힘/펼침은 현재 저장된 과거 QA로 수락한 범위가 아닙니다. 현장에서는 해당 운영 조건을 따로 시험합니다.

## 버전·기기·서명

현재 **0.1/code21**은 [최신 공개 검증 요약](validation-0.1-build21.json)에서 호스트279개(Android259+Python20), 일반 UI6건, 총 controller 실행24건(8개 ID·16개 고유 encoded 입력)을 기록했습니다. 이 중 행렬은17개 모델·입력 조합(6개 ID·13개 고유 입력)이며 선택 유지·보고서 회귀는 구분합니다. 나머지60개 ID는 이번 기기 실행 기록에서 미시험입니다.

이전 공개 **0.1/code20**은 [해당 버전 검증 요약](validation-0.1.json)에 기록한 실제 release APK에서 호스트260개(Android244+bootstrap16), 선택 engine 실행5건, UI 관측5건을 통과했습니다. 기기 범위는 SM-F956N/API37·글자 배율1.5이며 다섯 실행은 각각 공개 입력 한 개입니다. 이 결과는 전체19기능·68개ID의 기기 수락을 뜻하지 않습니다. 이전 4.4.0/code19의 물리22건·호스트244개·대표4실행은 역사적 근거로 따로 유지합니다. Android minSdk24 선언은 모든 API24 이상 기기의 모델별 수락을 뜻하지 않습니다.

다른 휴대폰, AI Kit 보드, 구형 Android, 다른 ABI·메모리·열 조건의 실측 결과를 별도로 확인합니다. 공개 소스의 debug 인증서·외부 release 인증서는 과거 내부 배포본 인증서와 같다고 가정하지 않습니다. 기기 데이터 삭제나 기존 설치본 제거는 이 문서의 기본 절차에 포함하지 않습니다.

## 권리와 재배포

앱 독자 코드 Apache-2.0과 모델·SDK·자료의 조건을 분리합니다. 작성 fixture33개는 무료 앱 동봉 범위의 별도 허락입니다. Release pack의 hash 검증은 파일 동일성 검증이며 포괄적 재허가가 아닙니다. [DATA_AND_LICENSES](DATA_AND_LICENSES.md)의 자료별 원문을 유지합니다.

큰 모델·미디어·runtime은 Git와 별도 Release pack에서 복원합니다. 복원되지 않은 소스 상태의 호스트 시험, 복원 후 APK 빌드, 실제 설치·기기 수락은 각각 기록합니다. 공개 배포·호스트 검증·기기 수락의 완료 상태를 섞지 않습니다.
