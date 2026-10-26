package com.yonlab.vision;
import android.app.Activity;
import android.graphics.*;
import android.view.View;
import java.util.*;

/** The same center-fit geometry is used for photos and live camera overlays. */
public final class VisionCanvas extends View {
    private Bitmap bitmap;private List<Observations.Face> faces=Collections.emptyList();private List<Observations.Detection> detections=Collections.emptyList();
    private int sourceW=1,sourceH=1,selected=-1;private boolean mirror;private float lineX=-1;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final float density;
    public VisionCanvas(Activity a){super(a);density=a.getResources().getDisplayMetrics().density;setContentDescription("분석 사진과 검출 영역");}
    public void show(Bitmap image,List<Observations.Face> f,List<Observations.Detection> d){bitmap=image;sourceW=image==null?1:image.getWidth();sourceH=image==null?1:image.getHeight();faces=f==null?Collections.emptyList():new ArrayList<>(f);detections=d==null?Collections.emptyList():new ArrayList<>(d);mirror=false;invalidate();}
    public void overlay(List<Observations.Face> f,List<Observations.Detection> d,int w,int h,boolean m){bitmap=null;sourceW=Math.max(1,w);sourceH=Math.max(1,h);faces=f==null?Collections.emptyList():new ArrayList<>(f);detections=d==null?Collections.emptyList():new ArrayList<>(d);mirror=m;invalidate();}
    public void select(int index){selected=index;invalidate();}
    public void line(float value){lineX=value;invalidate();}
    protected void onDraw(Canvas canvas){super.onDraw(canvas);float s=Math.min(getWidth()/(float)sourceW,getHeight()/(float)sourceH),w=sourceW*s,h=sourceH*s,x=(getWidth()-w)/2,y=(getHeight()-h)/2;
        if(bitmap!=null&&!bitmap.isRecycled()){paint.setStyle(Paint.Style.FILL);canvas.drawBitmap(bitmap,null,new RectF(x,y,x+w,y+h),paint);}
        if(lineX>=0){paint.setColor(Ui.ACCENT);paint.setStrokeWidth(2*density);canvas.drawLine(x+lineX*w,y,x+lineX*w,y+h,paint);}
        int i=0;for(Observations.Face f:faces){String label=f.label==null||f.label.isEmpty()?"얼굴 "+(i+1):f.label;if(!Float.isNaN(f.similarity)&&!Float.isInfinite(f.similarity))label+=" · "+String.format(Locale.ROOT,"%.3f",f.similarity);box(canvas,x,y,w,h,f.left,f.top,f.right,f.bottom,label,i==selected?Ui.SUCCESS:Ui.ACCENT);i++;}
        for(Observations.Detection d:detections){String label=OneScoreLabels.detection(localized(d.label),d.score);box(canvas,x,y,w,h,d.left,d.top,d.right,d.bottom,label,Ui.ACCENT);}
    }
    private void box(Canvas c,float x,float y,float w,float h,float l,float t,float r,float b,String label,int color){if(mirror){float a=l;l=1-r;r=1-a;}RectF box=new RectF(x+l*w,y+t*h,x+r*w,y+b*h);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.5f*density);paint.setColor(color);c.drawRect(box,paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(13*density);float textWidth=Math.min(w,paint.measureText(label)+12*density),baseline=Math.max(y+19*density,box.top-5*density);float left=Math.min(box.left,x+w-textWidth);c.drawRoundRect(new RectF(left,baseline-17*density,left+textWidth,baseline+5*density),4*density,4*density,paint);paint.setColor(Color.WHITE);c.drawText(label,left+6*density,baseline,paint);}
    private static String localized(String s){if("person".equals(s))return "사람";if("dog".equals(s))return "개";if("cat".equals(s))return "고양이";if("car".equals(s))return "자동차";return s==null?"객체":s;}
}
