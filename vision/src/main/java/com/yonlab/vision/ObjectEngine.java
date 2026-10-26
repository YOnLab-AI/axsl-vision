package com.yonlab.vision;
import android.content.Context;
import android.graphics.Bitmap;
import org.tensorflow.lite.*;
import java.io.*;
import java.nio.*;
import java.util.*;

/** Bundled quantized COCO detectors; the default path remains SSD MobileNet. */
public final class ObjectEngine implements AutoCloseable {
    private final Interpreter engine;private final ByteBuffer model,input;private final int side,count;
    private final int[] pixels;private final List<String> labels=new ArrayList<>();
    private final float[][][] boxes;private final float[][] classes,scores;private final float[] number=new float[1];
    private final Map<Integer,Object> outputs=new HashMap<>();private boolean closed;
    public ObjectEngine(Context c)throws IOException {this(c,false);}
    public ObjectEngine(Context c,boolean dedicatedBall)throws IOException {
        String asset=dedicatedBall?"models/ball-efficientdet.tflite":"models/detect.tflite";
        model=ModelBuffers.acquire(c.getAssets(),asset);
        int threads=OneExecutionSettings.threads(c);
        Interpreter initialized=new Interpreter(model,new Interpreter.Options().setNumThreads(threads));
        try {
            // EfficientDet serializes dynamic output shapes; allocate before querying them.
            initialized.allocateTensors();
            if(initialized.getInputTensorCount()!=1||initialized.getOutputTensorCount()!=4)throw new IOException("객체 모델 입출력을 확인해 주세요");
            int[] shape=initialized.getInputTensor(0).shape();
            DataType[] types=new DataType[4];int[][] shapes=new int[4][];
            for(int i=0;i<4;i++){Tensor tensor=initialized.getOutputTensor(i);types[i]=tensor.dataType();shapes[i]=tensor.shape();}
            count=validateContract(initialized.getInputTensor(0).dataType(),shape,types,shapes);side=shape[1];
            pixels=new int[side*side];input=ByteBuffer.allocateDirect(side*side*3).order(ByteOrder.nativeOrder());boxes=new float[1][count][4];classes=new float[1][count];scores=new float[1][count];outputs.put(0,boxes);outputs.put(1,classes);outputs.put(2,scores);outputs.put(3,number);
            // Both models emit the same90 sparse class IDs; this shared file has a leading??? line.
            try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getAssets().open("models/labelmap.txt"),"UTF-8"))){String s;while((s=r.readLine())!=null)labels.add(s.trim());}
            engine=initialized;
        } catch(IOException|RuntimeException|Error failure) {
            try{initialized.close();}catch(RuntimeException|Error cleanup){failure.addSuppressed(cleanup);}
            throw failure;
        }
    }
    static int validateContract(DataType inputType,int[] shape,DataType[] types,int[][] shapes)throws IOException {
        if(inputType!=DataType.UINT8||shape==null||shape.length!=4||shape[0]!=1||shape[1]!=shape[2]||shape[3]!=3||shape[1]<32||shape[1]>1024)throw new IOException("객체 모델 입력을 확인해 주세요");
        if(types==null||types.length!=4||shapes==null||shapes.length!=4)throw new IOException("객체 모델 출력을 확인해 주세요");
        for(DataType type:types)if(type!=DataType.FLOAT32)throw new IOException("객체 모델 출력 형식을 확인해 주세요");
        int[] box=shapes[0];if(box==null||box.length!=3||box[0]!=1||box[2]!=4||box[1]<1||box[1]>1000)throw new IOException("객체 모델 박스를 확인해 주세요");
        int n=box[1];if(!Arrays.equals(shapes[1],new int[]{1,n})||!Arrays.equals(shapes[2],new int[]{1,n})||!Arrays.equals(shapes[3],new int[]{1}))throw new IOException("객체 모델 출력 크기를 확인해 주세요");
        return n;
    }
    public synchronized List<Observations.Detection> analyze(Bitmap bitmap,float threshold){
        if(closed)throw new IllegalStateException("분석이 종료되었습니다");if(bitmap==null||bitmap.isRecycled())throw new IllegalArgumentException("사진을 확인해 주세요");if(!finite(threshold)||threshold<0||threshold>1)throw new IllegalArgumentException("검출 기준을 확인해 주세요");
        Bitmap scaled=Bitmap.createScaledBitmap(bitmap,side,side,true);scaled.getPixels(pixels,0,side,0,0,side,side);if(scaled!=bitmap)scaled.recycle();input.clear();for(int p:pixels){input.put((byte)(p>>>16));input.put((byte)(p>>>8));input.put((byte)p);}input.rewind();
        engine.runForMultipleInputsOutputs(new Object[]{input},outputs);List<Observations.Detection> result=new ArrayList<>();int n=Math.min(count,Math.max(0,(int)number[0]));
        for(int i=0;i<n;i++){float score=scores[0][i];int id=Math.round(classes[0][i]);float[] b=boxes[0][i];if(!finite(score)||score<threshold||id<0||id+1>=labels.size())continue;float t=clamp(b[0]),l=clamp(b[1]),bottom=clamp(b[2]),r=clamp(b[3]);if(!finite(l+t+r+bottom)||l>=r||t>=bottom)continue;String label=labels.get(id+1);if("???".equals(label))continue;result.add(new Observations.Detection(id,label,score,l,t,r,bottom));}return result;
    }
    private static float clamp(float x){return Math.max(0,Math.min(1,x));}
    private static boolean finite(float x){return !Float.isNaN(x)&&!Float.isInfinite(x);}
    public synchronized void close(){if(!closed){closed=true;engine.close();}}
}
