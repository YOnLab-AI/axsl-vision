package com.yonlab.vision;

import android.app.Activity;
import android.content.*;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** App library keeps the chosen region, condition, search and position across resize. */
public final class DemoLibraryActivity extends YOnActivity {
 private static final int IMPORT=901;
 private String feature,group="ALL",type="any",difficulty="all",query="",filterType="any";
 private int limit=18;private float restoredPosition;
 private LinearLayout body,cards;private ScrollView contentScroll;private TextView status;private EditText search;
 private boolean preparing,destroyed;private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());private Runnable pendingSearch;private final ExecutorService worker=Executors.newSingleThreadExecutor();
 @Override public void onCreate(Bundle state){super.onCreate(state);feature=getIntent().getStringExtra("feature");type=getIntent().getStringExtra("media_type");if(type==null)type="any";String initial=getIntent().getStringExtra("group");if(Arrays.asList("KO","GO","ALL").contains(initial))group=initial;if(state!=null){group=state.getString("group",group);limit=state.getInt("limit",18);difficulty=state.getString("difficulty","all");query=state.getString("query","");filterType=state.getString("filterType","any");restoredPosition=state.getFloat("scrollFraction",0);}render();OneUi.restoreScroll(contentScroll,restoredPosition);}
 @Override protected void onSaveInstanceState(Bundle out){out.putString("group",group);out.putInt("limit",limit);out.putString("difficulty",difficulty);out.putString("query",query);out.putString("filterType",filterType);out.putFloat("scrollFraction",OneUi.scrollFraction(contentScroll));super.onSaveInstanceState(out);}
 @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);float position=OneUi.scrollFraction(contentScroll);render();OneUi.restoreScroll(contentScroll,position);}
 private void render(){
  cancelSearch();
  LinearLayout frame=OneUi.col(this);frame.setBackgroundColor(Ui.BACK);frame.addView(OneUi.header(this,"라이브러리",this::finish));contentScroll=new ScrollView(this);body=Ui.column(this);contentScroll.addView(body);frame.addView(contentScroll,new LinearLayout.LayoutParams(-1,0,1));
  body.addView(Ui.heading(this,feature==null?"어떤 데모를 해볼까요?":"데모를 선택하세요",26));status=Ui.text(this,"사진·영상·음성을 바로 실행하세요",15);status.setTextColor(Ui.MUTED);body.addView(status);
  search=new EditText(this);search.setSingleLine(true);search.setTextSize(16);search.setHint("자료 이름·내용·조건 검색");search.setContentDescription("데모 검색");search.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,12));search.setBackground(Ui.outline(this,android.graphics.Color.WHITE,12));search.setText(query);search.setEnabled(!preparing);body.addView(search,Ui.fill());OneUi.gap(this,body,8);
  LinearLayout regions=OneUi.row(this);String[] ids={"ALL","KO","GO"},labels={"전체","한국 (KO)","글로벌 (GO)"};for(int k=0;k<ids.length;k++){final String id=ids[k];regions.addView(OneUi.chip(this,labels[k],id.equals(group),()->{if(!preparing){group=id;resetList();}}));}OneUi.horizontal(this,body,regions);
  if("any".equals(type)){LinearLayout kinds=OneUi.row(this);for(String[] choice:new String[][]{{"any","모든 자료"},{"image","사진"},{"video","영상"},{"audio","음성·소리"}})if("any".equals(choice[0])||allowedKind(choice[0]))kinds.addView(OneUi.chip(this,choice[1],choice[0].equals(filterType),()->{if(!preparing){filterType=choice[0];resetList();}}));OneUi.horizontal(this,body,kinds);}
  LinearLayout levels=OneUi.row(this);for(String id:new String[]{"all","easy","medium","hard","negative"})levels.addView(OneUi.chip(this,"all".equals(id)?"모든 조건":OneDemoText.difficulty(id),id.equals(difficulty),()->{if(!preparing){difficulty=id;resetList();}}));OneUi.horizontal(this,body,levels);
  body.addView(Ui.actions(this,Ui.secondary(this,"내 파일 가져오기",v->importFile()),Ui.secondary(this,"필터 초기화",v->{if(!preparing){group="ALL";difficulty="all";filterType="any";query="";resetList();}})),Ui.fill());cards=OneUi.col(this);cards.addOnLayoutChangeListener((view,l,t,r,b,ol,ot,or,ob)->{if(r-l>0&&r-l!=or-ol)cards.post(()->{if(!destroyed&&!preparing){float position=OneUi.scrollFraction(contentScroll);renderCards();OneUi.restoreScroll(contentScroll,position);}});});body.addView(cards,Ui.fill());renderCards();
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence text,int start,int count,int after){}public void onTextChanged(CharSequence text,int start,int before,int count){if(preparing)return;query=text.toString();limit=18;cancelSearch();pendingSearch=()->{pendingSearch=null;if(!destroyed&&!preparing)renderCards();};main.postDelayed(pendingSearch,180);}public void afterTextChanged(Editable text){}});setContentView(frame);
 }
 private void resetList(){limit=18;render();contentScroll.scrollTo(0,0);}
 private void renderCards(){
  cards.removeAllViews();try{
   List<DemoStore.Item> items=new ArrayList<>();String requested="any".equals(type)?filterType:type,needle=query.trim().toLowerCase(Locale.ROOT);
   for(DemoStore.Item item:DemoStore.items(this,feature,group,requested)){if(!"all".equals(difficulty)&&!difficulty.equals(item.difficulty))continue;String haystack=item.id+" "+item.title+" "+item.summary+" "+OneDemoText.summary(item.difficulty,item.conditions);if(needle.isEmpty()||haystack.toLowerCase(Locale.ROOT).contains(needle))items.add(item);}
   status.setText(preparing?"데모를 준비 중입니다":"자료 "+items.size()+"개 · 같은 조건과 다른 조건을 골라 비교하세요");int columns=Ui.contentColumns(this,cards,items.size());LinearLayout row=null;int shown=Math.min(limit,items.size());
   for(int k=0;k<shown;k++){
    DemoStore.Item item=items.get(k);if(k%columns==0){row=OneUi.row(this);row.setGravity(Gravity.TOP);row.setBaselineAligned(false);LinearLayout.LayoutParams rp=Ui.fill();rp.topMargin=Ui.dp(this,16);cards.addView(row,rp);}LinearLayout card=Ui.panel(this);ImageView thumb=new ImageView(this);thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
    if(!item.thumb.isEmpty()){try{thumb.setImageBitmap(thumbnail(item.thumb));}catch(IOException ignored){}card.addView(thumb,new LinearLayout.LayoutParams(-1,Ui.dp(this,140)));}else card.addView(Ui.heading(this,"audio".equals(item.type)?"♫":"video".equals(item.type)?"▶":"▧",28));OneUi.gap(this,card,12);
    card.addView(Ui.pill(this,("SHARED".equals(item.group)?"공통":item.group)+" · "+("image".equals(item.type)?"사진":"video".equals(item.type)?"영상":"음성"),false));String condition=OneDemoText.summary(item.difficulty,item.conditions);if(!condition.isEmpty())card.addView(OneUi.muted(this,condition,13));card.addView(Ui.heading(this,item.title,18));card.addView(OneUi.muted(this,item.summary,14));card.addView(Ui.actions(this,Ui.secondary(this,"이 데모 사용",v->choose(item))),Ui.fill());card.setContentDescription(item.title+", "+condition+", "+item.summary);
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);if(k%columns>0)cp.leftMargin=Ui.dp(this,16);row.addView(card,cp);if(k==shown-1)for(int x=k%columns+1;x<columns;x++)row.addView(new android.view.View(this),new LinearLayout.LayoutParams(0,1,1));
   }
   if(items.isEmpty())cards.addView(Ui.text(this,"검색어나 지역·조건을 바꿔보세요",16));if(items.size()>shown)cards.addView(Ui.secondary(this,"더 보기",v->{if(!preparing){limit+=18;renderCards();}}),Ui.fill());
  }catch(Exception e){cards.addView(Ui.text(this,"자료 목록을 열 수 없습니다",16));}
 }
 private void cancelSearch(){if(pendingSearch!=null){main.removeCallbacks(pendingSearch);pendingSearch=null;}}
 private android.graphics.Bitmap thumbnail(String path)throws IOException{BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(InputStream stream=getAssets().open(path)){BitmapFactory.decodeStream(stream,null,bounds);}if(bounds.outWidth<1||bounds.outHeight<1)return null;BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>320)options.inSampleSize*=2;try(InputStream stream=getAssets().open(path)){return BitmapFactory.decodeStream(stream,null,options);}}
 private void choose(DemoStore.Item item){if(preparing)return;cancelSearch();preparing=true;search.setEnabled(false);status.setText("데모를 준비 중입니다");worker.execute(()->{try{Uri uri=DemoStore.uri(this,item);runOnUiThread(()->{if(destroyed||isFinishing())return;Intent result=new Intent().setDataAndType(uri,mime(item.type)).putExtra("input_uri",uri.toString()).putExtra("demo_id",item.id).putExtra("demo_group",item.group).putExtra("media_type",item.type).putExtra("pair_demo_id",item.pairId).putExtra("reference_text",item.reference).putExtra("reference_verified",item.referenceVerified).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);result.setClipData(ClipData.newRawUri("AXSL Vision demo",uri));setResult(Activity.RESULT_OK,result);finish();});}catch(Exception e){runOnUiThread(()->{if(!destroyed){preparing=false;search.setEnabled(true);status.setText("자료를 준비할 수 없습니다. 다시 선택하세요");}});}});}
 private boolean allowedKind(String kind){OneCatalog.Feature task=OneCatalog.find(feature);return task==null||OneCatalog.accepts(task,kind,false);}
 private static String mime(String type){return "image".equals(type)?"image/*":"video".equals(type)?"video/*":"audio/*";}
 private void importFile(){if(preparing)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("any".equals(type)?"*/*":type.contains("/")?type:type+"/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);if("any".equals(type)){List<String> mimes=new ArrayList<>();for(String kind:new String[]{"image","video","audio"})if(allowedKind(kind))mimes.add(mime(kind));i.putExtra(Intent.EXTRA_MIME_TYPES,mimes.toArray(new String[0]));}try{startActivityForResult(i,IMPORT);}catch(ActivityNotFoundException e){status.setText("파일을 선택할 앱을 확인하세요");}}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=IMPORT||result!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null||!"content".equals(uri.getScheme()))return;String mime=getContentResolver().getType(uri);String kind=mime==null?null:mime.startsWith("image/")?"image":mime.startsWith("video/")?"video":mime.startsWith("audio/")?"audio":null;if(kind==null||!DemoRules.typeMatches(kind,type)||!allowedKind(kind)){status.setText("사진·영상·음성 파일을 선택하세요");return;}
  try{if((data.getFlags()&Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)!=0)getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}
  Intent out=new Intent().setDataAndType(uri,mime).putExtra("input_uri",uri.toString()).putExtra("media_type",kind).putExtra("demo_group","USER").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);out.setClipData(ClipData.newRawUri("AXSL Vision input",uri));setResult(RESULT_OK,out);finish();
 }
 @Override protected void onDestroy(){destroyed=true;cancelSearch();main.removeCallbacksAndMessages(null);worker.shutdownNow();super.onDestroy();}
}
