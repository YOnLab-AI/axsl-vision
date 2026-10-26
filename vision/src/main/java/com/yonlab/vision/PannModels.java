package com.yonlab.vision;
/** Publisher-pinned PANNs weights; constants are generated from converted bytes. */
final class PannModels {
 private PannModels(){}
 static boolean supports(String id){return "panns-cnn6".equals(id)||"panns-cnn10".equals(id);}
 static String sha(String id){switch(id){
 case "panns-cnn6":return "00fd20f5fae17b5d5a1dadcc86f8fc394be5c55366e85455a835031c5f1731b3";
 case "panns-cnn10":return "b6caad166df74fa25e1f1983b3e85a21ce6324a03458c123374a5f2703b21318";
 default:throw new IllegalArgumentException("소리 모델을 선택하세요");}}
 static String frontendSha(String id){switch(id){
 case "panns-cnn6":return "59ee4583ee2fdd42a0738fa35c408aad80f65e85a43dcf1dfb61778260ce5a0d";
 case "panns-cnn10":return "59ee4583ee2fdd42a0738fa35c408aad80f65e85a43dcf1dfb61778260ce5a0d";
 default:throw new IllegalArgumentException("소리 모델을 선택하세요");}}
 static final String LABELS_SHA="09bd2687536d2ef358df4feb33dc37a7ae4da107aaf8d67a1bea523487dff1bc";
}
