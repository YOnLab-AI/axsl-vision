package com.yonlab.vision;

import java.util.*;

/** Input diversity uses declared metadata; it makes no model quality prediction. */
public final class OneTestSamples {
 private OneTestSamples(){}
 public static final class Case {
  public final String id,group,media,difficulty;public final String[] conditions;
  public Case(String id,String group,String media,String difficulty,String... conditions){this.id=id;this.group=group;this.media=media;this.difficulty=difficulty==null?"":difficulty;this.conditions=conditions==null?new String[0]:conditions.clone();}
 }
 public static boolean matches(Case item,String group,String media,String difficulty,String query){
  if(!"ALL".equals(group)&&!item.group.equals(group)&&!"SHARED".equals(item.group))return false;
  if(!"any".equals(media)&&!item.media.equals(media))return false;
  if(!"ALL".equals(difficulty)&&!("unspecified".equals(difficulty)?item.difficulty.isEmpty():item.difficulty.equals(difficulty)))return false;
  StringBuilder haystack=new StringBuilder(item.id);for(String condition:item.conditions)haystack.append(' ').append(condition);
  return query==null||query.isEmpty()||haystack.toString().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
 }
 public static List<Case> diverse(List<Case> source,int limit){
  List<Case> out=new ArrayList<>();Set<String> ids=new HashSet<>(),seen=new HashSet<>();
  while(out.size()<Math.max(0,Math.min(limit,source.size()))){Case best=null;int bestScore=-1;
   for(Case item:source){if(ids.contains(item.id))continue;int score=0;for(String token:tokens(item))if(!seen.contains(token))score+=token.startsWith("difficulty:")?8:token.startsWith("group:")?4:token.startsWith("media:")?2:1;if(score>bestScore){best=item;bestScore=score;}}
   if(best==null)break;out.add(best);ids.add(best.id);seen.addAll(tokens(best));
  }
  return Collections.unmodifiableList(out);
 }
 private static Set<String> tokens(Case item){Set<String> out=new LinkedHashSet<>();out.add("difficulty:"+(item.difficulty.isEmpty()?"unspecified":item.difficulty));out.add("group:"+item.group);out.add("media:"+item.media);for(String condition:item.conditions)if(condition!=null&&!condition.isEmpty())out.add("condition:"+condition);return out;}
}
