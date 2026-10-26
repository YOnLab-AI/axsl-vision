package com.yonlab.vision;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.*;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Rotation-aware capture with one owned camera, immutable review and explicit confirmation. */
public class CaptureActivity extends YOnActivity {
    static final String PHOTO_FILE="photos/capture.jpg";
    private static final int CAMERA_PERMISSION=720;
    private static final Object OWNER=new Object();
    private static CameraPipe ownedCamera;
    private static final AtomicReference<String> newestSession=new AtomicReference<>("");
    private static final AtomicInteger IO_PENDING=new AtomicInteger();
    private static final ThreadPoolExecutor IO=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(1),r->{Thread t=new Thread(r,"YOnCaptureIO");t.setDaemon(true);return t;});
    private static final ThreadPoolExecutor JOURNAL=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(64),r->{Thread t=new Thread(r,"YOnCaptureJournal");t.setDaemon(true);return t;});
    private static final AtomicReference<Snapshot> latestRecord=new AtomicReference<>();
    private static final AtomicLong journalRevision=new AtomicLong();
    private static final AtomicBoolean journalScheduled=new AtomicBoolean();
    private final String sessionId=UUID.randomUUID().toString();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final AtomicBoolean wantFrame=new AtomicBoolean();
    private TextureView texture;private ImageView review;private TextView status;private Button shutter,retake,use,cameraSwitch;
    private LinearLayout body,controls;private FrameLayout stage;
    private final Map<String,Integer> cameraFaces=new LinkedHashMap<>();
    private String cameraId="";
    private CameraPipe pipe;private Shot shot;private Bitmap reviewBitmap;
    private volatile boolean active;private volatile long generation,captureId;private Runnable captureTimeout;private String phase="PREVIEW";
    private boolean destroyed;private Intent savedResult;
    private boolean permissionRequested;
    private int width,height,sourceWidth,sourceHeight,sourceRotation,uprightWidth,uprightHeight,frames,previewFrames;private boolean mirrored,letterboxed,ready,completed,cameraClosed=true;

    public void onCreate(Bundle saved){
        super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        newestSession.set(sessionId);
        discoverCameras();
        LinearLayout root=Ui.column(this),header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(Ui.heading(this,"사진 촬영",26),new LinearLayout.LayoutParams(0,-2,1));header.addView(Ui.secondary(this,"취소",v->finish()));root.addView(header);
        status=Ui.text(this,"카메라 연결 중",14);root.addView(status);
        body=new LinearLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        stage=new FrameLayout(this);stage.setBackground(Ui.round(this,Ui.NAVY,20));stage.setClipToOutline(true);texture=new TextureView(this);texture.setContentDescription("촬영 미리보기");review=new ImageView(this);review.setContentDescription("촬영한 사진");review.setScaleType(ImageView.ScaleType.FIT_CENTER);review.setVisibility(View.GONE);
        stage.addView(texture,new FrameLayout.LayoutParams(-1,-1));stage.addView(review,new FrameLayout.LayoutParams(-1,-1));body.addView(stage);
        controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);cameraSwitch=Ui.secondary(this,"카메라 전환",v->switchCamera());shutter=Ui.button(this,"촬영",v->capture());retake=Ui.secondary(this,"다시 촬영",v->retake());use=Ui.button(this,"사진 사용",v->use());
        for(Button b:new Button[]{cameraSwitch,shutter,retake,use})controls.addView(b);body.addView(controls);applyLayout();setContentView(root);buttons();
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){begin();}public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){if(pipe!=null&&"PREVIEW".equals(phase))pipe.updateTransform();}public boolean onSurfaceTextureDestroyed(SurfaceTexture s){stopCamera();return true;}public void onSurfaceTextureUpdated(SurfaceTexture s){previewFrames++;}});
    }
    @Override public void onConfigurationChanged(Configuration configuration){
        super.onConfigurationChanged(configuration);applyLayout();
        // Keep an in-flight Shot and the captured pixels exactly as they were. Only
        // the live preview changes orientation; review uses its original aspect ratio.
        if(pipe!=null&&"PREVIEW".equals(phase))pipe.updateTransform();
        buttons();
    }
    private void applyLayout(){
        boolean wide=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        body.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        controls.setOrientation(wide?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        stage.setLayoutParams(wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        int rail=Ui.dp(this,Math.min(208,Math.round(152*Math.min(1.4f,getResources().getConfiguration().fontScale))));
        LinearLayout.LayoutParams cp=wide?new LinearLayout.LayoutParams(rail,-1):new LinearLayout.LayoutParams(-1,-2);
        cp.setMargins(wide?Ui.dp(this,12):0,wide?0:Ui.dp(this,12),0,0);controls.setLayoutParams(cp);
        for(Button b:new Button[]{cameraSwitch,shutter,retake,use}){
            LinearLayout.LayoutParams bp=wide?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-1,1);
            int gap=Ui.dp(this,4);bp.setMargins(wide?0:gap,wide?gap:0,wide?0:gap,wide?gap:0);b.setLayoutParams(bp);
        }
    }
    private void discoverCameras(){
        cameraFaces.clear();
        try{CameraManager manager=(CameraManager)getSystemService(CAMERA_SERVICE);for(String id:manager.getCameraIdList()){
            Integer face=manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);cameraFaces.put(id,face==null?CameraCharacteristics.LENS_FACING_EXTERNAL:face);
        }}catch(Exception ignored){}
        String saved=getSharedPreferences("YOn",0).getString("cameraId","");
        if(cameraFaces.containsKey(saved)){cameraId=saved;return;}
        int wanted=getSharedPreferences("YOn",0).getString("cameraFacing",android.os.Build.MODEL.equalsIgnoreCase("aikit")?"front":"back").equals("front")?CameraCharacteristics.LENS_FACING_FRONT:CameraCharacteristics.LENS_FACING_BACK;
        for(Map.Entry<String,Integer> entry:cameraFaces.entrySet())if(entry.getValue()==wanted){cameraId=entry.getKey();return;}
        cameraId=cameraFaces.isEmpty()?"":cameraFaces.keySet().iterator().next();
    }
    private String nextCamera(){
        Integer current=cameraFaces.get(cameraId);int wanted=current!=null&&current==CameraCharacteristics.LENS_FACING_FRONT?CameraCharacteristics.LENS_FACING_BACK:CameraCharacteristics.LENS_FACING_FRONT;
        for(Map.Entry<String,Integer> entry:cameraFaces.entrySet())if(entry.getValue()==wanted)return entry.getKey();
        if(current==null||current==CameraCharacteristics.LENS_FACING_EXTERNAL)for(Map.Entry<String,Integer> entry:cameraFaces.entrySet())if(entry.getValue()==CameraCharacteristics.LENS_FACING_BACK)return entry.getKey();
        return null;
    }
    private String cameraName(String id){Integer face=cameraFaces.get(id);return face!=null&&face==CameraCharacteristics.LENS_FACING_FRONT?"전면":face!=null&&face==CameraCharacteristics.LENS_FACING_BACK?"후면":"외부";}
    private void switchCamera(){
        if(!active||!"PREVIEW".equals(phase))return;String next=nextCamera();if(next==null)return;
        generation++;captureId++;wantFrame.set(false);cancelTimeout();stopCamera();cameraId=next;
        getSharedPreferences("YOn",0).edit().putString("cameraId",cameraId).putString("cameraFacing",cameraName(cameraId).equals("전면")?"front":"back").apply();
        status.setText(cameraName(cameraId)+" 카메라 연결 중");buttons();begin();
    }
    protected void onResume(){super.onResume();active=true;if(savedResult!=null){setResult(RESULT_OK,savedResult);finish();return;}if("PREVIEW".equals(phase))begin();else if("REVIEW".equals(phase)){buttons();publish("REVIEW");}}
    protected void onPause(){active=false;if(!"REVIEW".equals(phase)&&!"SAVED".equals(phase)){generation++;captureId++;}cancelTimeout();wantFrame.set(false);if("PROCESSING".equals(phase)||"SAVING".equals(phase)){if(shot!=null)shot.cancel();phase="PREVIEW";}stopCamera();super.onPause();}
    protected void onDestroy(){destroyed=true;cancelTimeout();discardShot();review.setImageDrawable(null);if(reviewBitmap!=null&&!reviewBitmap.isRecycled())reviewBitmap.recycle();reviewBitmap=null;if(!completed)publish("CANCELED");super.onDestroy();}
    private void begin(){
        if(!active||!"PREVIEW".equals(phase)||pipe!=null||!texture.isAvailable())return;
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){status.setText("카메라 권한이 필요합니다");if(!permissionRequested){permissionRequested=true;requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);}buttons();return;}
        final long token=generation;
        synchronized(OWNER){
            if(ownedCamera!=null){CameraPipe prior=ownedCamera;WeakReference<CaptureActivity> weak=new WeakReference<>(this);prior.afterStopped(()->new Handler(Looper.getMainLooper()).post(()->{CaptureActivity a=weak.get();if(a!=null&&a.active&&a.generation==token)a.begin();}));prior.stop();return;}
            final CameraPipe[] holder=new CameraPipe[1];
            CameraPipe camera=new CameraPipe(this,texture,cameraId,new CameraPipe.Sink(){
                public boolean acceptsFrames(){return active&&generation==token&&wantFrame.get();}
                public void frame(BgrFrame frame){
                    try{
                        if(!active||generation!=token||!wantFrame.compareAndSet(true,false))return;
                        // ImageReader pool storage is never handed to the IO worker.
                        byte[] snapshot=frame.data.clone();boolean mirror=holder[0].mirrored();long requestId=captureId;
                        int rawW=holder[0].sourceWidth(),rawH=holder[0].sourceHeight(),rotation=holder[0].inputRotation();
                        main.post(()->{if(!active||generation!=token||captureId!=requestId||!"PROCESSING".equals(phase))return;frames++;width=frame.width;height=frame.height;uprightWidth=frame.width;uprightHeight=frame.height;sourceWidth=rawW;sourceHeight=rawH;sourceRotation=rotation;mirrored=mirror;stopCamera();Shot job=new Shot(getApplicationContext(),CaptureActivity.this,sessionId,token,requestId,snapshot,frame.width,frame.height,mirror);shot=job;IO_PENDING.incrementAndGet();try{IO.execute(job);}catch(RejectedExecutionException e){job.finishWork();failure(job,"capture busy");}});
                    }catch(Exception e){main.post(()->failure(token,e.getClass().getSimpleName()));}
                    finally{frame.close();}
                }
                public void status(String text){}
                public void previewReady(int w,int h,boolean mirror){int rawW=holder[0].sourceWidth(),rawH=holder[0].sourceHeight(),rotation=holder[0].inputRotation();main.post(()->{if(!active||generation!=token||pipe!=holder[0]||!"PREVIEW".equals(phase))return;width=w;height=h;uprightWidth=w;uprightHeight=h;sourceWidth=rawW;sourceHeight=rawH;sourceRotation=rotation;mirrored=mirror;ready=true;status.setText(cameraName(cameraId)+" 카메라");buttons();publish("PREVIEW");});}
                public void failed(String error){main.post(()->failure(token,error));}
            },android.os.Build.MODEL.equalsIgnoreCase("aikit")?0:null,1280,false);holder[0]=camera;pipe=camera;ownedCamera=camera;cameraClosed=false;
            camera.afterStopped(()->{synchronized(OWNER){if(ownedCamera==camera)ownedCamera=null;}main.post(()->{if(pipe!=null&&pipe!=camera)return;if(generation!=token&&active)return;cameraClosed=true;if(completed)publish("SAVED");else if(!active)publish(isFinishing()?"CANCELED":phase);else if("REVIEW".equals(phase))publish("REVIEW");});});camera.start();
        }
    }
    public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code!=CAMERA_PERMISSION)return;if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){discoverCameras();buttons();begin();}else{status.setText("카메라 접근을 허용해 주세요");publish("PERMISSION_DENIED");}}
    private void cancelTimeout(){if(captureTimeout!=null){main.removeCallbacks(captureTimeout);captureTimeout=null;}}
    private void capture(){if(!active||!"PREVIEW".equals(phase))return;if(!ready){permissionRequested=false;begin();return;}cancelTimeout();long requestId=++captureId;phase="PROCESSING";status.setText("촬영 중");buttons();wantFrame.set(true);long token=generation;captureTimeout=()->{if(active&&generation==token&&captureId==requestId&&"PROCESSING".equals(phase)){captureId++;wantFrame.set(false);discardShot();phase="PREVIEW";status.setText("촬영 실패");buttons();begin();publish("FAILED");}};main.postDelayed(captureTimeout,15000);}
    private void received(Shot job,Bitmap image){
        if(!active||generation!=job.generation||captureId!=job.captureId||shot!=job||job.canceled.get()||!"PROCESSING".equals(phase)){job.cancel();image.recycle();return;}
        cancelTimeout();width=image.getWidth();height=image.getHeight();letterboxed=job.letterboxed;reviewBitmap=image;review.setImageBitmap(image);review.setVisibility(View.VISIBLE);texture.setVisibility(View.INVISIBLE);phase="REVIEW";status.setText("사진 확인");buttons();publish("REVIEW");
    }
    private void failure(Shot job,String error){if(shot!=job||captureId!=job.captureId)return;failure(job.generation,error);}
    private void failure(long token,String error){if(!active||generation!=token)return;captureId++;cancelTimeout();wantFrame.set(false);discardShot();stopCamera();phase="PREVIEW";ready=false;status.setText("카메라를 연결할 수 없습니다");buttons();publish("FAILED");}
    private void retake(){if(!active||!"REVIEW".equals(phase))return;generation++;captureId++;cancelTimeout();discardShot();review.setImageDrawable(null);if(reviewBitmap!=null)reviewBitmap.recycle();reviewBitmap=null;review.setVisibility(View.GONE);texture.setVisibility(View.VISIBLE);phase="PREVIEW";ready=false;status.setText("카메라 연결 중");buttons();begin();publish("PREVIEW");}
    private void use(){
        Shot current=shot;if(!active||!"REVIEW".equals(phase)||current==null||current.canceled.get()||current.generation!=generation||current.captureId!=captureId)return;
        if(!current.commitQueued.compareAndSet(false,true))return;
        phase="SAVING";status.setText("저장 중");buttons();IO_PENDING.incrementAndGet();publish("SAVING");
        try{IO.execute(current::commit);}catch(RejectedExecutionException e){IO_PENDING.decrementAndGet();current.commitQueued.set(false);phase="REVIEW";status.setText("사진을 저장할 수 없습니다");buttons();publish("FAILED");refreshJournal();}
    }
    private void saved(Shot job,Intent result,Throwable error){
        if(destroyed||isFinishing()||shot!=job)return;
        if(result!=null){completed=true;phase="SAVED";savedResult=result;width=job.outputWidth;height=job.outputHeight;letterboxed=job.letterboxed;publish("SAVED");if(active){setResult(RESULT_OK,result);finish();}}
        else if(active&&"SAVING".equals(phase)&&generation==job.generation&&captureId==job.captureId){phase="REVIEW";status.setText("사진을 저장할 수 없습니다");buttons();publish("FAILED");}
    }
    private void stopCamera(){ready=false;CameraPipe old=pipe;pipe=null;if(old!=null)old.stop();}
    private void discardShot(){Shot prior=shot;shot=null;if(prior!=null)prior.cancel();}
    private void buttons(){boolean reviewPhase="REVIEW".equals(phase),saving="SAVING".equals(phase),previewPhase="PREVIEW".equals(phase);String next=nextCamera();cameraSwitch.setVisibility(reviewPhase||saving?View.GONE:View.VISIBLE);cameraSwitch.setText(next==null?"카메라 전환":cameraName(next)+"으로 전환");cameraSwitch.setContentDescription("현재 "+cameraName(cameraId)+" 카메라. "+(next==null?"전환할 카메라 없음":cameraName(next)+" 카메라로 전환"));cameraSwitch.setEnabled(active&&previewPhase&&next!=null);shutter.setVisibility(reviewPhase||saving?View.GONE:View.VISIBLE);shutter.setText(ready?"촬영":"카메라 연결");shutter.setEnabled(active&&previewPhase);retake.setVisibility(reviewPhase||saving?View.VISIBLE:View.GONE);use.setVisibility(reviewPhase||saving?View.VISIBLE:View.GONE);retake.setEnabled(active&&reviewPhase);use.setEnabled(active&&reviewPhase);}
    private void publish(String state){
        try{JSONObject record=new JSONObject().put("sessionId",sessionId).put("captureId",captureId).put("state",state).put("width",width).put("height",height)
            .put("sourceWidth",sourceWidth).put("sourceHeight",sourceHeight).put("sourceRotation",sourceRotation).put("frameSourceRotation",sourceRotation)
            .put("uprightWidth",uprightWidth).put("uprightHeight",uprightHeight).put("letterboxed",letterboxed).put("mirrored",mirrored)
            .put("screenWidth",getResources().getDisplayMetrics().widthPixels).put("screenHeight",getResources().getDisplayMetrics().heightPixels).put("displayRotation",getWindowManager().getDefaultDisplay().getRotation()*90)
            .put("frames",frames).put("previewFrames",previewFrames).put("closed",cameraClosed).put("externalCameraUsed",false);
            storeSnapshot(new Snapshot(getApplicationContext(),sessionId,record));
        }catch(Exception ignored){}
    }
    private static final class Snapshot{final Context app;final String id;final JSONObject data;Snapshot(Context a,String id,JSONObject d){app=a;this.id=id;data=d;}}
    private static void storeSnapshot(Snapshot value){while(true){Snapshot prior=latestRecord.get();if(!newestSession.get().equals(value.id))return;if(latestRecord.compareAndSet(prior,value)){refreshJournal();return;}}}
    private static void refreshJournal(){journalRevision.incrementAndGet();if(journalScheduled.compareAndSet(false,true))try{JOURNAL.execute(CaptureActivity::drainJournal);}catch(RejectedExecutionException e){journalScheduled.set(false);android.util.Log.w("YOn","capture_journal_busy");}}
    private static void drainJournal(){while(true){long revision=journalRevision.get();Snapshot snapshot=latestRecord.get();
        if(snapshot!=null&&newestSession.get().equals(snapshot.id))try{JSONObject value=new JSONObject(snapshot.data.toString()).put("workerClear",IO_PENDING.get()==0).put("ioJobs",IO_PENDING.get()).put("journalRevision",revision);Records.write(snapshot.app,"capture-session.json",value);}catch(Exception e){android.util.Log.w("YOn","capture_journal_failed",e);}
        journalScheduled.set(false);if(journalRevision.get()!=revision&&journalScheduled.compareAndSet(false,true))continue;return;}}
    private static void committedSnapshot(Shot shot){while(true){Snapshot prior=latestRecord.get();if(prior==null||!prior.id.equals(shot.session)||!newestSession.get().equals(shot.session))return;
        try{JSONObject value=new JSONObject(prior.data.toString()).put("lastCommittedCaptureId",shot.captureId).put("lastCommitCompleted",true);
            if(value.optLong("captureId")==shot.captureId)value.put("state","SAVED").put("width",shot.outputWidth).put("height",shot.outputHeight).put("letterboxed",shot.letterboxed);
            if(latestRecord.compareAndSet(prior,new Snapshot(prior.app,prior.id,value))){refreshJournal();return;}
        }catch(Exception e){return;}}}
    private static void cleanLater(Shot shot){try{JOURNAL.execute(shot::cleanup);}catch(RejectedExecutionException e){android.util.Log.w("YOn","capture_cleanup_busy");}}
    private static final class Shot implements Runnable{
        final Context app;final WeakReference<CaptureActivity> view;final String session;final long generation,captureId;final byte[] bgr;final int width,height;final boolean mirror;final File pending;
        final AtomicBoolean canceled=new AtomicBoolean(),finished=new AtomicBoolean(),commitQueued=new AtomicBoolean();final AtomicInteger commitState=new AtomicInteger();volatile boolean committed,letterboxed;volatile int outputWidth,outputHeight;
        Shot(Context app,CaptureActivity a,String session,long token,long captureId,byte[] pixels,int w,int h,boolean mirror){this.app=app;view=new WeakReference<>(a);this.session=session;generation=token;this.captureId=captureId;bgr=pixels;width=w;height=h;this.mirror=mirror;pending=new File(app.getFilesDir(),"yon-capture-"+UUID.randomUUID()+".pending.jpg");}
        public void run(){Bitmap bitmap=null;try{
            if(canceled.get())return;CaptureGeometry.Frame normalized=CaptureGeometry.normalize(bgr,width,height,mirror);outputWidth=normalized.width;outputHeight=normalized.height;letterboxed=normalized.letterboxed;bitmap=Bitmap.createBitmap(normalized.argb,outputWidth,outputHeight,Bitmap.Config.ARGB_8888);
            if(canceled.get())return;try(FileOutputStream out=new FileOutputStream(pending)){if(!bitmap.compress(Bitmap.CompressFormat.JPEG,92,out))throw new IOException("JPEG encode");out.flush();out.getFD().sync();}
            if(canceled.get())return;Bitmap ready=bitmap;bitmap=null;new Handler(Looper.getMainLooper()).post(()->{CaptureActivity a=view.get();if(a!=null)a.received(this,ready);else{ready.recycle();cancel();}});
        }catch(Exception e){new Handler(Looper.getMainLooper()).post(()->{CaptureActivity a=view.get();if(a!=null)a.failure(this,e.getClass().getSimpleName());});cancel();}
        finally{if(bitmap!=null)bitmap.recycle();finishWork();if(canceled.get())cleanup();}}
        void commit(){Intent result=null;Throwable failure=null;try{
            if(canceled.get()||!newestSession.get().equals(session))return;
            File target=new File(app.getFilesDir(),PHOTO_FILE);File directory=target.getParentFile();if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("photo directory");
            android.net.Uri uri=ImageIO.uri(app,target);
            if(canceled.get()||!newestSession.get().equals(session)||!commitState.compareAndSet(0,1))return;
            if(!pending.isFile()||!pending.renameTo(target))throw new IOException("photo commit");
            committed=true;commitState.set(3);committedSnapshot(this);
            result=new Intent().putExtra("photo_uri",uri.toString()).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra("photo_file",PHOTO_FILE)
                .putExtra("width",outputWidth).putExtra("height",outputHeight).putExtra("bytes",target.length()).putExtra("mirrored",mirror);
        }catch(Exception e){failure=e;commitState.compareAndSet(1,canceled.get()?2:0);}
        finally{commitQueued.set(false);IO_PENDING.decrementAndGet();refreshJournal();if(canceled.get())cleanup();Intent data=result;Throwable error=failure;new Handler(Looper.getMainLooper()).post(()->{CaptureActivity a=view.get();if(a!=null)a.saved(this,data,error);});}}
        void finishWork(){if(!finished.compareAndSet(false,true))return;IO_PENDING.decrementAndGet();refreshJournal();}
        void cleanup(){if(!committed&&commitState.get()!=1)pending.delete();}
        void cancel(){canceled.set(true);commitState.compareAndSet(0,2);if(IO.remove(this))finishWork();if(finished.get()&&!commitQueued.get())cleanLater(this);}
    }
}

