package com.yonlab.vision;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.InputStream;
import java.util.ArrayList;

/** Shared native views using AXSL Vision text branding and native visual tokens. */
public final class Ui {
    private Ui() {}
    public static final int INK=Color.rgb(7,27,69), BACK=Color.rgb(247,250,255), ACCENT=Color.rgb(0,104,248);
    public static final int MUTED=Color.rgb(75,94,131), SURFACE=Color.WHITE, BORDER=Color.rgb(216,228,245);
    public static final int NAVY=Color.rgb(0,24,80), SUCCESS=Color.rgb(15,159,110), ERROR=Color.rgb(217,45,32);

    public static int dp(Activity a,int value) { return Math.round(a.getResources().getDisplayMetrics().density*value); }
    public static boolean narrow(Activity a) { return a.getResources().getConfiguration().screenWidthDp<600; }
    public static int contentColumns(Activity a,int count){int width=a.getResources().getConfiguration().screenWidthDp-(narrow(a)?32:48);return OneLayoutPolicy.modelColumns(width,a.getResources().getConfiguration().fontScale,count);}
    public static int contentColumns(Activity a,View content,int count){if(content==null||content.getWidth()<1)return contentColumns(a,count);int width=Math.round((content.getWidth()-content.getPaddingLeft()-content.getPaddingRight())/a.getResources().getDisplayMetrics().density);return OneLayoutPolicy.modelColumns(width,a.getResources().getConfiguration().fontScale,count);}
    /** Actions wrap from their measured width, including cards that occupy only part of a screen. */
    public static View actions(Activity a,Button... buttons){return new ActionGrid(a,3,buttons);}
    public static View actions(Activity a,int maximum,Button... buttons){return new ActionGrid(a,maximum,buttons);}
    private static final class ActionGrid extends ViewGroup {
        private final Activity owner;private final int maximum;private final ArrayList<Integer> heights=new ArrayList<>();private int columns=1,gap,childWidth;
        ActionGrid(Activity a,int max,Button[] buttons){super(a);owner=a;maximum=max;gap=dp(a,12);setPadding(0,dp(a,8),0,dp(a,8));for(Button b:buttons)addView(b);}
        @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);}
        @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);int inside=Math.max(1,width-getPaddingLeft()-getPaddingRight());columns=OneLayoutPolicy.actionColumns(Math.round(inside/getResources().getDisplayMetrics().density),getResources().getConfiguration().fontScale,getChildCount(),maximum);childWidth=Math.max(1,(inside-gap*(columns-1))/columns);heights.clear();int height=getPaddingTop()+getPaddingBottom();for(int n=0;n<getChildCount();n++){View child=getChildAt(n);child.measure(MeasureSpec.makeMeasureSpec(childWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int row=n/columns;if(row==heights.size())heights.add(0);heights.set(row,Math.max(heights.get(row),Math.max(dp(owner,48),child.getMeasuredHeight())));}for(int n=0;n<heights.size();n++)height+=heights.get(n)+(n==0?0:gap);setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(height,heightSpec));}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){int top=getPaddingTop();for(int n=0;n<getChildCount();n++){int row=n/columns,col=n%columns;if(col==0&&row>0)top+=heights.get(row-1)+gap;int left=getPaddingLeft()+col*(childWidth+gap);getChildAt(n).layout(left,top,left+childWidth,top+heights.get(row));}}
    }
    public static LinearLayout column(Activity a) {
        LinearLayout v=new LinearLayout(a); v.setOrientation(LinearLayout.VERTICAL);
        int pad=dp(a,narrow(a)?16:24); v.setPadding(pad,dp(a,16),pad,dp(a,24)); v.setBackgroundColor(BACK); return v;
    }
    public static TextView text(Activity a,String value,int sp) {
        TextView v=new TextView(a); v.setText(value); v.setTextSize(sp); v.setTextColor(INK);
        v.setFontFeatureSettings("kern"); v.setIncludeFontPadding(false);
        v.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL)); v.setLineSpacing(dp(a,3),1);
        v.setPadding(0,dp(a,4),0,dp(a,4)); return v;
    }
    public static TextView heading(Activity a,String value,int sp) {
        TextView v=text(a,value,sp); v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        v.setLetterSpacing(-.025f); if(android.os.Build.VERSION.SDK_INT>=28) v.setAccessibilityHeading(true);
        return v;
    }
    public static GradientDrawable round(Activity a,int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(a,radius)); return d;
    }
    public static GradientDrawable outline(Activity a,int color,int radius) {
        GradientDrawable d=round(a,color,radius); d.setStroke(dp(a,1),BORDER); return d;
    }
    private static GradientDrawable stateSurface(Activity a,int color,int radius,int border,int stroke) {
        GradientDrawable d=round(a,color,radius);if(stroke>0)d.setStroke(dp(a,stroke),border);return d;
    }
    /** Native drawable states keep pointer, keyboard and unavailable actions distinct. */
    static Drawable interactiveSurface(Activity a,int color,int radius,boolean outlined) {
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled},stateSurface(a,BACK,radius,BORDER,1));
        states.addState(new int[]{android.R.attr.state_pressed},stateSurface(a,color==ACCENT?NAVY:0xffeaf2ff,radius,ACCENT,2));
        states.addState(new int[]{android.R.attr.state_focused},stateSurface(a,color,radius,color==ACCENT?NAVY:ACCENT,2));
        states.addState(new int[]{android.R.attr.state_selected},stateSurface(a,0xffeaf2ff,radius,ACCENT,2));
        states.addState(new int[]{},stateSurface(a,color,radius,BORDER,outlined?1:0));
        return new RippleDrawable(ColorStateList.valueOf(color==ACCENT?0x33ffffff:0x180068f8),states,round(a,Color.WHITE,radius));
    }
    static ColorStateList interactionText(int normal) {
        return new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{MUTED,normal});
    }
    /** Each rebuilt filter/navigation view exposes the same selection as its visual state. */
    static void selectionSemantics(View view,boolean selected) {
        view.setSelected(selected);
        view.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){
                super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.RadioButton");
                info.setCheckable(true);info.setChecked(host.isSelected());info.setSelected(host.isSelected());
            }
        });
    }
    static void actionSemantics(View view) {
        view.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){
                super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.Button");
            }
        });
    }
    public static LinearLayout panel(Activity a) {
        LinearLayout v=new LinearLayout(a); v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(a,18),dp(a,16),dp(a,18),dp(a,16)); v.setBackground(outline(a,SURFACE,12)); return v;
    }
    public static Button button(Activity a,String label,View.OnClickListener click) { return action(a,label,click,true); }
    public static Button secondary(Activity a,String label,View.OnClickListener click) { return action(a,label,click,false); }
    private static Button action(Activity a,String label,View.OnClickListener click,boolean primary) {
        Button b=new Button(a); b.setText(label); b.setTextSize(15); b.setAllCaps(false);
        b.setTextColor(interactionText(primary?Color.WHITE:ACCENT)); b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setOnClickListener(click); b.setGravity(Gravity.CENTER); b.setMinHeight(dp(a,48)); b.setMinimumHeight(dp(a,48));
        b.setMinWidth(dp(a,48)); b.setMinimumWidth(dp(a,48)); b.setPadding(dp(a,16),dp(a,10),dp(a,16),dp(a,10));
        Drawable base=interactiveSurface(a,primary?ACCENT:SURFACE,8,!primary);
        Drawable spaced=new android.graphics.drawable.InsetDrawable(base,dp(a,4),dp(a,4),dp(a,4),dp(a,4));
        b.setBackground(spaced);
        b.setStateListAnimator(null); b.setElevation(0); return b;
    }
    public static TextView pill(Activity a,String value,boolean active) {
        TextView v=text(a,value,12); v.setTextColor(active?ACCENT:MUTED);
        v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        v.setPadding(dp(a,9),dp(a,5),dp(a,9),dp(a,5)); v.setBackground(round(a,active?0xffeaf2ff:0xfff0f3f8,20)); return v;
    }
    public static ImageView icon(Activity a,String name,int size) {
        ImageView v=new ImageView(a);
        int id=a.getResources().getIdentifier("ic_"+name.replace('-','_'),"drawable",a.getPackageName());
        if(id!=0) v.setImageResource(id);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER); v.setContentDescription(null);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(a,size),dp(a,size))); return v;
    }
    public static ImageView image(Activity a,String name) {
        ImageView v=new ImageView(a); v.setScaleType(ImageView.ScaleType.CENTER_CROP); v.setContentDescription(null);
        try(InputStream stream=a.getAssets().open("axsl/images/"+name+".jpg")) {
            v.setImageDrawable(Drawable.createFromStream(stream,name));
        } catch(Exception e) { v.setBackgroundColor(0xffeaf2ff); }
        return v;
    }
    public static LinearLayout brandHeader(Activity a,String title) {
        LinearLayout row=new LinearLayout(a); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(Color.WHITE); row.setMinimumHeight(dp(a,narrow(a)?56:64));
        row.setPadding(dp(a,narrow(a)?16:24),dp(a,10),dp(a,narrow(a)?16:24),dp(a,10));
        LinearLayout identity=new LinearLayout(a); identity.setOrientation(LinearLayout.VERTICAL);
        TextView brand=heading(a,"AXSL Vision",narrow(a)?20:22); brand.setTextColor(NAVY);
        identity.addView(brand,new LinearLayout.LayoutParams(-1,-2));
        if(title!=null&&!title.isEmpty()&&!"AXSL Vision".equals(title)) {
            TextView name=heading(a,title,narrow(a)?16:18); name.setTextColor(MUTED);
            identity.addView(name,new LinearLayout.LayoutParams(-1,-2));
        }
        identity.setPadding(0,0,dp(a,8),0); row.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
        return row;
    }
    public static void error(Activity a,String heading,Throwable e) {
        if(a.isFinishing()||a.isDestroyed()) return;
        new AlertDialog.Builder(a).setTitle(heading).setMessage("다시 시도하세요.").setPositiveButton("확인",null).show();
    }
    public static LinearLayout.LayoutParams fill() { return new LinearLayout.LayoutParams(-1,-2); }
}
