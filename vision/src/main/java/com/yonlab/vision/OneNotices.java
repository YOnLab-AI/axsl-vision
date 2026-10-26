package com.yonlab.vision;
import android.content.res.AssetManager;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Offline original notices, read separately so later license files are not truncated by an aggregate cap. */
final class OneNotices {
 private OneNotices(){}
 static List<String> entries(AssetManager assets)throws IOException{LinkedHashSet<String> files=new LinkedHashSet<>();for(String root:new String[]{"one/licenses","licenses","models","one/models","one/CREDITS.md","axsl","offlinevoice"})collect(assets,root,0,files);List<String> result=new ArrayList<>(files);result.sort((a,b)->{boolean aa=a.startsWith("one/licenses/"),bb=b.startsWith("one/licenses/");return aa!=bb?(aa?-1:1):a.compareTo(b);});return Collections.unmodifiableList(result);}
 static String original(AssetManager assets,String path)throws IOException{if(path==null||path.contains("..")||path.startsWith("/")||!entries(assets).contains(path))throw new IOException("notice path");try(InputStream input=assets.open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=input.read(b))!=-1){if(out.size()+n>512*1024)throw new IOException("notice size");out.write(b,0,n);}return out.toString(StandardCharsets.UTF_8.name());}}
 static String read(AssetManager assets)throws IOException{StringBuilder b=new StringBuilder();for(String file:entries(assets)){b.append(file).append('\n').append(original(assets,file)).append("\n\n");if(b.length()>2*1024*1024)break;}return b.toString();}
 private static void collect(AssetManager assets,String path,int depth,Set<String> result)throws IOException{if(depth>5||result.size()>=256)return;String[] children=assets.list(path);if(children!=null&&children.length>0){for(String name:children)collect(assets,path+"/"+name,depth+1,result);return;}String name=path.substring(path.lastIndexOf('/')+1),upper=name.toUpperCase(Locale.ROOT);boolean text=upper.endsWith(".MD")||upper.endsWith(".TXT")||!name.contains(".")||upper.endsWith(".LICENSE");if(text&&(upper.contains("LICENSE")||upper.contains("NOTICE")||upper.contains("CREDITS")||path.startsWith("one/licenses/")))try(InputStream probe=assets.open(path)){result.add(path);}catch(IOException absentOrDirectory){}}
}
