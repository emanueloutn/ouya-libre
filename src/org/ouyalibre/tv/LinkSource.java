package org.ouyalibre.tv;
import java.util.*;
import java.net.URL;
import java.util.regex.*;

public final class LinkSource implements Source {
 public String name(){return "Enlace público";}
 public List<Item> search(String q){return Collections.emptyList();}
 public List<Stream> resolve(Item i)throws Exception{return resolve(i,0,new HashSet<String>());}
 private List<Stream> resolve(Item i,int depth,Set<String> visited)throws Exception{
  if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException("Resolución cancelada");
  if(!visited.add(i.url)||depth>2)throw new Exception("El vídeo embebido no contiene una fuente directa compatible");
  if(OkSource.accepts(i.url))return new OkSource().resolve(i);
  String raw=i.url;Net.safe(raw);String path=new URL(raw).getPath().toLowerCase(Locale.US);
  if(path.endsWith(".mp4")||path.endsWith(".m4v")||path.endsWith(".m3u8")||path.endsWith(".3gp"))return Collections.singletonList(new Stream("Reproducción directa",raw));
  java.net.HttpURLConnection check=Net.open(raw,null);String type=check.getContentType();check.disconnect();if(type!=null&&(type.startsWith("video/")||type.toLowerCase(Locale.US).contains("mpegurl")))return Collections.singletonList(new Stream("Vídeo directo · "+type,raw));
  String html=Net.text(raw).replace("\\/","/").replace("&amp;","&");List<Stream> out=new ArrayList<Stream>();Set<String> seen=new HashSet<String>();
  Pattern p=Pattern.compile("[\"']([^\"'<>\\s]+\\.(?:mp4|m3u8|m4v)(?:\\?[^\"'<>\\s]*)?)[\"']",Pattern.CASE_INSENSITIVE);Matcher m=p.matcher(html);
  while(m.find()&&out.size()<30){String u=new URL(new URL(raw),m.group(1)).toString();Net.safe(u);if(seen.add(u))out.add(new Stream("Vídeo público "+out.size()+" · "+new URL(u).getHost(),u));}
  if(out.isEmpty()&&depth<2){Matcher frames=Pattern.compile("<iframe\\b[^>]*\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']",Pattern.CASE_INSENSITIVE).matcher(html);int checked=0;
   while(frames.find()&&checked++<3){String url=new URL(new URL(raw),frames.group(1)).toString();try{out.addAll(resolve(new Item(name(),url,i.title,i.description,i.language,url),depth+1,visited));}catch(Exception ignored){}if(!out.isEmpty())break;}
  }
  if(out.isEmpty())throw new Exception("No hay un vídeo público directo o embebido compatible. No se ejecuta JavaScript ni se inicia sesión.");return out;
 }
}
