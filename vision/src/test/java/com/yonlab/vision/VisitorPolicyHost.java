package com.yonlab.vision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Tests the exact production planning policy, not SQLite or model inference. */
public final class VisitorPolicyHost {
    private static int checks;
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;}
    private static float[] vector(int axis){float[] x=new float[128];x[axis]=1;return x;}
    private static Observations.Face face(float[] vector){Observations.Face f=new Observations.Face(0,0,1,1);f.embedding=vector;return f;}
    private static VisitorPolicy.Row row(long id,int axis,long wall,long elapsed){return new VisitorPolicy.Row(id,Registry.MODEL_SIGNATURE,vector(axis),wall,wall,elapsed,1);}
    private static VisitorPolicy.Plan plan(List<VisitorPolicy.Row> rows,List<Observations.Face> faces,long wall,long elapsed,long ttl,int cap){return VisitorPolicy.plan(rows,faces,wall,elapsed,.5f,ttl,cap);}
    public static void main(String[] args){
        VisitorPolicy.Plan p=plan(Collections.emptyList(),Arrays.asList(face(vector(0))),1000,100,10000,2);
        check(p.additions.size()==1&&p.newVisitors==1&&p.unresolved==0,"new real feature planned");
        check(p.visitorIds[0]==-1&&p.created[0],"SQL owns new ID allocation");
        float[] original=vector(0);p=plan(Collections.emptyList(),Arrays.asList(face(original)),1000,100,10000,2);original[0]=0;
        check(p.additions.get(0).embedding[0]==1,"defensive feature copy");
        p=plan(Arrays.asList(row(4,0,1000,100)),Arrays.asList(face(vector(0))),1200,300,10000,2);
        check(p.visitorIds[0]==4&&p.matchedVisitors==1&&p.newVisitors==0,"cross session exact feature resolves same long ID");
        check(p.updates.get(0).lastWall==1200&&p.updates.get(0).lastElapsed==300,"last seen refresh");
        check(p.updates.get(0).seen==2,"seen increments once per batch");
        p=plan(Arrays.asList(row(9,0,1000,100),row(3,0,1000,100)),Arrays.asList(face(vector(0))),1200,300,10000,3);
        check(p.visitorIds[0]==3,"equal score uses stable smallest ID");
        p=plan(Arrays.asList(row(3,0,1000,100),row(9,0,1000,100)),Arrays.asList(face(vector(0))),1200,300,10000,3);
        check(p.visitorIds[0]==3,"row order does not affect tie");
        p=plan(Arrays.asList(row(3,0,1000,100)),Arrays.asList(face(vector(0)),face(vector(0))),1200,300,10000,3);
        check(p.visitorIds[0]==3&&p.visitorIds[1]==-1&&p.additions.size()==1,"existing ID assigned at most once within frame");
        check(p.created[1]&&!p.created[0],"same frame collision becomes separate anonymous record");
        p=plan(Arrays.asList(row(2,1,1000,100),row(1,0,1000,100)),Arrays.asList(face(vector(0)),face(vector(1))),1200,300,10000,3);
        check(p.visitorIds[0]==1&&p.visitorIds[1]==2,"two independent faces resolve one to one");
        float[] half=vector(0);half[0]=.5f;half[1]=(float)Math.sqrt(.75);
        p=plan(Arrays.asList(row(1,0,1000,100)),Arrays.asList(face(half)),1200,300,10000,3);
        check(p.matchedVisitors==1,"inclusive threshold");
        p=plan(Arrays.asList(row(1,0,1000,100)),Arrays.asList(face(vector(1))),1200,300,10000,3);
        check(p.newVisitors==1&&p.matchedVisitors==0,"below threshold creates new ID");
        p=plan(Arrays.asList(row(1,0,1000,100)),Collections.emptyList(),10999,10099,10000,3);
        check(p.expiredRows==0&&p.retainedRows==1,"TTL just before boundary retains");
        p=plan(Arrays.asList(row(1,0,1000,100)),Collections.emptyList(),11000,10100,10000,3);
        check(p.expiredRows==1&&p.retainedRows==0,"TTL exact boundary expires");
        p=plan(Arrays.asList(row(1,0,1000,100)),Collections.emptyList(),999,300,10000,3);
        check(p.expiredRows==1,"wall clock rollback conservatively expires");
        p=plan(Arrays.asList(row(1,0,1000,900)),Collections.emptyList(),1200,100,10000,3);
        check(p.retainedRows==1,"elapsed reset after reboot uses valid wall age");
        p=plan(Arrays.asList(row(1,0,1000,100)),Collections.emptyList(),1000,10100,10000,3);
        check(p.expiredRows==1,"same boot monotonic TTL prevents frozen wall retention");
        p=plan(Arrays.asList(row(1,0,1000,100)),Collections.emptyList(),1000,86_400_100,86_400_000,3);
        check(p.expiredRows==1,"default 24-hour TTL also expires with frozen wall");
        p=plan(Arrays.asList(row(1,0,1000,100),row(2,1,1100,200)),Arrays.asList(face(vector(2))),1200,300,10000,2);
        check(p.evictedRows==1&&p.deletedIds.contains(1L)&&p.retainedRows==2,"oldest unmatched row evicted at capacity");
        p=plan(Arrays.asList(row(1,0,1000,100)),Arrays.asList(face(vector(0)),face(vector(1))),1200,300,10000,1);
        check(p.matchedVisitors==1&&p.unresolved==1&&p.evictedRows==0,"current matches protected when full");
        p=plan(Collections.emptyList(),Arrays.asList(face(vector(0)),face(vector(1))),1200,300,10000,1);
        check(p.newVisitors==1&&p.unresolved==1&&p.retainedRows==1,"new batch honors row bound");
        p=plan(Collections.emptyList(),Arrays.asList(null,face(null),face(new float[128])),1200,300,10000,2);
        check(p.unresolved==3&&p.additions.isEmpty(),"missing invalid and zero vectors unresolved");
        float[] bad=vector(0);bad[1]=Float.NaN;p=plan(Collections.emptyList(),Arrays.asList(face(bad)),1200,300,10000,2);
        check(p.unresolved==1,"NaN vector unresolved");
        VisitorPolicy.Row other=new VisitorPolicy.Row(1,"other-model",vector(0),1000,1000,100,1);
        p=plan(Arrays.asList(other),Arrays.asList(face(vector(0))),1200,300,10000,2);
        check(p.expiredRows==1&&p.newVisitors==1,"other model record never matches");
        p=plan(Arrays.asList(row(1,0,1000,100),row(2,1,1000,100),row(3,2,1000,100)),Collections.emptyList(),1200,300,10000,1);
        check(p.evictedRows==2&&p.retainedRows==1,"reduced capacity pruned even without faces");
        check(p.deletedIds.equals(Arrays.asList(1L,2L)),"eviction tie deterministic");
        boolean rejected=false;try{plan(Collections.emptyList(),Collections.emptyList(),-1,0,1,1);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"negative time rejected");
        rejected=false;try{VisitorPolicy.plan(Collections.emptyList(),Collections.emptyList(),1,1,Float.NaN,1,1);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"invalid threshold rejected");
        rejected=false;try{plan(Collections.emptyList(),Collections.emptyList(),1,1,1,0);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"invalid row bound rejected");
        check(p.modelSignature.equals(Registry.MODEL_SIGNATURE),"result identifies model signature");
        System.out.println("Visitor policy PASS "+checks+" checks (production policy; no SQLite/model inference)");
    }
}
