package com.yonlab.vision;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Purpose-specific start screen; all incoming routes use an explicit role allowlist. */
public class LauncherActivity extends YOnActivity {
    private static final int CAPTURE=501, PICK_IMAGE=502;
    private String role;
    private String pendingMode;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); role=SuiteConfig.role(this); pendingMode=state==null?role:state.getString("pendingMode",role);
        render(); if(state==null) routeIncoming(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); routeIncoming(intent); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("pendingMode",pendingMode); super.onSaveInstanceState(state); }

    private void render() {
        LinearLayout frame=new LinearLayout(this); frame.setOrientation(LinearLayout.VERTICAL); frame.setBackgroundColor(Ui.BACK);
        LinearLayout header=Ui.brandHeader(this,SuiteConfig.title(role));
        Button settings=Ui.secondary(this,"설정",v->open("SettingsActivity",role));
        header.addView(settings,new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,48))); frame.addView(header,Ui.fill());
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        LinearLayout body=Ui.column(this); scroll.addView(body,Ui.fill()); frame.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if("lab".equals(role)) laboratory(body); else purpose(body);
        setContentView(frame);
    }

    private void laboratory(LinearLayout body) {
        FrameLayout hero=new FrameLayout(this); hero.setBackground(Ui.round(this,0xffeaf2ff,20)); hero.setClipToOutline(true);
        ImageView lake=Ui.image(this,"hero"); hero.addView(lake,new FrameLayout.LayoutParams(-1,-1));
        View wash=new View(this); android.graphics.drawable.GradientDrawable gradient=new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xf9ffffff,0xd9ffffff,0x28ffffff});
        wash.setBackground(gradient); hero.addView(wash,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout intro=new LinearLayout(this); intro.setOrientation(LinearLayout.VERTICAL); intro.setGravity(Gravity.CENTER_VERTICAL);
        intro.setPadding(Ui.dp(this,24),Ui.dp(this,18),Ui.dp(this,24),Ui.dp(this,18));
        TextView eyebrow=Ui.heading(this,"AXSL Vision",12); eyebrow.setTextColor(Ui.ACCENT); intro.addView(eyebrow);
        intro.addView(Ui.heading(this,"아이디어를 실행으로",Ui.narrow(this)?26:30));
        TextView desc=Ui.text(this,"사진과 카메라로 시작하는 비전 실험",15); desc.setTextColor(Ui.MUTED); intro.addView(desc);
        hero.addView(intro,new FrameLayout.LayoutParams(-1,-1));
        body.addView(hero,new LinearLayout.LayoutParams(-1,Ui.dp(this,Ui.narrow(this)?166:154))); gap(body,20);
        body.addView(Ui.heading(this,"무엇을 해볼까요?",20)); gap(body,12);
        String[] tools={"detect","access","face","compare","flow","drive","vis","signage"};
        String[] descriptions={"객체와 위치를 한눈에","현재 얼굴 인식","등록과 보관함 관리","두 얼굴의 특징 비교","유입과 유출 집계","눈과 고개의 변화","오늘의 방문 기록","미디어 재생 목록"};
        int width=getResources().getConfiguration().screenWidthDp;
        int columns=width<600?1:width<1000?2:4;
        for(int i=0;i<tools.length;i+=columns) {
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setBaselineAligned(false);
            for(int j=0;j<columns&&i+j<tools.length;j++) {
                String tool=tools[i+j]; LinearLayout card=featureCard(tool,descriptions[i+j],()->openMode(tool));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1); if(j>0) lp.leftMargin=Ui.dp(this,12); row.addView(card,lp);
            }
            body.addView(row,new LinearLayout.LayoutParams(-1,Ui.dp(this,Ui.narrow(this)?122:145))); gap(body,12);
        }
        addAction(body,"도구 모음 · QR · 텍스트 · 반려동물 · 자세",()->open("LabToolsActivity","lab"));
        inputActions(body,"lab"); addAction(body,"영상 분석",()->open("VideoAnalysisActivity","lab")); gap(body,16); relatedApps(body);
    }

    private LinearLayout featureCard(String target,String description,Runnable click) {
        LinearLayout card=Ui.panel(this); card.setGravity(Gravity.TOP); card.setClickable(true); card.setFocusable(true);
        card.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x180068f8),
            Ui.outline(this,Ui.SURFACE,12),Ui.round(this,android.graphics.Color.WHITE,12)));
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.addView(Ui.icon(this,SuiteConfig.icon(target),26));
        TextView title=Ui.heading(this,SuiteConfig.title(target),18); title.setPadding(Ui.dp(this,12),0,0,0);
        top.addView(title,new LinearLayout.LayoutParams(0,-2,1)); card.addView(top); gap(card,10);
        TextView desc=Ui.text(this,description,14); desc.setTextColor(Ui.MUTED); card.addView(desc);
        card.setContentDescription(SuiteConfig.title(target)+", "+description); card.setOnClickListener(v->click.run()); return card;
    }

    private void purpose(LinearLayout body) {
        LinearLayout focus=Ui.panel(this); focus.setPadding(Ui.dp(this,24),Ui.dp(this,24),Ui.dp(this,24),Ui.dp(this,24));
        if("signage".equals(role)||"vis".equals(role)) {
            ImageView visual=Ui.image(this,"signage".equals(role)?"hero":"workplace");
            visual.setClipToOutline(true); visual.setBackground(Ui.round(this,Ui.SURFACE,12));
            focus.addView(visual,new LinearLayout.LayoutParams(-1,Ui.dp(this,Ui.narrow(this)?148:176))); gap(focus,16);
        }
        focus.addView(Ui.icon(this,SuiteConfig.icon(role),36)); gap(focus,12);
        focus.addView(Ui.heading(this,SuiteConfig.title(role),Ui.narrow(this)?26:30));
        TextView description=Ui.text(this,SuiteConfig.purpose(role),16); description.setTextColor(Ui.MUTED); focus.addView(description); gap(focus,20);
        String action="시작";
        switch(role) { case "access":action="인식 시작";break; case "detect":action="탐지 시작";break;
            case "flow":action="측정 시작";break; case "signage":action="재생 목록 열기";break;
            case "face":action="얼굴 목록 열기";break; case "drive":action="모니터링 시작";break;
            case "vis":action="기록 시작";break; case "compare":action="얼굴 비교 열기";break; }
        focus.addView(Ui.button(this,action,v->openMode(role)),Ui.fill()); body.addView(focus,Ui.fill()); gap(body,16);
        if(!"signage".equals(role)&&!"face".equals(role)&&!"compare".equals(role)) inputActions(body,role);
        if(VideoAnalysisActivity.supports(role)) addAction(body,"영상 분석",()->open("VideoAnalysisActivity",role));
        if("access".equals(role)) addAction(body,"등록 목록",()->open("FaceGalleryActivity",role));
        if("face".equals(role)) { addAction(body,"사진으로 등록",()->pickImage(role)); addAction(body,"촬영하여 등록",()->capture(role)); }
        if("compare".equals(role)) addAction(body,"얼굴 목록",()->open("FaceGalleryActivity",role));
        if(!"signage".equals(role)) addAction(body,"기록 보기",()->open("ReportActivity",role));
        gap(body,20); relatedApps(body);
    }

    private void inputActions(LinearLayout body,String mode) {
        if(Ui.narrow(this)) { addAction(body,"사진 선택",()->pickImage(mode)); addAction(body,"촬영",()->capture(mode)); }
        else {
            LinearLayout row=new LinearLayout(this); row.addView(Ui.secondary(this,"사진 선택",v->pickImage(mode)),new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.leftMargin=Ui.dp(this,12);
            row.addView(Ui.secondary(this,"촬영",v->capture(mode)),lp); body.addView(row,Ui.fill());
        }
    }
    private void relatedApps(LinearLayout body) {
        body.addView(Ui.heading(this,"AXSL Vision 앱",18)); gap(body,8);
        String[] related=LabRoutes.otherRoles(SuiteConfig.ROLES,role);
        LinearLayout row=null; int columns=Ui.narrow(this)?1:3;
        for(int i=0;i<related.length;i++) {
            if(i%columns==0) { row=new LinearLayout(this); body.addView(row,Ui.fill()); }
            String next=related[i]; Button button=Ui.secondary(this,SuiteConfig.displayName(next),v->openRelated(next));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); if(i%columns>0) lp.leftMargin=Ui.dp(this,8); lp.bottomMargin=Ui.dp(this,8); row.addView(button,lp);
        }
    }
    private void addAction(LinearLayout body,String title,Runnable action) { gap(body,8); body.addView(Ui.secondary(this,title,v->action.run()),Ui.fill()); }
    private void gap(LinearLayout body,int height) { View spacer=new View(this); spacer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); body.addView(spacer,new LinearLayout.LayoutParams(1,Ui.dp(this,height))); }

    private void openMode(String mode) {
        if(!SuiteConfig.isRole(mode)) return;
        switch(mode) { case "face":open("FaceGalleryActivity",mode);break; case "compare":open("CompareActivity",mode);break;
            case "signage":open("SignageActivity",mode);break; default:open("LiveActivity",mode); }
    }
    private Intent target(String activity,String mode) { return new Intent().setClassName(this,"com.yonlab.vision."+activity).putExtra("mode",SuiteConfig.isRole(mode)?mode:role); }
    private void open(String activity,String mode) { try { startActivity(target(activity,mode)); } catch(ActivityNotFoundException|SecurityException e) { Ui.error(this,"화면을 열 수 없습니다",e); } }
    private void openRelated(String next) {
        SuiteShare.openApp(this,next);
    }
    private void pickImage(String mode) {
        pendingMode=mode; Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(pick,PICK_IMAGE); } catch(ActivityNotFoundException e) { Ui.error(this,"사진을 선택할 수 없습니다",e); }
    }
    private void capture(String mode) {
        pendingMode=mode;
        try { startActivityForResult(target("CaptureActivity",mode),CAPTURE); } catch(ActivityNotFoundException e) { Ui.error(this,"촬영을 열 수 없습니다",e); }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data); if(result!=RESULT_OK||data==null) return;
        if(request==PICK_IMAGE) {
            Uri uri=data.getData(); if(uri==null||!"content".equals(uri.getScheme())) { invalidInput(); return; }
            try { if((data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0) getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(SecurityException ignored) {}
            photo(uri,pendingMode);
        } else if(request==CAPTURE) {
            Object extra=data.getExtras()==null?null:data.getExtras().get("photo_uri");
            Uri uri=extra instanceof Uri?(Uri)extra:extra instanceof String?Uri.parse((String)extra):data.getData();
            if(uri==null||!"content".equals(uri.getScheme())||!(getPackageName()+".files").equals(uri.getAuthority())) { invalidInput(); return; }
            photo(uri,pendingMode);
        }
    }
    private void photo(Uri uri,String mode) {
        Intent intent=target("PhotoActivity",mode).setDataAndType(uri,"image/*").putExtra(Intent.EXTRA_STREAM,uri).putExtra("input_uri",uri.toString())
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("image",uri));
        try { startActivity(intent); } catch(ActivityNotFoundException|SecurityException e) { Ui.error(this,"사진을 열 수 없습니다",e); }
    }
    private void routeIncoming(Intent incoming) {
        if("lab".equals(role)&&incoming!=null) {
            String tool=incoming.getStringExtra("tool");
            if(ToolPhotoActivity.validTool(tool)) {
                Intent target=target("ToolPhotoActivity","lab").putExtra("tool",tool);
                Uri selected;try{selected=ImageIO.imageUri(incoming);}catch(RuntimeException e){invalidInput();return;}
                if(selected!=null&&"content".equals(selected.getScheme())) target.setDataAndType(selected,"image/*").putExtra("input_uri",selected.toString()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if(SuiteConfig.debug(this)) {target.putExtra("demo",incoming.getBooleanExtra("demo",false));String action=incoming.getStringExtra("action");if("generate".equals(action)&&"qr".equals(tool)){target.putExtra("action",action);target.putExtra("text",incoming.getStringExtra("text"));}}
                try {startActivity(target);}catch(ActivityNotFoundException e){Ui.error(this,"도구를 열 수 없습니다",e);}return;
            }
            if(SuiteConfig.debug(this)&&"device".equals(tool)) {
                Intent target=target("DeviceToolsActivity","lab");String action=incoming.getStringExtra("action");
                if(java.util.Arrays.asList("print","sound","led-on","led-off","restore","torch-on","torch-off","camera").contains(action))target.putExtra("action",action);
                if(incoming.hasExtra("text"))target.putExtra("text",incoming.getStringExtra("text"));
                try{startActivity(target);}catch(ActivityNotFoundException e){Ui.error(this,"도구를 열 수 없습니다",e);}return;
            }
        }

        try { routeValidated(incoming); } catch(RuntimeException malformed) { invalidInput(); }
    }
    private void routeValidated(Intent incoming) {
        if(incoming==null) return;
        String mode=incoming.getStringExtra("mode");
        if(Intent.ACTION_SEND.equals(incoming.getAction())) {
            if(SuiteShare.REGISTRY_MIME.equals(incoming.getType())) {
                Uri uri=ImageIO.imageUri(incoming);
                if((!"face".equals(role)&&!"access".equals(role)&&!"compare".equals(role))||!SuiteShare.isTrustedUri(this,uri)) { invalidInput(); return; }
                Intent registry=target("FaceGalleryActivity",role).setAction(Intent.ACTION_SEND).setDataAndType(uri,SuiteShare.REGISTRY_MIME)
                    .putExtra("registry_uri",uri.toString()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                registry.setClipData(ClipData.newRawUri("AXSL Vision 등록 목록",uri)); startActivity(registry); return;
            }
            if(incoming.getType()==null||!incoming.getType().startsWith("image/")) { invalidInput(); return; }
            Uri uri;
            try { uri=incoming.getParcelableExtra(Intent.EXTRA_STREAM); } catch(RuntimeException e) { invalidInput(); return; }
            if(uri==null) uri=incoming.getData();
            if(uri==null||!"content".equals(uri.getScheme())) { invalidInput(); return; }
            if("signage".equals(role)) {
                Intent signage=target("SignageActivity",role).setAction(Intent.ACTION_SEND).setDataAndType(uri,incoming.getType())
                    .putExtra(Intent.EXTRA_STREAM,uri).putExtra("input_uri",uri.toString()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                signage.setClipData(ClipData.newRawUri("AXSL Vision 이미지",uri)); startActivity(signage);
            } else photo(uri,SuiteConfig.isRole(mode)?mode:role);
            return;
        }
        if(Intent.ACTION_VIEW.equals(incoming.getAction())&&incoming.getData()!=null) {
            Uri uri=incoming.getData(); if(!"yonlab".equals(uri.getScheme())) { invalidInput(); return; }
            mode=uri.getQueryParameter("mode"); if(mode==null) mode=uri.getQueryParameter("role");
            if(!SuiteConfig.isRole(mode)) { invalidInput(); return; }
        }
        if(routeDebug(incoming,SuiteConfig.isRole(mode)?mode:role)) return;
        if(SuiteConfig.debug(this)&&incoming.hasExtra("input_path")) {
            Intent photo=target("PhotoActivity",SuiteConfig.isRole(mode)?mode:role).putExtra("input_path",incoming.getStringExtra("input_path"));
            try { startActivity(photo); } catch(ActivityNotFoundException e) { Ui.error(this,"사진을 열 수 없습니다",e); } return;
        }
        if(SuiteConfig.isRole(mode)&&!"lab".equals(mode)) openMode(mode);
    }
    private boolean routeDebug(Intent incoming,String mode) {
        if(!SuiteConfig.debug(this)||!incoming.hasExtra("qa_action")) return false;
        String action=incoming.getStringExtra("qa_action");
        if(action==null) { invalidInput(); return true; }
        switch(action) {
            case "live":open("LiveActivity",mode);break;
            case "photo":
                Intent photo=target("PhotoActivity",mode); Uri uri=ImageIO.imageUri(incoming);
                if(uri!=null) {
                    if(!"content".equals(uri.getScheme())) { invalidInput(); return true; }
                    photo.setDataAndType(uri,"image/*").putExtra("input_uri",uri.toString()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    photo.setClipData(ClipData.newRawUri("QA image",uri));
                }
                if(incoming.hasExtra("input_path")) photo.putExtra("input_path",incoming.getStringExtra("input_path"));
                if(incoming.hasExtra("face_index")) photo.putExtra("face_index",incoming.getIntExtra("face_index",0));
                String suggested=incoming.getStringExtra("enroll_name"); if(suggested!=null&&suggested.length()<=80) photo.putExtra("enroll_name",suggested);
                startActivity(photo);break;
            case "capture":capture(mode);break;
            case "gallery":open("FaceGalleryActivity",mode);break;
            case "compare":open("CompareActivity",mode);break;
            case "signage":open("SignageActivity",mode);break;
            case "settings":open("SettingsActivity",mode);break;
            case "report":open("ReportActivity",mode);break;
            default:invalidInput();
        }
        return true;
    }
    private void invalidInput() { new AlertDialog.Builder(this).setTitle("입력을 열 수 없습니다").setMessage("사진이나 앱 링크를 확인하세요.").setPositiveButton("확인",null).show(); }
}
