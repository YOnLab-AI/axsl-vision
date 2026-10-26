package com.yonlab.vision;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.List;

/** User-visible, actual same-signer request/ack round trip. */
public final class IpcActivity extends YOnActivity {
    private Spinner targets;private EditText payload;private Button send;private TextView status;private List<String> roles;private SuiteIPC.Handle handle;private boolean active;
    @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Ui.BACK);
        LinearLayout header=Ui.brandHeader(this,"앱 연결");header.addView(Ui.secondary(this,"닫기",v->finish()),new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,48)));root.addView(header,Ui.fill());
        ScrollView scroll=new ScrollView(this);LinearLayout body=Ui.column(this),panel=Ui.panel(this);panel.addView(Ui.heading(this,"AXSL Vision 앱 연결",24));
        panel.addView(Ui.text(this,"연결할 앱",15));targets=new Spinner(this);targets.setMinimumHeight(Ui.dp(this,48));targets.setContentDescription("연결 대상 앱");panel.addView(targets,Ui.fill());
        payload=new EditText(this);payload.setText("안녕하세요, AXSL Vision");payload.setHint("전달할 문장");payload.setMinHeight(Ui.dp(this,64));payload.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2048)});panel.addView(payload,Ui.fill());
        send=Ui.button(this,"연결 확인",v->ping());LinearLayout.LayoutParams lp=Ui.fill();lp.topMargin=Ui.dp(this,12);panel.addView(send,lp);
        status=Ui.text(this,"",16);panel.addView(status,Ui.fill());body.addView(panel,Ui.fill());scroll.addView(body,Ui.fill());root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    @Override protected void onResume(){super.onResume();active=true;roles=SuiteIPC.targets(this);String[] labels=new String[roles.size()];for(int i=0;i<labels.length;i++)labels[i]=SuiteConfig.displayName(roles.get(i));
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);targets.setAdapter(adapter);send.setEnabled(!roles.isEmpty());status.setText(roles.isEmpty()?"연결할 앱이 없습니다":"앱을 선택하세요");
    }
    @Override protected void onPause(){active=false;if(handle!=null)handle.cancel();handle=null;super.onPause();}
    private void ping(){if(!active||roles==null||roles.isEmpty())return;String text=payload.getText().toString();if(SuiteIpcPolicy.bytes(text)>SuiteIpcPolicy.MAX_PAYLOAD_BYTES){payload.setError("문장을 줄여 주세요");return;}
        int index=targets.getSelectedItemPosition();if(index<0||index>=roles.size())return;if(handle!=null)handle.cancel();send.setEnabled(false);status.setText("연결 확인 중");
        handle=SuiteIPC.ping(this,roles.get(index),text,result->{if(!active||isFinishing()||isDestroyed())return;send.setEnabled(true);
            switch(result.state){case "ACK":status.setText("연결 완료 · "+result.roundTripMs+" ms");break;case "TIMEOUT":status.setText("응답 시간이 초과됐습니다");break;case "NO_ACK":status.setText("응답이 없습니다");break;case "CANCELED":status.setText("취소했습니다");break;default:status.setText("앱을 연결할 수 없습니다");}
        });
    }
}
