package org.ouyalibre.tv;
import java.util.*;
import org.json.*;

public final class PeerTubeSource implements PagedSource {
 private final String base;
 public PeerTubeSource(String b)throws Exception{Net.safe(b);base=b.replaceAll("/+$","");}
 public String name(){return "PeerTube · "+base.replace("https://","").replace("http://","");}
 public List<Item> search(String query)throws Exception{return search(query,1);}
 public List<Item> search(String query,int page)throws Exception{
  JSONArray a=new JSONObject(Net.text(base+"/api/v1/search/videos?search="+Net.enc(query)+"&count=40&start="+((Math.max(1,page)-1)*40)+"&nsfw=false")).getJSONArray("data");List<Item> out=new ArrayList<Item>();
  for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);if(j.optBoolean("nsfw"))continue;JSONObject lang=j.optJSONObject("language");String l=lang==null?"":lang.optString("id","");out.add(new Item(name(),j.getString("uuid"),j.optString("name"),j.optString("description"),l,base));}return out;
 }
 public List<Stream> resolve(Item item)throws Exception{
  JSONObject j=new JSONObject(Net.text(base+"/api/v1/videos/"+Net.enc(item.id)));List<Stream> out=new ArrayList<Stream>();
  addFiles(j.optJSONArray("files"),out);JSONArray p=j.optJSONArray("streamingPlaylists");
  if(p!=null)for(int i=0;i<p.length();i++){JSONObject x=p.getJSONObject(i);addFiles(x.optJSONArray("files"),out);String u=x.optString("playlistUrl");if(u.startsWith("http"))out.add(new Stream("HLS · calidad automática",u));}
  return out;
 }
 private void addFiles(JSONArray a,List<Stream> out)throws Exception{if(a==null)return;for(int i=0;i<a.length();i++){JSONObject f=a.getJSONObject(i),r=f.optJSONObject("resolution");int h=r==null?0:r.optInt("id");String u=f.optString("fileUrl");String low=u.toLowerCase(Locale.US);if(h>1080||u.length()==0||(!low.contains(".mp4")&&!low.contains(".m4v")))continue;boolean dup=false;for(Stream s:out)if(s.url.equals(u))dup=true;if(!dup)out.add(new Stream((h>0?h+"p":"MP4")+" · "+f.optLong("size")/1048576+" MB",u));}}
}
