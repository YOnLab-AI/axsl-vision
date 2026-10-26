package com.yonlab.offlinevoice;
import android.content.*;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class VoiceReports {
 private VoiceReports(){}
 static File save(Context c,JSONArray rows)throws Exception{
  File base=new File(c.getFilesDir(),"exports");if(!base.isDirectory()&&!base.mkdirs())throw new IOException("Report storage");String name="AXSL-Vision-ASR-"+UUID.randomUUID();
  JSONObject report=new JSONObject().put("schema","YOn-ASR-1").put("createdAt",AsrRuns.now()).put("cerPolicy",AsrTextMetrics.POLICY).put("rows",rows);
  java.util.List<VoiceVisualReport.Row> visualRows=new java.util.ArrayList<>();
  String[] columns={"modelId","language","state","inputSha256","sourceSha256","loadMs","inferenceMs","elapsedMs","durationMs","rtf","referenceVerified","cer","transcript","editedText","reference","modelLanguage","datasetGroup","detectedLanguage","attribution"};StringBuilder csv=new StringBuilder();for(int i=0;i<columns.length;i++){if(i>0)csv.append(',');csv.append(VoiceReportText.csv(columns[i]));}csv.append("\r\n");
  for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);VoiceInputAttribution.Entry attribution=VoiceInputAttribution.find(c,row.optString("demoId"),row.optString("sourceSha256"));row.put("dataAttribution",attribution.json()).put("attribution",attribution.text());VoiceVisualReport.Row visual=new VoiceVisualReport.Row();visual.modelId=row.optString("modelId");visual.title=VoiceModels.title(c,visual.modelId);visual.language=row.optString("language");visual.modelLanguage=row.optString("modelLanguage",visual.language);visual.datasetGroup=row.optString("datasetGroup");visual.detectedLanguage=row.optString("detectedLanguage");visual.state=row.optString("state");visual.transcript=row.optString("transcript");visual.hasEdited=row.has("editedText");visual.edited=row.optString("editedText");visual.referenceVerified=row.optBoolean("referenceVerified");visual.reference=visual.referenceVerified?row.optString("reference"):"";visual.inputSha=row.optString("inputSha256");visual.sourceSha=row.optString("sourceSha256");visual.attribution=attribution.text();visual.timingScope=row.optString("timingScope");visual.loadMs=row.optLong("loadMs");visual.inferenceMs=row.optLong("inferenceMs");visual.elapsedMs=row.optLong("elapsedMs");visual.durationMs=row.optLong("durationMs");if(visual.referenceVerified&&row.has("cer")&&!row.isNull("cer"))visual.cer=row.getDouble("cer");if(row.has("rtf")&&!row.isNull("rtf"))visual.rtf=row.getDouble("rtf");visualRows.add(visual);for(int j=0;j<columns.length;j++){if(j>0)csv.append(',');csv.append(VoiceReportText.csv(row.isNull(columns[j])?"":row.optString(columns[j],"")));}csv.append("\r\n");}
  String logo="";String html=VoiceVisualReport.html(visualRows,logo);
  File json=new File(base,name+".json"),htmlFile=new File(base,name+".html"),csvFile=new File(base,name+".csv");try{write(json,report.toString(2));write(htmlFile,html);write(csvFile,"\ufeff"+csv);return htmlFile;}catch(Exception e){for(File f:new File[]{json,htmlFile,csvFile})if(f.exists()&&!f.delete())e.addSuppressed(new IOException("Incomplete report cleanup"));throw e;}
 }
 private static void write(File f,String value)throws IOException{byte[] bytes=value.getBytes(StandardCharsets.UTF_8);if(bytes.length>2*1024*1024)throw new IOException("Report size");File pending=new File(f.getPath()+".new");try{try(FileOutputStream out=new FileOutputStream(pending)){out.write(bytes);out.getFD().sync();}if(!pending.renameTo(f))throw new IOException("Report commit");}finally{if(pending.exists())pending.delete();}}
 static void share(Context c,File file)throws Exception{Uri uri=(Uri)Class.forName("androidx.core.content.FileProvider").getMethod("getUriForFile",Context.class,String.class,File.class).invoke(null,c,c.getPackageName()+".files",file);Intent i=new Intent(Intent.ACTION_SEND).setType("text/html").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(ClipData.newRawUri("AXSL Vision 음성 결과",uri));c.startActivity(Intent.createChooser(i,"음성 결과 공유"));}
}
