package org.ouyalibre.tv;

import org.json.JSONObject;

public final class Item {
 public String source, id, title, description, language, url;public boolean verified;
 public Item(String s,String i,String t,String d,String l,String u){source=s;id=i;title=t.length()>500?t.substring(0,500):t;description=d.length()>4000?d.substring(0,4000):d;language=l;url=u;}
 public String key(){return source+":"+id;}
 public JSONObject json() throws Exception {JSONObject j=new JSONObject();j.put("source",source);j.put("id",id);j.put("title",title);j.put("description",description);j.put("language",language);j.put("url",url);return j;}
 public static Item from(JSONObject j){return new Item(j.optString("source"),j.optString("id"),j.optString("title"),j.optString("description"),j.optString("language"),j.optString("url"));}
 public boolean spanish(){String s=(language+" "+title).toLowerCase(java.util.Locale.US);return s.contains("spanish")||s.contains("español")||s.contains("latino")||s.contains("castellano")||language.equals("es")||language.equals("spa");}
 public boolean related(){String s=java.text.Normalizer.normalize(title.toLowerCase(java.util.Locale.US),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","");return s.contains("analisis")||s.contains("review")||s.contains("resena")||s.contains("trailer")||s.contains("explained")||s.contains("explicad")||s.contains("critica");}
 public String toString(){return title+(verified?"  [Vídeo accesible]":"")+(related()?"  [Reseña / tráiler / análisis]":"")+"\n"+source+"  ·  "+(language.length()==0?"Idioma sin confirmar":language);}
}
