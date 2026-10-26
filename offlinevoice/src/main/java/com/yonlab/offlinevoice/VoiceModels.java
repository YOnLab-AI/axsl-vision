package com.yonlab.offlinevoice;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

/** Public executable ASR registry. Model titles describe engines, not measured superiority. */
public final class VoiceModels {
 public static final class Entry {
  public final String id,title,summary,group,language,engine;public final boolean recommended;
  private Entry(String id,String title,String summary,String group,String language,String engine,boolean recommended){this.id=id;this.title=title;this.summary=summary;this.group=group;this.language=language;this.engine=engine;this.recommended=recommended;}
 }
 private static final List<Entry> ENTRIES=Collections.unmodifiableList(Arrays.asList(
  new Entry(VoiceModelPolicy.KO,"Vosk 한국어","한국어 · 스트리밍 음성 인식","KO","ko-KR","vosk",true),
  new Entry(VoiceModelPolicy.EN,"Vosk English","English · streaming speech recognition","GO","en-US","vosk",true),
  new Entry(VoiceModelPolicy.WHISPER,"Whisper Tiny INT8","한국어·English · 언어 자동 감지","ALL","auto","sherpa-whisper",false),
  new Entry(VoiceModelPolicy.WHISPER_BASE,"Whisper Base INT8","다국어 · 74M 파라미터 · 한국어·English 언어 자동 감지","ALL","auto","sherpa-whisper",false),
  new Entry(VoiceModelPolicy.WHISPER_SMALL,"Whisper Small INT8","다국어 · 244M 파라미터 · 한국어·English 언어 자동 감지","ALL","auto","sherpa-whisper",false)));
 private VoiceModels(){}
 private static SharedPreferences preferences(Context c){return c.getSharedPreferences("yon-one-voice-models",Context.MODE_PRIVATE);}
 public static List<Entry> list(Context c){return ENTRIES;}
 public static List<Entry> list(Context c,String group){if("ALL".equals(group))return ENTRIES;List<Entry> out=new ArrayList<>();for(Entry e:ENTRIES)if("ALL".equals(e.group)||e.group.equals("GO".equals(group)?"GO":"KO"))out.add(e);return Collections.unmodifiableList(out);}
 public static Entry find(String id){for(Entry e:ENTRIES)if(e.id.equals(id))return e;return null;}
 public static String selectedDefault(String group){return VoiceModelPolicy.defaultId(group);}
 public static String selected(Context c){return selected(c,"ALL");}
 public static String selected(Context c,String group){String value=preferences(c).getString("ALL".equals(group)?"last":"model-"+("GO".equals(group)?"GO":"KO"),"");return VoiceModelPolicy.compatible(group,value)?value:selectedDefault(group);}
 public static void select(Context c,String id){Entry e=find(id);if(e==null)throw new IllegalArgumentException("Unknown voice model");select(c,id,e.group);}
 /** Workspace selections can remain region-specific even for the shared multilingual weight set. */
 public static void select(Context c,String id,String group){Entry e=find(id);if(e==null||!VoiceModelPolicy.compatible(group,id))throw new IllegalArgumentException("Unknown voice model");SharedPreferences.Editor edit=preferences(c).edit().putString("last",id);if("ALL".equals(group))edit.putString("model-KO",id).putString("model-GO",id);else edit.putString("model-"+("GO".equals(group)?"GO":"KO"),id);edit.apply();}
 public static String title(Context c,String id){Entry e=find(id);return e==null?"음성 모델":e.title;}
}
