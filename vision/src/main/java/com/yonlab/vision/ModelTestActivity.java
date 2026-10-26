package com.yonlab.vision;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Choose one execution ID, then vary compatible inputs without replacing it. */
public final class ModelTestActivity extends YOnActivity {
 private static final int PICK=9621;
 private final ExecutorService io=Executors.newSingleThreadExecutor(r->new Thread(r,"axsl-model-test-inputs"));
 private final LinkedHashMap<String,OneModels.Entry> models=new LinkedHashMap<>();
 private final Map<String,List<OneCatalog.Feature>> features=new LinkedHashMap<>();
 private String modelId="mobilenet-v4-fp32",feature="classify",group="ALL",media="any",difficulty="ALL";
 private boolean diverse=true,closed,paused,busy;private int generation,limit=12,anchor=-1;private float position;
 private LinearLayout examples;private TextView status;private ScrollView scroll;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  for(OneCatalog.Feature f:OneCatalog.all())for(OneModels.Entry e:OneModels.list(this,f.id,"ALL")){models.putIfAbsent(e.id,e);features.computeIfAbsent(e.id,k->new ArrayList<>()).add(f);}
  if(models.isEmpty()){Ui.error(this,"실행 가능한 모델 목록을 열 수 없습니다",null);finish();return;}
  android.content.SharedPreferences saved=getSharedPreferences("AXSLModelTest",0);
  modelId=state==null?getIntent().getStringExtra("model_id"):state.getString("modelId");
  if(modelId==null)modelId=saved.getString("modelId","mobilenet-v4-fp32");
  feature=state==null?saved.getString("feature","classify"):state.getString("feature","classify");
  if(state!=null){group=state.getString("group","ALL");media=state.getString("media","any");difficulty=state.getString("difficulty","ALL");diverse=state.getBoolean("diverse",true);limit=state.getInt("limit",12);position=state.getFloat("position",0);}
  normalize();
 }
 private void normalize(){if(!models.containsKey(modelId))modelId=models.keySet().iterator().next();boolean found=false;for(OneCatalog.Feature f:features.get(modelId))if(f.id.equals(feature))found=true;if(!found)feature=features.get(modelId).get(0).id;if(!"any".equals(media)&&!OneCatalog.accepts(OneCatalog.find(feature),media))media="any";}
 @Override protected void onResume(){super.onResume();paused=false;build();}
 @Override protected void onPause(){position=OneUi.scrollFraction(scroll);anchor=scroll==null?-1:scroll.getScrollY();paused=true;generation++;busy=false;super.onPause();}
 @Override protected void onDestroy(){closed=true;generation++;io.shutdownNow();super.onDestroy();}
 @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);position=OneUi.scrollFraction(scroll);anchor=-1;build();}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("modelId",modelId);out.putString("feature",feature);out.putString("group",group);out.putString("media",media);out.putString("difficulty",difficulty);out.putBoolean("diverse",diverse);out.putInt("limit",limit);out.putFloat("position",OneUi.scrollFraction(scroll));}
 private boolean owns(int token){return !paused&&!closed&&!isFinishing()&&!isDestroyed()&&generation==token;}
 private void changed(){anchor=scroll==null?-1:scroll.getScrollY();limit=12;build();}
 private void restorePosition(){ScrollView target=scroll;int y=anchor;if(y>=0)target.post(()->target.scrollTo(0,y));else OneUi.restoreScroll(target,position);}
 private void build(){
  normalize();busy=false;int token=++generation;
  getSharedPreferences("AXSLModelTest",0).edit().putString("modelId",modelId).putString("feature",feature).apply();
  LinearLayout root=OneUi.col(this);root.setBackgroundColor(Ui.BACK);root.addView(OneUi.header(this,"모델 시험",this::finish),Ui.fill());
  scroll=new ScrollView(this);LinearLayout body=Ui.column(this);scroll.addView(body,Ui.fill());root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  body.addView(Ui.heading(this,"모델을 고르고, 입력을 바꿔 시험하세요",25));
  body.addView(OneUi.muted(this,"1 모델 선택 → 2 지원 기능 확인 → 3 조건이 다른 자료로 시험 → 4 같은 모델의 기록 비교",14));
  LinearLayout choice=Ui.panel(this);choice.addView(Ui.heading(this,"시험할 모델",20));
  List<String> ids=new ArrayList<>(models.keySet());String[] names=new String[ids.size()];for(int i=0;i<ids.size();i++)names[i]=models.get(ids.get(i)).title+" · "+ids.get(i);
  Spinner model=new Spinner(this);model.setContentDescription("시험할 모델");model.setMinimumHeight(Ui.dp(this,48));model.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));model.setSelection(ids.indexOf(modelId));choice.addView(model,Ui.fill());
  model.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,android.view.View v,int n,long id){if(owns(token)&&!modelId.equals(ids.get(n))){modelId=ids.get(n);normalize();media="any";difficulty="ALL";changed();}}public void onNothingSelected(AdapterView<?> p){}});
  choice.addView(Ui.heading(this,"시험 기능",18));List<OneCatalog.Feature> supported=features.get(modelId);String[] tasks=new String[supported.size()];int selected=0;for(int i=0;i<tasks.length;i++){tasks[i]=supported.get(i).title;if(supported.get(i).id.equals(feature))selected=i;}
  Spinner function=new Spinner(this);function.setContentDescription("시험 기능");function.setMinimumHeight(Ui.dp(this,48));function.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,tasks));function.setSelection(selected);choice.addView(function,Ui.fill());
  function.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,android.view.View v,int n,long id){if(owns(token)&&!feature.equals(supported.get(n).id)){feature=supported.get(n).id;media="any";difficulty="ALL";changed();}}public void onNothingSelected(AdapterView<?> p){}});
  choice.addView(OneUi.muted(this,models.size()+"개 실행 ID · 가중치를 공유하는 기능과 코드·재생 엔진을 포함합니다",13));body.addView(choice,Ui.fill());
  OneGuidance.add(this,body,feature,modelId,"ALL");
  body.addView(Ui.actions(this,Ui.secondary(this,"이 모델의 시험 기록",v->startActivity(new Intent(this,OneReportsActivity.class).putExtra("model_id",modelId))),Ui.secondary(this,"자료 선택 · 내 파일",v->pick())),Ui.fill());
  LinearLayout filters=Ui.panel(this);filters.addView(Ui.heading(this,"시험 자료",20));filters.addView(OneUi.muted(this,"자료를 바꿔도 시험 모델은 유지됩니다. KO·GO는 자료의 구분이며 모델 언어를 자동 변경하지 않습니다.",14));
  LinearLayout groups=OneUi.row(this);for(String[] item:new String[][]{{"ALL","전체 자료"},{"KO","한국 자료(KO)"},{"GO","글로벌 자료(GO)"},{"SHARED","공통 자료"}})groups.addView(OneUi.chip(this,item[1],group.equals(item[0]),()->{group=item[0];changed();}));OneUi.horizontal(this,filters,groups);
  LinearLayout kinds=OneUi.row(this);kinds.addView(OneUi.chip(this,"모든 입력",media.equals("any"),()->{media="any";changed();}));for(String type:OneCatalog.inputs(OneCatalog.find(feature),false))kinds.addView(OneUi.chip(this,mediaLabel(type),media.equals(type),()->{media=type;changed();}));OneUi.horizontal(this,filters,kinds);
  LinearLayout levels=OneUi.row(this);for(String[] item:new String[][]{{"ALL","모든 조건"},{"easy","쉬움"},{"medium","보통"},{"hard","어려움"},{"negative","대조군"},{"unspecified","난도 미표기"}})levels.addView(OneUi.chip(this,item[1],difficulty.equals(item[0]),()->{difficulty=item[0];changed();}));OneUi.horizontal(this,filters,levels);
  filters.addView(Ui.actions(this,Ui.secondary(this,"조건별 6개",v->{diverse=true;changed();}),Ui.secondary(this,"전체 목록",v->{diverse=false;changed();})),Ui.fill());filters.addView(OneUi.muted(this,"검증 참조값이 있어도 해당 기능이 지원하는 지표만 계산합니다. 정답 없는 결과를 정확도로 표시하지 않습니다.",13));body.addView(filters,Ui.fill());
  status=Ui.text(this,"호환되는 자료를 읽는 중",14);body.addView(status);examples=OneUi.col(this);body.addView(examples,Ui.fill());setContentView(root);restorePosition();
  final String task=feature,chosenGroup=group,chosenMedia=media,chosenDifficulty=difficulty;final boolean selectedDiverse=diverse;final int selectedLimit=limit;
  io.execute(()->{List<DemoStore.Item> candidates=new ArrayList<>();Exception error=null;try{for(DemoStore.Item item:DemoStore.items(getApplicationContext(),task,"ALL","any"))if(OneCatalog.accepts(OneCatalog.find(task),item.type)&&OneTestSamples.matches(asCase(item),chosenGroup,chosenMedia,chosenDifficulty,""))candidates.add(item);}catch(Exception e){error=e;}
   List<DemoStore.Item> result=new ArrayList<>();if(selectedDiverse){List<OneTestSamples.Case> meta=new ArrayList<>();Map<String,DemoStore.Item> byId=new HashMap<>();for(DemoStore.Item item:candidates){meta.add(asCase(item));byId.put(item.id,item);}for(OneTestSamples.Case c:OneTestSamples.diverse(meta,6))result.add(byId.get(c.id));}else result.addAll(candidates.subList(0,Math.min(selectedLimit,candidates.size())));
   int total=candidates.size();Exception failure=error;runOnUiThread(()->{if(!owns(token))return;examples.removeAllViews();if(failure!=null){status.setText("자료 목록을 열 수 없습니다 · 다시 진입해 주세요");return;}status.setText("호환 자료 "+total+"개 · "+(selectedDiverse?"조건별":"전체 목록")+" "+result.size()+"개 표시");for(DemoStore.Item item:result)addCase(item,token);if(result.isEmpty())examples.addView(OneUi.muted(this,"이 조건에 맞는 자료가 없습니다. 필터를 바꾸거나 내 파일을 선택하세요.",14));if(!selectedDiverse&&total>selectedLimit)examples.addView(Ui.secondary(this,"자료 더 보기",v->{anchor=scroll.getScrollY();limit+=12;build();}),Ui.fill());restorePosition();});
  });
 }
 private static OneTestSamples.Case asCase(DemoStore.Item item){return new OneTestSamples.Case(item.id,item.group,item.type,item.difficulty,item.conditions);}
 private static String mediaLabel(String type){return "image".equals(type)?"사진":"video".equals(type)?"영상":"음성";}
 private void addCase(DemoStore.Item item,int token){LinearLayout card=Ui.panel(this);card.addView(Ui.heading(this,item.title,19));card.addView(OneUi.muted(this,item.id+" · "+mediaLabel(item.type)+" · "+item.group,12));String condition=OneDemoText.summary(item.difficulty,item.conditions);card.addView(Ui.text(this,"입력 조건 · "+(condition.isEmpty()?"난도·조건 미표기":condition),14));if(!item.summary.isEmpty())card.addView(OneUi.muted(this,item.summary,13));card.addView(OneUi.muted(this,"참조값 · "+(item.referenceVerified?"검증됨":item.reference.isEmpty()?"정답 없음":"미검증 참고값"),13));Button go=Ui.button(this,"이 자료로 시험",v->open(item,token));go.setContentDescription("시험 자료 "+item.id);card.addView(go,Ui.fill());examples.addView(card,Ui.fill());}
 private void open(DemoStore.Item item,int token){if(busy||!owns(token))return;busy=true;String testedModel=modelId,testedFeature=feature;status.setText("자료의 크기와 SHA-256을 확인하고 있습니다");io.execute(()->{Uri uri=null;Exception error=null;try{uri=DemoStore.uri(getApplicationContext(),item);}catch(Exception e){error=e;}Uri input=uri;Exception failure=error;runOnUiThread(()->{if(!owns(token))return;busy=false;if(failure!=null){Ui.error(this,"시험 자료를 열 수 없습니다",failure);return;}OneLaunch.openTest(this,testedFeature,testedModel,input,item.type,item.id,item.group);});});}
 private void pick(){if(busy)return;startActivityForResult(OneInputs.picker(this,media,feature).putExtra("group",group),PICK);}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK||result!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null){String value=data.getStringExtra("input_uri");if(value!=null)uri=Uri.parse(value);}if(uri==null||!"content".equals(uri.getScheme()))return;String demo=data.getStringExtra("demo_id");String type=null;try{DemoStore.Item item=DemoStore.find(this,demo==null?"":demo);type=item==null?getContentResolver().getType(uri):item.type;}catch(Exception e){Ui.error(this,"입력 정보를 확인할 수 없습니다",e);return;}if(type==null||!OneCatalog.accepts(OneCatalog.find(feature),type)){Ui.error(this,"이 기능이 지원하는 입력 형식을 선택하세요",null);return;}OneLaunch.openTest(this,feature,modelId,uri,type,demo,"ALL");}
}
