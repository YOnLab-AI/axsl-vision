package com.yonlab.vision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure planning used by VisitorRegistry's SQLite transaction and host policy tests. */
final class VisitorPolicy {
    interface Clock { Stamp read(); }
    static final class Stamp {
        final long wall,elapsed;
        Stamp(long wall,long elapsed){this.wall=wall;this.elapsed=elapsed;}
    }
    static final class Row {
        final long id,firstWall;
        final String signature;
        final float[] embedding;
        long lastWall,lastElapsed,seen;
        Row(long id,String signature,float[] vector,long firstWall,long lastWall,long lastElapsed,long seen){
            this.id=id;this.signature=signature;this.embedding=Registry.normalize(vector);this.firstWall=firstWall;this.lastWall=lastWall;this.lastElapsed=lastElapsed;this.seen=seen;
        }
        Row copy(){return new Row(id,signature,embedding,firstWall,lastWall,lastElapsed,seen);}
    }
    static final class Addition {
        final int faceIndex;final float[] embedding;
        Addition(int faceIndex,float[] embedding){this.faceIndex=faceIndex;this.embedding=embedding.clone();}
    }
    static final class Plan {
        final long[] visitorIds;
        final float[] similarities;
        final boolean[] created;
        final List<Long> deletedIds=new ArrayList<>();
        final List<Row> updates=new ArrayList<>();
        final List<Addition> additions=new ArrayList<>();
        final String modelSignature=Registry.MODEL_SIGNATURE;
        int newVisitors,matchedVisitors,unresolved,retainedRows,expiredRows,evictedRows;
        long commitWall,commitElapsed;
        Plan(int count){visitorIds=new long[count];Arrays.fill(visitorIds,-1);similarities=new float[count];Arrays.fill(similarities,Float.NaN);created=new boolean[count];}
    }
    /** Called only after the database writer transaction has been admitted. */
    static Plan planAtCommit(List<Row> inputRows,List<Observations.Face> faces,Clock clock,float threshold,long ttl,int maxRows){
        if(clock==null)throw new IllegalArgumentException("관측 시각을 확인해 주세요");
        Stamp stamp=clock.read();if(stamp==null)throw new IllegalArgumentException("관측 시각을 확인해 주세요");
        return plan(inputRows,faces,stamp.wall,stamp.elapsed,threshold,ttl,maxRows);
    }
    private static final class Edge {
        final int face;final Row row;final float score;
        Edge(int face,Row row,float score){this.face=face;this.row=row;this.score=score;}
    }
    static void validate(float threshold,long ttl,int maxRows){
        if(Float.isNaN(threshold)||Float.isInfinite(threshold)||threshold < -1||threshold>1)throw new IllegalArgumentException("방문자 비교 기준을 확인해 주세요");
        if(ttl<1||maxRows<1||maxRows>5000)throw new IllegalArgumentException("방문자 보관 설정을 확인해 주세요");
    }
    private static boolean expired(Row row,long wall,long elapsed,long ttl){
        if(row.id<1||row.firstWall<0||row.lastWall<row.firstWall||row.lastElapsed<0||row.seen<1||!Registry.MODEL_SIGNATURE.equals(row.signature))return true;
        // Wall-clock rollback expires conservatively, instead of extending retention indefinitely.
        if(wall<row.lastWall||wall-row.lastWall>=ttl)return true;
        // A monotonic reset uses wall age. Otherwise either elapsed age or wall age may expire a row.
        // Conservative expiry also bounds retention while the wall clock is frozen or adjusted.
        if(elapsed>=row.lastElapsed&&elapsed-row.lastElapsed>=ttl)return true;
        return false;
    }
    static Plan plan(List<Row> inputRows,List<Observations.Face> faces,long wall,long elapsed,float threshold,long ttl,int maxRows){
        validate(threshold,ttl,maxRows);
        if(wall<0||elapsed<0)throw new IllegalArgumentException("방문자 관측 시각을 확인해 주세요");
        int count=faces==null?0:faces.size();if(count>128)throw new IllegalArgumentException("한 번에 처리할 얼굴 수를 줄여 주세요");
        Plan p=new Plan(count);p.commitWall=wall;p.commitElapsed=elapsed;List<Row> rows=new ArrayList<>();
        if(inputRows!=null)for(Row input:inputRows){
            if(input==null)continue;
            if(expired(input,wall,elapsed,ttl)){p.deletedIds.add(input.id);p.expiredRows++;}else rows.add(input.copy());
        }
        float[][] queries=new float[count][];
        for(int i=0;i<count;i++)try{Observations.Face f=faces.get(i);if(f!=null)queries[i]=Registry.normalize(f.embedding);}catch(IllegalArgumentException invalid){/* Remains unresolved; never invent a vector or identity. */}
        List<Edge> edges=new ArrayList<>();
        for(int i=0;i<count;i++)if(queries[i]!=null)for(Row row:rows){
            double sum=0;for(int j=0;j<Registry.DIMENSION;j++)sum+=(double)queries[i][j]*row.embedding[j];
            float score=(float)Math.max(-1,Math.min(1,sum));if(score>=threshold)edges.add(new Edge(i,row,score));
        }
        Collections.sort(edges,(a,b)->{int c=Float.compare(b.score,a.score);if(c==0)c=Long.compare(a.row.id,b.row.id);return c!=0?c:Integer.compare(a.face,b.face);});
        Set<Long> assigned=new HashSet<>();
        for(Edge edge:edges){
            if(p.matchedVisitors>=maxRows)break;
            if(p.visitorIds[edge.face]>0||assigned.contains(edge.row.id))continue;
            p.visitorIds[edge.face]=edge.row.id;p.similarities[edge.face]=edge.score;assigned.add(edge.row.id);p.matchedVisitors++;
            edge.row.lastWall=wall;edge.row.lastElapsed=elapsed;if(edge.row.seen<Long.MAX_VALUE)edge.row.seen++;p.updates.add(edge.row);
        }
        List<Row> evictable=new ArrayList<>();for(Row row:rows)if(!assigned.contains(row.id))evictable.add(row);
        Collections.sort(evictable,new Comparator<Row>() { public int compare(Row a,Row b) { int time=Long.compare(a.lastWall,b.lastWall); return time!=0?time:Long.compare(a.id,b.id); } });
        int pending=0;for(int i=0;i<count;i++)if(queries[i]!=null&&p.visitorIds[i]<0)pending++;
        int target=Math.min(maxRows,assigned.size()+pending),needed=Math.max(0,rows.size()+target-assigned.size()-maxRows);
        int remove=Math.min(needed,evictable.size());
        for(int i=0;i<remove;i++){Row row=evictable.get(i);p.deletedIds.add(row.id);rows.remove(row);p.evictedRows++;}
        int available=maxRows-rows.size();
        for(int i=0;i<count;i++)if(p.visitorIds[i]<0){
            if(queries[i]!=null&&available>0){p.additions.add(new Addition(i,queries[i]));p.created[i]=true;p.newVisitors++;available--;}else p.unresolved++;
        }
        p.retainedRows=rows.size()+p.newVisitors;
        return p;
    }
}
