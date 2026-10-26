package com.yonlab.vision;

import java.util.*;

/** Portable rules over actual observations. All times are monotonic elapsed milliseconds.
 * Session counts describe tracks, not permanent identities. No age/gender is estimated. */
public final class Analytics {
 private Analytics(){}
 private static boolean finite(float value){return !Float.isNaN(value)&&!Float.isInfinite(value);}
 private static boolean box(float l,float t,float r,float b){return finite(l)&&finite(t)&&finite(r)&&finite(b)&&l>=0&&t>=0&&r<=1&&b<=1&&r>l&&b>t;}
 private static boolean face(Observations.Face f){return f!=null&&box(f.left,f.top,f.right,f.bottom);}
 private static float iou(float l,float t,float r,float b,float l2,float t2,float r2,float b2){float intersection=Math.max(0,Math.min(r,r2)-Math.max(l,l2))*Math.max(0,Math.min(b,b2)-Math.max(t,t2));float union=(r-l)*(b-t)+(r2-l2)*(b2-t2)-intersection;return union>0?intersection/union:0;}
 public static final class Track {
  public final int id;public final float left,top,right,bottom;public final long lastSeen;
  Track(Moving p){id=p.id;left=p.l;top=p.t;right=p.r;bottom=p.b;lastSeen=p.seen;}
 }
 public static final class FlowSnapshot {
  public final int leftToRight,rightToLeft,activeTracks,total;public final long frames;public final List<Track> tracks;
  FlowSnapshot(int lr,int rl,long n,List<Moving> current){leftToRight=lr;rightToLeft=rl;total=lr+rl;frames=n;ArrayList<Track> copy=new ArrayList<>();for(Moving m:current)copy.add(new Track(m));tracks=Collections.unmodifiableList(copy);activeTracks=copy.size();}
 }
 private static final class Moving {
  int id,side;float l,t,r,b,vx,vy;long seen;
  Moving(int i,Observations.Detection d,long now){id=i;set(d,now);}
  void set(Observations.Detection d,long now){if(seen!=now){vx=(d.left+d.right-l-r)/2;vy=(d.top+d.bottom-t-b)/2;}l=d.left;t=d.top;r=d.right;b=d.bottom;seen=now;}
 }
 private static final class Edge {int track,observation;float cost;Edge(int a,int b,float c){track=a;observation=b;cost=c;}}
 public static final class Flow {
  private final float lineX,hysteresis;private final long ttlMs;private final ArrayList<Moving> tracks=new ArrayList<>();private int nextId=1,lr,rl;private long frames,last=-1;
  public Flow(){this(.5f,.04f,1500);}
  public Flow(float lineX,float hysteresis,long ttlMs){if(!finite(lineX)||lineX<=0||lineX>=1||!finite(hysteresis)||hysteresis<0||hysteresis>=Math.min(lineX,1-lineX)||ttlMs<1)throw new IllegalArgumentException("line/hysteresis/ttl");this.lineX=lineX;this.hysteresis=hysteresis;this.ttlMs=ttlMs;}
  public synchronized FlowSnapshot update(List<Observations.Detection> input,long now){if(now<0)throw new IllegalArgumentException("time");if(last>=0&&now<last)reset();last=now;frames++;for(Iterator<Moving> it=tracks.iterator();it.hasNext();)if(now-it.next().seen>ttlMs)it.remove();
   ArrayList<Observations.Detection> valid=new ArrayList<>();if(input!=null)for(Observations.Detection d:input)if(d!=null&&d.classId==0&&finite(d.score)&&d.score>=.25f&&box(d.left,d.top,d.right,d.bottom))valid.add(d);
   ArrayList<Edge> edges=new ArrayList<>();for(int i=0;i<tracks.size();i++){Moving m=tracks.get(i);float cx=(m.l+m.r)/2,cy=(m.t+m.b)/2;for(int j=0;j<valid.size();j++){Observations.Detection d=valid.get(j);float dx=(d.left+d.right)/2-cx,dy=(d.top+d.bottom)/2-cy;float distance=(float)Math.hypot(dx,dy),overlap=iou(m.l,m.t,m.r,m.b,d.left,d.top,d.right,d.bottom);if(distance<=.26f&&(overlap>.01f||distance<=.21f)){float predicted=(float)Math.hypot(dx-m.vx,dy-m.vy);edges.add(new Edge(i,j,predicted*.75f+distance*.25f+(1-overlap)*.025f));}}}
   Collections.sort(edges,(a,b)->Float.compare(a.cost,b.cost));boolean[] usedT=new boolean[tracks.size()],usedD=new boolean[valid.size()];for(Edge e:edges){if(usedT[e.track]||usedD[e.observation])continue;usedT[e.track]=true;usedD[e.observation]=true;Moving m=tracks.get(e.track);m.set(valid.get(e.observation),now);cross(m);}
   for(int j=0;j<valid.size();j++)if(!usedD[j]){Moving m=new Moving(nextId++,valid.get(j),now);m.vx=m.vy=0;m.side=side((m.l+m.r)/2);tracks.add(m);}return snapshot();
  }
  private int side(float x){return x<lineX-hysteresis?-1:x>lineX+hysteresis?1:0;}
  private void cross(Moving m){int s=side((m.l+m.r)/2);if(s!=0){if(m.side==-1&&s==1)lr++;if(m.side==1&&s==-1)rl++;m.side=s;}}
  public synchronized FlowSnapshot snapshot(){return new FlowSnapshot(lr,rl,frames,tracks);}
  public synchronized void pause(){tracks.clear();last=-1;}
  public synchronized void reset(){tracks.clear();nextId=1;lr=rl=0;frames=0;last=-1;}
 }
 public static final class DriveSnapshot {
  public final boolean facePresent,eyesClosed,drowsy,lookingAway,mouthOpen,mouthKnown,calibrated;
  public final long closedMs,lookAwayMs,mouthOpenMs,blinks;
  public final float yaw,pitch,rawYaw,rawPitch,baselineYaw,baselinePitch,mouthRatio,leftEyeAspect,rightEyeAspect;
  public final boolean eyeAspectBased;
  public final String status;
  DriveSnapshot(boolean present,boolean eyes,boolean tired,boolean away,boolean known,float ratio,long mouthDuration,long closed,long look,long blink,float y,float p,float rawY,float rawP,float baseY,float baseP,boolean cal,float earLeft,float earRight){
   facePresent=present;eyesClosed=eyes;drowsy=tired;lookingAway=away;mouthKnown=known;mouthRatio=known?ratio:Float.NaN;mouthOpen=known&&ratio>=.6f;mouthOpenMs=mouthDuration;
   closedMs=closed;lookAwayMs=look;blinks=blink;yaw=y;pitch=p;rawYaw=rawY;rawPitch=rawP;baselineYaw=baseY;baselinePitch=baseP;calibrated=cal;leftEyeAspect=earLeft;rightEyeAspect=earRight;eyeAspectBased=finite(earLeft)&&finite(earRight)&&earLeft>=0&&earLeft<=1&&earRight>=0&&earRight<=1;
   status=!present?"얼굴을 맞춰주세요":tired?"눈 감음 지속":away?"시선 이탈":eyes?"눈 감음":mouthOpen?"입 벌림":"얼굴 확인";
  }
 }
 public static final class Drive {
  public static final long MAX_OBSERVATION_GAP_MS=1500;
  private final long eyeThreshold,awayThreshold;
  private long closedStart=-1,awayStart=-1,mouthStart=-1,blinks,last=-1;
  private int driver=-1,calibrationDriver=-1;
  private boolean hadFace,calibrated;
  private float baselineYaw,baselinePitch,rawYaw=Float.NaN,rawPitch=Float.NaN,leftEyeAspect=Float.NaN,rightEyeAspect=Float.NaN;
  private DriveSnapshot snapshot;
  public Drive(){this(1000,800);}
  public Drive(long eyeClosedMs,long lookAwayMs){if(eyeClosedMs<1||lookAwayMs<1)throw new IllegalArgumentException("duration");eyeThreshold=eyeClosedMs;awayThreshold=lookAwayMs;snapshot=state(false,false,false,false,false,Float.NaN,0,0,0);}
  private static float relative(float raw,float baseline){if(!finite(raw))return Float.NaN;double value=((double)raw-baseline+180)%360;if(value<0)value+=360;return (float)(value-180);}
  private DriveSnapshot state(boolean present,boolean closed,boolean tired,boolean away,boolean mouthKnown,float mouthRatio,long mouthDuration,long close,long look){return new DriveSnapshot(present,closed,tired,away,mouthKnown,mouthRatio,mouthDuration,close,look,blinks,relative(rawYaw,baselineYaw),relative(rawPitch,baselinePitch),rawYaw,rawPitch,baselineYaw,baselinePitch,calibrated,leftEyeAspect,rightEyeAspect);}
  private void breakObservation(){closedStart=awayStart=mouthStart=-1;driver=-1;hadFace=false;rawYaw=rawPitch=leftEyeAspect=rightEyeAspect=Float.NaN;}
  private void clearNeutral(){baselineYaw=baselinePitch=0;calibrated=false;calibrationDriver=-1;awayStart=-1;}
  public synchronized DriveSnapshot update(List<Observations.Face> faces,long now){
   if(now<0)throw new IllegalArgumentException("time");
   if(last>=0&&now<last)resetCounts();
   else if(last>=0&&now-last>MAX_OBSERVATION_GAP_MS)breakObservation();
   last=now;Observations.Face best=null;
   if(faces!=null)for(Observations.Face f:faces)if(face(f)&&(best==null||(f.right-f.left)*(f.bottom-f.top)>(best.right-best.left)*(best.bottom-best.top)))best=f;
   if(best==null){breakObservation();return snapshot=state(false,false,false,false,false,Float.NaN,0,0,0);}
   if(calibrated&&best.trackingId>=0){if(calibrationDriver>=0&&best.trackingId!=calibrationDriver)clearNeutral();else calibrationDriver=best.trackingId;}
   if(!hadFace||(best.trackingId>=0&&driver>=0&&best.trackingId!=driver))closedStart=awayStart=mouthStart=-1;
   driver=best.trackingId;hadFace=true;rawYaw=best.yaw;rawPitch=best.pitch;
   leftEyeAspect=best.leftEyeAspect;rightEyeAspect=best.rightEyeAspect;boolean useEar=finite(leftEyeAspect)&&finite(rightEyeAspect)&&leftEyeAspect>=0&&leftEyeAspect<=1&&rightEyeAspect>=0&&rightEyeAspect<=1;
   boolean known=useEar||best.leftEye>=0&&best.leftEye<=1&&best.rightEye>=0&&best.rightEye<=1;
   boolean closed=known&&(useEar?leftEyeAspect<.18f&&rightEyeAspect<.18f:best.leftEye<.35f&&best.rightEye<.35f);long duration=0;
   if(closed){if(closedStart<0)closedStart=now;duration=now-closedStart;}else{if(known&&closedStart>=0){long d=now-closedStart;if(d>=60&&d<=1500)blinks++;}closedStart=-1;}
   float y=relative(rawYaw,baselineYaw),p=relative(rawPitch,baselinePitch);boolean off=(finite(y)&&Math.abs(y)>25)||(finite(p)&&Math.abs(p)>20);long look=0;
   if(off){if(awayStart<0)awayStart=now;look=now-awayStart;}else awayStart=-1;
   boolean mouthKnown=finite(best.mouthOpen)&&best.mouthOpen>=0&&best.mouthOpen<=1;long mouthDuration=0;
   if(mouthKnown&&best.mouthOpen>=.6f){if(mouthStart<0)mouthStart=now;mouthDuration=now-mouthStart;}else mouthStart=-1;
   return snapshot=state(true,closed,closed&&duration>=eyeThreshold,off&&look>=awayThreshold,mouthKnown,best.mouthOpen,mouthDuration,duration,look);
  }
  public synchronized boolean calibrate(){if(!snapshot.facePresent||!finite(rawYaw)||!finite(rawPitch))return false;baselineYaw=rawYaw;baselinePitch=rawPitch;calibrated=true;calibrationDriver=driver;awayStart=-1;snapshot=state(true,snapshot.eyesClosed,snapshot.drowsy,false,snapshot.mouthKnown,snapshot.mouthRatio,snapshot.mouthOpenMs,snapshot.closedMs,0);return true;}
  public synchronized void resetCalibration(){clearNeutral();snapshot=state(snapshot.facePresent,snapshot.eyesClosed,snapshot.drowsy,false,snapshot.mouthKnown,snapshot.mouthRatio,snapshot.mouthOpenMs,snapshot.closedMs,0);}
  public synchronized DriveSnapshot snapshot(){return snapshot;}
  /** Explicit pause preserves the user's neutral; tracking IDs belong to each new detector session. */
  public synchronized void pause(){breakObservation();last=-1;calibrationDriver=-1;snapshot=state(false,false,false,false,false,Float.NaN,0,0,0);}
  public synchronized void resetCounts(){blinks=0;pause();}
  public synchronized void reset(){clearNeutral();resetCounts();}
 }
 public static final class VisitorSnapshot {
  /** unique is the historical session-count field; distinct is anonymous IDs within this counter scope. */
  public final int visible,unique,sessions,distinct,smiling,untracked;public final boolean distinctComplete;public final long totalDwellMs;public final float meanDwellMs;
  VisitorSnapshot(int v,int visits,int identities,boolean exact,int s,int un,long dwell){visible=v;unique=visits;sessions=visits;distinct=identities;distinctComplete=exact;smiling=s;untracked=un;totalDwellMs=dwell;meanDwellMs=visits==0?0:(float)dwell/visits;}
 }
 private static final class Visit {long seen,frame;Visit(long n,long f){seen=n;frame=f;}}
 public static final class Visitors {
  public static final int MAX_DISTINCT_IDS=20000;
  private final long ttlMs;private final HashMap<Long,Visit> tracks=new HashMap<>();private final HashSet<Long> identities=new HashSet<>();private boolean distinctComplete=true;private int unique;private long dwell,last=-1,frame;private VisitorSnapshot snapshot=new VisitorSnapshot(0,0,0,true,0,0,0);
  public Visitors(){this(3000);}
  public Visitors(long ttlMs){if(ttlMs<1)throw new IllegalArgumentException("ttl");this.ttlMs=ttlMs;}
  public synchronized VisitorSnapshot update(List<Observations.Face> faces,long now){int n=faces==null?0:faces.size();long[] ids=new long[n];for(int i=0;i<n;i++)ids[i]=faces.get(i)==null?-1:faces.get(i).trackingId;return update(faces,ids,now);}
  /** Identity values may be persistent anonymous long IDs; they are never narrowed to int. */
  public synchronized VisitorSnapshot update(List<Observations.Face> faces,long[] ids,long now){if(now<0)throw new IllegalArgumentException("time");int size=faces==null?0:faces.size();if(ids==null||ids.length!=size)throw new IllegalArgumentException("identity count");if(last>=0&&now<last)reset();long previous=last;last=now;frame++;for(Iterator<Visit> it=tracks.values().iterator();it.hasNext();)if(now-it.next().seen>ttlMs)it.remove();HashSet<Long> seen=new HashSet<>();int visible=0,smiling=0,untracked=0;
   for(int i=0;i<size;i++){Observations.Face f=faces.get(i);if(!face(f))continue;long id=ids[i];if(id>=0&&!seen.add(id))continue;visible++;if(f.smile>=.5f&&f.smile<=1)smiling++;if(id<0){untracked++;continue;}if(!identities.contains(id)){if(identities.size()<MAX_DISTINCT_IDS)identities.add(id);else distinctComplete=false;}Visit v=tracks.get(id);if(v==null){unique++;v=new Visit(now,frame);tracks.put(id,v);}else{if(v.frame==frame-1&&previous>=0)dwell+=now-previous;v.seen=now;v.frame=frame;}}
   return snapshot=new VisitorSnapshot(visible,unique,identities.size(),distinctComplete,smiling,untracked,dwell);
  }
  public synchronized VisitorSnapshot snapshot(){return snapshot;}
  /** Preserve existing identities until TTL but exclude every paused interval from dwell. */
  public synchronized void pause(){last=-1;for(Visit v:tracks.values())v.frame=Long.MIN_VALUE;snapshot=new VisitorSnapshot(0,unique,identities.size(),distinctComplete,0,0,dwell);}
  public synchronized void reset(){tracks.clear();identities.clear();distinctComplete=true;unique=0;dwell=0;last=-1;frame=0;snapshot=new VisitorSnapshot(0,0,0,true,0,0,0);}
 }
}
