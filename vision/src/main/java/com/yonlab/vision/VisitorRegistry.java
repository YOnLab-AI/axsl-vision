package com.yonlab.vision;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/** Separate anonymous, bounded, TTL-limited SFace registry; never reads or modifies manual enrollments. */
public final class VisitorRegistry implements AutoCloseable {
    public static final long DEFAULT_TTL_MS=24L*60*60*1000;
    public static final int DEFAULT_MAX_ROWS=1000;
    public static final String DATABASE_NAME="yon-anonymous-visitors.db";
    private final Db helper;
    private final float threshold;
    private final long ttl;
    private final int maxRows;
    private boolean closed;
    public static final class Result {
        public final long[] visitorIds;
        public final float[] similarities;
        public final boolean[] created;
        public final int newVisitors,matchedVisitors,unresolved,retainedRows,expiredRows,evictedRows;
        public final String modelSignature;
        private Result(VisitorPolicy.Plan p){visitorIds=p.visitorIds.clone();similarities=p.similarities.clone();created=p.created.clone();newVisitors=p.newVisitors;matchedVisitors=p.matchedVisitors;unresolved=p.unresolved;retainedRows=p.retainedRows;expiredRows=p.expiredRows;evictedRows=p.evictedRows;modelSignature=p.modelSignature;}
    }
    private static final class Db extends SQLiteOpenHelper {
        Db(Context c){super(c,DATABASE_NAME,null,1);setWriteAheadLoggingEnabled(true);}
        public void onCreate(SQLiteDatabase db){
            db.execSQL("CREATE TABLE visitors (_id INTEGER PRIMARY KEY AUTOINCREMENT, embedding BLOB NOT NULL, model_signature TEXT NOT NULL, first_wall INTEGER NOT NULL, last_wall INTEGER NOT NULL, last_elapsed INTEGER NOT NULL, seen INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX visitors_last_seen ON visitors(last_wall,_id)");
        }
        public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("방문자 저장 버전을 확인해 주세요");}
    }
    public VisitorRegistry(Context context){this(context,Registry.DEFAULT_THRESHOLD);}
    public VisitorRegistry(Context context,float threshold){this(context,threshold,DEFAULT_TTL_MS,DEFAULT_MAX_ROWS);}
    public VisitorRegistry(Context context,float threshold,long ttlMs,int maxRows){
        VisitorPolicy.validate(threshold,ttlMs,maxRows);this.threshold=threshold;this.ttl=ttlMs;this.maxRows=maxRows;helper=new Db(context.getApplicationContext());
    }
    /** Production entry: sample time after SQLite admits the writer, never before waiting for its lock. */
    public synchronized Result resolve(List<Observations.Face> faces){
        return resolveAtCommit(faces,()->new VisitorPolicy.Stamp(System.currentTimeMillis(),SystemClock.elapsedRealtime()));
    }
    /** Explicit-clock entry for deterministic replay; live sessions use resolve(faces). */
    public synchronized Result resolve(List<Observations.Face> faces,long nowWall,long elapsed){
        return resolveAtCommit(faces,()->new VisitorPolicy.Stamp(nowWall,elapsed));
    }
    private Result resolveAtCommit(List<Observations.Face> faces,VisitorPolicy.Clock clock){
        if(closed)throw new IllegalStateException("방문자 목록이 닫혔습니다");
        SQLiteDatabase db=helper.getWritableDatabase();VisitorPolicy.Plan p;
        db.beginTransaction();
        try{
            List<VisitorPolicy.Row> rows=new ArrayList<>();List<Long> corrupt=new ArrayList<>();
            try(Cursor c=db.query("visitors",null,null,null,null,null,"_id ASC")){
                while(c.moveToNext()){
                    long id=c.getLong(c.getColumnIndexOrThrow("_id"));
                    try{rows.add(new VisitorPolicy.Row(id,c.getString(c.getColumnIndexOrThrow("model_signature")),unpack(c.getBlob(c.getColumnIndexOrThrow("embedding"))),c.getLong(c.getColumnIndexOrThrow("first_wall")),c.getLong(c.getColumnIndexOrThrow("last_wall")),c.getLong(c.getColumnIndexOrThrow("last_elapsed")),c.getLong(c.getColumnIndexOrThrow("seen"))));}
                    catch(IllegalArgumentException invalid){corrupt.add(id);}
                }
            }
            p=VisitorPolicy.planAtCommit(rows,faces,clock,threshold,ttl,maxRows);
            p.expiredRows+=corrupt.size();p.deletedIds.addAll(corrupt);
            for(long id:p.deletedIds)db.delete("visitors","_id=?",new String[]{Long.toString(id)});
            for(VisitorPolicy.Row row:p.updates){
                ContentValues v=new ContentValues();v.put("last_wall",row.lastWall);v.put("last_elapsed",row.lastElapsed);v.put("seen",row.seen);
                if(db.update("visitors",v,"_id=?",new String[]{Long.toString(row.id)})!=1)throw new IllegalStateException("방문자 기록을 갱신할 수 없습니다");
            }
            for(VisitorPolicy.Addition addition:p.additions){
                ContentValues v=new ContentValues();v.put("embedding",pack(addition.embedding));v.put("model_signature",Registry.MODEL_SIGNATURE);v.put("first_wall",p.commitWall);v.put("last_wall",p.commitWall);v.put("last_elapsed",p.commitElapsed);v.put("seen",1);
                long id=db.insertOrThrow("visitors",null,v);p.visitorIds[addition.faceIndex]=id;
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return new Result(p);
    }
    private static byte[] pack(float[] values){ByteBuffer b=ByteBuffer.allocate(Registry.DIMENSION*4).order(ByteOrder.LITTLE_ENDIAN);for(float f:Registry.normalize(values))b.putFloat(f);return b.array();}
    private static float[] unpack(byte[] bytes){
        if(bytes==null||bytes.length!=Registry.DIMENSION*4)throw new IllegalArgumentException("방문자 특징 형식을 확인해 주세요");
        ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);float[] vector=new float[Registry.DIMENSION];for(int i=0;i<vector.length;i++)vector[i]=b.getFloat();return Registry.normalize(vector);
    }
    public synchronized void close(){if(closed)return;closed=true;helper.close();}
}
