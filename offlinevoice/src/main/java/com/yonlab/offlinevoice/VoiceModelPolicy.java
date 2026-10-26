package com.yonlab.offlinevoice;

/** Language defaults are independent from a deliberate cross-language comparison. */
public final class VoiceModelPolicy {
 public static final String KO="vosk-ko-small-0.22",EN="vosk-en-us-small-0.15",MOON="moonshine-ko-tiny-quantized-20260227",WHISPER="whisper-tiny-multilingual-int8",WHISPER_BASE="whisper-base-multilingual-int8",WHISPER_SMALL="whisper-small-multilingual-int8";
 private VoiceModelPolicy(){}
 public static boolean whisper(String id){return WHISPER.equals(id)||WHISPER_BASE.equals(id)||WHISPER_SMALL.equals(id);}
 public static String whisperSize(String id){if(WHISPER.equals(id))return "tiny";if(WHISPER_BASE.equals(id))return "base";if(WHISPER_SMALL.equals(id))return "small";throw new IllegalArgumentException("Whisper model");}
 public static String defaultId(String group){return "GO".equals(group)?EN:KO;}
 /** MOON remains a legacy identifier for old saved state; it is not executable. */
 public static boolean known(String id){return KO.equals(id)||EN.equals(id)||whisper(id);}
 public static String group(String id){if(!known(id))throw new IllegalArgumentException("Unknown voice model");return whisper(id)?"ALL":EN.equals(id)?"GO":"KO";}
 public static boolean compatible(String group,String id){return known(id)&&(whisper(id)||"ALL".equals(group)||VoiceModelPolicy.group(id).equals("GO".equals(group)?"GO":"KO"));}
 public static String resolve(String group,String requested,String preference){if(requested!=null&&!requested.isEmpty()){if(!known(requested))throw new IllegalArgumentException("Unknown voice model");return requested;}return compatible(group,preference)?preference:defaultId(group);}
}
