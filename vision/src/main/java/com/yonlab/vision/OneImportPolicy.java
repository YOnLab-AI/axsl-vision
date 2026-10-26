package com.yonlab.vision;
/** Media admission for a nonexported internal destination. */
public final class OneImportPolicy {
 private OneImportPolicy(){}
 public static boolean media(String type){return type!=null&&(type.startsWith("image/")||type.startsWith("video/"));}
 public static boolean owned(String authority,String pkg){return pkg!=null&&(pkg+".files").equals(authority);}
 public static boolean direct(String scheme,String mime,boolean requested){return requested&&"content".equals(scheme)&&media(mime);}
 public static boolean snapshot(String authority,String pkg,String path,String mime){return owned(authority,pkg)&&path!=null&&path.startsWith("/photos/")&&mime!=null&&mime.startsWith("image/");}
}
