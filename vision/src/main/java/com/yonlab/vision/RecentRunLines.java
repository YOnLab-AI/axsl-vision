package com.yonlab.vision;
import java.io.*;import java.util.*;
public final class RecentRunLines {
 private RecentRunLines(){}
 public static List<String> tail(BufferedReader reader,int limit)throws IOException{
  if(limit<1||limit>10000)throw new IllegalArgumentException("limit");ArrayDeque<String> queue=new ArrayDeque<>();String line;
  while((line=reader.readLine())!=null){if(line.isEmpty()||line.length()>32768)continue;if(queue.size()==limit)queue.removeFirst();queue.addLast(line);}return new ArrayList<>(queue);
 }
}
