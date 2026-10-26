package com.yonlab.vision;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

/** Bounded source buffers; each ObjectEngine still owns its Interpreter. */
final class ModelBuffers {
    private static final Store MODELS=new Store();
    private ModelBuffers() {}

    static ByteBuffer acquire(AssetManager assets,String asset) throws IOException {
        return MODELS.acquire(asset,name->load(assets,name));
    }

    private static int expectedSize(String asset) throws IOException {
        if("models/detect.tflite".equals(asset))return 4183312;
        if("models/ball-efficientdet.tflite".equals(asset))return 4563519;
        throw new IOException("객체 모델 경로를 확인해 주세요");
    }

    private static ByteBuffer load(AssetManager assets,String asset) throws IOException {
        int length=expectedSize(asset);
        // Release APKs store .tflite uncompressed. Mapping retains no open descriptor.
        try(AssetFileDescriptor fd=assets.openFd(asset);
            FileInputStream stream=new FileInputStream(fd.getFileDescriptor());
            FileChannel channel=stream.getChannel()) {
            if(fd.getLength()!=length)throw new IOException("객체 모델 크기를 확인해 주세요");
            return channel.map(FileChannel.MapMode.READ_ONLY,fd.getStartOffset(),length);
        } catch(IOException mappingFailure) {
            // Compatible with differently packaged/compressed APK assets; one fixed-size copy only.
            try(InputStream stream=assets.open(asset)) {
                return readCompressed(stream,length);
            } catch(IOException fallbackFailure) {
                fallbackFailure.addSuppressed(mappingFailure);throw fallbackFailure;
            }
        }
    }

    static ByteBuffer readCompressed(InputStream stream,int expected) throws IOException {
        if(expected!=4183312&&expected!=4563519)throw new IOException("객체 모델 크기를 확인해 주세요");
        ByteBuffer source=ByteBuffer.allocateDirect(expected).order(ByteOrder.nativeOrder());
        byte[] chunk=new byte[16384];int total=0;
        while(total<expected){int n=stream.read(chunk,0,Math.min(chunk.length,expected-total));if(n<0)break;if(n==0){int next=stream.read();if(next<0)break;source.put((byte)next);total++;}else{source.put(chunk,0,n);total+=n;}}
        if(total!=expected||stream.read()!=-1)throw new IOException("객체 모델 크기를 확인해 주세요");
        source.rewind();return source;
    }

    interface Loader { ByteBuffer load(String asset) throws IOException; }
    static final class Store {
        private final Map<String,ByteBuffer> sources=new HashMap<>(2);
        synchronized ByteBuffer acquire(String asset, Loader loader) throws IOException {
            int expected=expectedSize(asset);
            ByteBuffer source=sources.get(asset);
            if(source==null){
                source=loader.load(asset);
                if(source==null||!source.isDirect()||source.capacity()!=expected)throw new IOException("객체 모델 크기를 확인해 주세요");
                ByteBuffer header=source.duplicate();header.clear();
                if(header.get(4)!='T'||header.get(5)!='F'||header.get(6)!='L'||header.get(7)!='3')throw new IOException("객체 모델을 확인해 주세요");
                if(sources.size()>=2)throw new IOException("객체 모델 캐시를 확인해 주세요");
                source=source.asReadOnlyBuffer().order(ByteOrder.nativeOrder());source.clear();sources.put(asset,source);
            }
            // Neither a caller's cursor nor its Interpreter can mutate the cached bytes.
            ByteBuffer view=source.asReadOnlyBuffer().order(ByteOrder.nativeOrder());view.clear();return view;
        }
    }
}
