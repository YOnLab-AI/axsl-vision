package com.yonlab.vision;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

/** App-owned camera and analysis preferences shared by the suite source. */
public class SettingsActivity extends YOnActivity {
    private SharedPreferences prefs;
    private boolean clearing;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); prefs=getSharedPreferences("YOn",MODE_PRIVATE);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Ui.BACK);
        LinearLayout header=Ui.brandHeader(this,"설정");
        header.addView(Ui.secondary(this,"닫기",v->finish()),new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,48)));
        root.addView(header,Ui.fill()); ScrollView scroll=new ScrollView(this); LinearLayout body=Ui.column(this);
        scroll.addView(body,Ui.fill()); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        boolean board="aikit".equalsIgnoreCase(Build.MODEL);
        LinearLayout camera=section(body,"카메라");
        String facing=stringPreference("cameraFacing",board?"front":"back",new String[]{"back","front"});
        select(camera,"사용 카메라",new String[]{"후면","전면"},"front".equals(facing)?1:0,
            index->prefs.edit().putString("cameraFacing",index==1?"front":"back").apply());
        String profile=stringPreference("cameraProfile",board?"aikit":"auto",new String[]{"auto","aikit"});
        select(camera,"카메라 방향",new String[]{"휴대폰 자동","장착 기기 0°"},"aikit".equals(profile)?1:0,
            index->prefs.edit().putString("cameraProfile",index==1?"aikit":"auto").putInt("cameraRotation",0).apply());

        LinearLayout analysis=section(body,"분석");
        int quality=intPreference("analysisWidth",640,new int[]{640,960,1280});
        select(analysis,"입력 크기",new String[]{"640 · 빠르게","960 · 균형","1280 · 선명하게"},quality>=1280?2:quality>=960?1:0,
            index->prefs.edit().putInt("analysisWidth",new int[]{640,960,1280}[index]).apply());
        int threads=intPreference("analysisThreads",2,new int[]{1,2,4});
        select(analysis,"분석 스레드",new String[]{"1","2","4"},threads>=4?2:threads>=2?1:0,
            index->OneExecutionSettings.select(SettingsActivity.this,"cpu"+new int[]{1,2,4}[index]));

        LinearLayout history=section(body,"기록");
        TextView description=Ui.text(this,"이벤트 기록을 관리하세요.",15); description.setTextColor(Ui.MUTED); history.addView(description);
        Button clear=Ui.secondary(this,"기록 지우기",v->confirmClear((Button)v));
        LinearLayout.LayoutParams clearParams=Ui.fill(); clearParams.topMargin=Ui.dp(this,12); history.addView(clear,clearParams);
        prefs.edit().putString("cameraFacing",facing).putString("cameraProfile",profile).putInt("cameraRotation",0)
            .putInt("analysisWidth",quality).putInt("analysisThreads",threads).apply();
        setContentView(root);
    }

    private String stringPreference(String key,String fallback,String[] allowed) {
        Object raw=prefs.getAll().get(key); if(raw instanceof String) for(String value:allowed) if(value.equals(raw)) return value;
        return fallback;
    }
    private int intPreference(String key,int fallback,int[] allowed) {
        Object raw=prefs.getAll().get(key); if(raw instanceof Integer) for(int value:allowed) if(value==(Integer)raw) return value;
        return fallback;
    }

    private LinearLayout section(LinearLayout body,String title) {
        LinearLayout panel=Ui.panel(this); panel.addView(Ui.heading(this,title,20));
        LinearLayout.LayoutParams lp=Ui.fill(); lp.bottomMargin=Ui.dp(this,16); body.addView(panel,lp); return panel;
    }
    private interface Choice { void selected(int index); }
    private void select(LinearLayout panel,String label,String[] values,int selected,Choice choice) {
        TextView title=Ui.heading(this,label,15); LinearLayout.LayoutParams labelParams=Ui.fill(); labelParams.topMargin=Ui.dp(this,12);
        panel.addView(title,labelParams); Spinner spinner=new Spinner(this); spinner.setContentDescription(label);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
        spinner.setMinimumHeight(Ui.dp(this,48)); spinner.setSelection(selected); panel.addView(spinner,Ui.fill());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int previous=selected;
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                if(position!=previous) { previous=position; choice.selected(position); }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void confirmClear(Button button) {
        if(clearing) return;
        new AlertDialog.Builder(this).setTitle("기록을 지울까요?").setMessage("이벤트 기록을 지웁니다.")
            .setNegativeButton("취소",null).setPositiveButton("지우기",(dialog,which)-> {
                clearing=true; button.setEnabled(false); button.setText("지우는 중");
                new Thread(()-> {
                    Throwable failure=null;
                    try { Records.clear(getApplicationContext()); } catch(Exception e) { failure=e; }
                    final Throwable error=failure;
                    runOnUiThread(()-> {
                        clearing=false; if(isFinishing()||isDestroyed()) return;
                        button.setEnabled(true); button.setText("기록 지우기");
                        if(error==null) new AlertDialog.Builder(this).setTitle("기록을 지웠습니다").setPositiveButton("확인",null).show();
                        else Ui.error(this,"기록을 지울 수 없습니다",error);
                    });
                },"yon-clear-records").start();
            }).show();
    }
}
