package com.yonlab.vision;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class OneYoloXMathTest{
 @Test public void rawHeadRequiresStrideDecode(){float[][] r=new float[3549][85];r[100][0]=.5f;r[100][1]=.5f;r[100][2]=0;r[100][3]=0;r[100][4]=.9f;r[100][5]=.8f;List<OneYoloXMath.Box>b=OneYoloXMath.decode(r,416,416,.5f);assertEquals(1,b.size());assertEquals(384,b.get(0).left,.0001);assertEquals(8,b.get(0).top,.0001);assertEquals(.72,b.get(0).score,.0001);}
 @Test public void topLeftLetterboxHasNoCenteredOffset(){float[][] r=new float[3549][85];r[52*10+20][0]=.5f;r[52*10+20][1]=.5f;r[52*10+20][4]=1;r[52*10+20][5]=1;OneYoloXMath.Box b=OneYoloXMath.decode(r,832,416,.4f).get(0);assertEquals(320,b.left,.001);assertEquals(160,b.top,.001);}
 @Test public void classAgnosticNmsRemovesOverlappingOtherLabel(){float[][] r=new float[3549][85];r[20][0]=.5f;r[20][1]=.5f;r[20][4]=1;r[20][5]=.9f;r[21][0]=-.5f;r[21][1]=.5f;r[21][4]=1;r[21][6]=.8f;List<OneYoloXMath.Box> out=OneYoloXMath.decode(r,416,416,.4f);assertEquals(1,out.size());assertEquals(0,out.get(0).cls);}
 @Test(expected=IllegalArgumentException.class)public void unsupportedHeadShapeCannotBeGuessed(){OneYoloXMath.decode(new float[8400][85],416,416,.5f);}
}
