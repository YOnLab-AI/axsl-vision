package com.yonlab.offlinevoice;
import org.junit.Test;
public final class AsrPolicyTest {
 @Test public void languageMetricsAndExports()throws Exception{AsrPolicyHost.main(new String[0]);}
 @Test public void englishModelExtraction()throws Exception{VoiceZipHost.main(new String[0]);}
 @Test public void legacyInputPolicy()throws Exception{InputPolicyHost.main(new String[0]);}
 @Test public void nativeOpenAndCleanupFailure()throws Exception{VoiceCleanupHost.main(new String[0]);}
}
