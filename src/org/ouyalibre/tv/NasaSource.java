package org.ouyalibre.tv;
import java.util.*;
import org.json.*;

/** NASA Image and Video Library: public search and asset API, no key or login. */
public final class NasaSource implements PagedSource {
 public String name(){return "NASA · vídeos y documentales";}
 public List<Item> search(String query)throws Exception{return search(query,1);}
 public List<Item> search(String query,int page)throws Exception{
  JSONArray a=new JSONObject(Net.text("https://images-api.nasa.gov/search?q="+Net.enc(query)+"&media_type=video&page_size=40&page="+Math.max(1,page))).getJSONObject("collection").getJSONArray("items");List<Item> out=new ArrayList<Item>();
  for(int i=0;i<a.length();i++){JSONObject record=a.getJSONObject(i);JSONArray data=record.optJSONArray("data");if(data==null||data.length()==0)continue;JSONObject d=data.getJSONObject(0);String id=d.optString("nasa_id");if(id.length()>0)out.add(new Item(name(),id,d.optString("title",id),d.optString("description"),d.optString("language"),"https://images.nasa.gov/details/"+Net.enc(id).replace("+","%20")));}return out;
 }
 public List<Stream> resolve(Item item)throws Exception{
  JSONArray a=new JSONObject(Net.text("https://images-api.nasa.gov/asset/"+Net.enc(item.id).replace("+","%20"))).getJSONObject("collection").getJSONArray("items");List<Stream> out=new ArrayList<Stream>();String subtitle="";
  for(int i=0;i<a.length();i++){String u=a.getJSONObject(i).optString("href");if(u.toLowerCase(Locale.US).endsWith(".srt"))subtitle=u;}
  for(int i=0;i<a.length();i++){String u=a.getJSONObject(i).optString("href");String l=u.toLowerCase(Locale.US);if(l.endsWith(".mp4")&&!l.contains("~orig.")){String label=u.substring(u.lastIndexOf('/')+1);out.add(new Stream(label+" · resolución según fuente",u.replace(" ","%20"),subtitle));}}
  return out;
 }
}
