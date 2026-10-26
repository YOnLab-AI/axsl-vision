package com.yonlab.vision;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Explicit, same-signer suite handoff. All exported inputs are unique immutable snapshots. */
public final class SuiteShare {
    public static final String REGISTRY_MIME="application/vnd.yonlab.registry+json";
    private static final String[] IMAGE_ROLES={"lab","access","detect","flow","signage","face","drive","vis","compare"};
    private static final String[] REGISTRY_ROLES={"face","access","compare"};
    private static final String LAUNCHER="com.yonlab.vision.LauncherActivity";
    private static final Map<Activity,Job> JOBS=new WeakHashMap<>();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final ThreadPoolExecutor IO=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),r->{
        Thread thread=new Thread(r,"yon-suite-share"); thread.setDaemon(true); return thread;
    },new ThreadPoolExecutor.AbortPolicy());
    private SuiteShare() {}

    public static boolean trusted(Context context,String role) {
        if(!Arrays.asList(IMAGE_ROLES).contains(role)) return false;
        try {
            String target=SuiteConfig.packageName(role); PackageManager pm=context.getPackageManager();
            if(pm.checkSignatures(context.getPackageName(),target)!=PackageManager.SIGNATURE_MATCH) return false;
            ActivityInfo info=pm.getActivityInfo(new ComponentName(target,LAUNCHER),0);
            return info.exported&&info.enabled&&info.applicationInfo.enabled&&target.equals(info.packageName);
        } catch(PackageManager.NameNotFoundException|SecurityException e) { return false; }
    }
    public static boolean isTrustedUri(Context context,Uri uri) {
        if(uri==null||!"content".equals(uri.getScheme())||uri.getAuthority()==null) return false;
        String authority=uri.getAuthority(); if(!authority.endsWith(".files")) return false;
        String source=authority.substring(0,authority.length()-6); boolean known=false;
        for(String role:IMAGE_ROLES) if(SuiteConfig.packageName(role).equals(source)) { known=true; break; }
        if(!known) return false;
        try {
            PackageManager pm=context.getPackageManager(); ProviderInfo provider=pm.resolveContentProvider(authority,0);
            return provider!=null&&provider.enabled&&source.equals(provider.packageName)
                &&pm.checkSignatures(context.getPackageName(),source)==PackageManager.SIGNATURE_MATCH;
        } catch(SecurityException e) { return false; }
    }
    public static void openApp(Activity activity,String role) {
        if(!alive(activity)) return;
        if(!trusted(activity,role)) { unavailable(activity); return; }
        try { activity.startActivity(new Intent().setClassName(SuiteConfig.packageName(role),LAUNCHER)); }
        catch(Exception e) { Ui.error(activity,"앱을 열 수 없습니다",e); }
    }
    public static void shareImage(Activity activity,Bitmap image) {
        if(!alive(activity)||image==null||image.isRecycled()) return;
        choose(activity,IMAGE_ROLES,"열 앱 선택",target->new AlertDialog.Builder(activity)
            .setTitle(SuiteConfig.displayName(target)+"에서 열까요?").setNegativeButton("취소",null).setPositiveButton("열기",(dialog,which)-> {
                if(!alive(activity)||image.isRecycled()) return;
                Bitmap snapshot=null;
                try {
                    if(image.getWidth()>1600||image.getHeight()>1600||(long)image.getWidth()*image.getHeight()>2560000L)
                        throw new IllegalArgumentException("사진 크기를 확인하세요");
                    // A bounded in-memory copy makes worker encoding independent of the caller's Bitmap lifecycle.
                    snapshot=image.copy(Bitmap.Config.ARGB_8888,false); if(snapshot==null) throw new IllegalStateException("사진을 준비할 수 없습니다");
                    submit(new Job(activity,target,snapshot,null));
                } catch(Exception|OutOfMemoryError e) { if(snapshot!=null&&!snapshot.isRecycled()) snapshot.recycle(); Ui.error(activity,"사진을 준비할 수 없습니다",e); }
            }).show());
    }
    /** Pass a fresh Registry.exportJson() document; do not mutate it after handoff. */
    public static void shareRegistry(Activity activity,JSONObject document) {
        if(!alive(activity)||document==null) return;
        choose(activity,REGISTRY_ROLES,"전달할 앱 선택",target->{
            JSONArray rows=document.optJSONArray("entries"); int count=rows==null?0:rows.length();
            new AlertDialog.Builder(activity).setTitle("등록 목록을 전달할까요?").setMessage(SuiteConfig.displayName(target)+" · "+count+"명")
                .setNegativeButton("취소",null).setPositiveButton("전달",(dialog,which)-> { if(alive(activity)) submit(new Job(activity,target,null,document)); }).show();
        });
    }
    public static void cancel(Activity activity) {
        Job job; synchronized(JOBS) { job=JOBS.remove(activity); } if(job!=null) job.cancel();
    }
    private interface Chosen { void accept(String role); }
    private static void choose(Activity activity,String[] roles,String title,Chosen chosen) {
        if(!alive(activity)) return; List<String> available=new ArrayList<>(),labels=new ArrayList<>();
        for(String role:roles) if(!activity.getPackageName().equals(SuiteConfig.packageName(role))&&trusted(activity,role)) {
            available.add(role); labels.add(SuiteConfig.displayName(role));
        }
        if(available.isEmpty()) { unavailable(activity); return; }
        new AlertDialog.Builder(activity).setTitle(title).setItems(labels.toArray(new String[0]),(dialog,index)-> {
            if(alive(activity)&&trusted(activity,available.get(index))) chosen.accept(available.get(index)); else if(alive(activity)) unavailable(activity);
        }).setNegativeButton("취소",null).show();
    }
    private static void submit(Job job) {
        Activity activity=job.view.get(); if(!alive(activity)) { job.cancel(); job.recycle(); return; }
        Job prior; synchronized(JOBS) { prior=JOBS.put(activity,job); } if(prior!=null) prior.cancel();
        try { IO.execute(job); Toast.makeText(activity,"준비 중",Toast.LENGTH_SHORT).show(); }
        catch(java.util.concurrent.RejectedExecutionException e) { job.cancel(); job.recycle(); remove(job); Ui.error(activity,"잠시 후 다시 시도하세요",e); }
    }
    private static void remove(Job job) { Activity activity=job.view.get(); if(activity!=null) synchronized(JOBS) { if(JOBS.get(activity)==job) JOBS.remove(activity); } }
    private static boolean alive(Activity activity) { return activity!=null&&!activity.isFinishing()&&!activity.isDestroyed(); }
    private static void unavailable(Activity activity) { new AlertDialog.Builder(activity).setTitle("앱을 확인할 수 없습니다").setMessage("AXSL Vision 앱 설치를 확인하세요.").setPositiveButton("확인",null).show(); }

    private static final class Job implements Runnable {
        final Context app; final WeakReference<Activity> view; final String target; final JSONObject registry;
        final AtomicInteger state=new AtomicInteger(); // 0 preparing, 1 dispatch admitted, 2 canceled, 3 failed
        Bitmap bitmap; File output,pending; String sha=""; long size; int rows; volatile boolean workerFinished;
        Job(Activity activity,String target,Bitmap bitmap,JSONObject registry) {
            app=activity.getApplicationContext(); view=new WeakReference<>(activity); this.target=target; this.bitmap=bitmap; this.registry=registry;
        }
        boolean isRegistry() { return registry!=null; }
        public void run() {
            Throwable failure=null;
            try {
                if(state.get()!=0) return;
                output=ImageIO.exported(app,"AXSL-Vision-"+(isRegistry()?"registry-":"image-")+UUID.randomUUID()+(isRegistry()?".json":".jpg"));
                pending=new File(output.getParentFile(),output.getName()+".new");
                try(FileOutputStream out=new FileOutputStream(pending)) {
                    if(isRegistry()) {
                        if(!"yonlab-face-registry".equals(registry.optString("format"))||registry.optInt("version")!=1
                            ||registry.optInt("dimension")!=Registry.DIMENSION||!Registry.MODEL_SIGNATURE.equals(registry.optString("modelSignature")))
                            throw new IllegalArgumentException("등록 파일을 확인하세요");
                        JSONArray entries=registry.optJSONArray("entries"); if(entries==null||entries.length()>5000) throw new IllegalArgumentException("등록 수를 확인하세요");
                        rows=entries.length(); byte[] bytes=registry.toString().getBytes(StandardCharsets.UTF_8);
                        if(bytes.length>16*1024*1024) throw new IllegalArgumentException("등록 파일이 너무 큽니다"); out.write(bytes);
                    } else if(bitmap==null||bitmap.isRecycled()||!bitmap.compress(Bitmap.CompressFormat.JPEG,92,out)) throw new java.io.IOException("사진을 준비할 수 없습니다");
                    out.getFD().sync();
                }
                if(state.get()!=0) return;
                if(output.exists()||!pending.renameTo(output)) throw new java.io.IOException("공유 파일을 저장할 수 없습니다");
                sha=hash(output); size=output.length(); record("PREPARED",null);
                if(state.get()==0) MAIN.post(this::dispatch);
            } catch(Exception|OutOfMemoryError e) { failure=e; state.compareAndSet(0,3); }
            finally {
                workerFinished=true;
                recycle(); if(state.get()==2||state.get()==3) { cleanup(); remove(this); }
                if(state.get()==2) record("CANCELED",null);
                if(failure!=null&&state.get()==3) { final Throwable error=failure; record("FAILED",error); MAIN.post(()-> { Activity activity=view.get(); if(alive(activity)&&state.get()==3) Ui.error(activity,"공유를 준비할 수 없습니다",error); }); }
            }
        }
        private void dispatch() {
            Activity activity=view.get(); if(!alive(activity)) { cancel(); return; }
            if(!trusted(app,target)) { cancel(); unavailable(activity); return; }
            if(!state.compareAndSet(0,1)) return;
            remove(this);
            try {
                Uri uri=ImageIO.uri(app,output); String mime=isRegistry()?REGISTRY_MIME:"image/jpeg";
                Intent send=new Intent(Intent.ACTION_SEND).setClassName(SuiteConfig.packageName(target),LAUNCHER).setDataAndType(uri,mime)
                    .putExtra(Intent.EXTRA_STREAM,uri).putExtra("mode",target).putExtra("yonlab_contract",1).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                send.setClipData(ClipData.newRawUri(isRegistry()?"AXSL Vision 등록 목록":"AXSL Vision 이미지",uri)); activity.startActivity(send);
                recordLater("INTENT_DISPATCHED",null);
            } catch(Exception e) { state.set(3); recordLater("FAILED",e); Ui.error(activity,"앱에 전달할 수 없습니다",e); }
        }
        void cancel() {
            if(!state.compareAndSet(0,2)) return;
            if(IO.remove(this)) { recycle(); recordLater("CANCELED",null); }
            else if(workerFinished) recordLater("CANCELED",null);
        }
        private void recycle() { Bitmap image=bitmap; bitmap=null; if(image!=null&&!image.isRecycled()) image.recycle(); }
        private void cleanup() { if(pending!=null) pending.delete(); if(output!=null) output.delete(); }
        private void recordLater(String status,Throwable error) {
            try { IO.execute(()-> { if(state.get()==2||state.get()==3) cleanup(); record(status,error); }); }
            catch(java.util.concurrent.RejectedExecutionException ignored) { android.util.Log.w("YOn","share_journal_busy"); }
        }
        private void record(String status,Throwable error) {
            try {
                JSONObject record=new JSONObject().put("state",status).put("kind",isRegistry()?"registry":"image").put("target",SuiteConfig.packageName(target))
                    .put("snapshotName",output==null?"":output.getName()).put("snapshotSha256",sha).put("bytes",size).put("entries",rows);
                if(error!=null) record.put("errorType",error.getClass().getSimpleName()); Records.write(app,"share-result.json",record);
            } catch(Exception e) { android.util.Log.w("YOn","share_journal_failed",e); }
        }
    }
    private static String hash(File file)throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256"); try(FileInputStream in=new FileInputStream(file)) { byte[] block=new byte[16384]; int n; while((n=in.read(block))!=-1) digest.update(block,0,n); }
        StringBuilder text=new StringBuilder(); for(byte value:digest.digest()) text.append(String.format(java.util.Locale.ROOT,"%02x",value&255)); return text.toString();
    }
}
