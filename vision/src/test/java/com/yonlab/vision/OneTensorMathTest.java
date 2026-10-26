package com.yonlab.vision;
import org.junit.Test;import static org.junit.Assert.*;
public class OneTensorMathTest {
 @Test public void actualMetadataNormalizationAndQuantization(){assertEquals(-.9921875f,OneTensorMath.efficientNet(0),1e-7);assertEquals(1f,OneTensorMath.efficientNet(255),1e-7);assertEquals(131,OneTensorMath.quantize(0,.012566017f,131));assertEquals(52,OneTensorMath.quantize(OneTensorMath.efficientNet(0),.012566017f,131));assertEquals(211,OneTensorMath.quantize(OneTensorMath.efficientNet(255),.012566017f,131));}
 @Test public void letterboxCoordinatesDoNotSwapXAndY(){assertEquals(.5f,OneTensorMath.coordinate(.5f,48,96,192),1e-7);assertEquals(0,OneTensorMath.coordinate(.25f,48,96,192),1e-7);assertEquals(1,OneTensorMath.coordinate(.75f,48,96,192),1e-7);}
 @Test public void cocoJointMappingMatchesMlKitKnees(){int[] ids=OneTensorMath.cocoToMlKit();assertEquals(17,ids.length);assertEquals(23,ids[11]);assertEquals(25,ids[13]);assertEquals(28,ids[16]);}
 @Test(expected=IllegalArgumentException.class)public void invalidQuantizationRefused(){OneTensorMath.quantize(1,0,0);}
}
