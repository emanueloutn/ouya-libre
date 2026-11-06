package org.ouyalibre.tv;
import java.util.*;
import java.net.URL;

public final class M3uSource implements Source {
 private final String url;
 public M3uSource(String u)throws Exception{Net.safe(u);url=u;}
 public String name(){return "M3U · "+url;}
 public List<Item> search(String query)throws Exception{return parse(Net.text(url),url,name(),query);}
 public static List<Item> parse(String text,String base,String source,String query)throws Exception{
  List<Item> out=new ArrayList<Item>();String title="",group="";boolean hls=text.contains("#EXT-X-");if(hls)throw new Exception("Este enlace es una señal HLS individual; usa Abrir enlace");
  for(String raw:text.split("\n")){String l=raw.trim();if(l.startsWith("#EXTINF:")){int i=l.indexOf(',');title=i>=0?l.substring(i+1).trim():"Canal";java.util.regex.Matcher m=java.util.regex.Pattern.compile("group-title=\"([^\"]*)\"").matcher(l);group=m.find()?m.group(1):"";}
   else if(l.length()>0&&!l.startsWith("#")){String u=new URL(new URL(base),l).toString();String t=title.length()==0?u:title;String hay=(t+" "+group).toLowerCase(Locale.US);if(hay.contains(query.toLowerCase(Locale.US))){Net.safe(u);out.add(new Item(source,u,t,group,"",u));if(out.size()>=100)break;}title="";group="";}}
  return out;
 }
 public List<Stream> resolve(Item i){return Collections.singletonList(new Stream("Reproducir señal",i.url));}
}
