package org.ouyalibre.tv;

import android.text.Html;
import org.json.*;
import java.util.*;
import java.util.regex.*;

/** Only metadata and URLs published to anonymous visitors. No player scripts executed. */
public final class OkSource implements PagedSource {
 public String name(){return "OK.ru · vídeo público";}
 public static boolean accepts(String raw){try{java.net.URL u=Net.safe(raw);String h=u.getHost().toLowerCase(Locale.US);return (h.equals("ok.ru")||h.equals("www.ok.ru")||h.equals("m.ok.ru"))&&u.getPath().matches("/video(?:embed)?/[0-9]+/?");}catch(Exception e){return false;}}
 public List<Item> search(String q)throws Exception{return search(q,1);}
 public List<Item> search(String q,int page)throws Exception{
  if(page<1||page>3)return Collections.emptyList();
  final String query=q;List<Item> all=payload("https://ok.ru/video/search?st.cmd=anonymVideo&st.ft=search&st.m=SEARCH&st.gsq="+Net.enc(q),new Parser<List<Item>>(){public List<Item> parse(String html)throws Exception{return parseSearch(html,query);}});
  int start=(page-1)*12;return start>=all.size()?Collections.<Item>emptyList():new ArrayList<Item>(all.subList(start,Math.min(start+12,all.size())));
 }
 static List<Item> parseSearch(String html,String query)throws Exception{
  List<Item> out=new ArrayList<Item>();Matcher m=Pattern.compile("data-props=\"([^\"]+)\"").matcher(html);boolean found=false;
  while(m.find()){
   String raw=Html.fromHtml(m.group(1)).toString();if(!raw.contains("\"searchQuery\"")||!raw.contains("\"videos\""))continue;
   JSONObject j=new JSONObject(raw);if(!normalize(j.optString("searchQuery")).equals(normalize(query)))continue;
   JSONArray rows=j.optJSONObject("videos")==null?null:j.getJSONObject("videos").optJSONArray("list");if(rows==null)continue;found=true;
   for(int n=0;n<rows.length()&&out.size()<30;n++){
    JSONObject card=rows.optJSONObject(n);if(card==null)continue;JSONObject movie=card.optJSONObject("movie");if(movie==null||movie.optBoolean("blocked"))continue;
    String title=movie.optString("title",card.optString("name")),id=movie.optString("id");String u="https://ok.ru/video/"+id;
    if(!accepts(u)||!matches(title,query))continue;
    String lang=language(title);long seconds=movie.optLong("duration")/1000;
    Item item=new Item("OK.ru · vídeo público",id,title,"Duración: "+(seconds/60)+" min. Idioma indicado por el título, sin comprobar el audio.",lang,u);
    if(!item.related())out.add(item);
   }
  }
  if(!found)throw new Exception("OK.ru no devolvió una búsqueda pública reconocible; puede requerir acceso o haber cambiado la página.");
  Collections.sort(out,new Comparator<Item>(){public int compare(Item a,Item b){return Boolean.valueOf(b.language.contains("latino")).compareTo(Boolean.valueOf(a.language.contains("latino")));}});return out;
 }
 static String normalize(String s){return java.text.Normalizer.normalize(s.toLowerCase(Locale.US),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();}
 static boolean matches(String title,String query){String t=" "+normalize(title)+" ";int words=0,hits=0;for(String w:normalize(query).split(" +")){if(w.length()<2||Arrays.asList("el","la","de","en","un","al","los","las","del","por","para","con","una","ver","online","pelicula","peliculas","completa","latino","espanol","gratis","audio","serie","series").contains(w))continue;words++;if(t.contains(" "+w+" "))hits++;}return words==0?t.contains(" "+normalize(query)+" "):hits==words;}
 static String language(String title){String t=normalize(title);return t.contains("latino")?"Español latino (según título)":t.contains("subtit")||t.contains("vose")?"Subtitulado (según título)":t.contains("espanol")||t.contains("castellano")?"Español (según título)":"";}
 public List<Stream> resolve(Item item)throws Exception{
  if(!accepts(item.url))throw new Exception("Enlace de OK.ru no reconocido");
  return payload(item.url,new Parser<List<Stream>>(){public List<Stream> parse(String html)throws Exception{return parseStreams(html);}});
 }
 interface Parser<T>{T parse(String html)throws Exception;}
 /** Stop after a complete, validated public JSON attribute; ignore the unrelated page tail. */
 static <T>T payload(String url,Parser<T> parser)throws Exception{
  java.net.HttpURLConnection c=Net.open(url,null);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
  try{java.io.InputStream in=c.getInputStream();byte[] b=new byte[8192];int n,next=32768;
   while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException("Lectura cancelada");if(out.size()+n>4*1024*1024)throw new java.io.IOException("Página demasiado grande");out.write(b,0,n);
    if(out.size()>=next){next=out.size()+16384;try{return parser.parse(out.toString("UTF-8"));}catch(Exception incomplete){}}
   }return parser.parse(out.toString("UTF-8"));
  }catch(java.io.EOFException cut){return parser.parse(out.toString("UTF-8"));}finally{c.disconnect();}
 }
 static List<Stream> parseStreams(String html)throws Exception{
  Matcher m=Pattern.compile("data-options=\"([^\"]+)\"").matcher(html);
  while(m.find()){
   String raw=Html.fromHtml(m.group(1)).toString();if(!raw.contains("\"flashvars\""))continue;
   JSONObject options=new JSONObject(raw),flash=options.optJSONObject("flashvars");if(flash==null)continue;
   Object data=flash.opt("metadata");JSONObject meta=data instanceof JSONObject?(JSONObject)data:data instanceof String?new JSONObject((String)data):null;if(meta==null)continue;
   JSONObject movie=meta.optJSONObject("movie");if(movie==null||!"OK".equals(movie.optString("status")))throw new Exception("El vídeo no está disponible públicamente sin acceso");
   List<Stream> out=new ArrayList<Stream>();JSONArray videos=meta.optJSONArray("videos");
   if(videos!=null)for(int n=0;n<videos.length();n++){
    JSONObject v=videos.getJSONObject(n);String quality=v.optString("name"),url=v.optString("url");
    if(v.optBoolean("disallowed")||quality.equals("quad")||quality.equals("ultra")||url.length()==0)continue;
    Net.safe(url);String label=quality.equals("mobile")?"144p":quality.equals("lowest")?"240p":quality.equals("low")?"360p":quality.equals("sd")?"480p":quality.equals("hd")?"720p":quality.equals("full")?"1080p":quality;
    out.add(new Stream("OK.ru · "+label,url));
   }
   String hls=meta.optString("hlsManifestUrl");if(out.isEmpty()&&hls.length()>0){Net.safe(hls);out.add(new Stream("OK.ru · HLS automático",hls));}
   if(!out.isEmpty())return out;
  }
  throw new Exception("No hay fuentes públicas de OK.ru; el vídeo puede estar retirado, restringido o requerir acceso.");
 }
}
