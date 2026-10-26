package com.yonlab.vision;

import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/** Untrusted encoded input must fail within its ownership/size boundary, before native decode. */
public final class ImageIOInputBoundsTest {
    private interface Attempt { void run() throws IOException; }
    private static void rejected(Attempt attempt) throws IOException {
        try { attempt.run(); fail("Expected input rejection"); }
        catch (IOException expected) { }
    }

    @Test public void missingOwnedBytesAreRejectedBeforeAndroidDecode() throws IOException {
        rejected(() -> ImageIO.decode((byte[])null,1280));
        rejected(() -> ImageIO.decode(new byte[0],1280));
    }

    @Test public void ownedBytesKeepTheSame32MiBLimit() throws IOException {
        byte[] oversized=new byte[32*1024*1024+1];
        rejected(() -> ImageIO.decode(oversized,1280));
    }

    @Test public void streamFailureKeepsCallerOwnership() {
        final IOException failure=new IOException("provider failed");
        final boolean[] closed={false};
        InputStream stream=new InputStream(){
            @Override public int read() throws IOException { throw failure; }
            @Override public void close() { closed[0]=true; }
        };
        try { ImageIO.decode(stream,1280); fail("Expected provider failure"); }
        catch (IOException actual) { assertSame(failure,actual); }
        assertFalse("The caller owns the stream",closed[0]);
    }

    @Test public void streamLimitStopsAnUnboundedProvider() throws IOException {
        final long[] consumed={0};
        InputStream stream=new InputStream(){
            @Override public int read(){consumed[0]++;return 0;}
            @Override public int read(byte[] bytes,int offset,int length){
                consumed[0]+=length;return length;
            }
        };
        rejected(() -> ImageIO.decode(stream,1280));
        assertTrue(consumed[0]>32L*1024*1024);
        assertTrue("Only the detecting chunk may exceed the cap",consumed[0]<=32L*1024*1024+16384);
    }
}
