package com.yonlab.vision;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Two selected faces, real SFace embeddings and their unmodified cosine similarity. */
public class CompareActivity extends YOnActivity {
    private static final int PICK_A=821,PICK_B=822;
    private static final AtomicLong NEWEST=new AtomicLong(android.os.SystemClock.elapsedRealtimeNanos());
    private final long owner=NEWEST.incrementAndGet();
    private static final CompareJournalQueue<JournalSnapshot> JOURNAL=new CompareJournalQueue<>(32);
    private static final AtomicBoolean JOURNAL_SCHEDULED=new AtomicBoolean();
    private static final ThreadPoolExecutor JOURNAL_IO=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1),r->{Thread t=new Thread(r,"YOnCompareJournal");t.setDaemon(true);return t;});
    private static final class JournalSnapshot {
        final android.content.Context app;final String json;
        JournalSnapshot(android.content.Context app,String json){this.app=app;this.json=json;}
    }
    private static void scheduleJournal(){if(!JOURNAL_SCHEDULED.compareAndSet(false,true))return;try{JOURNAL_IO.execute(CompareActivity::drainJournal);}catch(java.util.concurrent.RejectedExecutionException busy){JOURNAL_SCHEDULED.set(false);android.util.Log.w("YOn","compare_journal_busy");}}
    private static void drainJournal(){try{for(;;){
        CompareJournalQueue.Item<JournalSnapshot> event=JOURNAL.pollEvent();
        if(event!=null&&event.owner==NEWEST.get()&&JOURNAL.isCurrent(event.owner))try{JSONObject detail=new JSONObject(event.value.json).put("journalRevision",event.revision);Records.event(event.value.app,"compare","COMPLETED",detail);}catch(Exception error){android.util.Log.w("YOn","compare_event_failed",error);}
        CompareJournalQueue.Item<JournalSnapshot> state=JOURNAL.takeState();
        if(state!=null&&state.owner==NEWEST.get()&&JOURNAL.isCurrent(state.owner))try{JSONObject value=new JSONObject(state.value.json).put("journalRevision",state.revision).put("queuedEventsDropped",JOURNAL.droppedEvents()).put("staleQueuedDiscarded",JOURNAL.staleQueuedDiscarded());if(!Records.write(state.value.app,"compare-result.json",value))android.util.Log.w("YOn","compare_journal_failed");}catch(Exception error){android.util.Log.w("YOn","compare_journal_failed",error);}
        if(event==null&&state==null)return;
    }}finally{JOURNAL_SCHEDULED.set(false);if(JOURNAL.hasWork())scheduleJournal();}}

    private final Handler main=new Handler(Looper.getMainLooper());
    private final AtomicBoolean destroyed=new AtomicBoolean();
    private final AtomicInteger pending=new AtomicInteger();
    private final AtomicReference<FaceEngine> running=new AtomicReference<>();
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(2),r->new Thread(r,"YOnCompare"));
    private final Slot[] slots={new Slot(),new Slot()};
    private String modelId;private boolean autoCompare;
    private TextView result;private EditText threshold;private Button compare;
    private long operation;
    private static final class Slot {
        Uri uri;Bitmap bitmap;List<Observations.Face> faces=Collections.emptyList();int selected=-1,generation;
        VisionCanvas canvas;Spinner spinner;TextView summary;Button pick;
    }
    public void onCreate(Bundle saved){
        super.onCreate(saved);modelId=OneModels.resolve(this,"compare",getIntent().getStringExtra("model_id"));build(saved==null?Float.toString(Registry.DEFAULT_THRESHOLD):saved.getString("threshold",Float.toString(Registry.DEFAULT_THRESHOLD)),"두 사진에서 얼굴을 선택하세요");JOURNAL.claim(owner);publish("IDLE",new JSONObject());
        if(saved!=null){for(int i=0;i<2;i++){String value=saved.getString("uri"+i);slots[i].selected=saved.getInt("selected"+i,-1);if(value!=null)load(i,Uri.parse(value));}}
        else{
            Uri first=ImageIO.imageUri(getIntent());if(first!=null)load(0,first);
            ArrayList<Uri> shared=null;try{shared=getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);}catch(RuntimeException ignored){}if(shared!=null){if(first==null&&!shared.isEmpty())load(0,shared.get(0));if(shared.size()>1)load(1,shared.get(1));}String demo=getIntent().getStringExtra("demo_id");if(demo==null)demo=getIntent().getStringExtra("pair_demo_id");if(demo!=null&&!demo.isEmpty())loadPair(demo);
        }
    }
    private void build(String criterion,String message) {
        LinearLayout root=Ui.column(this);root.addView(Ui.brandHeader(this,"얼굴 비교"));boolean wide=getResources().getConfiguration().screenWidthDp>=900*Math.max(1f,getResources().getConfiguration().fontScale);
        ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);scroll.addView(page);
        LinearLayout photos=new LinearLayout(this);boolean narrow=Ui.narrow(this);photos.setOrientation(narrow?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        for(int i=0;i<2;i++){final int index=i;Slot slot=slots[i];LinearLayout panel=Ui.panel(this);panel.addView(Ui.heading(this,i==0?"첫 번째 사진":"두 번째 사진",20));
            String priorSummary=slot.summary==null?"사진을 선택하세요":slot.summary.getText().toString();slot.pick=Ui.secondary(this,"사진 선택",v->pick(index));panel.addView(slot.pick);slot.canvas=new VisionCanvas(this);panel.addView(slot.canvas,new LinearLayout.LayoutParams(-1,Ui.dp(this,220)));
            slot.summary=Ui.text(this,priorSummary,14);panel.addView(slot.summary);slot.spinner=new Spinner(this);panel.addView(slot.spinner);slot.spinner.setEnabled(false);
            slot.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> a,View v,int position,long id){if(position>=0&&position<slot.faces.size()){boolean changed=slot.selected!=position;slot.selected=position;slot.canvas.select(position);if(changed)result.setText("얼굴 비교를 눌러 결과를 확인하세요");}}public void onNothingSelected(AdapterView<?> a){}});
            LinearLayout.LayoutParams lp=narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1);if(i==0){if(narrow)lp.bottomMargin=Ui.dp(this,12);else lp.rightMargin=Ui.dp(this,12);}photos.addView(panel,lp);
        }
        page.addView(photos);LinearLayout controls=Ui.panel(this);controls.addView(Ui.text(this,"비교 기준",14));threshold=new EditText(this);threshold.setSingleLine();threshold.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);threshold.setText(criterion);controls.addView(threshold);
        compare=Ui.button(this,"얼굴 비교",v->compare());controls.addView(compare);result=Ui.heading(this,"두 사진에서 얼굴을 선택하세요",20);controls.addView(result);LinearLayout.LayoutParams controlsLp=new LinearLayout.LayoutParams(-1,-2);controlsLp.topMargin=Ui.dp(this,14);if(!wide)page.addView(controls,controlsLp);
        if(wide){LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.HORIZONTAL);body.addView(scroll,new LinearLayout.LayoutParams(0,-1,3));ScrollView side=new ScrollView(this);side.addView(controls,new ScrollView.LayoutParams(-1,-2));LinearLayout.LayoutParams sideLp=new LinearLayout.LayoutParams(0,-1,1.2f);sideLp.leftMargin=Ui.dp(this,14);body.addView(side,sideLp);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));}else root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));root.addView(Ui.secondary(this,"닫기",v->finish()));setContentView(root);root.setFocusableInTouchMode(true);root.requestFocus();threshold.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);threshold.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){android.view.inputmethod.InputMethodManager ime=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);ime.hideSoftInputFromWindow(threshold.getWindowToken(),0);threshold.clearFocus();root.requestFocus();return true;}return false;});updateButtons();
        for(Slot slot:slots){
            slot.canvas.show(slot.bitmap,slot.faces,null);slot.canvas.select(slot.selected);
            int chosen=slot.selected;slot.spinner.setOnItemSelectedListener(null);
            String[] labels=new String[Math.max(1,slot.faces.size())];for(int i=0;i<labels.length;i++)labels[i]=slot.faces.isEmpty()?"얼굴 없음":"얼굴 "+(i+1);
            slot.spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
            slot.spinner.setEnabled(!slot.faces.isEmpty()&&pending.get()==0);if(chosen>=0)slot.spinner.setSelection(chosen,false);
            slot.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(position>=0&&position<slot.faces.size()){boolean changed=slot.selected!=position;slot.selected=position;slot.canvas.select(position);if(changed)result.setText("얼굴 비교를 눌러 결과를 확인하세요");}}public void onNothingSelected(AdapterView<?> parent){}});
        }
        result.setText(message);updateButtons();
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);build(threshold.getText().toString(),result.getText().toString());}
    private boolean visible(){return !destroyed.get()&&!isFinishing()&&!isDestroyed()&&owner==NEWEST.get();}
    private void pick(int index){if(pending.get()>0)return;Intent intent=OneInputs.picker(this,"image","compare");try{startActivityForResult(intent,index==0?PICK_A:PICK_B);}catch(Exception e){result.setText("사진 선택 앱을 열 수 없습니다");}}
    protected void onActivityResult(int request,int code,Intent data){
        super.onActivityResult(request,code,data);if(code!=RESULT_OK||data==null||data.getData()==null||(request!=PICK_A&&request!=PICK_B))return;
        Uri uri=data.getData();String demo=data.getStringExtra("demo_id");if(demo!=null&&!demo.isEmpty()){load(0,uri);loadPair(demo);return;}autoCompare=false;try{if((data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0) getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}load(request==PICK_A?0:1,uri);
    }
    private void loadPair(String demo){autoCompare=true;try{worker.execute(()->{try{Uri second=DemoStore.pairUri(getApplicationContext(),demo);main.post(()->{if(!visible())return;if(second!=null)load(1,second);else{autoCompare=false;result.setText("두 번째 사진을 선택하세요");}});}catch(Exception e){main.post(()->{if(visible()){autoCompare=false;result.setText("두 번째 사진을 선택하세요");}});}});}catch(java.util.concurrent.RejectedExecutionException e){autoCompare=false;}}
    private void load(int index,Uri uri){
        if(!visible())return;Slot slot=slots[index];int token=++slot.generation,remembered=slot.selected;slot.uri=uri;slot.faces=Collections.emptyList();slot.selected=-1;slot.spinner.setEnabled(false);slot.canvas.show(slot.bitmap,null,null);pending.incrementAndGet();updateButtons();slot.summary.setText("얼굴 분석 중");operation++;publish("RUNNING",new JSONObject());
        worker.execute(()->{
            Bitmap image=null;FaceEngine engine=null;List<Observations.Face> faces=null;Throwable error=null;
            try{if(destroyed.get()||slot.generation!=token)return;image=ImageIO.load(getApplicationContext(),uri,1280);engine=new FaceEngine(getApplicationContext(),"compare",modelId);running.set(engine);faces=engine.analyze(image,true);}
            catch(Exception|LinkageError|OutOfMemoryError e){error=e;}
            finally{if(engine!=null){running.compareAndSet(engine,null);try{engine.close();}catch(Exception e){if(error==null)error=e;}}}
            final Bitmap completed=image;final List<Observations.Face> observations=faces;final Throwable failure=error;
            main.post(()->{
                pending.decrementAndGet();if(!visible()||slot.generation!=token){if(completed!=null)completed.recycle();return;}updateButtons();
                if(failure!=null){if(completed!=null)completed.recycle();slot.summary.setText("사진을 분석할 수 없습니다");result.setText("다른 사진을 선택하거나 다시 시도하세요");publish("FAILED",details("error",failure.getClass().getSimpleName()));return;}
                if(completed==null||observations==null)return;Bitmap old=slot.bitmap;slot.bitmap=completed;slot.faces=observations;slot.canvas.show(completed,observations,null);if(old!=null&&old!=completed)old.recycle();
                String[] labels=new String[Math.max(1,observations.size())];for(int i=0;i<labels.length;i++)labels[i]=observations.isEmpty()?"얼굴 없음":"얼굴 "+(i+1);
                slot.spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));slot.spinner.setEnabled(!observations.isEmpty());slot.selected=observations.isEmpty()?-1:Math.max(0,Math.min(remembered,observations.size()-1));if(slot.selected>=0){slot.spinner.setSelection(slot.selected);slot.canvas.select(slot.selected);}
                slot.summary.setText(completed.getWidth()+" × "+completed.getHeight()+" · 얼굴 "+observations.size()+"명");result.setText(observations.isEmpty()?"얼굴이 보이는 사진을 선택하세요":"얼굴 비교를 눌러 결과를 확인하세요");publish("READY",details("loadedSlot",index));updateButtons();if(autoCompare&&pending.get()==0&&selected(slots[0])!=null&&selected(slots[1])!=null){autoCompare=false;compare();}
            });
        });
    }
    private void updateButtons(){boolean idle=pending.get()==0;for(Slot s:slots){if(s.pick!=null)s.pick.setEnabled(idle);if(s.spinner!=null)s.spinner.setEnabled(idle&&!s.faces.isEmpty());}if(compare!=null)compare.setEnabled(idle&&selected(slots[0])!=null&&selected(slots[1])!=null);}
    private static Observations.Face selected(Slot slot){return slot.selected>=0&&slot.selected<slot.faces.size()?slot.faces.get(slot.selected):null;}
    private void compare(){
        Observations.Face first=selected(slots[0]),second=selected(slots[1]);if(first==null||second==null){result.setText("각 사진에서 얼굴을 선택하세요");return;}
        try{
            float criterion=Float.parseFloat(threshold.getText().toString().trim());if(Float.isNaN(criterion)||Float.isInfinite(criterion)||criterion < -1||criterion>1)throw new IllegalArgumentException();
            float similarity=Registry.cosine(first.embedding,second.embedding);boolean matched=similarity>=criterion;
            result.setText(String.format(Locale.KOREA,"유사도 %.4f\n비교 기준 %.3f · %s",similarity,criterion,matched?"기준 이상":"기준 미만"));operation++;
            JSONObject record=details("similarity",similarity).put("threshold",criterion).put("matched",matched).put("modelId",modelId).put("feature","compare");publish("COMPLETED",record);enqueueComparisonEvent(record);
        }catch(Exception e){result.setText("비교 기준은 -1부터 1까지 입력하세요");publish("FAILED",details("error",e.getClass().getSimpleName()));}
    }
    private static JSONObject details(String key,Object value){JSONObject j=new JSONObject();try{j.put(key,value);}catch(Exception ignored){}return j;}
    private void publish(String state,JSONObject detail){if(!visible())return;try{
        JSONObject record=new JSONObject().put("state",state).put("modelId",modelId).put("session",owner).put("operation",operation).put("pending",pending.get()).put("modelSignature",Registry.MODEL_SIGNATURE).put("faceCountA",slots[0].faces.size()).put("faceCountB",slots[1].faces.size()).put("selectedA",slots[0].selected).put("selectedB",slots[1].selected).put("details",detail);if(JOURNAL.state(owner,new JournalSnapshot(getApplicationContext(),record.toString())))scheduleJournal();
    }catch(Exception ignored){}}
    private void enqueueComparisonEvent(JSONObject detail){if(!visible())return;try{JSONObject event=new JSONObject(detail.toString()).put("session",owner).put("operation",operation).put("occurredAt",System.currentTimeMillis());if(!JOURNAL.event(owner,new JournalSnapshot(getApplicationContext(),event.toString())))publish("COMPLETED",detail);scheduleJournal();}catch(Exception error){android.util.Log.w("YOn","compare_event_snapshot_failed",error);}}
    protected void onSaveInstanceState(Bundle saved){super.onSaveInstanceState(saved);saved.putString("threshold",threshold.getText().toString());for(int i=0;i<slots.length;i++){Slot slot=slots[i];if(slot.uri!=null)saved.putString("uri"+i,slot.uri.toString());saved.putInt("selected"+i,slot.selected);}}
    protected void onDestroy(){
        destroyed.set(true);operation++;for(Slot slot:slots){slot.generation++;if(slot.canvas!=null)slot.canvas.show(null,null,null);if(slot.bitmap!=null){slot.bitmap.recycle();slot.bitmap=null;}}FaceEngine engine=running.getAndSet(null);if(engine!=null)engine.close();worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();
    }
}
