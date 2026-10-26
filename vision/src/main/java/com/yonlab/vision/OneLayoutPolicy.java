package com.yonlab.vision;

/** Width and font-scale policy shared by actual measured native layouts. */
public final class OneLayoutPolicy {
 private OneLayoutPolicy(){}
 private static float scale(float value){return Float.isFinite(value)&&value>=1?value:1;}
 public static boolean rail(int widthDp,float fontScale){return widthDp>=840&&scale(fontScale)<=1.3f;}
 /** Width left inside OneActivity after its optional rail and normal body padding. */
 public static int availableContentWidth(int windowWidthDp,float fontScale){
  int padding=windowWidthDp<600?32:48;
  return (int)Math.max(0L,(long)windowWidthDp-padding-(rail(windowWidthDp,fontScale)?180:0));
 }
 /** Match the actual 4:6 workspace panels, including their sixteen dp gutter. */
 public static boolean workspaceSplit(int windowWidthDp,int heightDp,float fontScale){
  return workspaceSplitContent(availableContentWidth(windowWidthDp,fontScale),heightDp,fontScale);
 }
 /** Real content width already excludes navigation, padding and display insets. */
 public static boolean workspaceSplitContent(int contentWidthDp,int heightDp,float fontScale){
  if(heightDp<480)return false;
  int inside=(int)Math.max(0L,(long)contentWidthDp-16L);
  double textScale=scale(fontScale);
  return inside*.4>=300*textScale&&inside*.6>=360*textScale;
 }
 private static int fittedColumns(int widthDp,float fontScale,int count,int maximum,int minimumDp){
  long minimum=Math.min(Integer.MAX_VALUE,Math.round(minimumDp*(double)scale(fontScale)));
  long fit=(Math.max(0L,(long)widthDp)+12L)/(minimum+12L);
  return (int)Math.max(1L,Math.min(Math.min(Math.max(1,maximum),Math.max(1,count)),fit));
 }
 public static int modelColumns(int widthDp,float fontScale,int count){
  return fittedColumns(widthDp,fontScale,count,3,270);
 }
 public static int actionColumns(int widthDp,float fontScale,int count,int maximum){
  return fittedColumns(widthDp,fontScale,count,Math.min(3,Math.max(1,maximum)),148);
 }
}
