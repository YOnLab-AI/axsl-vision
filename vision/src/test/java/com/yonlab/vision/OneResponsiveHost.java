package com.yonlab.vision;
import java.util.*;
public final class OneResponsiveHost {
 private static int count;
 private static void check(boolean yes,String why){count++;if(!yes)throw new AssertionError(why);}
 public static void main(String[] args){
  check(OneLayoutPolicy.modelColumns(328,1,3)==1,"portrait phone shows one result per row");
  check(OneLayoutPolicy.modelColumns(608,1,3)==2,"landscape phone can compare two images");
  check(OneLayoutPolicy.modelColumns(1232,1,4)==3,"large landscape caps at three columns");
  check(OneLayoutPolicy.modelColumns(1232,1,2)==2,"two models fill two columns");
  check(OneLayoutPolicy.modelColumns(608,1.5f,3)==1,"large font does not squeeze two cards");
  check(OneLayoutPolicy.modelColumns(1232,2,3)==2,"large-font tablet uses readable two columns");
  check(OneLayoutPolicy.modelColumns(0,1,3)==1,"unmeasured content is safe");
  check(OneLayoutPolicy.actionColumns(320,1,4,3)==2,"short phone actions share a row");
  check(OneLayoutPolicy.actionColumns(608,1,6,3)==3,"landscape actions use three columns");
  check(OneLayoutPolicy.actionColumns(320,2,4,3)==1,"large-font actions stack");
  check(OneLayoutPolicy.actionColumns(1200,1,2,3)==2,"no empty third action slot");
  check(OneLayoutPolicy.actionColumns(1200,1,4,1)==1,"caller can deliberately stack");
  check(OneLayoutPolicy.modelColumns(800,Float.NaN,3)==2,"invalid font scale is normalized");
  check(!OneLayoutPolicy.rail(640,1),"landscape phone keeps full-width content");
  check(OneLayoutPolicy.rail(1280,1),"wide standard-font workspace uses rail");
  check(!OneLayoutPolicy.rail(1280,2),"large font does not clip rail labels");
  check(OneLayoutPolicy.actionColumns(1232,1,6,6)==3,"six actions remain in two readable rows on a wide display");
  check(OneLayoutPolicy.actionColumns(10000,1,20,Integer.MAX_VALUE)==3,"caller maximum cannot create a fourth action column");
  check(OneLayoutPolicy.actionColumns(0,1,6,6)==1,"unmeasured action content stacks safely");
  check(OneLayoutPolicy.modelColumns(Integer.MAX_VALUE,1,3)==3,"very large content width does not overflow into one column");
  check(OneLayoutPolicy.modelColumns(1200,Float.MAX_VALUE,3)==1,"extreme font scale stacks instead of overflowing");
  check(contentWidth(900,1)==672,"home content excludes rail and both body paddings");
  check(contentWidth(599,1)==567,"phone content has sixteen dp on each side");
  check(contentWidth(600,1)==552,"tablet content has twenty four dp on each side");
  check(contentWidth(840,1.5f)==792,"large font removes the rail without consuming its width");
  check(contentWidth(-1,1)==0,"negative window width never creates negative content");
  check(!split(900,600,1),"rail plus two panels must not squeeze controls at nine hundred dp");
  check(!split(960,600,1),"four to six panels respect the minimum controls width");
  check(split(1024,600,1),"sufficient measured content allows two useful panels");
  check(!split(1024,600,1.3f),"larger text can return a two-panel workspace to one column");
  check(!split(1280,400,1),"short landscape workspace scrolls in one column");
  check(split(1280,600,1.3f),"wide landscape accommodates scaled controls and examples");
  check(!split(1280,600,2),"very large text does not squeeze the controls pane");
  System.out.println("One responsive layout PASS "+count+" checks");
 }
 private static int contentWidth(int width,float fontScale){try{return (Integer)OneLayoutPolicy.class.getMethod("availableContentWidth",int.class,float.class).invoke(null,width,fontScale);}catch(ReflectiveOperationException e){throw new AssertionError("available-content-width policy is required",e);}}
 private static boolean split(int width,int height,float fontScale){try{return (Boolean)OneLayoutPolicy.class.getMethod("workspaceSplit",int.class,int.class,float.class).invoke(null,width,height,fontScale);}catch(ReflectiveOperationException e){throw new AssertionError("comfortable workspace split policy is required",e);}}
}
