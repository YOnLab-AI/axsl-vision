package com.yonlab.vision;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.*;
import java.nio.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ModelBuffersTest {
    private static final String SSD="models/detect.tflite", BALL="models/ball-efficientdet.tflite";
    private static ByteBuffer model(int size) {
        ByteBuffer b=ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        if(size>=8){b.put(4,(byte)'T');b.put(5,(byte)'F');b.put(6,(byte)'L');b.put(7,(byte)'3');}return b;
    }
    @Test public void repeatedAcquireLoadsOnceAndPositionsAreIndependent() throws Exception {
        ModelBuffers.Store store=new ModelBuffers.Store();AtomicInteger loads=new AtomicInteger();
        ModelBuffers.Loader loader=name->{loads.incrementAndGet();return model(4183312);};
        ByteBuffer a=store.acquire(SSD,loader);a.position(31);a.limit(90);
        ByteBuffer b=store.acquire(SSD,loader);
        assertEquals(1,loads.get());assertNotSame(a,b);assertEquals(0,b.position());
        assertEquals(4183312,b.limit());assertEquals(31,a.position());
        assertTrue(b.isDirect());assertTrue(b.isReadOnly());assertEquals(ByteOrder.nativeOrder(),b.order());
        try{b.put(0,(byte)1);fail("model must be immutable");}catch(ReadOnlyBufferException expected){}
    }
    @Test public void bothAllowedAssetsHaveSeparateBoundedEntries() throws Exception {
        ModelBuffers.Store store=new ModelBuffers.Store();AtomicInteger loads=new AtomicInteger();
        ModelBuffers.Loader loader=name->{loads.incrementAndGet();return model(name.equals(SSD)?4183312:4563519);};
        assertEquals(4183312,store.acquire(SSD,loader).capacity());
        assertEquals(4563519,store.acquire(BALL,loader).capacity());
        store.acquire(SSD,loader);store.acquire(BALL,loader);assertEquals(2,loads.get());
    }
    @Test public void unknownAssetIsRejectedBeforeOpening() throws Exception {
        ModelBuffers.Store store=new ModelBuffers.Store();AtomicInteger loads=new AtomicInteger();
        for(String name:new String[]{"models/../detect.tflite","models/other.tflite",null}) {
            try{store.acquire(name,n->{loads.incrementAndGet();return model(4183312);});fail("unexpected asset");}catch(IOException expected){}
        }
        assertEquals(0,loads.get());
    }
    @Test public void invalidSizeOrHeaderNeverPoisonsCache() throws Exception {
        ModelBuffers.Store store=new ModelBuffers.Store();AtomicInteger loads=new AtomicInteger();
        ModelBuffers.Loader loader=name->{int n=loads.incrementAndGet();if(n==1)return model(7);ByteBuffer b=model(4183312);if(n==2)b.put(4,(byte)'X');return b;};
        for(int i=0;i<2;i++){try{store.acquire(SSD,loader);fail("invalid model");}catch(IOException expected){}}
        assertEquals(4183312,store.acquire(SSD,loader).capacity());store.acquire(SSD,loader);assertEquals(3,loads.get());
    }
    @Test public void concurrentAcquirePublishesOnlyOneSource() throws Exception {
        ModelBuffers.Store store=new ModelBuffers.Store();AtomicInteger loads=new AtomicInteger();
        ModelBuffers.Loader loader=name->{loads.incrementAndGet();return model(4183312);};
        ExecutorService executor=Executors.newFixedThreadPool(4);
        try{java.util.List<Future<ByteBuffer>> futures=new java.util.ArrayList<>();for(int i=0;i<12;i++)futures.add(executor.submit(()->store.acquire(SSD,loader)));
            for(Future<ByteBuffer> result:futures){assertEquals(0,result.get().position());assertTrue(result.get().isReadOnly());}
            assertEquals(1,loads.get());
        }finally{executor.shutdownNow();}
    }
    @Test public void compressedFallbackRejectsTruncationAndTrailingBytes() throws Exception {
        for(int size:new int[]{4183311,4183313}) {
            try{ModelBuffers.readCompressed(new ByteArrayInputStream(new byte[size]),4183312);fail("invalid stream length");}catch(IOException expected){}
        }
        byte[] bytes=new byte[4183312];bytes[4]='T';bytes[5]='F';bytes[6]='L';bytes[7]='3';
        ByteBuffer result=ModelBuffers.readCompressed(new ByteArrayInputStream(bytes),4183312);
        assertTrue(result.isDirect());assertEquals(0,result.position());assertEquals(bytes.length,result.capacity());assertEquals('T',result.get(4));
    }
}
