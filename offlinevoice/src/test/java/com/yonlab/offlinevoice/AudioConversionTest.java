package com.yonlab.offlinevoice;
import org.junit.Test;

/** Two discovered JUnit batches; assertion counts are reported by the actual production-code harnesses. */
public final class AudioConversionTest {
 @Test public void conversionAndWavContracts()throws Exception{AudioConversionHost.main(new String[0]);}
 @Test public void cancellationAndLateOpenOwnership()throws Exception{InputCancellationHost.main(new String[0]);}
}
