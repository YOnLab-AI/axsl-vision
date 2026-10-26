package com.yonlab.vision;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** One native guide for the current executable selection; no measured superiority is implied. */
public final class OneGuidance {
    private static volatile JSONObject modelMetadata;
    private OneGuidance() {}

    /** Context signature also permits the independent voice module to call this through reflection. */
    public static void add(Context context, LinearLayout parent, String feature, String modelId, String group) {
        Activity owner = activity(context);
        if (owner == null || parent == null) throw new IllegalArgumentException("안내를 표시할 화면을 확인하세요");
        OneCatalog.Feature task = OneCatalog.find(OneModelPolicy.canonicalFeature(feature));
        if (task == null) throw new IllegalArgumentException("안내할 기능을 확인하세요");
        LinearLayout panel = Ui.panel(owner);
        panel.addView(Ui.heading(owner, "현재 기능·모델 안내", 18), Ui.fill());
        field(owner, panel, "시험 기능", task.title + "\n" + task.summary);
        OneModels.Entry selected = entry(context, task.id, modelId);
        String title = selected == null ? "선택 모델 확인 필요" : selected.title;
        field(owner, panel, "시험 모델·엔진", title + "\nID · " + safe(modelId)
                + (selected == null ? "" : "\n" + description(context, task.id, modelId, group)));
        String recordedKind = metadata(context, modelId).optString("kind");
        String kind = "code_engine".equals(recordedKind) ? "코드 엔진 · 학습 가중치 없음"
                : "trained_pipeline".equals(recordedKind) ? "학습 모델 또는 복합 파이프라인" : "실행 구성 정보 확인 필요";
        field(owner, panel, "실행 방식", kind + "\n" + runtime(context, modelId));
        field(owner, panel, "입력 조건", inputConditions(task.id));
        String caution = modelCaution(modelId);
        field(owner, panel, "결과 해석", interpretation(task.id) + (caution.isEmpty() ? "" : "\n" + caution));
        LinearLayout.LayoutParams layout = Ui.fill();
        layout.bottomMargin = Ui.dp(owner, 12);
        parent.addView(panel, layout);
    }

    private static void field(Activity owner, LinearLayout parent, String label, String value) {
        TextView name = Ui.heading(owner, label, 14);
        name.setTextColor(Ui.MUTED);
        parent.addView(name, Ui.fill());
        parent.addView(Ui.text(owner, value, 14), Ui.fill());
    }

    private static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) break;
            context = next;
        }
        return context instanceof Activity ? (Activity) context : null;
    }

    private static String safe(String text) { return text == null || text.isEmpty() ? "미선택" : text; }

    private static OneModels.Entry entry(Context context, String feature, String id) {
        for (OneModels.Entry value : OneModels.list(context, feature, "ALL")) if (value.id.equals(id)) return value;
        return null;
    }

    public static String description(Context context, String feature, String modelId, String group) {
        OneModels.Entry value = entry(context, OneModelPolicy.canonicalFeature(feature), modelId);
        if (value == null) return "이 기능에서 선택한 모델 정보를 확인하세요";
        if ("vosk-en-us-small-0.15".equals(modelId)) return "영어 음성용 · 오프라인 스트리밍 음성 인식";
        return value.summary;
    }

    public static String runtime(Context context, String modelId) {
        return metadata(context, modelId).optString("runtime", "실행 방식 정보 확인 필요");
    }

    private static JSONObject metadata(Context context, String modelId) {
        JSONObject known = modelMetadata;
        if (known == null) synchronized (OneGuidance.class) {
            known = modelMetadata;
            if (known == null) {
                try (InputStream input = context.getAssets().open("one/model-guide.json");
                     ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        if (bytes.size() + read > 128 * 1024) throw new IllegalArgumentException("모델 안내 크기 확인 필요");
                        bytes.write(buffer, 0, read);
                    }
                    JSONObject guide = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
                    if (!"axsl-vision-in-app-model-guide-v1".equals(guide.getString("schema")))
                        throw new IllegalArgumentException("모델 안내 형식 확인 필요");
                    known = guide.getJSONObject("models");
                } catch (Exception error) { known = new JSONObject(); }
                modelMetadata = known;
            }
        }
        JSONObject model = known.optJSONObject(modelId == null ? "" : modelId);
        return model == null ? new JSONObject() : model;
    }

    public static String inputConditions(String feature) {
        switch (OneModelPolicy.canonicalFeature(feature == null ? "" : feature)) {
            case "detect": return "사진으로 물체 위치를 시험하고, 영상·실시간 입력에서는 연속 검출을 확인하세요. 크기·거리·가림이 다른 자료를 바꿔보세요.";
            case "classify": return "사진 한 장을 입력합니다. 같은 장면의 밝기·흐림·구도와 다른 종류의 장면을 바꿔보세요.";
            case "segment": case "studio": return "사진 한 장을 입력합니다. 인물·배경 대비, 머리카락·경계와 복잡한 배경을 바꿔보세요.";
            case "faces": case "access": return "얼굴이 보이는 사진·영상·실시간 입력을 사용합니다. 정면·측면·가림·작은 얼굴 조건을 바꿔보세요.";
            case "compare": return "모델 테스트는 사진 한 장의 얼굴 검출·특징을 확인합니다. 두 얼굴의 유사도 대조는 얼굴 비교 화면에서 실행하세요.";
            case "drive": return "사진은 눈·입·고개의 한 시점만 관측합니다. 깜박임·눈 감음 지속 시간은 영상·실시간 입력에서 시험하세요.";
            case "flow": return "사진 모델 비교는 person 검출을 확인합니다. 기준선 통과와 이동 집계는 영상·실시간 입력에서 시험하세요.";
            case "vis": return "사진 모델 비교는 얼굴 관측을 확인합니다. 방문 세션·체류 시간은 영상·실시간 입력에서 시험하세요.";
            case "pose": case "coach": return "관절이 보이는 사진 한 장을 입력합니다. 정면·측면·관절 가림·화면 밖 신체 조건을 바꿔보세요.";
            case "ocr": case "document": return "문자가 보이는 사진 한 장을 입력합니다. 모델의 지원 문자와 글자 크기·회전·흐림·반사를 바꿔보세요.";
            case "qr": return "코드가 보이는 사진을 입력합니다. 코드 종류·크기·회전·부분 가림을 바꿔보세요. QR 생성은 별도 도구입니다.";
            case "speech": return "음성 파일 또는 마이크 입력을 사용합니다. 모델 언어와 발화 길이·속도·주변 잡음 조건을 바꿔보세요.";
            case "signage": return "사진·영상으로 재생목록을 구성합니다. 사진 간격·영상 종료·전체 화면 전환을 시험하세요.";
            case "search": return "기준 사진과 비교할 갤러리를 선택합니다. 같은 물체의 구도 변화와 비슷하지만 다른 장면을 바꿔보세요.";
            case "sound": return "소리 파일 또는 마이크 입력을 사용합니다. 소리 종류·음량·혼합 소음 조건을 바꿔보세요. 발화 문장 인식은 말을 글로 기능입니다.";
            default: return "선택한 기능에서 지원하는 자료 유형과 모델 조건을 확인하세요.";
        }
    }

    public static String interpretation(String feature) {
        switch (OneModelPolicy.canonicalFeature(feature == null ? "" : feature)) {
            case "detect": return "상자와 분류 점수는 모델의 검출 결과입니다. 점수 자체가 검출 정확도나 실제 개수를 보증하지 않습니다.";
            case "classify": return "사진 전체의 분류 후보와 점수입니다. 점수가 높아도 정답 검증을 대신하지 않습니다.";
            case "segment": return "모델마다 사람·배경 또는 장면 종류 등 나누는 대상이 다릅니다. 같은 색이나 점수를 같은 의미로 비교하지 마세요.";
            case "faces": return "얼굴 위치·표정·나이 등은 모델의 관측·추정입니다. 실제 감정·나이·신원에 대한 확정 값이 아닙니다.";
            case "compare": return "모델 테스트의 단일 사진 결과는 얼굴 검출·특징 추출입니다. 두 얼굴의 코사인 유사도 대조는 얼굴 비교 화면에서 별도로 실행하세요.";
            case "access": return "얼굴 특징 유사도와 판정 기준을 함께 확인하세요. 한 번의 일치 결과는 신원 확인이나 출입 보안 성능 검증을 대신하지 않습니다.";
            case "drive": return "단일 사진으로 깜박임이나 지속 시간을 판단하지 않습니다. 관측 지표를 운전자 안전 성능으로 확대하지 마세요.";
            case "flow": return "사진의 person 검출 수와 영상의 기준선 통과 수는 다른 결과입니다. 정지 사진 결과를 이동 집계로 해석하지 마세요.";
            case "vis": return "고유 얼굴 특징 관측·방문 세션·체류 시간을 구분하세요. 사진 한 장의 얼굴 수는 방문자 집계가 아닙니다.";
            case "pose": return "표시된 지점은 관절 위치의 추정입니다. 가려진 관절이나 화면 밖 신체에서 오차가 커질 수 있습니다.";
            case "ocr": return "읽은 원문과 사용자가 수정한 글을 구분하세요. 정답이 확인된 자료에서만 인식 품질 수치를 평가합니다.";
            case "qr": return "해독한 원문과 코드 형식을 확인하세요. 검증된 원문이 있는 자료에서만 정확 일치를 평가합니다.";
            case "speech": return "인식 원문과 수정 결과를 구분하세요. CER은 검증된 정답이 있는 동일 입력에만 표시하며, 한 파일의 값을 전체 정확도로 해석하지 않습니다.";
            case "signage": return "Android Media는 학습 모델이 아닌 재생 엔진입니다. 안내판 재생과 선택적 관객 분석은 별도 동작입니다.";
            case "document": return "날짜·금액 정리는 OCR 원문을 규칙으로 해석한 결과입니다. 원문 인식 오류와 추출된 값을 함께 확인하세요.";
            case "studio": return "인물 마스크 경계가 편집 품질에 영향을 줍니다. 머리카락·투명한 물체·비슷한 색의 배경을 확대해 확인하세요.";
            case "search": return "유사도는 같은 특징 공간 안의 가까운 정도입니다. 다른 모델의 점수를 같은 확률이나 정확도로 비교하지 마세요.";
            case "sound": return "소리 종류별 점수와 시간 구간을 확인하세요. 모델마다 분류 체계가 다르며 점수는 실제 발생 확률이나 정확도 보증이 아닙니다.";
            case "coach": return "사진의 관절 각도·좌우 상태를 관측합니다. 반복 횟수·운동 효과·건강 상태를 평가한 결과가 아닙니다.";
            default: return "실제 출력·실패 상태와 정답 유무를 함께 확인하세요. 한 입력의 결과를 전체 모델 성능으로 확대하지 않습니다.";
        }
    }

    private static String modelCaution(String id) {
        if ("paddle-ocr-v6-tiny".equals(id) || "doc-paddle-v6-tiny".equals(id))
            return "이 구성의 인식기는 영문·중문용입니다. 한국어 자료의 결과는 언어 조건 차이를 고려하세요.";
        if ("paddle-ocr-v6-ko".equals(id) || "doc-paddle-v6-ko".equals(id))
            return "v6 검출기와 한국어 v5 인식기를 조합합니다. 이름의 v6가 모든 구성 요소의 버전을 뜻하지 않습니다.";
        if ("vosk-en-us-small-0.15".equals(id)) return "영어용 모델입니다. 다른 언어 입력에서는 언어 조건 차이를 함께 기록하세요.";
        if ("vosk-ko-small-0.22".equals(id)) return "한국어용 모델입니다. 다른 언어 입력에서는 언어 조건 차이를 함께 기록하세요.";
        return "";
    }
}
