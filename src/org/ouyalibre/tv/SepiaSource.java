package org.ouyalibre.tv;

import java.net.URL;
import java.util.*;
import org.json.*;

/** Public PeerTube global index; media is resolved and checked at its origin. */
public final class SepiaSource implements PagedSource {
 public String name(){return "PeerTube · búsqueda global (SepiaSearch)";}
 public List<Item> search(String query)throws Exception{return search(query,1);}
 public List<Item> search(String query,int page)throws Exception{
  if(page<1||page>20)return Collections.emptyList();
  String url="https://sepiasearch.org/api/v1/search/videos?search="+Net.enc(SearchQuery.title(query))+"&count=40&start="+((page-1)*40)+"&nsfw=false"+(SearchQuery.wantsSpanish(query)?"&languageOneOf=es":"");
  return parse(Net.text(url));
 }
 static List<Item> parse(String body)throws Exception{
  JSONArray data=new JSONObject(body).getJSONArray("data");List<Item> out=new ArrayList<Item>();Set<String> seen=new HashSet<String>();
  for(int i=0;i<data.length()&&out.size()<40;i++){
   JSONObject video=data.getJSONObject(i),privacy=video.optJSONObject("privacy");if(video.optBoolean("nsfw")||video.optBoolean("requiresPassword")||(privacy!=null&&privacy.optInt("id",1)!=1))continue;
   String id=video.optString("uuid"),link=video.optString("url");if(!id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")||link.length()==0)continue;
   try{URL u=Net.safe(link);String base=new URL(u.getProtocol(),u.getHost(),u.getPort(),"").toString();JSONObject language=video.optJSONObject("language");String lang=language==null?"":language.optString("id","");if(lang.equals("null"))lang="";
    Item item=new Item("PeerTube · "+u.getHost(),id,video.optString("name"),video.optString("description")+"\n"+Texts.get("Encontrado en el índice global de PeerTube; se comprueba el servidor de origen."),lang,base);
    if(!item.related()&&seen.add(SearchQuery.identity(item)))out.add(item);
   }catch(Exception ignored){}
  }return out;
 }
 public List<Stream> resolve(Item item)throws Exception{return new PeerTubeSource(item.url).resolve(item);}
}
