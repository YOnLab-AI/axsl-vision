package com.yonlab.offlinevoice;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.*;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** User-started Korean offline ASR. This module has no network client, TTS engine or vision dependency. */
public final class OfflineVoiceActivity extends VoiceActivity {
 private static final int MIC=901,WAV=902,BLUE=0xff0068f8,NAVY=0xff001850,INK=0xff071b45;
 private static final ThreadPoolExecutor WORKER=VoiceWork.EXECUTOR;
 private static final ThreadPoolExecutor AUDIO_STOP=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"YOnVoiceStop");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
 private ScrollView page;private TextView status;private EditText recognized;private Spinner duration,models;private Button start,stop,file,copy,clear,compare,save;private String modelId,group="KO",demoId="",rawText="";private JSONObject lastRow;private Uri lastInput;
 private boolean resumed,awaitingPermission,pendingMic,pendingPermissionDenied;
 private Session session;
 private Uri pendingWav;
 @Override public void onCreate(Bundle saved){super.onCreate(saved);group=VoiceNavigation.group(getIntent());String explicit=getIntent().getStringExtra("model_id");try{modelId=VoiceModelPolicy.resolve(group,explicit,VoiceModels.selected(this,group));}catch(IllegalArgumentException e){modelId=VoiceModelPolicy.defaultId(group);}demoId=getIntent().getStringExtra("demo_id");if(demoId==null)demoId="";if(saved!=null){group=saved.getString("group",group);String stored=saved.getString("modelId",modelId);if(VoiceModelPolicy.known(stored))modelId=stored;demoId=saved.getString("demoId",demoId);}build();if(saved!=null){String value=saved.getString("pendingWav");if(value!=null)pendingWav=Uri.parse(value);}else{pendingWav=VoiceNavigation.input(getIntent());if(explicit!=null&&!VoiceModelPolicy.known(explicit)){pendingWav=null;status.setText("선택한 음성 모델을 찾을 수 없습니다. 모델을 다시 선택하세요");}}}

 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(INK);t.setPadding(0,dp(4),0,dp(4));return t;}
 private Button button(String value,boolean primary,Runnable action){return VoiceUi.button(this,value,primary,action);}
 private void build(){ScrollView scroll=new ScrollView(this);page=scroll;scroll.setFillViewport(true);scroll.setBackgroundColor(0xfff7faff);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(24),dp(18),dp(24),dp(18));scroll.addView(root);
  root.addView(VoiceUi.header(this,"음성 인식",this::finish));
  TextView title=text("음성 인식",29);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(title);status=text("말하기를 시작하거나 음성 파일을 선택하세요",16);root.addView(status);
  models=new Spinner(this);java.util.List<VoiceModels.Entry> entries=VoiceModels.list(this);String[] names=new String[entries.size()];int selection=0;for(int i=0;i<entries.size();i++){names[i]=entries.get(i).title;if(entries.get(i).id.equals(modelId))selection=i;}models.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));models.setSelection(selection);models.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,android.view.View v,int pos,long n){modelId=entries.get(pos).id;if(!"ALL".equals(entries.get(pos).group))group=entries.get(pos).group;VoiceModels.select(OfflineVoiceActivity.this,modelId,group);}});models.setMinimumHeight(dp(48));root.addView(models,new LinearLayout.LayoutParams(-1,-2));
  duration=new Spinner(this);duration.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"마이크15초","마이크30초"}));duration.setMinimumHeight(dp(48));
  start=button("말하기 시작",true,this::microphone);stop=button("중지",false,()->{Session current=session;if(current!=null){current.requestStop();status.setText("인식을 마무리하는 중");stop.setEnabled(false);}});file=button("음성 파일 선택",false,()->{if(session!=null)return;try{startActivityForResult(VoiceNavigation.picker(this,group),WAV);}catch(Exception e){status.setText("음성 파일 선택 앱을 열 수 없습니다");}});root.addView(VoiceUi.actions(this,duration,start,stop,file),VoiceUi.fill());
  root.addView(text("인식한 내용",16));recognized=new EditText(this);recognized.setSaveEnabled(false);recognized.setTextSize(24);recognized.setTextColor(INK);recognized.setHint("인식한 문장을 바로 다듬어 보세요");recognized.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);recognized.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);recognized.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8192)});recognized.setGravity(Gravity.TOP);recognized.setMinHeight(dp(180));recognized.setPadding(dp(18),dp(16),dp(18),dp(16));GradientDrawable card=new GradientDrawable();card.setColor(Color.WHITE);card.setCornerRadius(dp(16));card.setStroke(dp(1),0xffd8e4f5);recognized.setBackground(card);LinearLayout.LayoutParams result=new LinearLayout.LayoutParams(-1,-2);result.topMargin=dp(8);root.addView(recognized,result);
  copy=button("내용 복사",true,()->{ClipboardManager manager=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(manager!=null&&recognized.length()>0){manager.setPrimaryClip(ClipData.newPlainText("AXSL Vision 음성",recognized.getText().toString()));status.setText("내용을 복사했습니다");}});clear=button("내용 지우기",false,()->recognized.setText(""));compare=button("모델 비교",false,()->{Intent i=new Intent(this,VoiceCompareActivity.class).putExtra("model_id",modelId).putExtra("group",group).putExtra("demo_id",demoId);if(lastInput!=null)i.setData(lastInput).putExtra("input_uri",lastInput.toString());startActivity(i);});save=button("결과 보고서",false,this::saveReport);root.addView(VoiceUi.actions(this,copy,clear,compare,save),VoiceUi.fill());
  recognized.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(Editable e){buttons();}});setContentView(scroll);buttons();
 }
 @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);String message=status.getText().toString(),visible=recognized.getText().toString();int seconds=duration.getSelectedItemPosition(),startAt=recognized.getSelectionStart(),endAt=recognized.getSelectionEnd();boolean editing=recognized.hasFocus();float position=page.getChildCount()==0?0:page.getScrollY()/(float)Math.max(1,page.getChildAt(0).getHeight()-page.getHeight());build();duration.setSelection(seconds);recognized.setText(visible);if(editing){recognized.requestFocus();recognized.setSelection(Math.max(0,Math.min(startAt,visible.length())),Math.max(0,Math.min(endAt,visible.length())));}status.setText(message);buttons();final ScrollView current=page;current.post(()->{if(page==current&&current.getChildCount()>0)current.scrollTo(0,Math.round(position*Math.max(0,current.getChildAt(0).getHeight()-current.getHeight())));});}
 private void buttons(){boolean idle=session==null;start.setEnabled(idle);file.setEnabled(idle);duration.setEnabled(idle);stop.setEnabled(!idle);models.setEnabled(idle);compare.setEnabled(idle);save.setEnabled(idle&&lastRow!=null);recognized.setEnabled(idle);copy.setEnabled(idle&&recognized.length()>0);clear.setEnabled(idle&&recognized.length()>0);}
 private void microphone(){if(!resumed||session!=null)return;if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){pendingMic=true;awaitingPermission=true;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC);return;}launch(null);}
 private void launch(Uri input){if(!resumed||session!=null)return;pendingMic=false;lastInput=input;lastRow=null;rawText="";recognized.setText("");status.setText(VoiceModels.title(this,modelId)+" 준비 중");Session next=new Session(this,input,duration.getSelectedItemPosition()==1?30:15,modelId,input==null?"":demoId);session=next;VoiceRecords.begin(next.id);next.recordAsync();buttons();try{WORKER.execute(next);}catch(RejectedExecutionException e){next.state="BUSY";next.workerDone=true;next.recordAsync();session=null;buttons();status.setText("앞선 인식을 마무리하고 있습니다. 잠시 후 다시 시작하세요");}}
 private boolean owns(Session value){return resumed&&session==value&&!value.canceled.get()&&!isFinishing()&&!isDestroyed();}
 @Override protected void onResume(){super.onResume();resumed=true;buttons();if(pendingPermissionDenied)permissionDenied();if(pendingWav!=null){Uri input=pendingWav;pendingWav=null;launch(input);}else if(pendingMic&&!awaitingPermission&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)launch(null);}
 @Override protected void onSaveInstanceState(Bundle state){if(pendingWav!=null)state.putString("pendingWav",pendingWav.toString());state.putString("modelId",modelId);state.putString("group",group);state.putString("demoId",demoId);super.onSaveInstanceState(state);}
 @Override protected void onPause(){resumed=false;if(!awaitingPermission)pendingMic=false;Session current=session;session=null;if(current!=null)current.cancel("BACKGROUND");buttons();super.onPause();}
 @Override protected void onDestroy(){Session current=session;session=null;if(current!=null)current.cancel("DESTROYED");super.onDestroy();}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request!=MIC)return;awaitingPermission=false;boolean granted=grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED;if(granted){if(resumed&&pendingMic)launch(null);}else{pendingMic=false;pendingPermissionDenied=true;if(resumed)permissionDenied();}}
 private void permissionDenied(){if(!resumed||isFinishing()||isDestroyed())return;pendingPermissionDenied=false;status.setText("마이크 권한을 허용하면 말하기를 시작할 수 있습니다");new AlertDialog.Builder(this).setTitle("마이크 권한").setMessage("음성 인식에 마이크를 사용합니다").setPositiveButton("다시 요청",(d,w)->microphone()).setNeutralButton("설정",(d,w)->{try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}}).setNegativeButton("닫기",null).show();}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=WAV||result!=RESULT_OK||data==null)return;demoId=data.getStringExtra("demo_id");if(demoId==null)demoId="";String returnedGroup=data.getStringExtra("demo_group");if("GO".equals(returnedGroup)||"KO".equals(returnedGroup)){group=returnedGroup;if(!VoiceModelPolicy.compatible(group,modelId)){modelId=VoiceModels.selected(this,group);java.util.List<VoiceModels.Entry> options=VoiceModels.list(this);for(int n=0;n<options.size();n++)if(options.get(n).id.equals(modelId))models.setSelection(n);}}VoiceModels.select(this,modelId,group);Uri uri=VoiceNavigation.input(data);if(uri==null||!"content".equals(uri.getScheme())){status.setText("파일 선택 화면에서 음성을 선택하세요");return;}if(resumed)launch(uri);else pendingWav=uri;}

 private void saveReport(){if(lastRow==null||session!=null)return;try{JSONObject row=new JSONObject(lastRow.toString()).put("editedText",recognized.getText().toString());org.json.JSONArray rows=new org.json.JSONArray().put(row);final JSONObject snapshot=new JSONObject(lastRow.toString());save.setEnabled(false);WORKER.execute(()->{try{File report=VoiceReports.save(getApplicationContext(),rows);JSONObject meta=new JSONObject(snapshot.toString());meta.remove("transcript");meta.remove("reference");meta.remove("editedText");meta.put("reportPath","exports/"+report.getName());AsrRuns.append(getApplicationContext(),meta);runOnUiThread(()->{if(!resumed)return;buttons();status.setText("보고서를 준비했습니다");try{VoiceReports.share(this,report);}catch(Exception e){status.setText("보고서 저장 완료");}});}catch(Exception e){runOnUiThread(()->{if(resumed){buttons();status.setText("보고서를 저장하지 못했습니다");}});}});}catch(Exception e){buttons();status.setText("보고서를 준비하지 못했습니다");}}

 private static final class Session implements Runnable {
  final String id=UUID.randomUUID().toString();final Context app;final WeakReference<OfflineVoiceActivity> view;final Uri input;final int seconds;final String modelId,demoId;final java.security.MessageDigest pcmDigest;String pcmSha="",sourceSha="",detectedLanguage="";JSONObject reportRow;final AtomicBoolean canceled=new AtomicBoolean(),stopRequested=new AtomicBoolean();final AtomicLong recordSequence=new AtomicLong();
  volatile AudioRecord audio;volatile String reason="";long pcmBytes,modelMs,recognitionMs,totalMs,inputPrepareMs,inputDecodeMs;int segments,recognizedChars;boolean modelClosed=true,recognizerClosed=true,audioClosed=true,workerDone,temporaryInputRemains;String state="QUEUED",stage="QUEUED",failureStage="",failureCode="",errorType="",causeErrorType="",cleanupErrorType="",temporaryInputState;Boolean prepareExtractorReleased,prepareDecoderReleased,selectedSourceClosed,providerOpenFinished;long lastUi;AudioInput.Prepared prepared;final AudioInput.Operation sourceOperation;
  Session(OfflineVoiceActivity a,Uri input,int seconds,String modelId,String demoId){this.modelId=modelId;this.demoId=demoId;try{pcmDigest=java.security.MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}app=a.getApplicationContext();view=new WeakReference<>(a);this.input=input;this.seconds=seconds;temporaryInputState=input==null?"NOT_USED":"NOT_CREATED";sourceOperation=input==null?null:new AudioInput.Operation();}
  void requestStop(){stopRequested.set(true);if(sourceOperation!=null)sourceOperation.cancel();stopAudio();}
  void cancel(String reason){this.reason=reason;canceled.set(true);if(sourceOperation!=null)sourceOperation.cancel();stopAudio();if(WORKER.remove(this)){state="CANCELED";workerDone=true;recordAsync();}}
  private void stopAudio(){AudioRecord current=audio;if(current==null)return;try{AUDIO_STOP.execute(()->{try{if(current.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)current.stop();}catch(IllegalStateException ignored){}});}catch(RejectedExecutionException ignored){}}
  private void check()throws InterruptedIOException{if(canceled.get()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Canceled");}
  private boolean preparationCanceled(){return canceled.get()||stopRequested.get()||Thread.currentThread().isInterrupted();}
  private void ui(String message,String text,boolean complete){OfflineVoiceActivity a=view.get();if(a==null)return;a.runOnUiThread(()->{OfflineVoiceActivity screen=view.get();if(screen==null||!screen.owns(this))return;if(message!=null)screen.status.setText(message);if(text!=null)screen.recognized.setText(text);if(complete){screen.rawText=text==null?"":text;screen.lastRow=reportRow;screen.session=null;screen.buttons();}});}
  private void partial(VoiceEngine recognizer,StringBuilder result)throws Exception{long now=SystemClock.elapsedRealtime();if(now-lastUi<350)return;lastUi=now;String text=new JSONObject(recognizer.getPartialResult()).optString("partial","");ui(null,result.length()==0?text:result+"\n"+text,false);}
  private void append(StringBuilder result,String json)throws Exception{String text=new JSONObject(json).optString("text","").trim();if(text.isEmpty())return;if(result.length()>0)result.append('\n');if(result.length()+text.length()>8192)throw new IOException("인식한 내용이 너무 깁니다");result.append(text);segments++;recognizedChars=result.length();}
  public void run(){long started=SystemClock.elapsedRealtime(),recognitionStarted=0;VoiceEngine recognizer=null;StringBuilder result=new StringBuilder();
   try{
    state="PREPARING";stage="INPUT_PREPARE";record();check();if(input!=null){long preparation=SystemClock.elapsedRealtime();temporaryInputState="PREPARING";prepared=AudioInput.prepare(app,input,this::preparationCanceled,sourceOperation);temporaryInputState="OWNED";inputPrepareMs=SystemClock.elapsedRealtime()-preparation;sourceSha=prepared.sourceSha256(this::preparationCanceled);}
    stage="MODEL_PREPARE";record();long load=SystemClock.elapsedRealtime();check();if(stopRequested.get())throw new InterruptedIOException("Stopped");stage="MODEL_LOAD";recognizer=VoiceEngine.open(app,modelId,this::preparationCanceled,()->canceled.get()||Thread.currentThread().isInterrupted());modelClosed=false;recognizerClosed=false;modelMs=SystemClock.elapsedRealtime()-load;

    recognitionStarted=SystemClock.elapsedRealtime();state=input==null?"LISTENING":"READING_AUDIO";stage=input==null?"MICROPHONE":"INPUT_DECODE";record();ui(input==null?"듣고 있습니다":"음성 파일을 인식 중",null,false);
    if(input==null)microphone(recognizer,result,recognitionStarted);else decode(recognizer,result);
    check();stage="FINAL_RESULT";append(result,recognizer.getFinalResult());recognitionMs=SystemClock.elapsedRealtime()-recognitionStarted;state=stopRequested.get()?"STOPPED":"COMPLETED";stage="COMPLETE";
   }catch(Exception|LinkageError|OutOfMemoryError e){
    modelClosed=VoiceCleanup.reportedClosed(e,modelClosed);recognizerClosed=VoiceCleanup.reportedClosed(e,recognizerClosed);if(!modelClosed&&recognizer==null)cleanupErrorType="NativeCleanupIncomplete";errorType=e.getClass().getSimpleName();causeErrorType=e.getCause()==null?"":e.getCause().getClass().getSimpleName();failureStage=e instanceof AudioInputException&&"RECOGNITION".equals(((AudioInputException)e).code)?"RECOGNITION":stage;failureCode=e instanceof AudioInputException?((AudioInputException)e).code:e instanceof SecurityException?"PERMISSION":e instanceof OutOfMemoryError?"MEMORY":e instanceof LinkageError?"NATIVE_RUNTIME":failureStage;
    if("MODEL_PREPARE".equals(failureStage)&&e.getMessage()!=null&&(e.getMessage().contains("hash")||e.getMessage().contains("manifest")))failureCode="MODEL_INTEGRITY";
    if(e instanceof AudioInputException){AudioInputException diagnostic=(AudioInputException)e;if(!"UNKNOWN".equals(diagnostic.temporaryInputState))temporaryInputState=diagnostic.temporaryInputState;prepareExtractorReleased=diagnostic.extractorReleased;prepareDecoderReleased=diagnostic.decoderReleased;selectedSourceClosed=diagnostic.selectedSourceClosed;providerOpenFinished=diagnostic.providerOpenFinished;}
    if("PREPARING".equals(temporaryInputState))temporaryInputState="UNKNOWN";
    temporaryInputRemains="REMAINS".equals(temporaryInputState);state=canceled.get()?"CANCELED":stopRequested.get()?"STOPPED":"FAILED";
    if(stopRequested.get()&&!canceled.get()&&recognizer!=null&&pcmBytes>0)try{append(result,recognizer.getFinalResult());}catch(Exception ignored){}
   }
   finally{
    AudioRecord current=audio;audio=null;if(current!=null){try{if(current.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)current.stop();}catch(IllegalStateException ignored){}try{current.release();audioClosed=true;}catch(RuntimeException e){cleanupErrorType=e.getClass().getSimpleName();}}
    if(recognizer!=null)try{recognizer.close();recognizerClosed=true;}catch(RuntimeException|LinkageError e){cleanupErrorType=e.getClass().getSimpleName();}
    if(recognizerClosed)modelClosed=true;
    if(prepared!=null)try{prepared.close();temporaryInputRemains=false;temporaryInputState=prepared.temporaryInputState;}catch(AudioInputException e){cleanupErrorType=e.getClass().getSimpleName();failureStage="INPUT_CLEANUP";failureCode=e.code;temporaryInputRemains=true;temporaryInputState="REMAINS";}
    if(recognizer!=null)detectedLanguage=recognizer.getDetectedLanguage();
    if(recognitionStarted>0&&recognitionMs==0)recognitionMs=SystemClock.elapsedRealtime()-recognitionStarted;pcmSha=SafeModelZip.hex(pcmDigest.digest());workerDone=true;totalMs=SystemClock.elapsedRealtime()-started;if(canceled.get())state="CANCELED";else if(!cleanupErrorType.isEmpty())state="FAILED";record();try{JSONObject meta=metric();VoiceNavigation.Reference ref=VoiceNavigation.reference(app,demoId,sourceSha);boolean verified=input!=null&&ref.verified;Double cer="COMPLETED".equals(state)?AsrTextMetrics.cer(ref.text,result.toString(),verified):null;meta.put("referenceVerified",verified).put("cer",cer==null?JSONObject.NULL:cer).put("cerPolicy",AsrTextMetrics.POLICY);AsrRuns.append(app,meta);reportRow=new JSONObject(meta.toString()).put("transcript",result.toString()).put("reference",verified?ref.text:"");}catch(Exception ignored){reportRow=null;}
    if(!canceled.get())ui("COMPLETED".equals(state)?result.length()>0?"인식 완료 · 문장을 다듬고 복사하세요":"인식된 말이 없습니다":"STOPPED".equals(state)?"인식을 중지했습니다":failureMessage(),result.toString(),true);
   }
  }
  private void accept(VoiceEngine recognizer,StringBuilder result,byte[] block,int count)throws Exception{check();if((count&1)!=0)throw new IOException("PCM alignment");pcmDigest.update(block,0,count);pcmBytes+=count;if(recognizer.acceptWaveForm(block,count)){append(result,recognizer.getResult());ui(null,result.toString(),false);}else partial(recognizer,result);}
  private void microphone(VoiceEngine recognizer,StringBuilder result,long started)throws Exception{
   if(stopRequested.get())return;
   int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(min<=0||min>64000)throw new IOException("Microphone format unsupported");
   if(app.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Microphone permission revoked");
   AudioRecord recorder=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.min(64000,Math.max(16000,min*2)));audio=recorder;audioClosed=false;if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IOException("Microphone unavailable");check();if(stopRequested.get())return;recorder.startRecording();if(recorder.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IOException("Microphone start failed");
   byte[] block=new byte[3200];while(!stopRequested.get()&&SystemClock.elapsedRealtime()-started<seconds*1000L&&pcmBytes<seconds*32000L){check();int n=recorder.read(block,0,(int)Math.min(block.length,seconds*32000L-pcmBytes),AudioRecord.READ_NON_BLOCKING);if(n<0){if(stopRequested.get())break;throw new IOException("Microphone read failed");}if(n==0){Thread.sleep(10);continue;}accept(recognizer,result,block,n);}
  }
  private void decode(VoiceEngine recognizer,StringBuilder result)throws Exception{long start=SystemClock.elapsedRealtime();try{prepared.decode((block,count)->{try{accept(recognizer,result,block,count);}catch(InterruptedIOException e){throw e;}catch(Exception e){throw new AudioInputException("RECOGNITION",e);}},this::preparationCanceled);}finally{inputDecodeMs=SystemClock.elapsedRealtime()-start;}}
  private String failureMessage(){
   switch(failureCode){
    case "TOO_LARGE":return "32MB 이하 음성 파일을 선택하세요";
    case "TOO_LONG":return "30초 이하 구간으로 나눈 음성을 선택하세요";
    case "SOURCE_PERMISSION":case "SOURCE_UNAVAILABLE":return "음성 파일을 다시 선택해 접근 권한을 연결하세요";
    case "SOURCE_READ":case "SOURCE_TIMEOUT":return "음성 파일 읽기를 마치지 못했습니다. 기기에 내려받은 파일로 다시 선택하세요";
    case "SOURCE_BUSY":return "파일 제공자의 응답을 기다리고 있습니다. 다른 파일 앱을 사용하거나 이 앱을 다시 여세요";
    case "INPUT_STORAGE":return "기기 저장 공간을 확보한 뒤 음성을 다시 선택하세요";
    case "NO_AUDIO_TRACK":return "소리가 담긴 음성 파일을 선택하세요";
    case "ENCRYPTED_AUDIO":return "암호화되지 않은 원본 음성 파일로 내보내세요";
    case "EMPTY_AUDIO":return "선택한 파일에 음성 데이터가 없습니다";
    case "TRUNCATED":case "MALFORMED":case "INVALID_PCM":return "음성 파일이 손상되었습니다. 원본 파일로 다시 선택하세요";
    case "UNSUPPORTED_FORMAT":return "이 음성 형식을 변환할 수 없습니다. PCM 또는 float WAV로 내보내세요";
    case "UNSUPPORTED_CONTAINER":case "CODEC_UNAVAILABLE":return "이 기기의 오디오 디코더가 지원하지 않는 파일입니다. WAV로 내보내세요";
    case "CODEC_ERROR":case "FORMAT_CHANGED":case "DECODE_TIMEOUT":return "음성 디코딩을 마치지 못했습니다. 다른 파일로 다시 선택하세요";
    case "INPUT_CLEANUP":case "DECODER_CLEANUP":case "SOURCE_CLEANUP":return "음성 자원 정리를 마치지 못했습니다. 화면을 닫고 다시 여세요";
    case "PERMISSION":return "설정에서 마이크 권한을 허용한 뒤 다시 시작하세요";
    case "MEMORY":return "메모리가 부족합니다. 다른 앱을 닫고 다시 시작하세요";
    case "NATIVE_RUNTIME":return "이 기기의 음성 엔진을 불러올 수 없습니다. 앱 업데이트를 확인하세요";
    case "MODEL_INTEGRITY":return "음성 모델 파일을 확인할 수 없습니다. 앱을 업데이트해 모델을 복구하세요";
    default:if(failureStage.startsWith("MODEL"))return "음성 모델을 준비하지 못했습니다. 저장 공간을 확인하고 다시 시작하세요";
     if("RECOGNIZER_INIT".equals(failureStage))return "음성 인식기를 열지 못했습니다. 화면을 닫고 다시 시작하세요";
     if("MICROPHONE".equals(failureStage))return "마이크를 열지 못했습니다. 다른 녹음 앱을 닫고 다시 시작하세요";
     return "음성 인식을 마치지 못했습니다. 다시 시작하세요";
   }
  }
  private JSONObject metric()throws Exception{return AsrRuns.metric(modelId,pcmSha,state,modelMs,recognitionMs,totalMs,pcmBytes/2,demoId).put("sourceSha256",sourceSha).put("modelLanguage",VoiceModels.find(modelId).language).put("datasetGroup",VoiceSampleMeta.group(app,demoId,sourceSha)).put("detectedLanguage",detectedLanguage).put("timingScope",input==null?"MIC_CAPTURE_AND_RECOGNITION":"FILE_DECODE_AND_RECOGNITION").put("resultCharacters",recognizedChars).put("workerDone",workerDone).put("temporaryInputState",temporaryInputState).put("selectedSourceClosed",prepared!=null&&prepared.selectedSourceClosed!=null?prepared.selectedSourceClosed:JSONObject.NULL).put("modelClosed",modelClosed).put("recognizerClosed",recognizerClosed).put("audioClosed",audioClosed).put("failureCode",failureCode);}
  private void record(){record(false);}private void recordAsync(){record(true);}
  private void record(boolean async){try{JSONObject value=new JSONObject().put("sessionId",id).put("recordSequence",recordSequence.incrementAndGet()).put("state",state).put("stage",stage).put("failureStage",failureStage).put("failureCode",failureCode).put("source",input==null?"MICROPHONE":"AUDIO_FILE").put("modelLanguage",VoiceModels.find(modelId).language).put("datasetGroup",VoiceSampleMeta.group(app,demoId,sourceSha)).put("detectedLanguage",detectedLanguage).put("language",VoiceModels.find(modelId).language).put("model",modelId).put("modelId",modelId).put("modelZipSha256",VoiceAssetStore.archiveSha(modelId)).put("modelManifestSha256",VoiceAssetStore.manifestSha(modelId)).put("modelWeightsFingerprint",VoiceAssetStore.weightsFingerprint(modelId)).put("sdk",VoiceModels.find(modelId).engine.equals("vosk")?"vosk-android0.3.75":"sherpa-onnx2.0.0").put("inputSha256",pcmSha).put("sourceSha256",sourceSha).put("demoId",demoId).put("sampleRate",16000).put("channels",1).put("bits",16).put("maxMicrophoneSeconds",seconds).put("maxFileSeconds",PcmAudio.MAX_SECONDS).put("maxFileBytes",PcmAudio.MAX_FILE_BYTES).put("pcmBytes",pcmBytes).put("segments",segments).put("recognizedCharacters",recognizedChars).put("inputPrepareMs",inputPrepareMs).put("inputDecodeMs",inputDecodeMs).put("modelMs",modelMs).put("recognitionMs",recognitionMs).put("totalMs",totalMs).put("modelClosed",modelClosed).put("recognizerClosed",recognizerClosed).put("audioClosed",audioClosed).put("workerDone",workerDone).put("reason",reason).put("errorType",errorType).put("causeErrorType",causeErrorType).put("cleanupErrorType",cleanupErrorType).put("rawAudioStored","UNKNOWN".equals(temporaryInputState)?JSONObject.NULL:temporaryInputRemains||(prepared!=null&&prepared.tempExists())).put("transcriptJournaled",false);
   value.put("temporaryInputState",temporaryInputState).put("temporaryInputDeleted",prepared!=null?prepared.tempDeleted:"DELETED".equals(temporaryInputState)?true:"REMAINS".equals(temporaryInputState)?false:JSONObject.NULL).put("decoderReleased",prepared!=null?prepared.decoderReleased:prepareDecoderReleased==null?JSONObject.NULL:prepareDecoderReleased).put("extractorReleased",prepared!=null?prepared.extractorReleased:prepareExtractorReleased==null?JSONObject.NULL:prepareExtractorReleased);
   value.put("selectedSourceClosed",prepared!=null?prepared.selectedSourceClosed:selectedSourceClosed==null?JSONObject.NULL:selectedSourceClosed).put("providerOpenFinished",prepared!=null?prepared.providerOpenFinished:providerOpenFinished==null?JSONObject.NULL:providerOpenFinished);
   if(prepared!=null)value.put("inputBytes",prepared.inputBytes).put("inputMime",prepared.mime).put("decodedEncoding",prepared.encoding).put("decodedSampleRate",prepared.sampleRate).put("decodedChannels",prepared.channels).put("decodedBits",prepared.bits).put("sourceFrames",prepared.sourceFrames).put("convertedSamples",prepared.outputSamples).put("clippedFrames",prepared.clippedFrames).put("sourceDurationMs",prepared.durationMs);
   if(async)VoiceRecords.later(app,id,value);else VoiceRecords.write(app,id,value);}catch(Exception ignored){}}
 }
}
