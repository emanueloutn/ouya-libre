package org.ouyalibre.tv;
import java.util.*;
import org.json.*;

/** Uses only the public anonymous API and the page's own published playback metadata. */
public final class RtvcSource implements PagedSource {
 public String name(){return "RTVCPlay · español";}
 public List<Item> search(String query)throws Exception{return search(query,1);}
 public List<Item> search(String query,int page)throws Exception{
  JSONObject j=new JSONObject(Net.text("https://cms.rtvcplay.co/api/v1/search?key="+Net.enc(query)+"&page="+(Math.max(1,page)-1)+"&page_size=40&_format=json"));List<Item> out=new ArrayList<Item>();JSONArray widgets=j.optJSONArray("widgets");
  if(widgets!=null)for(int w=0;w<widgets.length();w++){JSONArray a=widgets.getJSONObject(w).optJSONArray("contents");if(a==null)continue;for(int i=0;i<a.length()&&out.size()<80;i++){JSONObject v=a.getJSONObject(i);if(v.optInt("chapters_videos",0)==0&&!v.optString("content_type").equals("video"))continue;String slug=v.optString("slug");if(!slug.startsWith("/"))continue;out.add(new Item(name(),slug,v.optString("title"),v.optString("category")+"\n"+v.optString("micro_synopsis"),"es", "https://www.rtvcplay.co"+slug));}}
  return out;
 }
 public List<Stream> resolve(Item item)throws Exception{
  JSONObject state=state(Net.text(item.url));JSONObject c=state.getJSONObject("content").getJSONObject("currentContent");if(c.length()==0)throw new Exception("Este contenido ya no está disponible");
  JSONArray geo=c.optJSONArray("geoblocking");if(geo!=null&&geo.length()>0){String country=state.optJSONObject("country")==null?"":state.getJSONObject("country").optString("country");boolean allowed=false;for(int i=0;i<geo.length();i++)if(geo.getJSONObject(i).optString("value").equalsIgnoreCase(country))allowed=true;if(!allowed)throw new Exception("La fuente limita este contenido por país. No está disponible en tu región.");}
  List<Stream> out=new ArrayList<Stream>();JSONObject video=c.optJSONObject("video");
  if(video!=null){String uri=video.optString("uri","");String asset=video.optString("assetid","");String provider=c.optString("streaming_provider");
   if((uri.length()==0||uri.equals("null"))&&provider.equals("amazon_backup")&&asset.matches("[A-Za-z0-9_-]+")){String template=c.optString("base_url_hls","");if(template.startsWith("https://"))uri=template.replace("[node:field_asset_id]",asset);}
   if(uri.startsWith("http")&&(uri.contains(".m3u8")||uri.contains(".mp4")))out.add(new Stream("Reproducir · "+c.optString("title")+" · "+video.optString("duration",c.optString("duration")),uri));
  }
  if(out.isEmpty()){JSONArray widgets=c.optJSONArray("widgets");if(widgets!=null)for(int w=0;w<widgets.length();w++){JSONObject widget=widgets.getJSONObject(w);if(!widget.optString("type").equals("seasonList"))continue;JSONArray seasons=widget.optJSONArray("contents");if(seasons==null)continue;for(int si=0;si<seasons.length();si++){JSONArray episodes=seasons.getJSONObject(si).optJSONArray("contents");if(episodes==null)continue;for(int ei=0;ei<episodes.length()&&out.size()<200;ei++){JSONObject e=episodes.getJSONObject(ei);String slug=e.optString("slug");if(!slug.startsWith("/"))continue;String label=e.optString("title")+" · "+e.optString("subtitle");Item child=new Item(name(),slug,item.title+" · "+label,e.optString("duration"),"es","https://www.rtvcplay.co"+slug);out.add(new Stream(label,child));}}}}
  if(out.isEmpty())throw new Exception("La fuente no publica una señal de vídeo directa para este contenido.");return out;
 }
 static JSONObject state(String html)throws Exception{
  String marker="window.__RTVCPLAY_STATE__";int start=html.indexOf(marker);if(start<0)throw new Exception("La página cambió de formato");start=html.indexOf('{',start+marker.length());int end=html.indexOf("</script>",start);if(start<0||end<0)throw new Exception("Metadatos públicos incompletos");String json=html.substring(start,end).trim();if(json.endsWith(";"))json=json.substring(0,json.length()-1);return new JSONObject(json);
 }
}
