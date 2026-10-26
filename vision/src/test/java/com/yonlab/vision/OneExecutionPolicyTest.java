package com.yonlab.vision;
import org.junit.Test;
import static org.junit.Assert.*;

public class OneExecutionPolicyTest {
 @Test public void automaticKeepsBaselineAndBoardIsOptIn(){assertEquals(2,OneExecutionPolicy.threads("auto",false,8,false));assertEquals(4,OneExecutionPolicy.threads("auto",true,8,false));}
 @Test public void everySupportedManualThreadCountIsHonored(){for(int n:new int[]{1,2,4})assertEquals(n,OneExecutionPolicy.threads("cpu"+n,false,8,false));}
 @Test public void manualChoiceDoesNotSilentlyBecomeBoardTurbo(){assertEquals(1,OneExecutionPolicy.threads("cpu1",true,8,false));assertEquals(2,OneExecutionPolicy.threads("cpu2",true,8,false));}
 @Test public void scarceResourcesOverrideEveryProfile(){for(String mode:new String[]{"auto","cpu1","cpu2","cpu4",null,"bad"})for(boolean board:new boolean[]{false,true})assertEquals(1,OneExecutionPolicy.threads(mode,board,8,true));}
 @Test public void neverSchedulesMoreWorkersThanAvailableCores(){assertEquals(1,OneExecutionPolicy.threads("cpu4",false,1,false));assertEquals(2,OneExecutionPolicy.threads("cpu4",false,2,false));assertEquals(2,OneExecutionPolicy.threads("cpu4",false,3,false));}
 @Test public void missingCoreCountUsesBoundedFallback(){assertEquals(2,OneExecutionPolicy.threads("cpu4",false,0,false));assertEquals(2,OneExecutionPolicy.threads("auto",true,-8,false));}
 @Test public void invalidPreferencesCannotRequestArbitraryThreadCounts(){for(String mode:new String[]{null,"","cpu0","cpu3","cpu1024","CPU4"," auto "})assertEquals(2,OneExecutionPolicy.threads(mode,false,8,false));}
}
