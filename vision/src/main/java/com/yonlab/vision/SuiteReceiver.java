package com.yonlab.vision;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Only signature-permission callers may reach this manifest receiver. No command execution. */
public final class SuiteReceiver extends BroadcastReceiver {
    private static final Set<String> KEYS=new HashSet<>(Arrays.asList("ver","sender","target","nonce","payload"));
    private static final ThreadPoolExecutor WORK=new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8),r->{Thread t=new Thread(r,"yon-ipc-receive");t.setDaemon(true);return t;});
    @Override public void onReceive(Context context,Intent intent){
        if(!isOrderedBroadcast())return;
        try{
            if(intent==null||!SuiteIPC.ACTION.equals(intent.getAction())||!SuiteIPC.permissionReady(context))throw new IllegalArgumentException("ACTION_OR_PERMISSION");
            ComponentName component=intent.getComponent();if(component==null||!context.getPackageName().equals(component.getPackageName())||!SuiteIPC.RECEIVER.equals(component.getClassName()))throw new IllegalArgumentException("COMPONENT");
            Bundle data=intent.getExtras();if(data==null||!KEYS.containsAll(data.keySet()))throw new IllegalArgumentException("FIELDS");
            int version=intent.getIntExtra("ver",-1);String sender=intent.getStringExtra("sender"),target=intent.getStringExtra("target"),nonce=intent.getStringExtra("nonce"),payload=intent.getStringExtra("payload");
            String error=SuiteIpcPolicy.request(version,sender,target,context.getPackageName(),nonce,payload);if(error!=null)throw new IllegalArgumentException(error);
            if(context.getPackageManager().checkSignatures(context.getPackageName(),sender)!=PackageManager.SIGNATURE_MATCH)throw new SecurityException("SIGNATURE");
            Context app=context.getApplicationContext();PendingResult pending=goAsync();
            try{WORK.execute(()->{try{
                JSONObject record=new JSONObject().put("state","RECEIVED").put("sender",sender).put("target",target).put("nonce",nonce).put("version",version)
                    .put("payloadBytes",SuiteIpcPolicy.bytes(payload)).put("payloadSha256",SuiteIPC.hash(payload)).put("receivedAtElapsedMs",SystemClock.elapsedRealtime());
                boolean saved=Records.write(app,"ipc-received.json",record);
                Bundle ack=new Bundle();ack.putBoolean("ack",true);ack.putInt("ver",SuiteIpcPolicy.VERSION);ack.putString("responder",app.getPackageName());ack.putString("target",sender);ack.putString("nonce",nonce);ack.putString("echo",payload);ack.putBoolean("recorded",saved);
                pending.setResult(Activity.RESULT_OK,"ACK",ack);
            }catch(Exception e){pending.setResult(Activity.RESULT_CANCELED,"REJECTED",null);}finally{pending.finish();}});}
            catch(java.util.concurrent.RejectedExecutionException e){pending.setResult(Activity.RESULT_CANCELED,"REJECTED",null);pending.finish();}
        }catch(Exception e){setResult(Activity.RESULT_CANCELED,"REJECTED",null);}
    }
}
