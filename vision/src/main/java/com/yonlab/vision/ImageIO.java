package com.yonlab.vision;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;

/** Bounded SAF input; orientation normalized once before every vision workflow. */
public final class ImageIO {
    private ImageIO(){}
    public static Bitmap load(Context c,Uri uri,int maxSide)throws IOException {
        if(uri==null||!"content".equals(uri.getScheme()))throw new IOException("사진을 선택해 주세요");
        try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("사진을 열 수 없습니다");return decode(in,maxSide);}
    }
    static Bitmap owned(Context c,String path,int maxSide)throws IOException {
        File f=new File(path);String base=c.getFilesDir().getCanonicalPath()+File.separator;if(!f.getCanonicalPath().startsWith(base))throw new IOException("사진 경로를 확인해 주세요");
        try(InputStream in=new FileInputStream(f)){return decode(in,maxSide);}
    }
    static Bitmap decode(InputStream in,int maxSide)throws IOException {
        if(in==null)throw new IOException("사진을 열 수 없습니다");ByteArrayOutputStream data=new ByteArrayOutputStream();byte[] block=new byte[16384];int n;
        while((n=in.read(block))!=-1){if(data.size()+n>32*1024*1024)throw new IOException("사진 용량이 너무 큽니다");data.write(block,0,n);}return decode(data.toByteArray(),maxSide);
    }
    /** Decode already-owned source bytes without another full encoded-file copy. */
    static Bitmap decode(byte[] bytes,int maxSide)throws IOException {
        if(bytes==null||bytes.length==0)throw new IOException("사진을 열 수 없습니다");if(bytes.length>32*1024*1024)throw new IOException("사진 용량이 너무 큽니다");maxSide=Math.max(320,Math.min(1600,maxSide));
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
        if(bounds.outWidth<1||bounds.outHeight<1||(long)bounds.outWidth*bounds.outHeight>80000000L)throw new IOException("사진 형식을 확인해 주세요");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>maxSide)options.inSampleSize*=2;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)throw new IOException("사진을 읽을 수 없습니다");
        int orientation=1;try{orientation=new ExifInterface(new ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);}catch(IOException ignored){}
        Matrix m=new Matrix();switch(orientation){case 2:m.setScale(-1,1);break;case 3:m.setRotate(180);break;case 4:m.setScale(1,-1);break;case 5:m.setRotate(90);m.postScale(-1,1);break;case 6:m.setRotate(90);break;case 7:m.setRotate(270);m.postScale(-1,1);break;case 8:m.setRotate(270);break;}
        if(!m.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}return bitmap;
    }
    public static Uri uri(Context c,File file){return FileProvider.getUriForFile(c,c.getPackageName()+".files",file);}
    public static Uri imageUri(Intent i){Uri u=i.getData();if(u==null)u=i.getParcelableExtra(Intent.EXTRA_STREAM);if(u==null){String s=i.getStringExtra("input_uri");if(s!=null)u=Uri.parse(s);}return u;}
    public static File exported(Context c,String name)throws IOException{if(!name.matches("[a-zA-Z0-9_.-]{1,100}"))throw new IOException("파일 이름을 확인해 주세요");File dir=new File(c.getFilesDir(),"exports");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("저장 공간을 확인해 주세요");return new File(dir,name);}
}
