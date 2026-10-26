package com.yonlab.vision;
import java.util.*;
public final class OneUiPolicyHost {
 static int count; static void check(boolean ok,String message){count++;if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
 check(OneExportCore.escape("<script>&\"'").equals("&lt;script&gt;&amp;&quot;&#39;"),"HTML must escape all five delimiter characters");
 check(OneExportCore.escape(null).equals(""),"null field");
 check(OneExportCore.cell("a,b\"c\n").equals("\"a,b\"\"c\n\""),"CSV comma quote newline round trip");
 check(OneExportCore.cell("  =2+2").startsWith("\"'"),"CSV formula neutralization");
 check(OneExportCore.cell("\t@SUM(A1)").startsWith("\"'"),"tab prefixed formula");
 List<String[]> rows=Collections.singletonList(new String[]{"<img src=x onerror=x>","42"});
 String html=OneExportCore.html("<Title>",new String[]{"A","B"},rows,"javascript:alert(1)");
 check(!html.contains("<img src=x")&&html.contains("&lt;img")&&html.contains("&lt;Title&gt;"),"export must be escaped human readable actual data");
 check(!html.contains("javascript:"),"logo URI allowlist");check(html.contains("connect-src 'none'"),"offline report CSP");
 check(OneExportCore.csv(new String[]{"A","B"},rows).startsWith("\ufeff\"A\",\"B\"\r\n"),"Excel UTF8 header and CRLF");
 Set<String> expected=new HashSet<>(Arrays.asList("detect,classify,segment,faces,compare,access,drive,flow,vis,pose,ocr,qr,speech,signage,document,studio,search,sound,coach".split(",")));
 Set<String> actual=new HashSet<>();for(OneCatalog.Feature f:OneCatalog.all()){check(expected.contains(f.id),"unsupported feature");check(f.media.length>0,"feature input kinds");actual.add(f.id);}
 check(actual.equals(expected)&&OneCatalog.all().size()==19,"19 unique executable feature IDs");check(OneCatalog.find("../../../bad")==null,"unrecognized feature");
 boolean immutable=false;try{OneCatalog.all().clear();}catch(UnsupportedOperationException e){immutable=true;}check(immutable,"catalog list is immutable");
 OneCatalog.Feature first=OneCatalog.find("detect");first.media[0]="bad";check(!OneCatalog.find("detect").media[0].equals("bad"),"catalog input kinds not globally mutable");
 check(OneBoardPolicy.detected("sdm845","",""),"board hardware");check(OneBoardPolicy.detected("","","aikit"),"board model");check(!OneBoardPolicy.detected("tensor","pixel","phone"),"generic device");
 check(OneBoardPolicy.threads(true,false)==2&&OneBoardPolicy.threads(false,true)==2&&OneBoardPolicy.threads(true,true)==4,"OFF default and generic path are2; detected board opt-in4");
 java.io.File folder=new java.io.File(System.getProperty("java.io.tmpdir"),"yon-one-host-exports");
 check(OneExportCore.safeReportFile(folder,new java.io.File(folder,"ok.html")),"saved report child allowlist");
 check(!OneExportCore.safeReportFile(folder,new java.io.File(folder,"../private.json")),"saved report traversal");
 check(!OneExportCore.safeReportFile(folder,new java.io.File(folder.getParentFile(),folder.getName()+"-other/ok.html")),"saved report prefix boundary");
 check(!OneExportCore.safeReportFile(folder,new java.io.File(folder,"file.exe")),"saved report file type");
 check(OneExportCore.safeReportFile(folder,new java.io.File(folder,"sub/table.CSV")),"saved report nested CSV");
 for(String id:new String[]{"detect","faces","access","drive","flow","vis"}){
  OneCatalog.Feature f=OneCatalog.find(id);
  check(OneCatalog.accepts(f,"video",false),"normal video route remains available: "+id);
  check(!OneCatalog.accepts(f,"video",true),"comparison rejects video before image decoder: "+id);
  check(OneCatalog.accepts(f,"image/jpeg",true),"comparison accepts still image: "+id);
  check(Arrays.equals(OneCatalog.inputs(f,true),new String[]{"image"}),"restored comparison input is normalized to image: "+id);
 }
 check(Arrays.equals(OneCatalog.inputs(OneCatalog.find("speech"),true),new String[]{"audio"}),"speech comparison audio-only");
 check(OneCatalog.accepts(OneCatalog.find("speech"),"audio/wav",true)&&!OneCatalog.accepts(OneCatalog.find("speech"),"image",true),"speech comparison MIME gate");
 check(OneCatalog.inputs(OneCatalog.find("signage"),true).length==0,"no model-comparison route for media player");
 check(OneCatalog.accepts(OneCatalog.find("signage"),"video",false),"media-player normal video remains available");
 check(OneCatalog.inputs(null,true).length==0&&!OneCatalog.accepts(null,"image",true),"invalid comparison feature is rejected");
 check("KO".equals(OneCatalog.comparisonGroup("speech","KO","ALL")),"Korean speech demo comparison overrides ALL");
 check("GO".equals(OneCatalog.comparisonGroup("speech","GO","KO")),"English speech demo comparison overrides Korean filter");
 check("GO".equals(OneCatalog.comparisonGroup("speech",null,"GO")),"user input keeps selected language group");
 check("KO".equals(OneCatalog.comparisonGroup("speech","SHARED","KO")),"shared sample does not invent a language");
 check("ALL".equals(OneCatalog.comparisonGroup("detect","KO","ALL")),"vision model comparison keeps region filter");
 System.out.println("One UI policy PASS "+count+" checks"); }
}
