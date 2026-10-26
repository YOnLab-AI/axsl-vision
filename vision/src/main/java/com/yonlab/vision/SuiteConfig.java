package com.yonlab.vision;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import java.util.Arrays;

/** Stable allowlist for the nine independent suite applications. */
public final class SuiteConfig {
    private SuiteConfig() {}
    public static final String[] ROLES={"lab","access","detect","flow","signage","face","drive","vis","compare"};
    private static final String[] TITLES={"실험실","출입 인식","객체 탐지","사람 흐름","사이니지","얼굴 목록","드라이버","방문자","얼굴 비교"};
    private static final String[] ICONS={"chip","scan-face","search","users","image","user-plus","camera","users","scan-face"};
    private static final String[] PURPOSES={
        "입력과 분석을 연결하는 실험 공간", "얼굴을 인식하고 최근 확인을 살펴보세요", "사진과 카메라의 객체를 탐지하세요",
        "기준선을 지나는 사람의 흐름을 확인하세요", "콘텐츠를 모아 원하는 순서로 재생하세요", "얼굴을 등록하고 목록을 관리하세요",
        "눈과 고개의 변화를 확인하세요", "방문 관측과 오늘의 기록을 살펴보세요", "두 사진에서 선택한 얼굴을 비교하세요"
    };
    public static boolean isRole(String role) { return role!=null&&Arrays.asList(ROLES).contains(role); }
    private static int index(String role) { int n=Arrays.asList(ROLES).indexOf(role); return n<0?0:n; }
    public static String role(Context context) {
        try {
            ApplicationInfo info=context.getPackageManager().getApplicationInfo(context.getPackageName(),PackageManager.GET_META_DATA);
            Bundle meta=info.metaData; String role=meta==null?null:meta.getString("yon.role");
            return isRole(role)?role:"lab";
        } catch(PackageManager.NameNotFoundException e) { return "lab"; }
    }
    public static String title(String role) { return TITLES[index(role)]; }
    public static String displayName(String role) { return "AXSL Vision · "+title(role); }
    public static String icon(String role) { return ICONS[index(role)]; }
    public static String purpose(String role) { return PURPOSES[index(role)]; }
    public static String packageName(String role) { return "com.yonlab."+(isRole(role)?role:"lab"); }
    public static boolean debug(Context context) { return (context.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0; }
}
