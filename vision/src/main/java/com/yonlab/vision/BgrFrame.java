package com.yonlab.vision;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

final class BgrFrame implements AutoCloseable {
    final byte[] data;final int width,height;final long queuedNs,convertNs;final ArrayBlockingQueue<byte[]> pool;
    private final AtomicBoolean returned=new AtomicBoolean();
    BgrFrame(byte[] d,int w,int h,long ns,long convert,ArrayBlockingQueue<byte[]> p){data=d;width=w;height=h;queuedNs=ns;convertNs=convert;pool=p;}
    public void close(){if(returned.compareAndSet(false,true)&&!pool.offer(data))android.util.Log.e("YOn","buffer_return_overflow");}
}
