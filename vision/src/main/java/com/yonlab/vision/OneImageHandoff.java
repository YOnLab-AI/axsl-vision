package com.yonlab.vision;
import android.app.*;import android.graphics.Bitmap;import android.net.Uri;import java.io.*;import java.util.*;import java.lang.ref.WeakReference;import java.security.MessageDigest;import java.util.concurrent.atomic.AtomicBoolean;import java.util.function.BooleanSupplier;import android.content.Context;import android.content.res.AssetFileDescriptor;import android.os.CancellationSignal;
/** Internal image reuse in the single package; no old sample application is required. */
final class OneImageHandoff {
 private OneImageHandoff(){}
 static void open(Activity owner,Bitmap image){if(image==null||image.isRecycled())return;List<String> ids=new ArrayList<>(),titles=new ArrayList<>();for(OneCatalog.Feature f:OneCatalog.all())if(Arrays.asList(f.media).contains("image")){ids.add(f.id);titles.add(f.title);}new AlertDialog.Builder(owner).setTitle("사진으로 이어서 하기").setItems(titles.toArray(new String[0]),(dialog,index)->{Bitmap snapshot;try{snapshot=image.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)throw new IOException("snapshot");}catch(Exception|OutOfMemoryError e){Ui.error(owner,"사진을 준비할 수 없습니다",e);return;}String feature=ids.get(index);new Thread(()->{try{File output=ImageIO.exported(owner.getApplicationContext(),"handoff-"+UUID.randomUUID()+".jpg"),pending=new File(output.getPath()+".new");try{try(FileOutputStream stream=new FileOutputStream(pending)){if(!snapshot.compress(Bitmap.CompressFormat.JPEG,95,stream))throw new IOException("image export");stream.getFD().sync();}if(!pending.renameTo(output))throw new IOException("image commit");}finally{pending.delete();}Uri uri=ImageIO.uri(owner,output);owner.runOnUiThread(()->{if(!owner.isFinishing()&&!owner.isDestroyed())OneLaunch.open(owner,feature,OneModels.selected(owner,feature),uri,"image/jpeg",null);});}catch(Exception|OutOfMemoryError e){owner.runOnUiThread(()->{if(!owner.isFinishing()&&!owner.isDestroyed())Ui.error(owner,"사진을 열 수 없습니다",e);});}finally{snapshot.recycle();}},"YOnImageHandoff").start();}).setNegativeButton("닫기",null).show();}
 private static final Map<Activity,Transfer> TRANSFERS=new WeakHashMap<>();
 static void open(Activity owner,Uri source){open(owner,source,null,null);}
 static void open(Activity owner,Uri source,String expectedSha,Runnable completion){
  if(owner.isFinishing()||owner.isDestroyed()||source==null||!"content".equals(source.getScheme())){if(completion!=null)completion.run();return;}
  cancel(owner);Transfer transfer=new Transfer(owner,source,expectedSha,completion);synchronized(TRANSFERS){TRANSFERS.put(owner,transfer);}transfer.choose(owner);
 }
 static void cancel(Activity owner){Transfer transfer;synchronized(TRANSFERS){transfer=TRANSFERS.remove(owner);}if(transfer!=null)transfer.cancel();}
 private static final class Transfer implements Runnable {
  final WeakReference<Activity> owner;final Context app;final Uri source;final String expectedSha;final AtomicBoolean canceled=new AtomicBoolean(),finished=new AtomicBoolean();final CancellationSignal signal=new CancellationSignal();
  volatile InputStream stream;volatile Thread thread;volatile File committed;Runnable completion;AlertDialog dialog;String feature,mime;
  Transfer(Activity a,Uri source,String sha,Runnable complete){owner=new WeakReference<>(a);app=a.getApplicationContext();this.source=source;expectedSha=sha;completion=complete;}
  void choose(Activity a){List<String> ids=new ArrayList<>(),titles=new ArrayList<>();for(OneCatalog.Feature f:OneCatalog.all())if(Arrays.asList(f.media).contains("image")){ids.add(f.id);titles.add(f.title);}dialog=new AlertDialog.Builder(a).setTitle("사진으로 이어서 하기").setItems(titles.toArray(new String[0]),(d,index)->{dialog=null;feature=ids.get(index);thread=new Thread(this,"YOnEncodedHandoff");thread.start();}).setNegativeButton("닫기",(d,w)->cancel()).setOnCancelListener(d->cancel()).create();dialog.show();}
  void cancel(){canceled.set(true);signal.cancel();Thread running=thread;if(running!=null)running.interrupt();InputStream current=stream;if(current!=null)new Thread(()->{try{current.close();}catch(IOException ignored){}},"YOnHandoffClose").start();File file=committed;committed=null;if(file!=null)file.delete();finish();}
  void finish(){if(!finished.compareAndSet(false,true))return;Activity a=owner.get();if(a!=null)synchronized(TRANSFERS){if(TRANSFERS.get(a)==this)TRANSFERS.remove(a);}AlertDialog current=dialog;dialog=null;if(current!=null&&current.isShowing())current.dismiss();Runnable done=completion;completion=null;if(done!=null)done.run();}
  public void run(){File output=null;Exception failure=null;try{
   String declared=app.getContentResolver().getType(source);
   try(AssetFileDescriptor descriptor=app.getContentResolver().openAssetFileDescriptor(source,"r",signal)){
    if(descriptor==null)throw new IOException("source unavailable");PushbackInputStream input=new PushbackInputStream(descriptor.createInputStream(),32);stream=input;
    try{byte[] head=new byte[32];int count=0;while(count<head.length){checkCanceled(canceled::get);int n=input.read(head,count,head.length-count);if(n<0)break;if(n==0)continue;count+=n;}if(count>0)input.unread(head,0,count);mime=detectMime(head,count,declared);output=ImageIO.exported(app,"handoff-"+UUID.randomUUID()+"."+extension(mime));copyEncoded(input,new File(output.getPath()+".new"),output,expectedSha,canceled::get);}
    finally{stream=null;input.close();}
   }
   checkCanceled(canceled::get);committed=output;
  }catch(Exception e){failure=e;}catch(OutOfMemoryError e){failure=new IOException("image memory",e);}
  File ready=output;Exception error=failure;Activity a=owner.get();if(a==null){if(ready!=null)ready.delete();completion=null;return;}a.runOnUiThread(()->deliver(ready,error));}
  void deliver(File file,Exception error){Activity a=owner.get();if(canceled.get()||finished.get()||a==null||a.isFinishing()||a.isDestroyed()){if(file!=null)file.delete();finish();return;}if(error!=null){if(file!=null)file.delete();committed=null;finish();Ui.error(a,"source changed".equals(error.getMessage())?"사진이 변경되었습니다. 다시 분석하세요":"사진을 열 수 없습니다",error);return;}
   try{Uri input=ImageIO.uri(app,file);committed=null;finish();OneLaunch.open(a,feature,OneModels.selected(a,feature),input,mime,null);}catch(Exception e){if(file!=null)file.delete();finish();Ui.error(a,"사진을 열 수 없습니다",e);}
  }
 }
 private static void checkCanceled(BooleanSupplier cancel)throws InterruptedIOException{if(cancel.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("canceled");}
 /** This worker operation preserves encoded bytes, closes the source before commit, and owns only its fresh staging file. */
 static String copyEncoded(InputStream source,File pending,File output,String expectedSha,BooleanSupplier cancel)throws Exception{
  boolean owned=false,committed=false,complete=false;String sha="";
  try{
   try(InputStream input=source){checkCanceled(cancel);if(!pending.getCanonicalPath().equals(output.getCanonicalPath()+".new")||output.exists()||pending.exists())throw new IOException("snapshot destination");if(!pending.createNewFile())throw new IOException("snapshot staging");owned=true;
    MessageDigest digest=MessageDigest.getInstance("SHA-256");long bytes=0;
    try(FileOutputStream stream=new FileOutputStream(pending)){byte[] block=new byte[16384];int n;while((n=input.read(block))!=-1){checkCanceled(cancel);if(n==0)continue;bytes+=n;if(bytes>32L*1024*1024)throw new IOException("image size");digest.update(block,0,n);stream.write(block,0,n);}if(bytes==0)throw new IOException("empty image");checkCanceled(cancel);stream.getFD().sync();}
    StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));sha=hex.toString();if(expectedSha!=null&&!expectedSha.isEmpty()&&!expectedSha.equals(sha))throw new IOException("source changed");
   }
   checkCanceled(cancel);if(output.exists()||!pending.renameTo(output))throw new IOException("image commit");committed=true;checkCanceled(cancel);complete=true;return sha;
  }finally{if(owned&&pending.exists())pending.delete();if(committed&&!complete)output.delete();}
 }
 static String detectMime(byte[] h,int n,String declared)throws IOException{
  if(n>=3&&(h[0]&255)==255&&(h[1]&255)==216&&(h[2]&255)==255)return "image/jpeg";
  if(n>=8&&(h[0]&255)==137&&h[1]==80&&h[2]==78&&h[3]==71&&h[4]==13&&h[5]==10&&h[6]==26&&h[7]==10)return "image/png";
  if(n>=6&&h[0]=='G'&&h[1]=='I'&&h[2]=='F'&&h[3]=='8'&&(h[4]=='7'||h[4]=='9')&&h[5]=='a')return "image/gif";
  if(n>=12&&h[0]=='R'&&h[1]=='I'&&h[2]=='F'&&h[3]=='F'&&h[8]=='W'&&h[9]=='E'&&h[10]=='B'&&h[11]=='P')return "image/webp";
  if(n>=2&&h[0]=='B'&&h[1]=='M')return "image/bmp";
  if(n>=12&&h[4]=='f'&&h[5]=='t'&&h[6]=='y'&&h[7]=='p'){String brand=new String(h,8,4,java.nio.charset.StandardCharsets.US_ASCII);if(brand.equals("avif")||brand.equals("avis"))return "image/avif";if(Arrays.asList("heic","heix","hevc","hevx").contains(brand))return "image/heic";if(brand.equals("mif1")||brand.equals("msf1"))return "image/heif";}
  String mime=declared==null?"":declared.toLowerCase(Locale.ROOT).split(";",2)[0].trim();if("image/jpg".equals(mime))mime="image/jpeg";if("image/x-png".equals(mime))mime="image/png";extension(mime);return mime;
 }
 static String extension(String mime)throws IOException{switch(mime){case "image/jpeg":return "jpg";case "image/png":return "png";case "image/webp":return "webp";case "image/gif":return "gif";case "image/bmp":return "bmp";case "image/heic":return "heic";case "image/heif":return "heif";case "image/avif":return "avif";default:throw new IOException("image type");}}

}
