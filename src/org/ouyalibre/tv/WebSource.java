package org.ouyalibre.tv;
import android.text.Html;
import java.util.*;
import java.net.*;
import java.util.regex.*;

/** Public search pages only. Captchas/access challenges are reported, never bypassed. */
public final class WebSource implements PagedSource {
 public String name(){return "Web · vídeos públicos";}
 public List<Item> search(String q)throws Exception{return search(q,1);}
 public List<Item> search(String q,int page)throws Exception{
  if(page<1||page>10)return Collections.emptyList();List<Item> out=new ArrayList<Item>();Set<String> seen=new HashSet<String>();String failure="";
  try{String html=Net.text("https://search.brave.com/search?q="+Net.enc(q+" latino")+"&source=web"+(page==1?"":"&offset="+(page-1)));out.addAll(parseBrave(html,q));for(Item i:out)seen.add(i.url);
  }catch(Exception e){failure="Brave: "+e.getMessage();}
  if(page==1&&out.size()<8)try{String xml=Net.text("https://www.bing.com/search?format=rss&q="+Net.enc(q+" latino video"));Matcher m=Pattern.compile("<item>([\\s\\S]*?)</item>").matcher(xml);
   while(m.find()){String block=m.group(1);add(out,seen,tag(block,"link"),tag(block,"title"),tag(block,"description"),q);}
  }catch(Exception e){failure+=(failure.length()>0?"; ":"")+"Bing: "+e.getMessage();}
  if(page==1&&out.size()<4)try{
   String html=Net.text("https://html.duckduckgo.com/html/?q="+Net.enc(q+" latino video"));
   if(html.contains("anomaly.js")||html.contains("anomaly-modal")||html.contains("challenge-form"))throw new Exception("DuckDuckGo exige una comprobación de acceso");
   Matcher m=Pattern.compile("<a\\b[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>",Pattern.CASE_INSENSITIVE).matcher(html);
   while(m.find())add(out,seen,redirect(clean(m.group(1))),clean(m.group(2)),"Resultado web; fuente de vídeo comprobada antes de mostrar.",q);
  }catch(Exception e){failure=(failure.length()>0?failure+"; ":"")+e.getMessage();}
  if(out.isEmpty()&&failure.length()>0)throw new Exception("Sin candidatos verificables. "+failure);
  return out;
 }
 static List<Item> parseBrave(String html,String q)throws Exception{
  if(html.contains("id=\"challenge-form\"")||html.contains("captcha-container")||html.contains("<title>PoW"))throw new Exception("El buscador exige una comprobación de acceso");
  List<Item> out=new ArrayList<Item>();Set<String> seen=new HashSet<String>();
  Matcher links=Pattern.compile("<a\\b[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>",Pattern.CASE_INSENSITIVE).matcher(html);
  WebSource source=new WebSource();while(links.find()){
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
  if(out.size()>=20||!OkSource.matches(title,q))return;URL u=Net.safe(url);String h=u.getHost().toLowerCase(Locale.US);
  if(h.contains("google.")||h.equals("bing.com")||h.endsWith(".bing.com")||h.equals("duckduckgo.com")||h.equals("search.brave.com"))return;
  for(String paid:new String[]{"netflix.com","disneyplus.com","primevideo.com","apple.com","max.com","hbo.com","paramountplus.com","clarovideo.com"})if(h.equals(paid)||h.endsWith("."+paid))return;
  if(OkSource.accepts(url)){url="https://ok.ru/video/"+u.getPath().replaceAll("[^0-9]","");}
  Item item=new Item(name(),url,title,desc,OkSource.language(title),url);if(item.related()||!seen.add(url))return;out.add(item);
 }catch(Exception ignored){}}
 public List<Stream> resolve(Item item)throws Exception{return new LinkSource().resolve(item);}
}
