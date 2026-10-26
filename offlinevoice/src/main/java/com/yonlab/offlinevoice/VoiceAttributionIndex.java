package com.yonlab.offlinevoice;
import java.util.*;

/** Exact source binding only; this index makes no independent claim about the linked license. */
final class VoiceAttributionIndex {
 private final Map<String,String> byId=new LinkedHashMap<>(),bySha=new LinkedHashMap<>();
 VoiceAttributionIndex(Map<String,String> source){for(Map.Entry<String,String> entry:source.entrySet()){String id=entry.getKey(),sha=entry.getValue();if(id!=null&&id.matches("[a-z0-9-]{1,80}")&&sha!=null&&sha.matches("[a-fA-F0-9]{64}")){sha=sha.toLowerCase(Locale.ROOT);byId.put(id,sha);if(!bySha.containsKey(sha))bySha.put(sha,id);}}}
 String resolve(String selectedId,String actualSha){if(actualSha==null||actualSha.isEmpty())return byId.containsKey(selectedId)?selectedId:"";if(!actualSha.matches("[a-fA-F0-9]{64}"))return "";String sha=actualSha.toLowerCase(Locale.ROOT);if(sha.equals(byId.get(selectedId)))return selectedId;String known=bySha.get(sha);return known==null?"":known;}
}
