package com.yonlab.vision;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Actual Android Korean SpeechRecognizer and TTS, with text input when a service is absent. */
public final class VoiceActivity extends YOnActivity {
    private static final int MICROPHONE=831;
    private static final ThreadPoolExecutor JOURNAL=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"yon-voice-journal");t.setDaemon(true);return t;});
    private static final AtomicReference<String> newestSession=new AtomicReference<>("");
    private final String session=UUID.randomUUID().toString();
    private final Handler main=new Handler(Looper.getMainLooper());
    private static final AtomicReference<JSONObject> pendingRecord=new AtomicReference<>();
    private static final AtomicBoolean recordScheduled=new AtomicBoolean();
    private TextView speechStatus,ttsStatus;
    private EditText text;
    private Button listen,read;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean active,closed,recognitionAvailable,onDeviceRecognizer,ttsReady,ttsInitialized,koreanTts,listening,ttsStarted,ttsDone;
    private int generation,recognitionId,recognizedChars,speakChars,speechError,ttsError;
    private String recognitionState="IDLE",ttsState="IDLE",utterance="",engine="";
    private Runnable speechTimeout;

    @Override public void onCreate(Bundle state){super.onCreate(state);newestSession.set(session);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Ui.BACK);
        LinearLayout header=Ui.brandHeader(this,"음성");header.addView(Ui.secondary(this,"닫기",v->finish()),new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,48)));root.addView(header,Ui.fill());
        ScrollView scroll=new ScrollView(this);LinearLayout body=Ui.column(this);
        LinearLayout speech=Ui.panel(this);speech.addView(Ui.heading(this,"말로 입력",24));speechStatus=Ui.text(this,"준비",16);speech.addView(speechStatus,Ui.fill());
        listen=Ui.button(this,"듣기 시작",v->startRecognition());speech.addView(listen,space());speech.addView(Ui.secondary(this,"중지",v->stopRecognition("STOPPED",true)),space());
        Intent offline=new Intent().setClassName(this,"com.yonlab.offlinevoice.OfflineVoiceActivity");if(offline.resolveActivity(getPackageManager())!=null)speech.addView(Ui.button(this,"한국어로 말하기",v->startActivity(offline)),space());
        speech.addView(Ui.secondary(this,"음성 설정",v->settings(Settings.ACTION_VOICE_INPUT_SETTINGS)),space());body.addView(speech,Ui.fill());
        LinearLayout reading=Ui.panel(this);LinearLayout.LayoutParams panel=Ui.fill();panel.topMargin=Ui.dp(this,16);body.addView(reading,panel);
        reading.addView(Ui.heading(this,"문장 읽기",24));text=new EditText(this);text.setHint("문장을 입력하세요");text.setText("안녕하세요. AXSL Vision입니다.");text.setTextSize(17);text.setMinLines(3);text.setMaxLines(8);text.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3000)});text.setContentDescription("인식한 문장 또는 읽을 문장");
        if(state!=null)text.setText(state.getString("text",text.getText().toString()));reading.addView(text,Ui.fill());
        ttsStatus=Ui.text(this,"준비 중",16);reading.addView(ttsStatus,Ui.fill());read=Ui.button(this,"읽기",v->speak());read.setEnabled(false);reading.addView(read,space());
        reading.addView(Ui.secondary(this,"읽기 중지",v->stopReading("STOPPED")),space());reading.addView(Ui.secondary(this,"음성 데이터",v->settings(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)),space());
        scroll.addView(body,Ui.fill());root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    private LinearLayout.LayoutParams space(){LinearLayout.LayoutParams lp=Ui.fill();lp.topMargin=Ui.dp(this,10);return lp;}
    @Override protected void onSaveInstanceState(Bundle state){state.putString("text",text.getText().toString());super.onSaveInstanceState(state);}
    @Override protected void onResume(){super.onResume();active=true;recognitionAvailable=SpeechRecognizer.isRecognitionAvailable(this);listen.setEnabled(recognitionAvailable);
        if(!recognitionAvailable){recognitionState="UNAVAILABLE";speechStatus.setText("음성 인식 서비스가 없습니다");}else if(!listening){recognitionState="READY";speechStatus.setText("준비");}
        initializeTts();record();
    }
    @Override protected void onPause(){active=false;generation++;stopRecognition("CANCELED",false);stopReading("STOPPED");
        TextToSpeech prior=tts;tts=null;ttsReady=false;ttsInitialized=false;read.setEnabled(false);if(prior!=null)try{prior.shutdown();}catch(Exception ignored){}record();super.onPause();}
    @Override protected void onDestroy(){closed=true;removeTimeout();record();super.onDestroy();}
    private void initializeTts(){
        if(tts!=null)return;read.setEnabled(false);int owner=++generation;WeakReference<VoiceActivity> weak=new WeakReference<>(this);
        try{tts=new TextToSpeech(getApplicationContext(),status->main.post(()->{VoiceActivity activity=weak.get();if(activity!=null&&activity.active&&!activity.closed&&activity.generation==owner)activity.ttsInitialized(owner,status);}));}
        catch(Exception e){ttsState="UNAVAILABLE";ttsStatus.setText("음성 읽기 서비스를 열 수 없습니다");record();}
    }
    private void ttsInitialized(int owner,int code){
        if(tts==null||!active||owner!=generation)return;ttsInitialized=code==TextToSpeech.SUCCESS;
        try{
            if(!ttsInitialized)throw new IllegalStateException("tts init");engine=tts.getDefaultEngine();int language=tts.isLanguageAvailable(Locale.KOREA);
            koreanTts=language>=TextToSpeech.LANG_AVAILABLE;if(!koreanTts){ttsState="LANGUAGE_UNAVAILABLE";ttsStatus.setText("한국어 음성 데이터를 확인하세요");read.setEnabled(false);record();return;}
            if(tts.setLanguage(Locale.KOREA)<TextToSpeech.LANG_AVAILABLE)throw new IllegalStateException("tts language");
            tts.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                public void onStart(String id){main.post(()->ttsProgress(owner,id,"SPEAKING",0));}
                public void onDone(String id){main.post(()->ttsProgress(owner,id,"COMPLETED",0));}
                @Deprecated public void onError(String id){main.post(()->ttsProgress(owner,id,"ERROR",TextToSpeech.ERROR));}
                public void onError(String id,int error){main.post(()->ttsProgress(owner,id,"ERROR",error));}
            });ttsReady=true;ttsState="READY";ttsStatus.setText("준비");read.setEnabled(true);record();
        }catch(Exception e){ttsReady=false;ttsState="UNAVAILABLE";ttsStatus.setText("음성 읽기 서비스를 열 수 없습니다");read.setEnabled(false);record();}
    }
    private void ttsProgress(int owner,String id,String state,int error){if(!active||closed||owner!=generation||!id.equals(utterance))return;ttsState=state;ttsError=error;
        if("SPEAKING".equals(state)){ttsStarted=true;ttsStatus.setText("읽는 중");}else if("COMPLETED".equals(state)){ttsDone=true;ttsStatus.setText("읽기 완료");}else ttsStatus.setText("문장을 읽을 수 없습니다");record();}
    private void speak(){if(!active||tts==null||!ttsReady)return;String value=text.getText().toString().trim();if(value.isEmpty()){text.setError("문장을 입력하세요");return;}
        if(value.length()>TextToSpeech.getMaxSpeechInputLength()){text.setError("문장을 줄여 주세요");return;}stopRecognition("STOPPED",false);stopReading("STOPPED");
        utterance=UUID.randomUUID().toString();ttsStarted=false;ttsDone=false;ttsError=0;speakChars=value.length();ttsState="PREPARING";ttsStatus.setText("준비 중");
        try{if(tts.speak(value,TextToSpeech.QUEUE_FLUSH,new Bundle(),utterance)!=TextToSpeech.SUCCESS){ttsState="ERROR";ttsStatus.setText("문장을 읽을 수 없습니다");}}catch(Exception e){ttsState="ERROR";ttsStatus.setText("문장을 읽을 수 없습니다");}record();
    }
    private void stopReading(String state){utterance="";if(tts!=null)try{tts.stop();}catch(Exception ignored){}if(ttsReady){ttsState=state;ttsStatus.setText("중지");}record();}
    private void startRecognition(){
        if(!active)return;if(!SpeechRecognizer.isRecognitionAvailable(this)){recognitionAvailable=false;listen.setEnabled(false);recognitionState="UNAVAILABLE";speechStatus.setText("음성 인식 서비스가 없습니다");record();return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MICROPHONE);return;}
        stopReading("STOPPED");stopRecognition("STOPPED",false);int owner=++recognitionId;speechError=0;recognitionState="PREPARING";speechStatus.setText("준비 중");
        try{
            onDeviceRecognizer=android.os.Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
            if(android.os.Build.VERSION.SDK_INT>=31 && onDeviceRecognizer) recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);else recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizer.setRecognitionListener(new RecognitionListener(){
                private boolean valid(){return active&&!closed&&owner==recognitionId;}
                public void onReadyForSpeech(Bundle params){if(valid()){recognitionState="LISTENING";speechStatus.setText("말씀하세요");record();}}
                public void onBeginningOfSpeech(){if(valid()){recognitionState="LISTENING";speechStatus.setText("듣는 중");}}
                public void onRmsChanged(float rms){}public void onBufferReceived(byte[] buffer){}
                public void onEndOfSpeech(){if(valid()){recognitionState="PROCESSING";speechStatus.setText("분석 중");record();}}
                public void onError(int error){if(!valid())return;speechError=error;recognitionState="ERROR";speechStatus.setText(speechMessage(error));finishRecognition();record();}
                public void onResults(Bundle results){if(!valid())return;ArrayList<String> values=results==null?null:results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if(values==null||values.isEmpty()||values.get(0)==null||values.get(0).trim().isEmpty()){recognitionState="NO_MATCH";speechStatus.setText("다시 말씀해 주세요");}
                    else{String value=values.get(0);recognizedChars=Math.min(3000,value.length());text.setText(value.substring(0,recognizedChars));recognitionState="RECOGNIZED";speechStatus.setText("인식 완료");}finishRecognition();record();}
                public void onPartialResults(Bundle partialResults){}public void onEvent(int eventType,Bundle params){}
            });
            Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR").putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false).putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
            listening=true;listen.setEnabled(false);text.setEnabled(false);recognizer.startListening(intent);speechTimeout=()->{if(active&&owner==recognitionId&&listening){stopRecognition("TIMEOUT",false);speechStatus.setText("응답 시간이 초과됐습니다");record();}};main.postDelayed(speechTimeout,15000);record();
        }catch(Exception e){recognitionState="ERROR";speechStatus.setText("음성 인식을 시작할 수 없습니다");finishRecognition();record();}
    }
    private void finishRecognition(){removeTimeout();listening=false;recognitionId++;SpeechRecognizer prior=recognizer;recognizer=null;if(prior!=null)try{prior.destroy();}catch(Exception ignored){}text.setEnabled(true);listen.setEnabled(active&&recognitionAvailable);}
    private void stopRecognition(String state,boolean show){boolean was=listening;recognitionId++;SpeechRecognizer prior=recognizer;recognizer=null;removeTimeout();listening=false;
        if(prior!=null){try{prior.cancel();}catch(Exception ignored){}try{prior.destroy();}catch(Exception ignored){}}text.setEnabled(true);listen.setEnabled(active&&recognitionAvailable);
        if(was){recognitionState=state;if(show)speechStatus.setText("중지");record();}}
    private void removeTimeout(){if(speechTimeout!=null){main.removeCallbacks(speechTimeout);speechTimeout=null;}}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code!=MICROPHONE)return;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){if(active)startRecognition();}
        else{recognitionState="PERMISSION_DENIED";speechStatus.setText("마이크 권한을 허용하세요");record();if(active&&!closed&&!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle("마이크 권한이 필요합니다")
            .setNegativeButton("닫기",null).setPositiveButton("앱 설정",(dialog,which)->{try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception e){Ui.error(this,"앱 설정을 열 수 없습니다",e);}}).show();}}
    private void settings(String action){try{Intent intent=new Intent(action);if(intent.resolveActivity(getPackageManager())==null)intent=new Intent(Settings.ACTION_SETTINGS);startActivity(intent);}catch(Exception e){Ui.error(this,"음성 설정을 열 수 없습니다",e);}}
    private static String speechMessage(int error){switch(error){case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:return "마이크 권한을 허용하세요";case SpeechRecognizer.ERROR_AUDIO:return "마이크를 확인하세요";
        case SpeechRecognizer.ERROR_NETWORK:case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:return "음성 인식 연결을 확인하세요";case SpeechRecognizer.ERROR_NO_MATCH:case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:return "다시 말씀해 주세요";
        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:return "잠시 후 다시 시도하세요";default:return "음성 인식을 다시 시작하세요";}}
    private void record(){try{JSONObject value=new JSONObject().put("sessionId",session).put("recognitionAvailable",recognitionAvailable).put("onDeviceRecognizer",onDeviceRecognizer).put("preferOffline",true).put("ttsInitialized",ttsInitialized).put("koreanTtsAvailable",koreanTts)
        .put("recognitionState",recognitionState).put("ttsState",ttsState).put("recognizedChars",recognizedChars).put("speakChars",speakChars).put("ttsOnStart",ttsStarted).put("ttsOnDone",ttsDone)
        .put("ttsEngine",engine==null?"":engine).put("speechErrorCode",speechError).put("ttsErrorCode",ttsError).put("closed",closed);pendingRecord.set(value);scheduleRecord();}catch(Exception ignored){}}
    private void scheduleRecord(){if(!recordScheduled.compareAndSet(false,true))return;try{JOURNAL.execute(()->{while(true){JSONObject value=pendingRecord.getAndSet(null);if(value!=null&&newestSession.get().equals(value.optString("sessionId")))Records.write(getApplicationContext(),"voice-result.json",value);
        recordScheduled.set(false);if(pendingRecord.get()!=null&&recordScheduled.compareAndSet(false,true))continue;return;}});}catch(java.util.concurrent.RejectedExecutionException e){recordScheduled.set(false);android.util.Log.w("YOn","voice_journal_busy");}}
}
