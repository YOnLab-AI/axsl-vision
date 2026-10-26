package com.yonlab.offlinevoice;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
public class WhisperThreadsTest {
 @Test public void selectedThreadsReachNativeModelConfiguration(){for(int n:new int[]{1,2,4})assertEquals(n,WhisperConfig.create(new File("weights"),n).getModelConfig().getNumThreads());}
 @Test public void invalidThreadCountsCannotReachSherpa(){for(int n:new int[]{-1,0,3,8,1024})try{WhisperConfig.create(new File("weights"),n);fail("accepted "+n);}catch(IllegalArgumentException expected){}}
}
