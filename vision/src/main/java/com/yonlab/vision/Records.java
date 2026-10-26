package com.yonlab.vision;
import android.content.Context;
import android.os.SystemClock;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Small bounded developer records and a separate end-user event journal. */
public final class Records {
    private Records(){}
    private static final Object LOCK=new Object();
    public static boolean write(Context c,String name,JSONObject value){synchronized(LOCK){try{
        if(!name.matches("[a-zA-Z0-9_.-]{1,80}\\.json"))throw new IllegalArgumentException("record name");
        byte[] bytes=value.toString(2).getBytes(StandardCharsets.UTF_8);if(bytes.length>2*1024*1024)throw new IOException("record too large");
        File target=new File(c.getFilesDir(),name),pending=new File(c.getFilesDir(),name+".new");
        try(FileOutputStream f=new FileOutputStream(pending)){f.write(bytes);f.getFD().sync();}if(!pending.renameTo(target))throw new IOException("record commit");try{OneDeviceSupport.observeRecord(c,name,value);}catch(RuntimeException|LinkageError|OutOfMemoryError ignored){}return true;
    }catch(Exception e){android.util.Log.e("YOn","record_write_failed "+name,e);return false;}}}
    public static JSONObject read(Context c,String name){synchronized(LOCK){try{
        if(!name.matches("[a-zA-Z0-9_.-]{1,80}\\.json"))return null;File file=new File(c.getFilesDir(),name);if(!file.isFile()||file.length()>2*1024*1024)return null;
        ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(file)){byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1)b.write(block,0,n);}return new JSONObject(b.toString("UTF-8"));
    }catch(Exception e){return null;}}}
    public static JSONArray events(Context c){JSONObject r=read(c,"events.json");return r==null?new JSONArray():r.optJSONArray("events")==null?new JSONArray():r.optJSONArray("events");}
    public static void event(Context c,String type,String state,JSONObject details){synchronized(LOCK){try{
        JSONArray prior=events(c),next=new JSONArray();for(int i=Math.max(0,prior.length()-499);i<prior.length();i++)next.put(prior.get(i));
        next.put(new JSONObject().put("type",type).put("state",state).put("at",System.currentTimeMillis()).put("elapsedMs",SystemClock.elapsedRealtime()).put("details",details==null?new JSONObject():details));write(c,"events.json",new JSONObject().put("events",next));
    }catch(Exception e){android.util.Log.e("YOn","event_write_failed",e);}}}
    public static void clear(Context c){if(!write(c,"events.json",new JSONObject()))throw new IllegalStateException("기록을 지울 수 없습니다");}
}
