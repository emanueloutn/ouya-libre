package org.ouyalibre.tv;
import android.content.*;
import org.json.*;
import java.util.*;

public final class Store {
 private final SharedPreferences p;
 public Store(Context c){p=c.getSharedPreferences("library",0);}
 private JSONArray array(String key){try{return new JSONArray(p.getString(key,"[]"));}catch(Exception e){return new JSONArray();}}
 public List<Item> items(String key){List<Item> out=new ArrayList<Item>();JSONArray a=array(key);for(int i=0;i<a.length();i++)try{out.add(Item.from(a.getJSONObject(i)));}catch(Exception e){}return out;}
 public boolean favorite(Item item){for(Item i:items("favorites"))if(i.key().equals(item.key()))return true;return false;}
 public void toggle(Item item){boolean had=favorite(item);saveItem("favorites",item,!had);}
 private void saveItem(String key,Item item,boolean add){JSONArray a=new JSONArray();try{if(add)a.put(item.json());for(Item i:items(key))if(!i.key().equals(item.key())&&a.length()<150)a.put(i.json());p.edit().putString(key,a.toString()).apply();}catch(Exception e){}}
 public void remember(Item item){saveItem("history",item,true);}
 public int position(Item item){return p.getInt("pos:"+item.key(),0);}
 public void position(Item item,int pos){p.edit().putInt("pos:"+item.key(),Math.max(0,pos)).apply();}
 public List<String> sources(){List<String> out=new ArrayList<String>();JSONArray a=array("sources");for(int i=0;i<a.length();i++)out.add(a.optString(i));return out;}
 public void addSource(String s){List<String> old=sources();if(old.contains(s))return;old.add(s);JSONArray a=new JSONArray();for(String x:old)a.put(x);p.edit().putString("sources",a.toString()).apply();}
 public void removeSource(String s){JSONArray a=new JSONArray();for(String x:sources())if(!x.equals(s))a.put(x);p.edit().putString("sources",a.toString()).apply();}
 public void clearHistory(){SharedPreferences.Editor e=p.edit().remove("history");for(String k:p.getAll().keySet())if(k.startsWith("pos:"))e.remove(k);e.apply();}
}
