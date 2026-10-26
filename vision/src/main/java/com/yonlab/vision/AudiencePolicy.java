package com.yonlab.vision;
import java.util.*;
/** Actual age estimates only. Majority then largest-face tie break mirrors the original rule. */
final class AudiencePolicy {
 static final class Summary {final int faceCount,knownAges,group;final int[] groups;Summary(int n,int known,int selected,int[] counts){faceCount=n;knownAges=known;group=selected;groups=counts.clone();}}
 static Summary select(List<Observations.Face> faces){int n=0,known=0,index=0;int[] counts=new int[3],order={Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE};float[] largest=new float[3];if(faces!=null)for(Observations.Face f:faces){int current=index++;if(f==null||!ToolFilters.finite(f.left)||!ToolFilters.finite(f.top)||!ToolFilters.finite(f.right)||!ToolFilters.finite(f.bottom)||f.left<0||f.top<0||f.right>1||f.bottom>1||f.left>=f.right||f.top>=f.bottom)continue;n++;float age=f.ageYears;if(!ToolFilters.finite(age)||age<0||age>120)continue;int group=age<30?0:age<50?1:2;known++;counts[group]++;float area=(f.right-f.left)*(f.bottom-f.top);if(area>largest[group]){largest[group]=area;order[group]=current;}}
  int chosen=-1;for(int group=0;group<3;group++)if(counts[group]>0&&(chosen<0||counts[group]>counts[chosen]||(counts[group]==counts[chosen]&&(largest[group]>largest[chosen]||(largest[group]==largest[chosen]&&order[group]<order[chosen])))))chosen=group;return new Summary(n,known,chosen,counts);
 }
 static int assetIndex(int group){return group<0||group>2?-1:new int[]{2,1,0}[group];}
}
