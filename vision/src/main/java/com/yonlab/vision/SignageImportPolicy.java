package com.yonlab.vision;
import java.io.*;
/** Pure policy shared by the actual immutable-copy importer and host tests. */
final class SignageImportPolicy {
 private SignageImportPolicy(){}
 interface Cancellation { boolean canceled(); }
 static boolean privateUri(String uri){return uri!=null&&uri.matches("private:signage/imports/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}[.]img");}
 static String relativePath(String uri)throws IOException{if(!privateUri(uri))throw new IOException("content path");return uri.substring(8);}
 static long copy(InputStream input,OutputStream output,long limit,Cancellation cancellation)throws IOException{if(input==null||output==null||limit<1||cancellation==null)throw new IOException("copy input");byte[] block=new byte[16384];long bytes=0;int empty=0;for(;;){if(cancellation.canceled()||Thread.currentThread().isInterrupted())throw new IOException("canceled");int count=input.read(block);if(count<0)return bytes;if(count==0){if(++empty>32)throw new IOException("input stalled");continue;}empty=0;if(count>limit-bytes)throw new IOException("size");output.write(block,0,count);bytes+=count;}}
}
