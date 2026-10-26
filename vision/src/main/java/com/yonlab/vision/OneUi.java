package com.yonlab.vision;

import android.app.Activity;
import android.graphics.*;
import android.view.*;
import android.widget.*;

/** Native One workspace styling. Illustrative lines are decoration, never analysis output. */
final class OneUi {
 private OneUi(){}
 static int dp(Activity a,int n){return Ui.dp(a,n);}
 static LinearLayout row(Activity a){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
 static LinearLayout col(Activity a){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.VERTICAL);return v;}
 static LinearLayout.LayoutParams fill(){return new LinearLayout.LayoutParams(-1,-2);}
 static void gap(Activity a,LinearLayout parent,int height){View v=new View(a);parent.addView(v,new LinearLayout.LayoutParams(1,dp(a,height)));}
 static TextView muted(Activity a,String text,int size){TextView t=Ui.text(a,text,size);t.setTextColor(Ui.MUTED);return t;}
 static void addCard(Activity a,LinearLayout parent,View card){LinearLayout.LayoutParams p=fill();p.bottomMargin=dp(a,12);parent.addView(card,p);}
 static LinearLayout header(Activity a,String title,Runnable close){LinearLayout h=Ui.brandHeader(a,title);if(close!=null)h.addView(Ui.secondary(a,"닫기",v->close.run()),new LinearLayout.LayoutParams(dp(a,72),-2));return h;}
 static View nav(Activity a,String label,String icon,boolean selected,boolean vertical,Runnable click){
  LinearLayout v=col(a);v.setGravity(Gravity.CENTER);v.setMinimumHeight(dp(a,64));v.setPadding(dp(a,6),dp(a,7),dp(a,6),dp(a,7));
  if(!vertical)v.setOrientation(LinearLayout.HORIZONTAL);
  v.addView(Ui.icon(a,icon,24));TextView t=Ui.heading(a,label,vertical?12:15);t.setGravity(Gravity.CENTER);
  if(!vertical)t.setPadding(dp(a,12),dp(a,8),0,dp(a,8));v.addView(t);t.setTextColor(Ui.interactionText(selected?Ui.ACCENT:Ui.MUTED));
  v.setBackground(Ui.interactiveSurface(a,Color.WHITE,12,false));Ui.selectionSemantics(v,selected);
  v.setClickable(true);v.setFocusable(true);v.setOnClickListener(w->click.run());v.setContentDescription(label);return v;
 }
 static LinearLayout taskCard(Activity a,OneCatalog.Feature f,Runnable click){
  LinearLayout c=Ui.panel(a);LinearLayout top=row(a);top.addView(Ui.icon(a,f.icon,28));TextView title=Ui.heading(a,f.title,19);title.setPadding(dp(a,10),dp(a,2),0,dp(a,2));top.addView(title,new LinearLayout.LayoutParams(0,-2,1));c.addView(top);
  c.addView(muted(a,f.summary,14),fill());TextView action=Ui.text(a,"열기  →",13);action.setTextColor(Ui.ACCENT);c.addView(action);
  c.setBackground(Ui.interactiveSurface(a,Ui.SURFACE,12,true));c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->click.run());c.setContentDescription(f.title+". "+f.summary+". 열기");Ui.actionSemantics(c);return c;
 }
 static TextView chip(Activity a,String text,boolean active,Runnable click){TextView t=Ui.pill(a,text,active);t.setMinHeight(dp(a,48));t.setMinWidth(dp(a,48));t.setGravity(Gravity.CENTER);t.setPadding(dp(a,14),dp(a,10),dp(a,14),dp(a,10));t.setTextColor(Ui.interactionText(active?Ui.ACCENT:Ui.MUTED));t.setBackground(Ui.interactiveSurface(a,Color.WHITE,20,false));Ui.selectionSemantics(t,active);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=dp(a,8);p.topMargin=dp(a,4);p.bottomMargin=dp(a,4);t.setLayoutParams(p);t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->click.run());return t;}
 static void horizontal(Activity a,LinearLayout parent,LinearLayout content){HorizontalScrollView scroll=new HorizontalScrollView(a);scroll.setHorizontalScrollBarEnabled(false);scroll.addView(content);parent.addView(scroll,fill());}
 static View choices(Activity a,View... views){return new ChoiceGrid(a,views);}
 private static final class ChoiceGrid extends ViewGroup {
  private final Activity owner;private final java.util.ArrayList<Integer> heights=new java.util.ArrayList<>();private final int gap;private int columns=1,childWidth;
  ChoiceGrid(Activity a,View[] views){super(a);owner=a;gap=dp(a,12);for(View view:views)addView(view);}
  @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);}
  @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec),inside=Math.max(1,width-getPaddingLeft()-getPaddingRight());columns=OneLayoutPolicy.modelColumns(Math.round(inside/getResources().getDisplayMetrics().density),getResources().getConfiguration().fontScale,getChildCount());childWidth=Math.max(1,(inside-gap*(columns-1))/columns);heights.clear();int height=getPaddingTop()+getPaddingBottom();for(int i=0;i<getChildCount();i++){View child=getChildAt(i);child.measure(MeasureSpec.makeMeasureSpec(childWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int row=i/columns;if(row==heights.size())heights.add(0);heights.set(row,Math.max(heights.get(row),Math.max(dp(owner,48),child.getMeasuredHeight())));}for(int i=0;i<heights.size();i++)height+=heights.get(i)+(i==0?0:gap);setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(height,heightSpec));}
  @Override protected void onLayout(boolean changed,int l,int t,int r,int b){int top=getPaddingTop();for(int i=0;i<getChildCount();i++){int row=i/columns,col=i%columns;if(col==0&&row>0)top+=heights.get(row-1)+gap;int left=getPaddingLeft()+col*(childWidth+gap);getChildAt(i).layout(left,top,left+childWidth,top+heights.get(row));}}
 }
 static float scrollFraction(ScrollView scroll){if(scroll==null||scroll.getChildCount()==0)return 0;int range=Math.max(0,scroll.getChildAt(0).getHeight()-scroll.getHeight()+scroll.getPaddingTop()+scroll.getPaddingBottom());return range==0?0:Math.max(0,Math.min(1,scroll.getScrollY()/(float)range));}
 static void restoreScroll(ScrollView scroll,float fraction){if(scroll==null)return;final float position=Math.max(0,Math.min(1,fraction));scroll.post(()->{if(scroll.getChildCount()==0)return;int range=Math.max(0,scroll.getChildAt(0).getHeight()-scroll.getHeight()+scroll.getPaddingTop()+scroll.getPaddingBottom());scroll.scrollTo(0,Math.round(range*position));});}
 static final class Scene extends View {
  private final Paint paint=new Paint(3);Scene(Activity a){super(a);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();paint.setColor(0xffeaf2ff);c.drawRoundRect(0,0,w,h,28,28,paint);paint.setColor(0xffd3e5ff);c.drawCircle(w*.88f,h*.16f,h*.55f,paint);paint.setColor(Ui.NAVY);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);c.drawRoundRect(w*.18f,h*.22f,w*.73f,h*.76f,16,16,paint);paint.setColor(Ui.ACCENT);float x=w*.45f,y=h*.43f,r=h*.1f;c.drawCircle(x,y,r,paint);c.drawLine(w*.23f,h*.69f,w*.37f,h*.48f,paint);c.drawLine(w*.37f,h*.48f,w*.56f,h*.7f,paint);c.drawLine(w*.56f,h*.7f,w*.66f,h*.54f,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);c.drawRoundRect(w*.64f,h*.63f,w*.91f,h*.91f,14,14,paint);paint.setColor(Ui.ACCENT);for(int i=0;i<5;i++){float xx=w*.69f+i*w*.037f,hh=h*(.05f+.02f*(i%3));c.drawRoundRect(xx,h*.77f-hh,xx+w*.014f,h*.77f+hh,4,4,paint);}}
 }
}
