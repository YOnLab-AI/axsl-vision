package com.yonlab.vision;
import static org.junit.Assert.*;
import org.junit.Test;
import org.tensorflow.lite.DataType;
import java.io.IOException;
public final class ObjectContractTest {
 private static final DataType[] FLOATS={DataType.FLOAT32,DataType.FLOAT32,DataType.FLOAT32,DataType.FLOAT32};
 private static int[][] shapes(int n){return new int[][]{{1,n,4},{1,n},{1,n},{1}};}
 @Test public void existingSsdAndDynamicEfficientDetAllocatedContracts()throws Exception{assertEquals(10,ObjectEngine.validateContract(DataType.UINT8,new int[]{1,300,300,3},FLOATS,shapes(10)));assertEquals(25,ObjectEngine.validateContract(DataType.UINT8,new int[]{1,320,320,3},FLOATS,shapes(25)));}
 @Test(expected=IOException.class)public void floatInputIsRejected()throws Exception{ObjectEngine.validateContract(DataType.FLOAT32,new int[]{1,320,320,3},FLOATS,shapes(25));}
 @Test(expected=IOException.class)public void invalidBatchIsRejected()throws Exception{ObjectEngine.validateContract(DataType.UINT8,new int[]{2,320,320,3},FLOATS,shapes(25));}
 @Test(expected=IOException.class)public void missingDynamicAllocationIsRejected()throws Exception{ObjectEngine.validateContract(DataType.UINT8,new int[]{1,320,320,3},FLOATS,new int[][]{{},{},{},{}});}
 @Test(expected=IOException.class)public void classCountMismatchIsRejected()throws Exception{int[][] s=shapes(25);s[1]=new int[]{1,10};ObjectEngine.validateContract(DataType.UINT8,new int[]{1,320,320,3},FLOATS,s);}
 @Test(expected=IOException.class)public void wrongBoxLayoutIsRejected()throws Exception{int[][] s=shapes(25);s[0]=new int[]{1,25,5};ObjectEngine.validateContract(DataType.UINT8,new int[]{1,320,320,3},FLOATS,s);}
 @Test(expected=IOException.class)public void missingOutputIsRejected()throws Exception{ObjectEngine.validateContract(DataType.UINT8,new int[]{1,320,320,3},FLOATS,new int[][]{{1,25,4},{1,25},{1,25}});}
 @Test(expected=IOException.class)public void quantizedOutputIsRejected()throws Exception{DataType[] types=FLOATS.clone();types[2]=DataType.UINT8;ObjectEngine.validateContract(DataType.UINT8,new int[]{1,320,320,3},types,shapes(25));}
 @Test(expected=IOException.class)public void unboundedOutputIsRejected()throws Exception{ObjectEngine.validateContract(DataType.UINT8,new int[]{1,320,320,3},FLOATS,shapes(1001));}
 @Test(expected=IOException.class)public void wrongCountScalarIsRejected()throws Exception{int[][] s=shapes(25);s[3]=new int[]{1,1};ObjectEngine.validateContract(DataType.UINT8,new int[]{1,320,320,3},FLOATS,s);}
 @Test(expected=IOException.class)public void nonRgbInputIsRejected()throws Exception{ObjectEngine.validateContract(DataType.UINT8,new int[]{1,320,320,1},FLOATS,shapes(25));}
 @Test(expected=IOException.class)public void rectangularInputIsRejected()throws Exception{ObjectEngine.validateContract(DataType.UINT8,new int[]{1,240,320,3},FLOATS,shapes(25));}
}
