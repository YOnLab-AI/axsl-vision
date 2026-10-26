package com.yonlab.vision;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Human-readable app-owned events and explicit, user-chosen JSON/CSV sharing. */
public class ReportActivity extends YOnActivity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->new Thread(r,"yon-reports"));
    private LinearLayout list;
    private TextView count;
    private Button json,csv;
    private volatile int generation;
    private volatile boolean closed;
    private boolean exporting;
    private int displayLimit=50;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout frame=new LinearLayout(this); frame.setOrientation(LinearLayout.VERTICAL); frame.setBackgroundColor(Ui.BACK);
        LinearLayout header=Ui.brandHeader(this,"기록");
        header.addView(Ui.secondary(this,"닫기",v->finish()),new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,48)));
        frame.addView(header,Ui.fill()); ScrollView scroll=new ScrollView(this); LinearLayout body=Ui.column(this);
        count=Ui.heading(this,"기록을 읽는 중",22); body.addView(count,Ui.fill());
        LinearLayout actions=new LinearLayout(this);
        json=Ui.secondary(this,"JSON 내보내기",v->export(false)); csv=Ui.secondary(this,"CSV 내보내기",v->export(true));
        actions.addView(json,new LinearLayout.LayoutParams(0,-2,1)); LinearLayout.LayoutParams second=new LinearLayout.LayoutParams(0,-2,1); second.leftMargin=Ui.dp(this,8);
        actions.addView(csv,second); LinearLayout.LayoutParams actionParams=Ui.fill(); actionParams.topMargin=Ui.dp(this,12); actionParams.bottomMargin=Ui.dp(this,16); body.addView(actions,actionParams);
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); body.addView(list,Ui.fill());
        scroll.addView(body,Ui.fill()); frame.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(frame);
    }
    @Override protected void onResume() { super.onResume(); final int task=++generation;
        worker.execute(()-> { JSONArray records=Records.events(getApplicationContext()); runOnUiThread(()-> { if(current(task)) render(records); }); });
    }
    @Override protected void onPause() { generation++; super.onPause(); }
    @Override protected void onDestroy() { closed=true; generation++; worker.shutdownNow(); super.onDestroy(); }
    private boolean current(int task) { return !closed&&!isFinishing()&&!isDestroyed()&&task==generation; }
    private void render(JSONArray records) {
        json.setEnabled(!exporting); csv.setEnabled(!exporting); list.removeAllViews(); count.setText("기록 "+records.length()+"개");
        if(records.length()==0) { LinearLayout empty=Ui.panel(this); empty.addView(Ui.heading(this,"아직 기록이 없습니다",18)); list.addView(empty,Ui.fill()); return; }
        for(int i=records.length()-1;i>=Math.max(0,records.length()-displayLimit);i--) {
            JSONObject event=records.optJSONObject(i); if(event==null) continue;
            LinearLayout card=Ui.panel(this); LinearLayout top=new LinearLayout(this);
            top.addView(Ui.heading(this,typeName(event.optString("type")),18),new LinearLayout.LayoutParams(0,-2,1));
            top.addView(Ui.pill(this,stateName(event.optString("state")),true)); card.addView(top);
            TextView when=Ui.text(this,time(event.optLong("at")),13); when.setTextColor(Ui.MUTED); card.addView(when);
            String details=summary(event.optJSONObject("details")); if(!details.isEmpty()) card.addView(Ui.text(this,details,15));
            LinearLayout.LayoutParams lp=Ui.fill(); lp.bottomMargin=Ui.dp(this,12); list.addView(card,lp);
        }
        if(records.length()>displayLimit) list.addView(Ui.secondary(this,"더 보기",v->{displayLimit+=50;render(records);}),Ui.fill());
    }
    private void export(boolean asCsv) {
        if(exporting) return; exporting=true; json.setEnabled(false); csv.setEnabled(false); final int task=generation;
        worker.execute(()-> {
            File output=null; Throwable error=null;
            try {
                JSONArray events=Records.events(getApplicationContext());
                String text=asCsv?csv(events):new JSONObject().put("application",getPackageName()).put("exportedAt",System.currentTimeMillis()).put("events",events).toString(2);
                if(closed||Thread.currentThread().isInterrupted()) return;
                output=ImageIO.exported(getApplicationContext(),"AXSL-Vision-records-"+SystemClock.elapsedRealtime()+(asCsv?".csv":".json"));
                File pending=new File(output.getParentFile(),output.getName()+".new");
                try(FileOutputStream out=new FileOutputStream(pending)) { out.write(text.getBytes(StandardCharsets.UTF_8)); out.getFD().sync(); }
                if(!pending.renameTo(output)) throw new java.io.IOException("기록을 저장할 수 없습니다");
            } catch(Exception e) { error=e; }
            final File file=output; final Throwable failure=error;
            runOnUiThread(()-> {
                exporting=false; if(!current(task)) return; json.setEnabled(true); csv.setEnabled(true);
                if(failure!=null||file==null) { Ui.error(this,"기록을 내보낼 수 없습니다",failure); return; }
                try {
                    Uri uri=ImageIO.uri(this,file); Intent share=new Intent(Intent.ACTION_SEND).setType(asCsv?"text/csv":"application/json")
                        .putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    share.setClipData(ClipData.newRawUri("AXSL Vision 기록",uri)); startActivity(Intent.createChooser(share,"기록 내보내기"));
                } catch(Exception e) { Ui.error(this,"공유 앱을 열 수 없습니다",e); }
            });
        });
    }
    private String csv(JSONArray events) {
        StringBuilder out=new StringBuilder("\uFEFF시각,종류,상태,내용\r\n");
        for(int i=0;i<events.length();i++) { JSONObject event=events.optJSONObject(i); if(event==null) continue;
            out.append(cell(time(event.optLong("at")))).append(',').append(cell(typeName(event.optString("type")))).append(',')
                .append(cell(stateName(event.optString("state")))).append(',').append(cell(summary(event.optJSONObject("details")))).append("\r\n");
        } return out.toString();
    }
    private static String cell(String value) { String text=value==null?"":value; if(text.matches("^[\\s]*[=+@-].*")) text="'"+text; return "\""+text.replace("\"","\"\"")+"\""; }
    private static String time(long at) { return at<=0?"":DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.MEDIUM,Locale.KOREA).format(new Date(at)); }
    private static String typeName(String type) {
        if(type.startsWith("video-")) return "영상 · "+typeName(type.substring(6));
        if(type.contains("enroll")||type.contains("register")) return "얼굴 등록";
        if(type.contains("compare")) return "얼굴 비교"; if(type.contains("flow")) return "사람 흐름";
        if(type.contains("drive")||type.contains("dms")) return "드라이버"; if(type.contains("vis")) return "방문자";
        if(type.contains("signage")) return "사이니지"; if(type.contains("detect")||type.contains("object")) return "객체 탐지";
        if(type.contains("face")||type.contains("match")||type.contains("access")) return "얼굴 분석";
        if(type.contains("photo")) return "사진 분석"; return "분석 기록";
    }
    private static String stateName(String state) {
        String s=state.toUpperCase(Locale.ROOT);
        if(s.contains("FAIL")||s.contains("ERROR")) return "오류"; if(s.contains("CANCEL")) return "취소";
        if(s.contains("RUN")||s.contains("START")) return "시작"; if(s.contains("MATCH")) return "인식";
        if(s.contains("ARCHIV")) return "보관"; if(s.contains("RESTOR")) return "복구";
        if(s.contains("COMPLETE")||s.contains("SAVE")||s.contains("ENROLL")||s.contains("PASS")) return "완료";
        if(s.contains("STOP")||s.equals("CLOSED")) return "중지"; return "기록";
    }
    private static String summary(JSONObject details) {
        if(details==null) return ""; StringBuilder result=new StringBuilder();
        String[][] labels={{"name","이름"},{"label","대상"},{"analysisFrames","분석 프레임"},{"faces","얼굴"},{"faceCount","얼굴"},{"objects","최대 객체"},{"detections","객체"},{"detectionCount","객체"},
            {"matchCount","일치"},{"in","유입"},{"out","유출"},{"inside","화면 내"},{"entered","유입"},{"exited","유출"},{"visitors","관측"},
            {"flowIn","유입"},{"flowOut","유출"},{"distinctIdentities","고유 관측"},{"visitSessions","방문 세션"},{"matchObservations","일치 관측"},{"blinks","깜박임"},{"mouthOpenFrames","입 벌림 관측"},{"drowsyEvents","눈 감음 지속"},{"lookingAwayEvents","시선 이탈"},
            {"similarity","점수"},{"score","점수"},{"message","내용"},{"elapsedMs","처리 ms"},{"analysisMs","분석 ms"}};
        for(String[] pair:labels) { Object value=details.opt(pair[0]); if(value==null||value==JSONObject.NULL||value instanceof JSONObject||value instanceof JSONArray) continue;
            if(result.length()>0) result.append(" · "); result.append(pair[1]).append(' ').append(value); if(result.length()>350) break;
        }
        if(details.has("mouthKnown")) { if(result.length()>0) result.append(" · "); result.append("입 ").append(!details.optBoolean("mouthKnown")?"—":details.optBoolean("mouthOpen")?"벌림":"닫힘"); }
        for(String[] pair:new String[][]{{"mouthOpenMs","입 벌림"},{"totalDwellMs","총 체류"}}) if(details.has(pair[0])) { if(result.length()>0) result.append(" · ");result.append(pair[1]).append(' ').append(String.format(Locale.KOREA,"%.1f초",details.optLong(pair[0])/1000f)); }
        String reason=details.optString("reason"),reasonText="COMPLETE".equals(reason)?"끝까지 분석":"ERROR".equals(reason)?"처리 오류":"BACKGROUND".equals(reason)?"화면 나가기":"RESTART".equals(reason)?"처음부터 다시":"SELECT_OTHER".equals(reason)?"영상 변경":"DESTROYED".equals(reason)?"화면 닫기":"STOPPED".equals(reason)?"분석 중지":"";
        if(!reasonText.isEmpty()) { if(result.length()>0) result.append(" · ");result.append(reasonText); }
        if(!details.optString("cleanupErrorType").isEmpty()) result.append(" · 자원 정리 오류");
        if(details.has("distinctCountExact")&&!details.optBoolean("distinctCountExact")) result.append(" · 고유 관측은 하한값");
        return result.toString();
    }
}
