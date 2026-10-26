package com.yonlab.vision;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Explicit signature-protected ordered PING/ACK; never treats dispatch as acknowledgment. */
public final class SuiteIPC {
    public static final String PERMISSION="com.yonlab.permission.SUITE",ACTION="com.yonlab.action.SUITE_PING",RECEIVER="com.yonlab.vision.SuiteReceiver";
    public static final long TIMEOUT_MS=5000;
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final AtomicReference<String> latestNonce=new AtomicReference<>("");
    private static final ThreadPoolExecutor JOURNAL=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),r->{Thread t=new Thread(r,"yon-ipc-journal");t.setDaemon(true);return t;});
    private SuiteIPC(){}
    public interface Callback{void complete(Result result);}
    public static final class Result{
        public final String state,target,nonce,error;public final long roundTripMs;public final int payloadBytes;public final boolean acknowledged;
        Result(String state,String target,String nonce,String error,long elapsed,int bytes){this.state=state;this.target=target;this.nonce=nonce;this.error=error;roundTripMs=elapsed;payloadBytes=bytes;acknowledged="ACK".equals(state);}
    }
    public static boolean permissionReady(Context context){try{
        PackageManager pm=context.getPackageManager();PermissionInfo info=pm.getPermissionInfo(PERMISSION,0);
        return (info.protectionLevel&PermissionInfo.PROTECTION_MASK_BASE)==PermissionInfo.PROTECTION_SIGNATURE
            &&pm.checkSignatures(context.getPackageName(),info.packageName)==PackageManager.SIGNATURE_MATCH
            &&pm.checkPermission(PERMISSION,context.getPackageName())==PackageManager.PERMISSION_GRANTED;
    }catch(PackageManager.NameNotFoundException|SecurityException e){return false;}}
    public static boolean trustedTarget(Context context,String role){
        if(!SuiteIpcPolicy.knownRole(role)||!permissionReady(context))return false;
        try{String target=SuiteIpcPolicy.packageFor(role);PackageManager pm=context.getPackageManager();ActivityInfo info=pm.getReceiverInfo(new ComponentName(target,RECEIVER),0);
            return info.exported&&info.enabled&&info.applicationInfo.enabled&&target.equals(info.packageName)&&PERMISSION.equals(info.permission)
                &&pm.checkSignatures(context.getPackageName(),target)==PackageManager.SIGNATURE_MATCH&&pm.checkPermission(PERMISSION,target)==PackageManager.PERMISSION_GRANTED;
        }catch(PackageManager.NameNotFoundException|SecurityException e){return false;}
    }
    public static List<String> targets(Context context){List<String> result=new ArrayList<>();for(String role:SuiteConfig.ROLES)if(!context.getPackageName().equals(SuiteIpcPolicy.knownRole(role)?SuiteIpcPolicy.packageFor(role):"")&&trustedTarget(context,role))result.add(role);return result;}
    public static Handle ping(Context context,String role,String payload,Callback callback){
        if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("main thread required");
        Handle handle=new Handle(context.getApplicationContext(),role,payload,callback);latestNonce.set(handle.nonce);handle.send();return handle;
    }
    public static final class Handle{
        private final Context app;private final String role,payload,source,target,nonce=UUID.randomUUID().toString();private final Callback callback;
        private final long started=SystemClock.elapsedRealtime();private final AtomicBoolean ended=new AtomicBoolean();private final Runnable timeout=()->finish("TIMEOUT","");
        Handle(Context app,String role,String payload,Callback callback){this.app=app;this.role=role;this.payload=payload;this.callback=callback;source=app.getPackageName();target=SuiteIpcPolicy.knownRole(role)?SuiteIpcPolicy.packageFor(role):"";}
        private void send(){
            String error=SuiteIpcPolicy.request(SuiteIpcPolicy.VERSION,source,target,target,nonce,payload);
            if(error!=null){finish("REJECTED",error);return;}if(!trustedTarget(app,role)){finish("UNAVAILABLE","SIGNATURE_OR_PERMISSION");return;}
            Intent request=new Intent(ACTION).setComponent(new ComponentName(target,RECEIVER)).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES|Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra("ver",SuiteIpcPolicy.VERSION).putExtra("sender",source).putExtra("target",target).putExtra("nonce",nonce).putExtra("payload",payload);
            MAIN.postDelayed(timeout,TIMEOUT_MS);journal(app,new Result("SENT",target,nonce,"",0,SuiteIpcPolicy.bytes(payload)),payload);
            try{app.sendOrderedBroadcast(request,PERMISSION,new BroadcastReceiver(){public void onReceive(Context context,Intent intent){
                if(ended.get())return;
                try{Bundle ack=getResultExtras(false);
                    if(getResultCode()!=Activity.RESULT_OK||ack==null){finish("REJECTED".equals(getResultData())?"REJECTED":"NO_ACK",getResultData()==null?"":getResultData());return;}
                    boolean valid="ACK".equals(getResultData())&&ack.getBoolean("ack",false)&&SuiteIpcPolicy.ack(ack.getInt("ver",-1),ack.getString("responder"),ack.getString("target"),ack.getString("nonce"),ack.getString("echo"),target,source,nonce,payload)
                        &&trustedTarget(app,role);
                    finish(valid?"ACK":"REJECTED",valid?"":"ACK_MISMATCH");
                }catch(RuntimeException e){finish("REJECTED","MALFORMED_ACK");}
            }},MAIN,Activity.RESULT_CANCELED,null,null);}catch(Exception e){finish("ERROR",e.getClass().getSimpleName());}
        }
        public void cancel(){if(Looper.myLooper()!=Looper.getMainLooper()){MAIN.post(this::cancel);return;}finish("CANCELED","");}
        private void finish(String state,String error){if(!ended.compareAndSet(false,true))return;MAIN.removeCallbacks(timeout);
            Result result=new Result(state,target,nonce,error,SystemClock.elapsedRealtime()-started,SuiteIpcPolicy.bytes(payload));journal(app,result,payload);if(callback!=null)callback.complete(result);
        }
    }
    private static void journal(Context app,Result result,String payload){try{JOURNAL.execute(()->{if(!latestNonce.get().equals(result.nonce))return;try{
        JSONObject value=new JSONObject().put("state",result.state).put("target",result.target).put("nonce",result.nonce).put("version",SuiteIpcPolicy.VERSION).put("acknowledged",result.acknowledged)
            .put("roundTripMs",result.roundTripMs).put("payloadBytes",result.payloadBytes).put("payloadSha256",payload==null?"":hash(payload)).put("error",result.error);Records.write(app,"ipc-result.json",value);
    }catch(Exception e){android.util.Log.w("YOn","ipc_journal_failed",e);}});}catch(java.util.concurrent.RejectedExecutionException e){android.util.Log.w("YOn","ipc_journal_busy");}}
    static String hash(String value)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
}
