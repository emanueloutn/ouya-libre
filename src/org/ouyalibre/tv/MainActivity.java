package org.ouyalibre.tv;

import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.text.Html;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
 private final ExecutorService workers=Executors.newFixedThreadPool(3);
 private final Handler handler=new Handler();
 private final List<Future<?>> jobs=new ArrayList<Future<?>>();
 private final List<Item> results=new ArrayList<Item>();
 private final Map<String,Source> sources=new LinkedHashMap<String,Source>();
 private final List<String> diagnostics=new ArrayList<String>();
 private Store store;private TextView status;private Button search;private ListView list;private ArrayAdapter<Item> adapter;
 private String query="",view="search";private int generation=0,remaining=0,failures=0,page=1;private boolean destroyed=false;
 public void onCreate(Bundle b){super.onCreate(b);store=new Store(this);
  rebuildSources();LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,16,24,16);root.setBackgroundColor(Ui.BG);
  root.addView(Ui.text(this,"OUYA LIBRE   /   Vídeo público",26));
  LinearLayout first=new LinearLayout(this);search=add(first,"Buscar películas, series, anime…",new Runnable(){public void run(){Ui.keyboard(MainActivity.this,"Buscar en todas las fuentes",query,new Ui.Input(){public void done(String s){query=s;page=1;runSearch();}});}});
  add(first,"Favoritos",new Runnable(){public void run(){showSaved("favorites");}});add(first,"Historial / Continuar",new Runnable(){public void run(){showSaved("history");}});root.addView(first);
  LinearLayout second=new LinearLayout(this);add(second,"Abrir enlace público",new Runnable(){public void run(){Ui.keyboard(MainActivity.this,"URL de vídeo o página pública","https://",new Ui.Input(){public void done(String s){try{Net.safe(s);Item i=new Item("Enlace público",s,s,"Enlace añadido por ti","",s);details(i);}catch(Exception e){message("Enlace no válido",e.getMessage());}}});}});
  add(second,"Fuentes",new Runnable(){public void run(){manageSources();}});add(second,"Estado de búsqueda",new Runnable(){public void run(){message("Fuentes y resultados",join(diagnostics));}});root.addView(second);
  status=Ui.text(this,"Escribe una búsqueda. Se comprueban las fuentes antes de mostrar resultados.\nEspañol primero cuando la fuente declara el idioma; audio latino sin garantizar.",16);root.addView(status);
  list=new ListView(this);list.setDividerHeight(5);list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);list.setSelector(Ui.shape(android.graphics.Color.rgb(25,106,96)));
  adapter=new ArrayAdapter<Item>(this,android.R.layout.simple_list_item_1,results){public View getView(int p,View recycled,android.view.ViewGroup parent){TextView t=(TextView)super.getView(p,recycled,parent);t.setTextColor(Ui.TEXT);t.setTextSize(19);t.setPadding(18,14,18,14);t.setMaxLines(3);return t;}};
  list.setAdapter(adapter);list.setOnItemClickListener(new AdapterView.OnItemClickListener(){public void onItemClick(AdapterView<?> p,View v,int pos,long id){details(results.get(pos));}});
  root.addView(list,new LinearLayout.LayoutParams(-1,0,1));LinearLayout pages=new LinearLayout(this);add(pages,"Página anterior",new Runnable(){public void run(){if(page>1&&query.length()>0){page--;runSearch();}}});add(pages,"Más resultados",new Runnable(){public void run(){if(query.length()>0){page++;runSearch();}}});root.addView(pages);root.addView(Ui.text(this,"Cruceta: navegar   ·   O: aceptar   ·   A: volver   |   Solo vídeos accesibles comprobados · Idioma según la fuente",14));setContentView(root);search.requestFocus();
  if(b!=null){query=b.getString("query","");String saved=b.getString("view","search");if(!saved.equals("search"))showSaved(saved);else if(query.length()>0)runSearch();}
 }
 private Button add(LinearLayout row,String title,final Runnable action){Button b=Ui.button(this,title,new View.OnClickListener(){public void onClick(View v){action.run();}});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1);p.setMargins(3,3,3,3);row.addView(b,p);return b;}
 private void rebuildSources(){sources.clear();Source ok=new OkSource();sources.put(ok.name(),ok);Source web=new WebSource();sources.put(web.name(),web);Source archive=new ArchiveSource();sources.put(archive.name(),archive);Source rtvc=new RtvcSource();sources.put(rtvc.name(),rtvc);Source nasa=new NasaSource();sources.put(nasa.name(),nasa);
  for(String s:store.sources())try{Source x=s.startsWith("P|")?new PeerTubeSource(s.substring(2)):new M3uSource(s.substring(2));sources.put(x.name(),x);}catch(Exception e){}
 }
 private void cancel(){generation++;for(Future<?> f:jobs)f.cancel(true);jobs.clear();((java.util.concurrent.ThreadPoolExecutor)workers).purge();}
 private void runSearch(){
  if(query.trim().length()==0){message("Buscar","Escribe un título o palabras clave.");return;}
  cancel();view="search";final int gen=generation;final String q=query;final int requestedPage=page;
  results.clear();adapter.notifyDataSetChanged();diagnostics.clear();remaining=sources.size();failures=0;
  status.setText("Buscando y comprobando vídeos de «"+q+"»…");
  for(final Source source:new ArrayList<Source>(sources.values()))jobs.add(workers.submit(new Runnable(){public void run(){
   int accepted=0,checked=0;String err=null;
   try{
    List<Item> found=source instanceof PagedSource?((PagedSource)source).search(q,requestedPage):(requestedPage==1?source.search(q):Collections.<Item>emptyList());
    for(final Item item:found){
     if(Thread.currentThread().isInterrupted())return;
     if(item.related()||!OkSource.matches(item.title,q))continue;if(checked>=12)break;checked++;
     try{if(!Playable.item(source,item))continue;}catch(InterruptedException e){return;}catch(Exception e){continue;}
     item.verified=true;accepted++;
     handler.post(new Runnable(){public void run(){if(destroyed||gen!=generation)return;
      for(Item old:results)if(old.url.equals(item.url))return;
      if(results.size()<150)results.add(item);
      Collections.sort(results,new Comparator<Item>(){public int compare(Item a,Item b){boolean al=a.language.toLowerCase(Locale.US).contains("latino"),bl=b.language.toLowerCase(Locale.US).contains("latino");if(al!=bl)return al?-1:1;if(a.spanish()!=b.spanish())return a.spanish()?-1:1;return a.title.compareToIgnoreCase(b.title);}});
      adapter.notifyDataSetChanged();searchStatus(q,requestedPage);
     }});
    }
   }catch(Exception e){err=error(e);}
   final int valid=accepted,inspected=checked;final String failure=err;
   handler.post(new Runnable(){public void run(){if(destroyed||gen!=generation)return;remaining--;
    if(failure!=null){failures++;diagnostics.add(source.name()+": ERROR — "+failure);}
    else diagnostics.add(source.name()+": "+valid+" vídeos accesibles / "+inspected+" candidatos comprobados (hasta 12 por página). Los demás se omiten.");
    searchStatus(q,requestedPage);
   }});
  }}));
 }
 private void searchStatus(String q,int p){status.setText("«"+q+"» · Página "+p+" · "+results.size()+" vídeos accesibles · "+remaining+" fuentes pendientes · "+failures+" fallidas\n"+(remaining>0?"Comprobando enlaces; los resultados aparecen al validarse…":"Latino primero según el título; audio y reproducción completa sin garantizar. Ver Estado de búsqueda."));}
 private void showSaved(String key){cancel();view=key;results.clear();results.addAll(store.items(key));adapter.notifyDataSetChanged();status.setText((key.equals("favorites")?"Favoritos":"Historial / Continuar")+" · "+results.size()+" vídeos. Elige uno para reproducir o continuar.");}
 private void details(final Item item){
  String description=Html.fromHtml(item.description).toString();if(description.length()>3500)description=description.substring(0,3500)+"…";
  String pos=store.position(item)>0?"\nGuardado en "+PlayerActivity.time(store.position(item)):"";
  AlertDialog d=new AlertDialog.Builder(this).setTitle(item.title).setMessage(item.source+"\nIdioma: "+(item.language.length()==0?"sin confirmar":item.language)+pos+"\n\n"+description)
   .setPositiveButton(store.position(item)>0?"Continuar / reproducir":"Reproducir",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){resolve(item);}})
   .setNeutralButton(store.favorite(item)?"Quitar favorito":"Guardar favorito",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){store.toggle(item);if(view.equals("favorites"))showSaved(view);}}).setNegativeButton("Volver",null).create();d.show();Ui.controller(d);
 }
 private Source sourceFor(Item item)throws Exception{
  if(item.source.equals("OK.ru · vídeo público"))return new OkSource();if(item.source.equals("Web · vídeos públicos"))return new WebSource();if(item.source.equals("Enlace público"))return new LinkSource();if(item.source.equals("Internet Archive"))return new ArchiveSource();
  if(item.source.equals("NASA · vídeos y documentales"))return new NasaSource();
  if(item.source.equals("RTVCPlay · español"))return new RtvcSource();
  if(item.source.startsWith("PeerTube · "))return new PeerTubeSource(item.url);if(item.source.startsWith("M3U · "))return new M3uSource(item.source.substring(6));throw new Exception("Fuente no reconocida");
 }
 private void resolve(final Item item){final ProgressDialog progress=new ProgressDialog(this);progress.setMessage("Buscando archivos MP4 / HLS públicos…");progress.setCancelable(true);progress.show();Ui.controller(progress);final boolean[] dismissed={false};progress.setOnCancelListener(new DialogInterface.OnCancelListener(){public void onCancel(DialogInterface d){dismissed[0]=true;}});
  jobs.add(workers.submit(new Runnable(){public void run(){List<Stream> streams=null;String err=null;try{streams=sourceFor(item).resolve(item);if(streams.isEmpty())err="No hay archivos MP4/HLS públicos compatibles en este elemento.";}catch(Exception e){err=error(e);}final List<Stream> options=streams;final String failure=err;handler.post(new Runnable(){public void run(){if(destroyed)return;progress.dismiss();if(dismissed[0])return;if(failure!=null){message("No se puede reproducir",failure);return;}String[] labels=new String[options.size()];for(int i=0;i<labels.length;i++)labels[i]=options.get(i).label;AlertDialog d=new AlertDialog.Builder(MainActivity.this).setTitle("Elige archivo / calidad").setItems(labels,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int index){start(item,options.get(index));}}).setNegativeButton("Volver",null).create();d.show();Ui.controller(d);}});}}));
 }
 private void start(Item item,Stream stream){if(stream.target!=null){resolve(stream.target);return;}cancel();remaining=0;status.setText(results.size()+" vídeos accesibles · Búsqueda detenida al reproducir. Puedes buscar de nuevo.");try{Intent i=new Intent(this,PlayerActivity.class);i.putExtra("item",item.json().toString());i.putExtra("stream",stream.url);i.putExtra("subtitle",stream.subtitle);startActivity(i);}catch(Exception e){message("Error",error(e));}}
 private void manageSources(){final String[] choices={"Añadir servidor PeerTube","Añadir lista M3U por URL","Explorar una lista M3U completa","Eliminar una fuente añadida","Borrar historial y posiciones","Ver fuentes actuales"};AlertDialog d=new AlertDialog.Builder(this).setTitle("Fuentes públicas").setItems(choices,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int index){
  if(index==0||index==1){final boolean peer=index==0;if(store.sources().size()>=8){message("Fuentes","El prototipo admite hasta 8 fuentes añadidas.");return;}Ui.keyboard(MainActivity.this,peer?"URL base del servidor PeerTube":"URL de la lista M3U","https://",new Ui.Input(){public void done(String s){try{Net.safe(s);store.addSource((peer?"P|":"M|")+s);rebuildSources();message("Fuente añadida","Ya participará en la siguiente búsqueda.");}catch(Exception e){message("URL no válida",error(e));}}});}
  else if(index==2)chooseM3u();else if(index==3)removeSource();else if(index==4){AlertDialog c=new AlertDialog.Builder(MainActivity.this).setMessage("¿Borrar historial y posiciones de reproducción?").setPositiveButton("Borrar",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){store.clearHistory();if(view.equals("history"))showSaved(view);}}).setNegativeButton("Cancelar",null).create();c.show();Ui.controller(c);}
  else message("Fuentes actuales",join(new ArrayList<String>(sources.keySet()))+lastCrash());
 }}).setNegativeButton("Volver",null).create();d.show();Ui.controller(d);}
 private void removeSource(){final List<String> a=store.sources();if(a.isEmpty()){message("Fuentes","No has añadido fuentes.");return;}AlertDialog d=new AlertDialog.Builder(this).setTitle("Eliminar fuente").setItems(a.toArray(new String[0]),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int i){store.removeSource(a.get(i));rebuildSources();}}).setNegativeButton("Volver",null).create();d.show();Ui.controller(d);}
 private void chooseM3u(){final List<Source> a=new ArrayList<Source>();for(Source s:sources.values())if(s instanceof M3uSource)a.add(s);if(a.isEmpty()){message("M3U","Añade primero una lista en Fuentes.");return;}String[] names=new String[a.size()];for(int i=0;i<a.size();i++)names[i]=a.get(i).name();AlertDialog d=new AlertDialog.Builder(this).setTitle("Explorar M3U (hasta 100 canales)").setItems(names,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int i){browse(a.get(i));}}).setNegativeButton("Volver",null).create();d.show();Ui.controller(d);}
 private void browse(final Source source){cancel();view="search";final int gen=generation;results.clear();adapter.notifyDataSetChanged();status.setText("Cargando lista…");jobs.add(workers.submit(new Runnable(){public void run(){List<Item> data=null;String fail=null;try{data=source.search("");}catch(Exception e){fail=error(e);}final List<Item> r=data;final String err=fail;handler.post(new Runnable(){public void run(){if(destroyed||gen!=generation)return;if(r!=null){results.addAll(r);adapter.notifyDataSetChanged();status.setText(r.size()+" canales (máximo 100 al explorar; busca por nombre para encontrar otros)");}else status.setText("No se pudo cargar: "+err);}});}}));}
 private void message(String title,String text){AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setMessage(text).setPositiveButton("Aceptar",null).create();d.show();Ui.controller(d);}
 private String lastCrash(){try{java.io.FileInputStream in=openFileInput("last-crash.txt");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[2048];int n;try{while((n=in.read(b))!=-1&&out.size()<20000)out.write(b,0,n);}finally{in.close();}return "\nÚltimo cierre registrado:\n"+new String(out.toByteArray(),"UTF-8");}catch(Exception e){return "";}}
 private static String join(List<String> a){StringBuilder s=new StringBuilder();for(String x:a)s.append(x).append("\n\n");return s.length()==0?"Todavía no se ha ejecutado una búsqueda.":s.toString();}
 private static String error(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
 public boolean dispatchKeyEvent(KeyEvent e){int c=e.getKeyCode();if(c==KeyEvent.KEYCODE_BUTTON_A)return super.dispatchKeyEvent(new KeyEvent(e.getDownTime(),e.getEventTime(),e.getAction(),KeyEvent.KEYCODE_DPAD_CENTER,0));if(c==KeyEvent.KEYCODE_BUTTON_B){if(e.getAction()==KeyEvent.ACTION_UP)onBackPressed();return true;}return super.dispatchKeyEvent(e);}
 protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("query",query);b.putString("view",view);}
 protected void onResume(){super.onResume();if(store!=null&&!view.equals("search"))showSaved(view);}
 protected void onDestroy(){destroyed=true;cancel();workers.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
