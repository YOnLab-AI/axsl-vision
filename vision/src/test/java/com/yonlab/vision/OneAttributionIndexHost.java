package com.yonlab.vision;
import java.util.*;
public final class OneAttributionIndexHost {
 private static int checks;private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 public static void main(String[] args){
  Map<String,String> catalog=new LinkedHashMap<>();catalog.put("photo-a","a".repeat(64));catalog.put("photo-b","b".repeat(64));catalog.put("photo-a-copy","a".repeat(64));catalog.put("../private","c".repeat(64));catalog.put("bad-hash","abc");OneAttributionIndex index=new OneAttributionIndex(catalog);
  check(index.resolve("photo-a","a".repeat(64)).equals("photo-a"),"selected source and actual SHA agree");
  check(index.resolve("photo-a-copy","a".repeat(64)).equals("photo-a-copy"),"selected copy keeps its own provenance when bytes agree");
  check(index.resolve("","a".repeat(64)).equals("photo-a"),"immutable handoff recovers actual source without demo ID");
  check(index.resolve("photo-a","b".repeat(64)).equals("photo-b"),"actual hash wins over a mismatched source ID");
  check(index.resolve("photo-a","f".repeat(64)).isEmpty(),"unknown personal photo gets no assumed attribution");
  check(index.resolve("photo-a","broken").isEmpty(),"malformed actual SHA cannot fall back to claimed ID");
  check(index.resolve("photo-a","A".repeat(64)).equals("photo-a"),"canonical hash is case-insensitive");
  check(index.resolve("photo-a","").equals("photo-a"),"explicit bundled catalog metadata lookup remains possible");
  check(index.resolve("../private","c".repeat(64)).isEmpty(),"invalid catalog ID not indexed");
  check(index.resolve("bad-hash","").isEmpty(),"invalid catalog hash not indexed");
  catalog.clear();check(index.resolve("photo-a","a".repeat(64)).equals("photo-a"),"caller cannot mutate an already-built index");
  System.out.println("One attribution source binding PASS "+checks+" checks");
 }
}
