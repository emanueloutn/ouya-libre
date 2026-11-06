package org.ouyalibre.tv;
import java.util.*;
import org.json.*;

public final class ArchiveSource implements PagedSource {
 public String name(){return "Internet Archive";}
 public List<Item> search(String query)throws Exception{return search(query,1);}
 public List<Item> search(String query,int page)throws Exception{
  // Literal terms prevent user input from injecting search operators.
  String clean=query.replaceAll("[^\\p{L}\\p{N} \\-]"," ").trim();
  StringBuilder words=new StringBuilder();for(String term:clean.split(" +")){if(term.length()==0)continue;if(words.length()>0)words.append(" AND ");words.append("\"").append(term).append("\"");}
  String q="mediatype:movies AND ("+words+") AND -access-restricted-item:true";
  String url="https://archive.org/advancedsearch.php?q="+Net.enc(q)+"&fl[]=identifier&fl[]=title&fl[]=description&fl[]=language&rows=40&page="+Math.max(1,page)+"&output=json";
  JSONArray a=new JSONObject(Net.text(url)).getJSONObject("response").getJSONArray("docs");List<Item> out=new ArrayList<Item>();
  for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);String id=j.getString("identifier");out.add(new Item(name(),id,j.optString("title",id),j.optString("description"),j.optString("language"),"https://archive.org/details/"+Net.enc(id)));}return out;
 }
 public List<Stream> resolve(Item item)throws Exception{
  JSONObject j=new JSONObject(Net.text("https://archive.org/metadata/"+Net.enc(item.id)));
  if(j.optBoolean("is_dark")||j.optJSONObject("metadata")==null)throw new Exception("Este elemento no está disponible públicamente");
  JSONObject md=j.getJSONObject("metadata");if(md.optString("access-restricted-item").equals("true"))throw new Exception("Este elemento requiere acceso restringido");
  JSONArray a=j.optJSONArray("files");List<Stream> out=new ArrayList<Stream>();String sub="";if(a==null)return out;
  for(int i=0;i<a.length();i++){JSONObject f=a.getJSONObject(i);String n=f.optString("name");if(n.toLowerCase(Locale.US).endsWith(".srt")&&!f.optBoolean("private")){String u=file(item.id,n);if(sub.length()==0||n.toLowerCase(Locale.US).matches(".*(spanish|espanol|español|[._-]es[._-]).*"))sub=u;}}
  for(int i=0;i<a.length()&&out.size()<40;i++){JSONObject f=a.getJSONObject(i);String n=f.optString("name"),l=n.toLowerCase(Locale.US);if(f.optBoolean("private"))continue;
   if(l.endsWith(".mp4")||l.endsWith(".m3u8")||l.endsWith(".m4v")){int h=f.optInt("height",0);if(h>1080)continue;out.add(new Stream(n+(h>0?" · "+h+"p":""),file(item.id,n),sub));}
  }return out;
 }
 private String file(String id,String n)throws Exception{return "https://archive.org/download/"+Net.enc(id)+"/"+Net.enc(n).replace("+","%20").replace("%2F","/");}
}
