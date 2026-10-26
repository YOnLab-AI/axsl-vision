package com.yonlab.vision;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Switch;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Bounded photo analysis with worker-owned engines and explicit face enrollment. */
public class PhotoActivity extends YOnActivity {
    private static final int PICK=601,CAPTURE=501;
    private static final Object RECORD_LOCK=new Object();
    private static final AtomicReference<String> latestSession=new AtomicReference<>("");
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->new Thread(r,"yon-photo"));
    private String modelId;
    private String mode;
    private LinearLayout mediaBody;private ScrollView controlScroll;
    private VisionCanvas canvas;
    private TextView summary,attributes;
    private Spinner faces;
    private EditText name;
    private Button enroll,share;
    private volatile Token token;
    private Result result;
    private boolean active,closed,saving,analyzeAttributes;
    private Uri pendingUri;
    private String pendingPath;

    private static final class Token {
        final String id=UUID.randomUUID().toString();
        final AtomicBoolean canceled=new AtomicBoolean();
        final AtomicInteger commit=new AtomicInteger();
        void cancel() { canceled.set(true); commit.compareAndSet(0,2); }
        boolean beginCommit() { return !canceled.get()&&commit.compareAndSet(0,1); }
        void endCommit() { commit.compareAndSet(1,canceled.get()?2:0); }
    }
    private static final class Result {
        final String analysisSessionId;
        Bitmap image;
        List<Observations.Face> faces=Collections.emptyList();
        List<Observations.Detection> detections=Collections.emptyList();
        long decodeMs,analysisMs,totalMs,analyzedAt,storedId=-1;
        int matches,storedFaceIndex=-1;
        volatile int selected=-1;
        boolean engineStarted,engineClosed=true,attributesRequested;
        double expressionMs,ageGenderMs;int expressionFaces,ageGenderFaces;
        String state="RUNNING",errorType="",enrollErrorType="";
        Result(String analysisSessionId) { this.analysisSessionId=analysisSessionId; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); String requested=getIntent().getStringExtra("mode"); mode=SuiteConfig.isRole(requested)?requested:SuiteConfig.role(this);modelId=OneModels.resolve(this,mode,getIntent().getStringExtra("model_id"));
        analyzeAttributes=state!=null?state.getBoolean("analyzeAttributes","vis".equals(mode)):getPreferences(0).getBoolean("analyzeAttributes","vis".equals(mode));
        render();
        try {
            if(state!=null) { String uri=state.getString("input_uri"); if(uri!=null) pendingUri=Uri.parse(uri); if(SuiteConfig.debug(this)) pendingPath=state.getString("input_path"); }
            else { pendingUri=ImageIO.imageUri(getIntent()); if(SuiteConfig.debug(this)) pendingPath=getIntent().getStringExtra("input_path"); }
        } catch(RuntimeException e) { Ui.error(this,"사진을 열 수 없습니다",e); }
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        if(pendingUri!=null) state.putString("input_uri",pendingUri.toString()); if(pendingPath!=null&&SuiteConfig.debug(this)) state.putString("input_path",pendingPath);
        state.putBoolean("analyzeAttributes",analyzeAttributes);super.onSaveInstanceState(state);
    }
    @Override protected void onResume() {
        super.onResume(); active=true;
        if(result==null&&(pendingUri!=null||pendingPath!=null)) analyze(pendingUri,pendingPath);
        else if(result!=null&&token!=null&&token.canceled.get()) {
            token=new Token(); latestSession.set(token.id);
            selection(result.selected); publish(token,result);
        }
    }
    @Override protected void onPause() {
        active=false; SuiteShare.cancel(this); Token current=token; if(current!=null) current.cancel(); if(enroll!=null) enroll.setEnabled(false); super.onPause();
    }
    @Override protected void onDestroy() {
        closed=true; Token current=token; if(current!=null) current.cancel(); worker.shutdownNow();
        canvas.show(null,null,null); if(result!=null&&result.image!=null&&!result.image.isRecycled()) result.image.recycle(); result=null; super.onDestroy();
    }
    private boolean current(Token owner) { return active&&!closed&&!isFinishing()&&!isDestroyed()&&owner==token&&!owner.canceled.get(); }

    private void render() {
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Ui.BACK);
        LinearLayout header=Ui.brandHeader(this,"사진 · "+SuiteConfig.title(mode));
        header.addView(Ui.secondary(this,"닫기",v->finish()),new LinearLayout.LayoutParams(Ui.dp(this,80),-2)); root.addView(header,Ui.fill());
        LinearLayout content=mediaBody=new LinearLayout(this); boolean wide=MediaPanels.sideBySide(this);
        content.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL); content.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,12));
        canvas=new VisionCanvas(this); canvas.setBackground(Ui.round(this,Ui.NAVY,12)); canvas.setClipToOutline(true);
        content.addView(canvas,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView controls=controlScroll=new ScrollView(this); controls.setFillViewport(true); LinearLayout panel=Ui.panel(this); controls.addView(panel,Ui.fill());
        content.addView(controls,new LinearLayout.LayoutParams(-1,0,1));
        summary=Ui.heading(this,"사진을 선택하세요",22); panel.addView(summary,Ui.fill());
        attributes=Ui.text(this,"",15); attributes.setVisibility(View.GONE); panel.addView(attributes,Ui.fill());
        if(!"detect".equals(mode)&&!"flow".equals(mode)){Switch extras=new Switch(this);extras.setText("표정·연령 분석");extras.setChecked(analyzeAttributes);extras.setOnCheckedChangeListener((button,on)->{analyzeAttributes=on;getPreferences(0).edit().putBoolean("analyzeAttributes",on).apply();if(active&&(pendingUri!=null||pendingPath!=null)){discard();analyze(pendingUri,pendingPath);}});panel.addView(extras,space());}
        faces=new Spinner(this); faces.setMinimumHeight(Ui.dp(this,48)); faces.setContentDescription("분석할 얼굴 선택"); faces.setVisibility(View.GONE); panel.addView(faces,Ui.fill());
        faces.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) { selection(position); }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        name=new EditText(this); name.setSingleLine(true); name.setHint("등록 이름"); name.setTextSize(16); name.setMinHeight(Ui.dp(this,48));
        name.setImeOptions(EditorInfo.IME_ACTION_DONE);name.setOnEditorActionListener((view,action,event)->{if(action==EditorInfo.IME_ACTION_DONE||(event!=null&&event.getKeyCode()==android.view.KeyEvent.KEYCODE_ENTER)){hideKeyboard();if(enroll!=null)enroll.post(()->enroll.requestRectangleOnScreen(new android.graphics.Rect(0,0,enroll.getWidth(),enroll.getHeight()),false));return true;}return false;});
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)}); name.setVisibility(View.GONE); panel.addView(name,Ui.fill());
        enroll=Ui.button(this,"선택한 얼굴 등록",v->confirmEnrollment()); enroll.setVisibility(View.GONE); panel.addView(enroll,space());
        share=Ui.secondary(this,"다른 기능에서 열기",v->{Result value=result;if(value!=null&&value.image!=null)OneImageHandoff.open(this,value.image);});
        share.setEnabled(false); panel.addView(share,space());
        panel.addView(Ui.secondary(this,"사진 선택",v->pick()),space()); panel.addView(Ui.secondary(this,"촬영",v->capture()),space());
        panel.addView(Ui.secondary(this,"얼굴 목록",v->openGallery()),space()); panel.addView(Ui.secondary(this,"기록 보기",v->openReport()),space());
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);MediaPanels.arrange(this,mediaBody,canvas,controlScroll,1.7f);
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);MediaPanels.arrange(this,mediaBody,canvas,controlScroll,1.7f);canvas.invalidate();}
    private LinearLayout.LayoutParams space() { LinearLayout.LayoutParams lp=Ui.fill(); lp.topMargin=Ui.dp(this,10); return lp; }
    private void pick() {
        try { startActivityForResult(OneInputs.picker(this,"image",OneModelPolicy.canonicalFeature(mode)),PICK); }
        catch(Exception e) { Ui.error(this,"사진을 선택할 수 없습니다",e); }
    }
    private void capture() {
        try { startActivityForResult(new Intent().setClassName(this,"com.yonlab.vision.CaptureActivity").putExtra("mode",mode),CAPTURE); }
        catch(Exception e) { Ui.error(this,"촬영을 열 수 없습니다",e); }
    }
    private void openGallery() { try { startActivity(new Intent().setClassName(this,"com.yonlab.vision.FaceGalleryActivity")); } catch(Exception e) { Ui.error(this,"목록을 열 수 없습니다",e); } }
    private void openReport() { try { startActivity(new Intent().setClassName(this,"com.yonlab.vision.ReportActivity")); } catch(Exception e) { Ui.error(this,"기록을 열 수 없습니다",e); } }
    @Override protected void onActivityResult(int request,int status,Intent data) {
        super.onActivityResult(request,status,data); if(status!=RESULT_OK||data==null) return;
        try {
            Uri uri;
            if(request==PICK) {
                uri=data.getData(); if(uri!=null) try { if((data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0) getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(SecurityException ignored) {}
            } else if(request==CAPTURE) {
                Object extra=data.getExtras()==null?null:data.getExtras().get("photo_uri"); uri=extra instanceof Uri?(Uri)extra:extra instanceof String?Uri.parse((String)extra):data.getData();
                if(uri==null||!(getPackageName()+".files").equals(uri.getAuthority())) throw new IllegalArgumentException("촬영 사진을 확인하세요");
            } else return;
            if(uri==null||!"content".equals(uri.getScheme())) throw new IllegalArgumentException("사진을 선택하세요");
            pendingUri=uri; pendingPath=null; discard();
            if(active) analyze(uri,null);
        } catch(Exception e) { Ui.error(this,"사진을 열 수 없습니다",e); }
    }
    private void discard() {
        Token old=token; if(old!=null) old.cancel(); canvas.show(null,null,null); canvas.select(-1);
        if(result!=null&&result.image!=null&&!result.image.isRecycled()) result.image.recycle(); result=null;
        faces.setVisibility(View.GONE); attributes.setVisibility(View.GONE); name.setVisibility(View.GONE); enroll.setVisibility(View.GONE);
        share.setEnabled(false);
    }
    private void analyze(Uri uri,String path) {
        Token old=token; if(old!=null) old.cancel(); final Token owner=new Token(); token=owner;
        latestSession.set(owner.id); summary.setText("분석 중"); share.setEnabled(false);
        final android.content.Context app=getApplicationContext();final boolean requestedAttributes=analyzeAttributes;
        worker.execute(()-> {
            Result output=new Result(owner.id);output.attributesRequested=requestedAttributes; long started=SystemClock.elapsedRealtime(); FaceEngine faceEngine=null; OneObjectEngine objectEngine=null; Registry registry=null; Throwable failure=null;
            try {
                if(owner.canceled.get()) return;
                output.image=uri!=null?ImageIO.load(app,uri,1600):ImageIO.owned(app,path,1600); output.decodeMs=SystemClock.elapsedRealtime()-started;
                if(owner.canceled.get()) return; long analysis=SystemClock.elapsedRealtime();
                if("detect".equals(mode)||"flow".equals(mode)) {
                    objectEngine=new OneObjectEngine(app,modelId); output.engineStarted=true; output.engineClosed=false;
                    output.detections=new ArrayList<>(objectEngine.analyze(output.image,.4f));
                } else {
                    boolean embeddings="lab".equals(mode)||"access".equals(mode)||"face".equals(mode)||"compare".equals(mode)||"vis".equals(mode);
                    faceEngine=new FaceEngine(app,mode,modelId); output.engineStarted=true; output.engineClosed=false;
                    output.faces=new ArrayList<>(faceEngine.analyze(output.image,embeddings,requestedAttributes));
                    output.expressionMs=faceEngine.lastExpressionMs();output.ageGenderMs=faceEngine.lastAgeGenderMs();output.expressionFaces=faceEngine.lastExpressionFaces();output.ageGenderFaces=faceEngine.lastAgeGenderFaces();
                    if(embeddings&&!owner.canceled.get()) {
                        registry=new Registry(app);
                        for(Observations.Face face:output.faces) if(face.embedding!=null) {
                            Registry.Match match=registry.match(face.embedding,Registry.DEFAULT_THRESHOLD); face.similarity=match.similarity;
                            if(match.matched) { face.label=match.name; output.matches++; }
                        }
                    }
                }
                output.analysisMs=SystemClock.elapsedRealtime()-analysis; output.analyzedAt=System.currentTimeMillis(); output.selected=output.faces.isEmpty()?-1:0;
                output.state=owner.canceled.get()?"CANCELLED":"COMPLETED";
            } catch(Exception e) { failure=e; output.state=owner.canceled.get()?"CANCELLED":"ERROR"; output.errorType=e.getClass().getSimpleName(); }
            finally {
                try { if(registry!=null) registry.close(); } catch(Exception e) { if(failure==null) failure=e; output.errorType=e.getClass().getSimpleName(); }
                try { if(faceEngine!=null) faceEngine.close(); if(objectEngine!=null) objectEngine.close(); output.engineClosed=true; }
                catch(Exception e) { output.engineClosed=false; output.errorType=e.getClass().getSimpleName(); if(failure==null) failure=e; }
                output.totalMs=SystemClock.elapsedRealtime()-started;
                if(owner.canceled.get()) output.state="CANCELLED"; else if(failure!=null) output.state="ERROR";
                write(owner,output);
                if(!owner.canceled.get()&&"COMPLETED".equals(output.state)) {
                    try { Records.event(app,"photo-"+mode,"COMPLETED",new JSONObject().put("modelId",modelId).put("feature",OneModelPolicy.canonicalFeature(mode)).put("faces",output.faces.size()).put("detections",output.detections.size()).put("matchCount",output.matches).put("analysisMs",output.analysisMs).put("analysisSessionId",output.analysisSessionId).put("audience",AudienceJson.summary(output.faces))); } catch(Exception ignored) {}
                }
                final Throwable error=failure;
                runOnUiThread(()-> {
                    if(!current(owner)) { if(output.image!=null&&!output.image.isRecycled()) output.image.recycle(); return; }
                    if(error!=null) { if(output.image!=null&&!output.image.isRecycled()) output.image.recycle(); summary.setText("분석 오류"); Ui.error(this,"사진을 분석할 수 없습니다",error); return; }
                    result=output; show(output);
                });
            }
        });
    }
    private void show(Result value) {
        share.setEnabled(value.image!=null&&!value.image.isRecycled());
        canvas.show(value.image,value.faces,value.detections);
        if("detect".equals(mode)||"flow".equals(mode)) {
            summary.setText("객체 "+value.detections.size()+"개 · "+value.analysisMs+" ms");
            StringBuilder lines=new StringBuilder(); for(Observations.Detection item:value.detections) {
                if(lines.length()>0) lines.append('\n'); lines.append(localized(item.label)).append(" · 점수 ").append(decimal(item.score));
            }
            attributes.setText(lines); attributes.setVisibility(lines.length()==0?View.GONE:View.VISIBLE); return;
        }
        summary.setText("얼굴 "+value.faces.size()+"개 · "+value.analysisMs+" ms");
        if(value.faces.isEmpty()) return;
        String[] labels=new String[value.faces.size()]; for(int i=0;i<labels.length;i++) labels[i]="얼굴 "+(i+1);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels); adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        faces.setAdapter(adapter); faces.setVisibility(View.VISIBLE);
        int chosen=0; if(SuiteConfig.debug(this)) chosen=getIntent().getIntExtra("face_index",0); chosen=Math.max(0,Math.min(labels.length-1,chosen));
        faces.setSelection(chosen); selection(chosen);
        if(SuiteConfig.debug(this)) { String suggested=getIntent().getStringExtra("enroll_name"); if(suggested!=null) name.setText(suggested); }
    }
    private void selection(int index) {
        Result value=result; if(value==null||index<0||index>=value.faces.size()) return;
        value.selected=index; canvas.select(index); Observations.Face face=value.faces.get(index);
        StringBuilder lines=new StringBuilder("얼굴 "+(index+1));
        if(face.label!=null&&!face.label.isEmpty()) lines.append("\n").append(face.label);
        if(finite(face.similarity)) lines.append("\n점수 ").append(decimal(face.similarity));
        appendAngle(lines,"고개 좌우",face.yaw); appendAngle(lines,"고개 위아래",face.pitch); appendAngle(lines,"기울기",face.roll);
        appendProbability(lines,"왼쪽 눈",face.leftEye);appendProbability(lines,"오른쪽 눈",face.rightEye);appendProbability(lines,"미소",face.smile);
        if(face.expressionIndex>=0){lines.append("\n표정 · ").append(ExpressionPolicy.korean(face.expressionIndex));appendProbability(lines,"표정 확률",face.expressionConfidence);}
        if(AgeGenderPolicy.ageGroup(face.ageYears)>=0)lines.append("\n연령 · 약 ").append(Math.round(face.ageYears)).append("세");if(face.genderIndex>=0){lines.append("\n성별 · ").append(AgeGenderPolicy.genderLabel(face.genderIndex));appendProbability(lines,"여성",face.femaleProbability);appendProbability(lines,"남성",face.maleProbability);}
        attributes.setText(lines); attributes.setVisibility(View.VISIBLE);
        boolean canEnroll=face.embedding!=null&&face.embedding.length==Registry.DIMENSION;
        faces.setEnabled(!saving); name.setEnabled(!saving); enroll.setText(saving?"등록 중":"선택한 얼굴 등록");
        name.setVisibility(canEnroll?View.VISIBLE:View.GONE); enroll.setVisibility(canEnroll?View.VISIBLE:View.GONE); enroll.setEnabled(canEnroll&&!saving&&active);
        Token owner=token; if(owner!=null&&!owner.canceled.get()) publish(owner,value);
    }
    private void confirmEnrollment() {
        Result value=result; Token owner=token; if(value==null||owner==null||!current(owner)||saving||value.selected<0) return;
        hideKeyboard();String text=name.getText().toString().trim(); if(text.isEmpty()) { name.setError("이름을 입력하세요"); return; }
        int index=value.selected; Observations.Face face=value.faces.get(index); if(face.embedding==null) return;
        new AlertDialog.Builder(this).setTitle("선택한 얼굴을 등록할까요?").setMessage("얼굴 "+(index+1)+" · "+text)
            .setNegativeButton("취소",null).setPositiveButton("등록",(dialog,which)->save(owner,value,index,text)).show();
    }
    private void save(Token owner,Result value,int index,String text) {
        if(!current(owner)||saving||index<0||index>=value.faces.size()) return; final float[] feature=value.faces.get(index).embedding.clone();
        saving=true; faces.setEnabled(false); name.setEnabled(false); enroll.setEnabled(false); enroll.setText("등록 중");
        worker.execute(()-> {
            long id=-1; Throwable failure=null; Registry registry=null; boolean admitted=owner.beginCommit();
            try { if(admitted) { registry=new Registry(getApplicationContext()); id=registry.enroll(text,feature); value.storedId=id; value.storedFaceIndex=index; } }
            catch(Exception e) { failure=e; }
            finally {
                try { if(registry!=null) registry.close(); } catch(Exception e) { if(failure==null) failure=e; }
                if(failure!=null) value.enrollErrorType=failure.getClass().getSimpleName();
                if(admitted) owner.endCommit();
                if(id>0) { try { Records.event(getApplicationContext(),"face-enroll","COMPLETED",new JSONObject().put("id",id).put("name",text).put("faceIndex",index)); } catch(Exception ignored) {} }
                write(owner,value); final long stored=id; final Throwable error=failure;
                runOnUiThread(()-> {
                    saving=false; if(!active||closed||result!=value||isFinishing()||isDestroyed()) return;
                    faces.setEnabled(true); name.setEnabled(true); enroll.setEnabled(true); enroll.setText("선택한 얼굴 등록");
                    if(stored>0) { summary.setText("등록 완료 · "+text); if(error!=null) Ui.error(this,"등록 목록을 닫을 수 없습니다",error); }
                    else if(error!=null) Ui.error(this,"얼굴을 등록할 수 없습니다",error);
                });
            }
        });
    }
    private void publish(Token owner,Result value) { worker.execute(()->write(owner,value)); }
    private void write(Token owner,Result value) {
        synchronized(RECORD_LOCK) {
            if(!latestSession.get().equals(owner.id)) return;
            try {
                JSONObject record=new JSONObject().put("sessionId",value.analysisSessionId).put("analysisSessionId",value.analysisSessionId).put("viewSessionId",owner.id)
                    .put("recordKind",owner.id.equals(value.analysisSessionId)?"ANALYSIS":"VIEW_RESTORED").put("analysisExecutedForView",owner.id.equals(value.analysisSessionId)).put("analyzedAt",value.analyzedAt)
                    .put("mode",mode).put("state",value.state).put("faces",value.faces.size()).put("detections",value.detections.size())
                    .put("matchCount",value.matches).put("selectedIndex",value.selected).put("storedId",value.storedId).put("storedFaceIndex",value.storedFaceIndex).put("decodeMs",value.decodeMs).put("analysisMs",value.analysisMs).put("totalMs",value.totalMs)
                    .put("attributesRequested",value.attributesRequested).put("expressionMs",value.expressionMs).put("expressionFaces",value.expressionFaces).put("ageGenderMs",value.ageGenderMs).put("ageGenderFaces",value.ageGenderFaces).put("audience",AudienceJson.summary(value.faces)).put("engineStarted",value.engineStarted).put("engineClosed",value.engineClosed).put("featureDim",value.faces.isEmpty()||value.faces.get(0).embedding==null?0:value.faces.get(0).embedding.length);
                if(value.image!=null) record.put("width",value.image.getWidth()).put("height",value.image.getHeight());
                if(!value.errorType.isEmpty()) record.put("errorType",value.errorType);
                if(!value.enrollErrorType.isEmpty()) record.put("enrollErrorType",value.enrollErrorType);
                JSONArray summaries=new JSONArray(); for(int i=0;i<value.faces.size();i++) {
                    Observations.Face face=value.faces.get(i); JSONObject item=AudienceJson.face(face).put("index",i).put("featureDim",face.embedding==null?0:face.embedding.length);
                    if(finite(face.similarity)) item.put("similarity",face.similarity); summaries.put(item);
                }
                record.put("modelId",modelId);record.put("faceResults",summaries); Records.write(getApplicationContext(),"photo-result.json",record);
            } catch(Exception e) { android.util.Log.e("YOn","photo_record_failed",e); }
        }
    }
    private void hideKeyboard(){InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(name.getWindowToken(),0);name.clearFocus();}
    private static void appendAngle(StringBuilder text,String label,float value){if(finite(value))text.append('\n').append(label).append(' ').append(String.format(Locale.ROOT,"%.1f°",value));}
    private static void appendProbability(StringBuilder text,String label,float value){if(finite(value)&&value>=0&&value<=1)text.append('\n').append(label).append(' ').append(Math.round(value*100)).append('%');}
    private static boolean finite(float value) { return !Float.isNaN(value)&&!Float.isInfinite(value); }
    private static String decimal(float value) { return String.format(Locale.ROOT,"%.3f",value); }
    private static void append(StringBuilder text,String label,float value) { if(finite(value)) text.append('\n').append(label).append(' ').append(decimal(value)); }
    private static String localized(String label) { if("person".equals(label)) return "사람"; if("dog".equals(label)) return "개"; if("cat".equals(label)) return "고양이"; if("car".equals(label)) return "자동차"; return label==null?"객체":label; }
}
