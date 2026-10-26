package com.yonlab.vision;

import android.app.Activity;import android.os.Bundle;import android.text.InputType;import android.widget.*;import org.json.JSONObject;
import java.nio.charset.StandardCharsets;import java.util.UUID;import java.util.Arrays;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicReference;

/** User-triggered MQTT delivery with worker ownership and broker PUBACK evidence. */
public final class CloudActivity extends YOnActivity {
 private static final Object RECORD_LOCK=new Object();private static final AtomicReference<String> LATEST=new AtomicReference<>("");
 private EditText endpoint,topic,user,password,message;private TextView status;private Button send;
 private final ThreadPoolExecutor worker=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),r->new Thread(r,"YOnCloud"),new ThreadPoolExecutor.AbortPolicy());private volatile MqttWire current;private volatile char[] activeSecret;private volatile long generation;private boolean active,busy;
 @Override public void onCreate(Bundle state){super.onCreate(state);ScrollView scroll=new ScrollView(this);LinearLayout body=Ui.column(this);scroll.addView(body);body.addView(Ui.brandHeader(this,"클라우드 연결"));body.addView(Ui.heading(this,"MQTT 메시지",26));
  endpoint=field(body,"브로커 주소 · ssl://host:8883",getPreferences(0).getString("endpoint",""),false);topic=field(body,"토픽",getPreferences(0).getString("topic","yon/events"),false);
  user=field(body,"사용자 이름","",false);password=field(body,"비밀번호","",true);message=field(body,"메시지","{\"source\":\"AXSL Vision\",\"event\":\"check\"}",false);message.setSingleLine(false);message.setMinLines(3);
  status=Ui.text(this,"연결 정보를 입력하세요",16);body.addView(status);send=Ui.button(this,"연결하고 보내기",v->publish());body.addView(send);body.addView(Ui.secondary(this,"닫기",v->finish()));setContentView(scroll);
 }
 private EditText field(LinearLayout body,String hint,String value,boolean secret){EditText field=new EditText(this);field.setHint(hint);field.setContentDescription(hint);field.setText(value);field.setTextSize(16);field.setSingleLine(true);field.setMinHeight(Ui.dp(this,52));field.setInputType(secret?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);body.addView(field,Ui.fill());return field;}
 private void publish(){if(!active||busy)return;final MqttWire.Endpoint target;final String address=endpoint.getText().toString().trim(),channel=topic.getText().toString().trim(),username=user.getText().toString();final byte[] bytes=message.getText().toString().getBytes(StandardCharsets.UTF_8);final char[] secret=password.getText().toString().toCharArray();
  try{target=MqttWire.Endpoint.parse(address);MqttWire.validateTopic(channel);if(bytes.length>60000)throw new IllegalArgumentException("메시지는 60KB 이하로 입력하세요");if(secret.length>0&&username.isEmpty())throw new IllegalArgumentException("사용자 이름을 입력하세요");}catch(IllegalArgumentException e){Arrays.fill(secret,'\0');status.setText(e.getMessage());return;}
  busy=true;send.setEnabled(false);status.setText("연결 중");getPreferences(0).edit().putString("endpoint",address).putString("topic",channel).apply();final long token=++generation;final String id=UUID.randomUUID().toString();final MqttWire wire=new MqttWire();current=wire;
  activeSecret=secret;LATEST.set(id);try{worker.execute(()->{String state="ERROR",error="";int acknowledged=0;long begin=android.os.SystemClock.elapsedRealtime();try{MqttWire.Receipt receipt=wire.publish(target,"yon-"+id.substring(0,16),username,secret,channel,bytes);acknowledged=receipt.bytes;state="ACKNOWLEDGED";}catch(Exception e){state=token==generation?"ERROR":"CANCELED";error=e.getClass().getSimpleName();}finally{Arrays.fill(secret,'\0');if(activeSecret==secret)activeSecret=null;wire.close();if(current==wire)current=null;}
   synchronized(RECORD_LOCK){try{if(LATEST.get().equals(id))Records.write(getApplicationContext(),"cloud-result.json",new JSONObject().put("sessionId",id).put("state",state).put("bytes",acknowledged).put("qos",1).put("tls",target.tls).put("elapsedMs",android.os.SystemClock.elapsedRealtime()-begin).put("errorType",error));}catch(Exception ignored){}}
   final String finalState=state;runOnUiThread(()->{if(!active||token!=generation||isFinishing()||isDestroyed())return;busy=false;send.setEnabled(true);password.setText("");status.setText("ACKNOWLEDGED".equals(finalState)?"전송 완료 · 브로커 수신 확인":"연결을 확인하고 다시 보내세요");});
  });}catch(RejectedExecutionException e){Arrays.fill(secret,'\0');wire.close();if(current==wire)current=null;busy=false;send.setEnabled(true);status.setText("잠시 후 다시 보내세요");}
 }
 @Override protected void onResume(){super.onResume();active=true;if(!busy)send.setEnabled(true);}
 @Override protected void onPause(){active=false;generation++;MqttWire wire=current;if(wire!=null)wire.close();current=null;char[] secret=activeSecret;if(secret!=null)Arrays.fill(secret,'\0');activeSecret=null;busy=false;password.setText("");send.setEnabled(true);super.onPause();}
 @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
