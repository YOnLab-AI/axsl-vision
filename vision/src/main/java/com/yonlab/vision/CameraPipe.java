package com.yonlab.vision;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.*;
import android.os.*;
import android.util.Size;
import android.view.*;
import com.yonlab.vision.core.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

final class CameraPipe {
    interface Sink {boolean acceptsFrames();void frame(BgrFrame frame);void status(String text);default void failed(String text){status(text);}default void previewReady(int width,int height,boolean mirrored){}}
    private final Activity app;private final TextureView view;private final Sink sink;private final String requestedId;
    private static final Object CAMERA_LOCK=new Object();private static CameraPipe owner;
    private volatile boolean running;private final AtomicBoolean cleanupRequested=new AtomicBoolean(),failureReported=new AtomicBoolean();private HandlerThread thread;private Handler handler;private CameraDevice device;private CameraCaptureSession capture;private ImageReader reader;private Surface preview;private boolean opening,resourcesReleased;private CameraDevice closingDevice;
    private volatile int width,height,rotation;private int sensorOrientation,displayRotation,rotationOverride=-1;private volatile boolean mirror;private byte[] raw;private ArrayBlockingQueue<byte[]> pool;
    private final Integer forcedRotation;private final int forcedMaxWidth;private final boolean landscapeOnly;
    private final Object stopLock=new Object();private final List<Runnable> stoppedListeners=new ArrayList<>();private volatile boolean stopped;
    CameraPipe(Activity a,TextureView v,String id,Sink s){this(a,v,id,s,null,0,false);}
    CameraPipe(Activity a,TextureView v,String id,Sink s,Integer fixedRotation,int maxWidth,boolean landscape){app=a;view=v;requestedId=id;sink=s;forcedRotation=fixedRotation;forcedMaxWidth=maxWidth;landscapeOnly=landscape;}
    void start(){CameraPipe prior;synchronized(CAMERA_LOCK){if(running||stopped||cleanupRequested.get())return;prior=owner;if(prior==null||prior==this){owner=this;running=true;thread=new HandlerThread("YOnCamera");thread.start();handler=new Handler(thread.getLooper());handler.post(this::open);return;}}prior.afterStopped(this::start);prior.stop();}
    private void open(){
        try{
            if(!running)return;
            if(app.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("camera permission");
            CameraManager cm=(CameraManager)app.getSystemService(Activity.CAMERA_SERVICE);String[] ids=cm.getCameraIdList();if(ids.length==0)throw new IllegalStateException("no camera");
            String id=Arrays.asList(ids).contains(requestedId)?requestedId:ids[0];if(!Arrays.asList(ids).contains(requestedId)){boolean front=app.getSharedPreferences("YOn",0).getString("cameraFacing",android.os.Build.MODEL.equalsIgnoreCase("aikit")?"front":"back").equals("front");for(String candidate:ids){Integer lens=cm.getCameraCharacteristics(candidate).get(CameraCharacteristics.LENS_FACING);if(lens!=null&&lens==(front?CameraCharacteristics.LENS_FACING_FRONT:CameraCharacteristics.LENS_FACING_BACK)){id=candidate;break;}}}CameraCharacteristics cc=cm.getCameraCharacteristics(id);StreamConfigurationMap map=cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if(map==null)throw new IllegalStateException("camera formats unavailable");
            Size[] yuv=map.getOutputSizes(ImageFormat.YUV_420_888),texture=map.getOutputSizes(SurfaceTexture.class);Size selected=null;
            int maxWidth=forcedMaxWidth>0?forcedMaxWidth:app.getSharedPreferences("YOn",0).getInt("analysisWidth",640);if(maxWidth<320||maxWidth>1920)maxWidth=1280;
            if(yuv!=null&&texture!=null)for(Size s:yuv){if(s.getWidth()>maxWidth||s.getHeight()>1080||(landscapeOnly&&s.getWidth()<=s.getHeight())||!Arrays.asList(texture).contains(s))continue;if(selected==null||(long)s.getWidth()*s.getHeight()>(long)selected.getWidth()*selected.getHeight())selected=s;}
            if(selected==null)throw new IllegalStateException("no common preview/YUV size");
            width=selected.getWidth();height=selected.getHeight();Integer orientation=cc.get(CameraCharacteristics.SENSOR_ORIENTATION),facing=cc.get(CameraCharacteristics.LENS_FACING);mirror=facing!=null&&facing==CameraCharacteristics.LENS_FACING_FRONT;
            sensorOrientation=orientation==null?0:orientation;displayRotation=app.getWindowManager().getDefaultDisplay().getRotation()*90;
            rotationOverride=forcedRotation!=null?forcedRotation:("aikit".equals(app.getSharedPreferences("YOn",0).getString("cameraProfile",android.os.Build.MODEL.equalsIgnoreCase("aikit")?"aikit":"auto"))?0:-1);
            if(!quarterTurn(sensorOrientation)||!quarterTurn(displayRotation)||(rotationOverride!=-1&&!quarterTurn(rotationOverride)))throw new IllegalArgumentException("unsupported camera rotation");
            rotation=rotationOverride==-1?(sensorOrientation+(mirror?displayRotation:-displayRotation)+360)%360:rotationOverride;
            pool=new ArrayBlockingQueue<>(3);for(int i=0;i<3;i++)pool.add(new byte[width*height*3]);raw=new byte[width*height*3];
            SurfaceTexture st=view.getSurfaceTexture();if(st==null)throw new IllegalStateException("preview unavailable");st.setDefaultBufferSize(width,height);preview=new Surface(st);
            reader=ImageReader.newInstance(width,height,ImageFormat.YUV_420_888,2);reader.setOnImageAvailableListener(this::image,handler);
            updateTransform();String facingText=facing==null?"unknown":facing==CameraCharacteristics.LENS_FACING_FRONT?"front":facing==CameraCharacteristics.LENS_FACING_BACK?"back":"external";
            int inputWidth=rotation%180==0?width:height,inputHeight=rotation%180==0?height:width;
            sink.status("AXSL Vision camera "+id+" / raw "+width+"×"+height+" / sensor "+sensorOrientation+"° / "+facingText+" / display "+displayRotation+"° / input "+rotation+"° "+(rotationOverride==-1?"auto":"manual")+" / JNI "+inputWidth+"×"+inputHeight);
            if(!running)return;
            opening=true;cm.openCamera(id,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice d){opening=false;device=d;if(!running){closeDevice(d);finishStop();return;}session();}
                public void onDisconnected(CameraDevice d){opening=false;closeDevice(d);fail("카메라 연결이 끊겼습니다.");finishStop();}
                public void onError(CameraDevice d,int error){opening=false;closeDevice(d);fail("AXSL Vision 카메라 오류 "+error);finishStop();}
                public void onClosed(CameraDevice d){if(device==d)device=null;if(closingDevice==d)closingDevice=null;finishStop();}
            },handler);
        }catch(Exception e){opening=false;fail("AXSL Vision 카메라 준비 실패: "+e.getClass().getSimpleName());}
    }
    private void session(){try{device.createCaptureSession(Arrays.asList(preview,reader.getSurface()),new CameraCaptureSession.StateCallback(){public void onConfigured(CameraCaptureSession s){if(!running){s.close();return;}capture=s;try{CaptureRequest.Builder b=device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);b.addTarget(preview);b.addTarget(reader.getSurface());b.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);s.setRepeatingRequest(b.build(),null,handler);sink.previewReady(rotation%180==0?width:height,rotation%180==0?height:width,mirror);}catch(Exception e){fail("AXSL Vision 캡처 시작 실패: "+e.getClass().getSimpleName());}}public void onConfigureFailed(CameraCaptureSession s){s.close();fail("AXSL Vision 캡처 설정 실패");}},handler);}catch(Exception e){fail("AXSL Vision 세션 생성 실패: "+e.getClass().getSimpleName());}}
    private void image(ImageReader source){
        Image image=null;byte[] bytes=null;
        try{
            image=source.acquireLatestImage();if(image==null||!running||!sink.acceptsFrames())return;bytes=pool.poll();if(bytes==null)return;
            Rect crop=image.getCropRect();if(crop.left!=0||crop.top!=0||crop.width()!=width||crop.height()!=height)throw new IllegalArgumentException("unsupported crop");
            // A frame keeps one orientation even when the display changes mid-preview.
            final int frameRotation=rotation;
            long start=System.nanoTime();Image.Plane[] p=image.getPlanes();byte[] out=frameRotation==0?bytes:raw;
            Yuv420.toBgr(p[0].getBuffer(),p[0].getRowStride(),p[0].getPixelStride(),p[1].getBuffer(),p[1].getRowStride(),p[1].getPixelStride(),p[2].getBuffer(),p[2].getRowStride(),p[2].getPixelStride(),width,height,out);
            if(frameRotation!=0)BgrRotation.rotate(out,width,height,frameRotation,bytes);
            int w=frameRotation==90||frameRotation==270?height:width,h=frameRotation==90||frameRotation==270?width:height;
            BgrFrame f=new BgrFrame(bytes,w,h,System.nanoTime(),System.nanoTime()-start,pool);bytes=null;sink.frame(f);
        }catch(Exception e){fail("AXSL Vision 프레임 변환 실패: "+e.getClass().getSimpleName());}
        finally{if(bytes!=null)pool.offer(bytes);if(image!=null)image.close();}
    }
    /** Reflow the preview and upright analysis frames without replacing the camera session. */
    void updateTransform(){
        app.runOnUiThread(()->{
            if(!running)return;
            final int display=app.getWindowManager().getDefaultDisplay().getRotation()*90;
            Handler cameraHandler=handler;if(cameraHandler==null)return;
            // ImageReader delivery uses this same handler, so rotation and frame metadata
            // change between frames, never halfway through a YUV/BGR conversion.
            cameraHandler.post(()->{
                if(!running||width<1||height<1)return;
                int next=rotationOverride==-1?(sensorOrientation+(mirror?display:-display)+360)%360:rotationOverride;
                boolean changed=next!=rotation;displayRotation=display;rotation=next;
                if(changed&&capture!=null)sink.previewReady(next%180==0?width:height,next%180==0?height:width,mirror);
                app.runOnUiThread(()->{if(running)transform();});
            });
        });
    }
    private static boolean quarterTurn(int degrees){return degrees==0||degrees==90||degrees==180||degrees==270;}
    private static void rotateCorner(float x,float y,int w,int h,int degrees,float[] out,int at){
        if(degrees==90){out[at]=h-y;out[at+1]=x;}
        else if(degrees==180){out[at]=w-x;out[at+1]=h-y;}
        else if(degrees==270){out[at]=y;out[at+1]=w-x;}
        else{out[at]=x;out[at+1]=y;}
    }
    private void transform(){
        float vw=view.getWidth(),vh=view.getHeight();if(vw<=0||vh<=0)return;
        if(width<=0||height<=0)return;
        final int transformRotation=rotation;
        int uw=transformRotation==90||transformRotation==270?height:width,uh=transformRotation==90||transformRotation==270?width:height;float scale=Math.min(vw/uw,vh/uh),dx=(vw-uw*scale)/2,dy=(vh-uh*scale)/2;
        int implicitWidth=sensorOrientation%180==0?width:height,implicitHeight=sensorOrientation%180==0?height:width;
        float[] src=new float[6],dest=new float[6];float[][] corners={{0,0},{width,0},{0,height}};
        for(int i=0;i<3;i++){
            // Camera2 already rotates/mirrors the TextureView stream before stretching it
            // over the view. ImageReader BGR does not contain that presentation transform.
            rotateCorner(corners[i][0],corners[i][1],width,height,sensorOrientation,src,i*2);
            if(mirror)src[i*2]=implicitWidth-src[i*2];
            src[i*2]=src[i*2]*vw/implicitWidth;src[i*2+1]=src[i*2+1]*vh/implicitHeight;
            // The desired preview uses exactly the rotation selected for the JNI input,
            // followed by one display-only front-camera mirror and uniform center fit.
            rotateCorner(corners[i][0],corners[i][1],width,height,transformRotation,dest,i*2);
            dest[i*2]=dx+dest[i*2]*scale;dest[i*2+1]=dy+dest[i*2+1]*scale;
            if(mirror)dest[i*2]=vw-dest[i*2];
        }
        Matrix m=new Matrix();if(!m.setPolyToPoly(src,0,dest,0,3))throw new IllegalStateException("AXSL Vision preview transform is singular");view.setTransform(m);
    }
    boolean mirrored(){return mirror;}
    int sourceWidth(){return width;}
    int sourceHeight(){return height;}
    int inputRotation(){return rotation;}
    private void fail(String text){if(!running||!failureReported.compareAndSet(false,true))return;stop();sink.failed(text);}
    void afterStopped(Runnable callback){synchronized(stopLock){if(!stopped){stoppedListeners.add(callback);return;}}callback.run();}
    private void stopped(){synchronized(CAMERA_LOCK){if(owner==this)owner=null;}List<Runnable> callbacks;synchronized(stopLock){if(stopped)return;stopped=true;callbacks=new ArrayList<>(stoppedListeners);stoppedListeners.clear();}for(Runnable callback:callbacks)try{callback.run();}catch(RuntimeException ignored){}}
    private void closeDevice(CameraDevice d){if(d==null||closingDevice==d)return;closingDevice=d;if(device==d)device=null;d.close();}
    private void finishStop(){if(!running&&resourcesReleased&&!opening&&device==null&&closingDevice==null){thread.quitSafely();stopped();}}
    void stop(){Handler h;synchronized(CAMERA_LOCK){running=false;if(!cleanupRequested.compareAndSet(false,true))return;h=handler;}if(h==null){stopped();return;}h.post(()->{
        try{if(capture!=null)capture.close();}catch(Exception ignored){}capture=null;
        if(device!=null)try{closeDevice(device);}catch(Exception ignored){}
        try{if(reader!=null)reader.close();}catch(Exception ignored){}reader=null;
        if(preview!=null)preview.release();preview=null;resourcesReleased=true;finishStop();
    });}
}
