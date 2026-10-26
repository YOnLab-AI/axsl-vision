package com.yonlab.vision;
import org.junit.Test;
import static org.junit.Assert.*;
/** A window configuration is not the content width after system-bar and cutout insets. */
public final class OneMeasuredWorkspaceTest {
 private static boolean split(int contentWidth,int height,float scale){try{return (Boolean)OneLayoutPolicy.class.getMethod("workspaceSplitContent",int.class,int.class,float.class).invoke(null,contentWidth,height,scale);}catch(ReflectiveOperationException error){throw new AssertionError("Measured content must drive workspace panels",error);}}
 @Test public void sideInsetsBelowControlMinimumStackPanels(){assertTrue(OneLayoutPolicy.workspaceSplit(1000,600,1));assertFalse(split(708,600,1));}
 @Test public void sufficientMeasuredContentKeepsTwoPanels(){assertTrue(split(780,600,1));}
 @Test public void shortLandscapeStacksPanels(){assertFalse(split(1100,400,1));}
 @Test public void largerTextRequiresMoreContent(){assertFalse(split(780,600,1.3f));assertTrue(split(1000,600,1.3f));}
}
