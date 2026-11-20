package org.ouyalibre.tv;
import android.text.Html;
import java.util.*;
import java.net.*;
import java.util.regex.*;

/** Public search pages only. Captchas/access challenges are reported, never bypassed. */
public final class WebSource implements PagedSource {
 private final String engine,scope;
 private String report="";
 public String report(){return report;}
 public WebSource(){this("Brave");}
 public WebSource(String engine){this(engine,"");}
 public WebSource(String engine,String scope){if(!scope.equals("")&&!scope.equals("drive.google.com"))throw new IllegalArgumentException("Ámbito no admitido");this.engine=engine;this.scope=scope;}
 public String name(){return "Web · "+engine;}
 public List<Item> search(String q)throws Exception{return search(q,1);}
 public List<Item> search(String q,int page)throws Exception{
  if(page<1||page>10)return Collections.emptyList();if(engine.equals("DuckDuckGo")&&page>1){report=Texts.get("No hay más páginas disponibles en esta fuente");return Collections.emptyList();}List<Item> merged=new ArrayList<Item>();Set<String> seen=new HashSet<String>();int queries=0;report="";
  for(String variant:SearchQuery.webVariants(q)){if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException("Búsqueda cancelada");try{for(Item item:searchOne(variant,page)){if(scope.length()>0&&!DriveSource.accepts(item.url))continue;if(seen.add(SearchQuery.identity(item)))merged.add(item);if(merged.size()>=40)break;}queries++;}catch(Exception e){if(merged.isEmpty())throw e;report=Texts.get("Consulta adicional no disponible: ")+e.getMessage();break;}if(merged.size()>=40)break;}
  report=queries+Texts.get(" consultas públicas")+(report.length()==0?"":" · "+report);return merged;
 }
 private List<Item> searchOne(String q,int page)throws Exception{
  String query=q+(scope.length()==0?"":" site:"+scope);List<Item> out=new ArrayList<Item>();Set<String> seen=new HashSet<String>();
  if(engine.equals("Google")){String html=Net.text("https://www.google.com/search?q="+Net.enc(query)+"&hl=es&start="+((page-1)*10));return parseGoogle(html,q);}
  if(engine.equals("Brave")){String html=Net.text("https://search.brave.com/search?q="+Net.enc(query)+"&source=web"+(page==1?"":"&offset="+(page-1)));return parseBrave(html,q);}
  if(engine.equals("Bing")){String xml=Net.text("https://www.bing.com/search?format=rss&q="+Net.enc(query)+"&first="+((page-1)*10+1));Matcher m=Pattern.compile("<item>([\\s\\S]*?)</item>").matcher(xml);while(m.find()){String block=m.group(1);add(out,seen,tag(block,"link"),tag(block,"title"),tag(block,"description"),q);}if(!xml.contains("<rss")&&!xml.contains("<channel"))throw new Exception("El buscador no entregó resultados públicos reconocibles");return out;}
  if(page!=1)return out;String html=Net.text("https://html.duckduckgo.com/html/?q="+Net.enc(query));
  if(html.contains("anomaly.js")||html.contains("anomaly-modal")||html.contains("challenge-form"))throw new Exception("DuckDuckGo exige una comprobación de acceso");
  Matcher m=Pattern.compile("<a\\b[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>",Pattern.CASE_INSENSITIVE).matcher(html);
  while(m.find())add(out,seen,redirect(clean(m.group(1))),clean(m.group(2)),"Resultado web; fuente de vídeo comprobada antes de mostrar.",q);return out;
 }
 static List<Item> parseGoogle(String html,String q)throws Exception{
  if(html.contains("/sorry/")||html.contains("g-recaptcha")||html.contains("consent.google.com")||html.contains("enablejs"))throw new Exception("Google exige JavaScript, consentimiento o una comprobación de acceso");
  List<Item> out=new ArrayList<Item>();Set<String> seen=new HashSet<String>();WebSource source=new WebSource("Google");
  Matcher m=Pattern.compile("<a\\b[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>",Pattern.CASE_INSENSITIVE).matcher(html);
  while(m.find()){Matcher heading=Pattern.compile("<h3\\b[^>]*>([\\s\\S]*?)</h3>",Pattern.CASE_INSENSITIVE).matcher(m.group(2));String raw=clean(m.group(1));if(!heading.find())continue;if(raw.startsWith("/url?")){URL u=new URL("https://www.google.com"+raw);String target=null;for(String pair:u.getQuery().split("&"))if(pair.startsWith("q=")||pair.startsWith("url=")){target=URLDecoder.decode(pair.substring(pair.indexOf('=')+1),"UTF-8");break;}if(target==null)continue;raw=target;}source.add(out,seen,raw,clean(heading.group(1)),"Google: candidato público; se comprobará el vídeo antes de mostrarlo.",q);}
  if(out.isEmpty()&&!html.contains("<h3")&&!html.contains("did not match any documents"))throw new Exception("Google no entregó resultados públicos reconocibles");return out;
 }
 static List<Item> parseBrave(String html,String q)throws Exception{
  if(html.contains("id=\"challenge-form\"")||html.contains("captcha-container")||html.contains("<title>PoW"))throw new Exception("El buscador exige una comprobación de acceso");
  List<Item> out=new ArrayList<Item>();Set<String> seen=new HashSet<String>();
  Matcher links=Pattern.compile("<a\\b[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>",Pattern.CASE_INSENSITIVE).matcher(html);
  WebSource source=new WebSource("Brave");while(links.find()){
   Matcher title=Pattern.compile("<div[^>]*class=\"[^\"]*search-snippet-title[^\"]*\"[^>]*>([\\s\\S]*?)</div>").matcher(links.group(2));
   if(title.find())source.add(out,seen,clean(links.group(1)),clean(title.group(1)),"Encontrado en Brave Search. Vídeo comprobado sin sesión; idioma según el título.",q);
  }
  if(out.isEmpty()&&!html.contains("search-snippet"))throw new Exception("El buscador no entregó resultados públicos reconocibles");return out;
 }
 private static String tag(String xml,String tag){Matcher m=Pattern.compile("<"+tag+">([\\s\\S]*?)</"+tag+">").matcher(xml);return m.find()?clean(m.group(1)):"";}
 private static String clean(String s){return Html.fromHtml(s).toString().trim();}
 static String redirect(String raw)throws Exception{
  if(raw.startsWith("//"))raw="https:"+raw;URL u=Net.safe(raw);
  if(u.getHost().equals("duckduckgo.com")&&u.getQuery()!=null)for(String p:u.getQuery().split("&"))if(p.startsWith("uddg="))return URLDecoder.decode(p.substring(5),"UTF-8");return raw;
 }
 private void add(List<Item> out,Set<String> seen,String url,String title,String desc,String q){try{
  if(out.size()>=20||!SearchQuery.matches(title,q))return;URL u=Net.safe(url);String h=u.getHost().toLowerCase(Locale.US);
  if((h.contains("google.")&&!DriveSource.accepts(url))||h.equals("bing.com")||h.endsWith(".bing.com")||h.equals("duckduckgo.com")||h.equals("search.brave.com"))return;
  for(String paid:new String[]{"netflix.com","disneyplus.com","primevideo.com","apple.com","max.com","hbo.com","paramountplus.com","clarovideo.com"})if(h.equals(paid)||h.endsWith("."+paid))return;
  if(DriveSource.accepts(url))url=DriveSource.canonical(url);
  if(OkSource.accepts(url)){url="https://ok.ru/video/"+u.getPath().replaceAll("[^0-9]","");}
  Item item=new Item(name(),url,title,desc,OkSource.language(title),url);if(item.related()||!seen.add(url))return;out.add(item);
 }catch(Exception ignored){}}
 public List<Stream> resolve(Item item)throws Exception{return new LinkSource().resolve(item);}
}
