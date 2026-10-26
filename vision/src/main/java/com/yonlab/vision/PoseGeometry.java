package com.yonlab.vision;
import java.util.*;
/** Drawing only: points and likelihoods come from the SDK, never from this topology. */
final class PoseGeometry {
 static final class Point {final int id;final float x,y,likelihood;Point(int id,float x,float y,float likelihood){this.id=id;this.x=x;this.y=y;this.likelihood=likelihood;}}
 private static final int[][] PAIRS={{11,12},{11,13},{13,15},{15,17},{15,19},{15,21},{17,19},{12,14},{14,16},{16,18},{16,20},{16,22},{18,20},{11,23},{12,24},{23,24},{23,25},{25,27},{27,29},{29,31},{27,31},{24,26},{26,28},{28,30},{30,32},{28,32}};
 static boolean visible(Point p){return p!=null&&p.id>=0&&p.id<33&&ToolFilters.finite(p.x)&&ToolFilters.finite(p.y)&&ToolFilters.finite(p.likelihood)&&p.x>=0&&p.x<=1&&p.y>=0&&p.y<=1&&p.likelihood>=.5f&&p.likelihood<=1;}
 static List<Point> visible(List<Point> input){Point[] best=new Point[33];if(input!=null)for(Point p:input)if(visible(p)&&(best[p.id]==null||p.likelihood>best[p.id].likelihood))best[p.id]=p;List<Point> out=new ArrayList<>();for(Point p:best)if(p!=null)out.add(p);return out;}
 static List<Point[]> edges(List<Point> points){Point[] index=new Point[33];for(Point p:visible(points))index[p.id]=p;List<Point[]> edges=new ArrayList<>();for(int[] pair:PAIRS)if(index[pair[0]]!=null&&index[pair[1]]!=null)edges.add(new Point[]{index[pair[0]],index[pair[1]]});return edges;}
}
