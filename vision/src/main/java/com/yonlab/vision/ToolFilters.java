package com.yonlab.vision;
import java.util.*;
/** Filter real mapped COCO labels, preserving the model's actual score and box. */
final class ToolFilters {
 private ToolFilters(){}
 static boolean finite(float x){return !Float.isNaN(x)&&!Float.isInfinite(x);}
 static List<Observations.Detection> select(List<Observations.Detection> input,String tool){if(!"pet".equals(tool)&&!"ball".equals(tool))throw new IllegalArgumentException("tool");List<Observations.Detection> selected=new ArrayList<>();if(input==null)return selected;for(Observations.Detection d:input){if(d==null||d.label==null||!finite(d.score)||d.score<.4f||!finite(d.left)||!finite(d.top)||!finite(d.right)||!finite(d.bottom)||d.left<0||d.top<0||d.right>1||d.bottom>1||d.left>=d.right||d.top>=d.bottom)continue;String label=d.label.trim().toLowerCase(Locale.ROOT);if("pet".equals(tool)?("cat".equals(label)||"dog".equals(label)):"sports ball".equals(label))selected.add(d);}return selected;}
}
