package com.yonlab.vision;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** App-private SFace registry. Legacy SDK vectors and databases are never imported implicitly. */
public final class Registry implements AutoCloseable {
    public static final int DIMENSION=128;
    public static final String SFACE_SHA256="0ba9fbfa01b5270c96627c4ef784da859931e02f04419c829e83484087c34e79";
    public static final String MODEL_SIGNATURE="opencv-sface-2021dec:sha256:"+SFACE_SHA256;
    public static final float DEFAULT_THRESHOLD=.363f;
    private static final int MAX_BACKUP_ROWS=5000;
    private final Db helper;
    private boolean closed;

    public static final class Entry {
        public final long id,createdAt,updatedAt;
        public final String name,modelSignature,uuid;
        public final boolean archived;
        public final float[] embedding;
        public Entry(long id,String name,boolean archived,long createdAt,long updatedAt,String signature,float[] embedding){
            this(id,name,archived,createdAt,updatedAt,signature,embedding,"");
        }
        private Entry(long id,String name,boolean archived,long createdAt,long updatedAt,String signature,float[] embedding,String uuid){
            if(id<1||createdAt<0||updatedAt<0||signature==null)throw new IllegalArgumentException("등록 정보가 올바르지 않습니다");
            this.id=id;this.name=validName(name);this.archived=archived;this.createdAt=createdAt;this.updatedAt=updatedAt;
            this.modelSignature=signature;this.embedding=normalize(embedding);this.uuid=uuid;
        }
    }
    public static final class Match {
        public final long id;
        public final String name;
        public final float similarity;
        public final boolean matched;
        private Match(long id,String name,float similarity,boolean matched){this.id=id;this.name=name;this.similarity=similarity;this.matched=matched;}
    }
    private static final class Db extends SQLiteOpenHelper {
        Db(Context c){super(c,"yon-sface-registry.db",null,1);setWriteAheadLoggingEnabled(true);}
        public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE faces (_id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT NOT NULL UNIQUE, name TEXT NOT NULL, embedding BLOB NOT NULL, model_signature TEXT NOT NULL, archived INTEGER NOT NULL DEFAULT 0 CHECK(archived IN (0,1)), created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");}
        public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("등록 데이터 버전을 확인해 주세요");}
    }
    public Registry(Context context){helper=new Db(context.getApplicationContext());}
    private SQLiteDatabase db(){if(closed)throw new IllegalStateException("등록 목록이 닫혔습니다");return helper.getWritableDatabase();}

    public static float[] normalize(float[] value){
        if(value==null||value.length!=DIMENSION)throw new IllegalArgumentException("얼굴 특징 형식이 올바르지 않습니다");
        double squared=0;
        for(float v:value){if(Float.isNaN(v)||Float.isInfinite(v))throw new IllegalArgumentException("얼굴 특징에 유효하지 않은 값이 있습니다");squared+=(double)v*v;}
        if(!(squared>0)||Double.isNaN(squared)||Double.isInfinite(squared))throw new IllegalArgumentException("얼굴 특징이 비어 있습니다");
        double norm=Math.sqrt(squared);float[] copy=new float[DIMENSION];
        for(int i=0;i<copy.length;i++)copy[i]=(float)(value[i]/norm);
        return copy;
    }
    public static float cosine(float[] first,float[] second){
        float[] a=normalize(first),b=normalize(second);double sum=0;
        for(int i=0;i<DIMENSION;i++)sum+=(double)a[i]*b[i];
        return (float)Math.max(-1,Math.min(1,sum));
    }
    public static Match choose(List<Entry> entries,float[] embedding,float threshold){
        if(Float.isNaN(threshold)||Float.isInfinite(threshold)||threshold < -1||threshold>1)throw new IllegalArgumentException("비교 기준은 -1부터 1까지 입력하세요");
        float[] query=normalize(embedding);Entry best=null;float score=Float.NaN;
        if(entries!=null)for(Entry item:entries){
            if(item==null||item.archived||!MODEL_SIGNATURE.equals(item.modelSignature))continue;
            float current=cosine(query,item.embedding);
            if(best==null||current>score||(Float.compare(current,score)==0&&item.id<best.id)){best=item;score=current;}
        }
        boolean accepted=best!=null&&score>=threshold;
        return new Match(accepted?best.id:-1,accepted?best.name:"",score,accepted);
    }
    private static String validName(String name){
        if(name==null)throw new IllegalArgumentException("이름을 입력하세요");
        String clean=name.trim();if(clean.isEmpty()||clean.length()>80)throw new IllegalArgumentException("이름은 1~80자로 입력하세요");
        for(int i=0;i<clean.length();i++)if(Character.isISOControl(clean.charAt(i)))throw new IllegalArgumentException("이름을 확인해 주세요");
        return clean;
    }
    private static byte[] pack(float[] value){ByteBuffer b=ByteBuffer.allocate(DIMENSION*4).order(ByteOrder.LITTLE_ENDIAN);for(float f:normalize(value))b.putFloat(f);return b.array();}
    private static float[] unpack(byte[] value){
        if(value==null||value.length!=DIMENSION*4)throw new IllegalArgumentException("저장된 얼굴 특징을 확인해 주세요");
        ByteBuffer b=ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);float[] result=new float[DIMENSION];for(int i=0;i<result.length;i++)result[i]=b.getFloat();return normalize(result);
    }
    public synchronized List<Entry> entries(boolean includeArchived){
        List<Entry> result=new ArrayList<>();
        try(Cursor c=db().query("faces",null,includeArchived?null:"archived=0",null,null,null,"_id ASC")){
            while(c.moveToNext())result.add(new Entry(c.getLong(c.getColumnIndexOrThrow("_id")),c.getString(c.getColumnIndexOrThrow("name")),c.getInt(c.getColumnIndexOrThrow("archived"))!=0,c.getLong(c.getColumnIndexOrThrow("created_at")),c.getLong(c.getColumnIndexOrThrow("updated_at")),c.getString(c.getColumnIndexOrThrow("model_signature")),unpack(c.getBlob(c.getColumnIndexOrThrow("embedding"))),c.getString(c.getColumnIndexOrThrow("uuid"))));
        }
        return Collections.unmodifiableList(result);
    }
    public synchronized long enroll(String name,float[] embedding){
        String clean=validName(name);byte[] feature=pack(embedding);long now=System.currentTimeMillis();
        ContentValues v=values(UUID.randomUUID().toString(),clean,feature,MODEL_SIGNATURE,false,now,now);
        return db().insertOrThrow("faces",null,v);
    }
    public synchronized Match match(float[] embedding,float threshold){return choose(entries(false),embedding,threshold);}
    public synchronized void rename(long id,String name){
        ContentValues v=new ContentValues();v.put("name",validName(name));v.put("updated_at",System.currentTimeMillis());
        if(id<1||db().update("faces",v,"_id=?",new String[]{Long.toString(id)})!=1)throw new IllegalArgumentException("등록 항목을 찾을 수 없습니다");
    }
    public synchronized void archive(long id,boolean archived){
        ContentValues v=new ContentValues();v.put("archived",archived?1:0);v.put("updated_at",System.currentTimeMillis());
        if(id<1||db().update("faces",v,"_id=?",new String[]{Long.toString(id)})!=1)throw new IllegalArgumentException("등록 항목을 찾을 수 없습니다");
    }
    /** Explicit user backup, unlike diagnostic JSON this contains enrollment vectors. */
    public synchronized JSONObject exportJson()throws JSONException{
        List<Entry> all=entries(true);if(all.size()>MAX_BACKUP_ROWS)throw new IllegalStateException("내보낼 항목이 너무 많습니다");
        JSONArray rows=new JSONArray();
        for(Entry e:all){
            if(!MODEL_SIGNATURE.equals(e.modelSignature))throw new IllegalStateException("등록 모델 버전을 확인해 주세요");
            JSONArray vector=new JSONArray();for(float f:e.embedding)vector.put((double)f);
            rows.put(new JSONObject().put("uuid",e.uuid).put("name",e.name).put("archived",e.archived).put("createdAt",e.createdAt).put("updatedAt",e.updatedAt).put("modelSignature",e.modelSignature).put("embedding",vector));
        }
        return new JSONObject().put("format","yonlab-face-registry").put("version",1).put("modelSignature",MODEL_SIGNATURE).put("dimension",DIMENSION).put("exportedAt",System.currentTimeMillis()).put("entries",rows);
    }
    /** Complete validation without opening or modifying the registry. Used before import confirmation. */
    public static int inspectImport(JSONObject document)throws JSONException{return parseDocument(document).size();}
    private static List<ContentValues> parseDocument(JSONObject document)throws JSONException{
        if(document==null||!"yonlab-face-registry".equals(document.getString("format"))||document.getInt("version")!=1||document.getInt("dimension")!=DIMENSION||!MODEL_SIGNATURE.equals(document.getString("modelSignature")))throw new IllegalArgumentException("이 등록 파일의 모델 또는 버전이 다릅니다");
        JSONArray rows=document.getJSONArray("entries");if(rows.length()>MAX_BACKUP_ROWS)throw new IllegalArgumentException("가져올 항목이 너무 많습니다");
        List<ContentValues> validated=new ArrayList<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);String uid=UUID.fromString(row.getString("uuid")).toString();
            String signature=row.getString("modelSignature");if(!MODEL_SIGNATURE.equals(signature))throw new IllegalArgumentException("등록 모델이 일치하지 않습니다");
            JSONArray data=row.getJSONArray("embedding");if(data.length()!=DIMENSION)throw new IllegalArgumentException("얼굴 특징 차원이 다릅니다");
            float[] vector=new float[DIMENSION];for(int j=0;j<vector.length;j++){double value=data.getDouble(j);if(Double.isNaN(value)||Double.isInfinite(value)||Math.abs(value)>Float.MAX_VALUE)throw new IllegalArgumentException("얼굴 특징 값이 올바르지 않습니다");vector[j]=(float)value;}
            long created=row.getLong("createdAt"),updated=row.getLong("updatedAt");if(created<0||updated<0)throw new IllegalArgumentException("등록 시각이 올바르지 않습니다");
            validated.add(values(uid,validName(row.getString("name")),pack(vector),signature,row.getBoolean("archived"),created,updated));
        }
        return validated;
    }
    /** Revalidates before a single transaction; duplicate UUIDs preserve current rows. */
    public synchronized JSONObject importJson(JSONObject document)throws JSONException{
        List<ContentValues> validated=parseDocument(document);
        SQLiteDatabase db=db();int inserted=0,skipped=0;db.beginTransaction();
        try{
            for(ContentValues v:validated){
                boolean exists;try(Cursor c=db.query("faces",new String[]{"_id"},"uuid=?",new String[]{v.getAsString("uuid")},null,null,null)){exists=c.moveToFirst();}
                if(exists){skipped++;continue;}db.insertOrThrow("faces",null,v);inserted++;
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return new JSONObject().put("inserted",inserted).put("skipped",skipped);
    }
    private static ContentValues values(String uuid,String name,byte[] feature,String signature,boolean archived,long created,long updated){
        ContentValues v=new ContentValues();v.put("uuid",uuid);v.put("name",name);v.put("embedding",feature);v.put("model_signature",signature);v.put("archived",archived?1:0);v.put("created_at",created);v.put("updated_at",updated);return v;
    }
    public synchronized void close(){if(closed)return;closed=true;helper.close();}
}
