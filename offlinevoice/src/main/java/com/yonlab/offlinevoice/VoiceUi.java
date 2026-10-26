package com.yonlab.offlinevoice;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** AXSL Vision native spacing and responsive actions, local to the independent voice module. */
final class VoiceUi {
 static final int INK=0xff071b45,BLUE=0xff0068f8,BACK=0xfff7faff,BORDER=0xffd8e4f5,MUTED=0xff4b5e83;
 private VoiceUi(){}
 static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 static LinearLayout col(Activity a){LinearLayout view=new LinearLayout(a);view.setOrientation(LinearLayout.VERTICAL);return view;}
 static LinearLayout row(Activity a){LinearLayout view=new LinearLayout(a);view.setGravity(Gravity.CENTER_VERTICAL);return view;}
 static LinearLayout.LayoutParams fill(){return new LinearLayout.LayoutParams(-1,-2);}
 static TextView text(Activity a,String label,int size){TextView text=new TextView(a);text.setText(label);text.setTextColor(INK);text.setTextSize(size);text.setIncludeFontPadding(false);text.setLineSpacing(dp(a,3),1);text.setPadding(0,dp(a,4),0,dp(a,4));return text;}
 static GradientDrawable shape(Activity a,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(a,radius));d.setStroke(dp(a,1),BORDER);return d;}
 static LinearLayout panel(Activity a){LinearLayout card=col(a);card.setPadding(dp(a,20),dp(a,16),dp(a,20),dp(a,20));card.setBackground(shape(a,Color.WHITE,12));return card;}
 static Drawable actionSurface(Activity a,int color){StateListDrawable states=new StateListDrawable();GradientDrawable disabled=shape(a,BACK,8);states.addState(new int[]{-android.R.attr.state_enabled},disabled);GradientDrawable pressed=shape(a,color==BLUE?0xff001850:0xffeaf2ff,8);pressed.setStroke(dp(a,2),BLUE);states.addState(new int[]{android.R.attr.state_pressed},pressed);GradientDrawable focused=shape(a,color,8);focused.setStroke(dp(a,2),color==BLUE?0xff001850:BLUE);states.addState(new int[]{android.R.attr.state_focused},focused);states.addState(new int[]{},shape(a,color,8));return new InsetDrawable(new RippleDrawable(ColorStateList.valueOf(color==BLUE?0x33ffffff:0x180068f8),states,shape(a,Color.WHITE,8)),dp(a,4));}
 static Button button(Activity a,String text,boolean primary,Runnable action){Button button=new Button(a);button.setText(text);button.setAllCaps(false);button.setTextSize(15);button.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{MUTED,primary?Color.WHITE:BLUE}));button.setMinWidth(dp(a,48));button.setMinimumWidth(dp(a,48));button.setMinHeight(dp(a,48));button.setMinimumHeight(dp(a,48));button.setPadding(dp(a,16),dp(a,10),dp(a,16),dp(a,10));button.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));button.setBackground(actionSurface(a,primary?BLUE:Color.WHITE));button.setStateListAnimator(null);button.setElevation(0);button.setOnClickListener(v->action.run());return button;}
 static View header(Activity a,String title,Runnable close){
  LinearLayout header=row(a);header.setPadding(0,0,0,dp(a,16));
  LinearLayout identity=col(a);TextView brand=text(a,"AXSL Vision",22);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);identity.addView(brand,fill());
  if(title!=null&&!title.isEmpty()&&!"AXSL Vision".equals(title)){TextView label=text(a,title,18);label.setTextColor(MUTED);identity.addView(label,fill());}
  identity.setPadding(0,0,dp(a,12),0);header.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
  header.addView(button(a,"닫기",false,close),new LinearLayout.LayoutParams(dp(a,80),-2));return header;
 }

 static int columns(Activity a,int count){int width=a.getResources().getConfiguration().screenWidthDp-(a.getResources().getConfiguration().screenWidthDp<600?32:48);return columnCount(width,a.getResources().getConfiguration().fontScale,count);}
 static int columns(Activity a,View content,int count){if(content==null||content.getWidth()<1)return columns(a,count);int width=Math.round((content.getWidth()-content.getPaddingLeft()-content.getPaddingRight())/a.getResources().getDisplayMetrics().density);return columnCount(width,a.getResources().getConfiguration().fontScale,count);}
 static int columnCount(int width,float fontScale,int count){double scale=Float.isFinite(fontScale)&&fontScale>=1?fontScale:1;long minimum=Math.min(Integer.MAX_VALUE,Math.round(270*scale));long fit=(Math.max(0L,(long)width)+16)/(minimum+16);return (int)Math.max(1L,Math.min(Math.min(3,Math.max(1,count)),fit));}
 static View actions(Activity a,View... views){return new ActionGrid(a,views);}
 private static final class ActionGrid extends ViewGroup {
  final Activity activity;final List<Integer> heights=new ArrayList<>();int columns=1,width,gap;
  ActionGrid(Activity a,View[] views){super(a);activity=a;gap=dp(a,12);setPadding(0,dp(a,8),0,dp(a,8));for(View v:views)addView(v);}
  @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(-2,-2);}
  @Override protected void onMeasure(int ws,int hs){int full=MeasureSpec.getSize(ws);float density=getResources().getDisplayMetrics().density,scale=Math.max(1,getResources().getConfiguration().fontScale);columns=Math.max(1,Math.min(Math.min(3,Math.max(1,getChildCount())),(Math.round(full/density)+12)/(Math.round(148*scale)+12)));width=Math.max(1,(full-gap*(columns-1))/columns);heights.clear();int height=getPaddingTop()+getPaddingBottom();for(int n=0;n<getChildCount();n++){View child=getChildAt(n);child.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));if(n/columns==heights.size())heights.add(0);heights.set(n/columns,Math.max(heights.get(n/columns),Math.max(dp(activity,48),child.getMeasuredHeight())));}for(int n=0;n<heights.size();n++)height+=heights.get(n)+(n>0?gap:0);setMeasuredDimension(resolveSize(full,ws),resolveSize(height,hs));}
  @Override protected void onLayout(boolean changed,int l,int t,int r,int b){int top=getPaddingTop();for(int n=0;n<getChildCount();n++){int row=n/columns,col=n%columns;if(col==0&&row>0)top+=heights.get(row-1)+gap;int left=col*(width+gap);getChildAt(n).layout(left,top,left+width,top+heights.get(row));}}
 }
}


