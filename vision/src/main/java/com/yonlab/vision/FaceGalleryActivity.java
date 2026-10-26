package com.yonlab.vision;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Rename/archive/restore and explicit SAF backup of this app's own enrollment registry. */
public class FaceGalleryActivity extends YOnActivity {
    private static final int EXPORT=811,IMPORT=812,MAX_BACKUP_BYTES=16*1024*1024;
    private static final AtomicLong NEWEST=new AtomicLong();
    private final long owner=NEWEST.incrementAndGet();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(2),r->new Thread(r,"YOnGallery"));
    private final AtomicBoolean destroyed=new AtomicBoolean(),busy=new AtomicBoolean();
    private final AtomicReference<Job> active=new AtomicReference<>();
    private long operation;
    private TextView status;private LinearLayout list;private Switch archived;
    private String confirmedRegistry="";
    private boolean resumed;
    private Runnable resumedWork;
    private List<Registry.Entry> current=Collections.emptyList();
    private static final class Job {
        // 0 pending, 1 canceled, 2 commit accepted, 3 canceled after acceptance.
        final AtomicInteger state=new AtomicInteger();
        void check(){if(state.get()==1||state.get()==3||Thread.currentThread().isInterrupted())throw new CancellationException();}
        void accept(){if(!state.compareAndSet(0,2))throw new CancellationException();}
        void cancel(){int s;do{s=state.get();if(s==1||s==3)return;}while(!state.compareAndSet(s,s==2?3:1));}
    }
    private interface Work { Object run(Job job)throws Exception; }
    private interface Done { void show(Object value)throws Exception; }
    private static final class Rows {
        final List<Registry.Entry> rows;final JSONObject details;
        Rows(List<Registry.Entry> rows,JSONObject details){this.rows=rows;this.details=details;}
    }
    public void onCreate(Bundle saved){
        super.onCreate(saved);LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(Ui.BACK);shell.addView(Ui.brandHeader(this,"얼굴 목록"));LinearLayout root=Ui.column(this);
        status=Ui.text(this,"목록을 불러오는 중",15);root.addView(status);
        root.addView(Ui.actions(this,Ui.secondary(this,"새로고침",v->load()),Ui.secondary(this,"내보내기",v->chooseExport()),Ui.secondary(this,"가져오기",v->chooseImport())),Ui.fill());
        root.addView(Ui.secondary(this,"출입 인식 열기",v->OneLaunch.open(this,"access",OneModels.selected(this,"access"),null,"image",null)));
        archived=new Switch(this);archived.setText("보관 항목 보기");archived.setChecked(saved!=null&&saved.getBoolean("showArchived"));archived.setOnCheckedChangeListener((b,value)->render(current));root.addView(archived);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list,Ui.fill());ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));shell.addView(Ui.secondary(this,"닫기",v->finish()));setContentView(shell);
        if(saved!=null)confirmedRegistry=saved.getString("confirmedRegistry","");
        Uri incoming=incomingRegistry(getIntent());
        if(incoming!=null&&!incoming.toString().equals(confirmedRegistry)){previewImport(incoming,true);}else load();
    }
    protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("showArchived",archived.isChecked());out.putString("confirmedRegistry",confirmedRegistry);}
    @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);findViewById(android.R.id.content).requestLayout();}
    private Uri incomingRegistry(Intent intent){
        if(intent==null||!Intent.ACTION_SEND.equals(intent.getAction()))return null;
        if(!SuiteShare.REGISTRY_MIME.equals(intent.getType())){status.setText("등록 파일을 다시 전달하세요");return null;}
        String value=intent.getStringExtra("registry_uri");Uri uri=value==null?intent.getData():Uri.parse(value);
        if(uri==null||!SuiteShare.isTrustedUri(this,uri)){status.setText("AXSL Vision 앱의 등록 파일을 확인하세요");return null;}
        return uri;
    }
    private boolean visible(){return !destroyed.get()&&!isFinishing()&&!isDestroyed()&&owner==NEWEST.get();}
    private void submit(String action,Work work,Done done){
        if(!visible())return;if(!busy.compareAndSet(false,true)){status.setText("처리 중입니다");return;}
        Job job=new Job();active.set(job);long token=++operation;status.setText("처리 중");publish("RUNNING",action,new JSONObject());
        worker.execute(()->{
            Object result=null;Throwable error=null;try{job.check();result=work.run(job);}catch(Exception|LinkageError|OutOfMemoryError e){error=e;}
            final Object value=result;final Throwable failure=error;
            main.post(()->{
                active.compareAndSet(job,null);busy.set(false);if(!visible()||operation!=token||job.state.get()==1||job.state.get()==3)return;
                if(failure!=null){status.setText(friendly(failure));publish("FAILED",action,details("error",failure.getClass().getSimpleName()));return;}
                try{done.show(value);}catch(Exception e){status.setText(friendly(e));publish("FAILED",action,details("error",e.getClass().getSimpleName()));}
            });
        });
    }
    private void load(){submit("list",job->{try(Registry r=new Registry(getApplicationContext())){return r.entries(true);}},value->{
        @SuppressWarnings("unchecked") List<Registry.Entry> rows=(List<Registry.Entry>)value;render(rows);publish("COMPLETED","list",details("count",rows.size()));
    });}
    private void render(List<Registry.Entry> rows){
        current=rows;list.removeAllViews();int activeCount=0,archivedCount=0,shown=0;
        for(Registry.Entry e:rows){if(e.archived)archivedCount++;else activeCount++;if(e.archived&&!archived.isChecked())continue;shown++;
            LinearLayout card=Ui.panel(this);card.addView(Ui.heading(this,e.name,20));card.addView(Ui.text(this,"#"+e.id+(e.archived?" · 보관 중":" · 사용 중"),13));
            card.addView(Ui.actions(this,Ui.secondary(this,"이름 변경",v->rename(e)),Ui.secondary(this,e.archived?"복구":"보관",v->archive(e))),Ui.fill());
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=Ui.dp(this,12);list.addView(card,lp);
        }
        if(shown==0)list.addView(Ui.text(this,"등록된 얼굴이 없습니다",18));status.setText("사용 "+activeCount+"명 · 보관 "+archivedCount+"명");
    }
    private void rename(Registry.Entry item){
        if(busy.get())return;EditText name=new EditText(this);name.setSingleLine();name.setText(item.name);name.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("이름 변경").setView(name).setPositiveButton("저장",(d,w)->mutate("rename",item,name.getText().toString())).setNegativeButton("취소",null).show();
    }
    private void archive(Registry.Entry item){
        if(busy.get())return;new AlertDialog.Builder(this).setTitle(item.archived?"얼굴 복구":"얼굴 보관").setMessage(item.name).setPositiveButton(item.archived?"복구":"보관",(d,w)->mutate(item.archived?"restore":"archive",item,null)).setNegativeButton("취소",null).show();
    }
    private void mutate(String action,Registry.Entry item,String name){
        submit(action,job->{try(Registry r=new Registry(getApplicationContext())){job.accept();if("rename".equals(action))r.rename(item.id,name);else r.archive(item.id,!item.archived);
            JSONObject result=details("id",item.id);result.put("commitAcceptedBeforeCancel",job.state.get()==3);Records.event(getApplicationContext(),"gallery",action,result);return new Rows(r.entries(true),result);
        }},value->{Rows rows=(Rows)value;render(rows.rows);publish("COMPLETED",action,rows.details);});
    }
    private void chooseExport(){
        if(busy.get())return;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"AXSL-Vision-face-registry.json");try{startActivityForResult(i,EXPORT);}catch(Exception e){status.setText("파일 저장 앱을 열 수 없습니다");}
    }
    private void chooseImport(){
        if(busy.get())return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");try{startActivityForResult(i,IMPORT);}catch(Exception e){status.setText("파일 선택 앱을 열 수 없습니다");}
    }
    private void shareRegistry(){
        submit("share-preview",job->{try(Registry r=new Registry(getApplicationContext())){job.check();return r.exportJson();}},value->{
            if(!resumed){status.setText("전달을 다시 선택하세요");publish("CANCELED","share-preview",new JSONObject());return;}
            JSONObject document=(JSONObject)value;publish("READY","share-preview",details("count",document.getJSONArray("entries").length()));SuiteShare.shareRegistry(this,document);
        });
    }
    protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();if(!"content".equals(uri.getScheme())){status.setText("파일을 다시 선택하세요");return;}
        if(request==EXPORT)submit("export",job->{
            JSONObject document;try(Registry r=new Registry(getApplicationContext())){document=r.exportJson();}byte[] bytes=document.toString(2).getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BACKUP_BYTES)throw new IllegalArgumentException("등록 파일이 너무 큽니다");
            job.accept();try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IllegalStateException("파일을 열 수 없습니다");out.write(bytes);out.flush();}
            return details("count",document.getJSONArray("entries").length()).put("bytes",bytes.length);
        },value->{status.setText("내보내기 완료");publish("COMPLETED","export",(JSONObject)value);});
        if(request==IMPORT)previewImport(uri,false);
    }
    private void previewImport(Uri uri,boolean shared){
        submit("import-preview",job->{
            if(!"content".equals(uri.getScheme())||(shared&&!SuiteShare.isTrustedUri(getApplicationContext(),uri)))throw new IllegalArgumentException("등록 파일을 다시 선택하세요");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IllegalArgumentException("파일을 열 수 없습니다");byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){job.check();if(bytes.size()+n>MAX_BACKUP_BYTES)throw new IllegalArgumentException("등록 파일이 너무 큽니다");bytes.write(block,0,n);}}
            JSONObject document=new JSONObject(bytes.toString("UTF-8"));Registry.inspectImport(document);job.check();return document;
        },value->{JSONObject document=(JSONObject)value;int count=document.getJSONArray("entries").length();status.setText("가져올 항목 "+count+"명");publish("READY","import-preview",details("count",count));
            showWhenResumed(()->new AlertDialog.Builder(this).setTitle("얼굴 목록 가져오기").setMessage(count+"개 항목을 가져올까요?")
                .setPositiveButton("가져오기",(d,w)->importDocument(document,shared?uri.toString():""))
                .setNegativeButton("취소",(d,w)->{if(shared)confirmedRegistry=uri.toString();status.setText("가져오기를 취소했습니다");publish("CANCELED","import-preview",details("count",count));load();})
                .setOnCancelListener(d->{if(shared)confirmedRegistry=uri.toString();load();}).show());
        });
    }
    private void importDocument(JSONObject document,String sharedUri){
        submit("import",job->{try(Registry r=new Registry(getApplicationContext())){job.check();job.accept();JSONObject summary=r.importJson(document);summary.put("commitAcceptedBeforeCancel",job.state.get()==3);Records.event(getApplicationContext(),"gallery","import",summary);return new Rows(r.entries(true),summary);}},value->{Rows rows=(Rows)value;confirmedRegistry=sharedUri;render(rows.rows);status.setText("가져오기 완료 · 추가 "+rows.details.optInt("inserted")+"명 · 기존 "+rows.details.optInt("skipped")+"명");publish("COMPLETED","import",rows.details);});
    }
    private static JSONObject details(String key,Object value){JSONObject j=new JSONObject();try{j.put(key,value);}catch(Exception ignored){}return j;}
    private static String friendly(Throwable e){return e instanceof IllegalArgumentException&&e.getMessage()!=null?e.getMessage():"목록을 처리할 수 없습니다. 다시 시도하세요";}
    private void publish(String state,String action,JSONObject info){if(!visible())return;try{Records.write(this,"gallery-result.json",new JSONObject().put("session",owner).put("operation",operation).put("state",state).put("action",action).put("details",info));}catch(Exception ignored){}}
    private void showWhenResumed(Runnable work){if(resumed)work.run();else resumedWork=work;}
    protected void onResume(){super.onResume();resumed=true;Runnable work=resumedWork;resumedWork=null;if(work!=null&&visible())work.run();}
    protected void onPause(){resumed=false;SuiteShare.cancel(this);super.onPause();}
    protected void onDestroy(){SuiteShare.cancel(this);resumedWork=null;destroyed.set(true);operation++;Job job=active.getAndSet(null);if(job!=null)job.cancel();worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
