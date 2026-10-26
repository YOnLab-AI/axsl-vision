package com.yonlab.vision;
import android.content.Context;import android.content.res.AssetFileDescriptor;import java.io.*;import java.nio.*;import java.nio.channels.FileChannel;import java.util.*;import org.json.*;
/** Read-only bundled TFLite weights, byte-identity pinned before interpreter construction. */
final class OneLiteAsset {
 final ByteBuffer bytes;final String sha256;final long size;
 OneLiteAsset(Context c,String filename)throws Exception{
  if(filename==null||!filename.matches("[a-z0-9-]+\\.tflite"))throw new IOException("모델 경로를 확인하세요");JSONObject list;try(InputStream in=c.getAssets().open("one/core-model-manifest.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);list=new JSONObject(out.toString("UTF-8"));}
  JSONObject spec=list.getJSONObject(filename);sha256=spec.getString("sha256");size=spec.getLong("bytes");if(size<8||size>64*1024*1024)throw new IOException("모델 크기를 확인하세요");ByteBuffer source;
  try(AssetFileDescriptor fd=c.getAssets().openFd("one/models/"+filename);FileInputStream in=new FileInputStream(fd.getFileDescriptor());FileChannel channel=in.getChannel()){
   if(fd.getLength()!=size)throw new IOException("모델 크기가 다릅니다");source=channel.map(FileChannel.MapMode.READ_ONLY,fd.getStartOffset(),size);
  }
  ByteBuffer check=source.duplicate();java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");byte[] block=new byte[65536];while(check.hasRemaining()){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("canceled");int n=Math.min(block.length,check.remaining());check.get(block,0,n);digest.update(block,0,n);}StringBuilder hex=new StringBuilder();for(byte v:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",v&255));if(!hex.toString().equals(sha256))throw new IOException("모델 해시가 다릅니다");if(source.get(4)!='T'||source.get(5)!='F'||source.get(6)!='L'||source.get(7)!='3')throw new IOException("모델 형식이 다릅니다");bytes=source.asReadOnlyBuffer().order(ByteOrder.nativeOrder());bytes.rewind();
 }
}
