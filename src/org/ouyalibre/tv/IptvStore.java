package org.ouyalibre.tv;

import android.content.*;
import android.net.Uri;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.*;
import org.json.*;

/** Private per-playlist cache. Failed refreshes leave the previous catalog intact. */
public final class IptvStore {
 public static final class Entry {
  public String id,name,origin,base;public long updated;public int count;
  Entry(String i,String n,String o){id=i;name=n;origin=o;base=o;}
  JSONObject json()throws Exception{return new JSONObject().put("id",id).put("name",name).put("origin",origin).put("base",base).put("updated",updated).put("count",count);}
  public boolean remote(){return origin.startsWith("http://")||origin.startsWith("https://");}
 }
 private final Context context;private final SharedPreferences prefs;private final File dir;
 public IptvStore(Context c){context=c.getApplicationContext();prefs=context.getSharedPreferences("iptv",0);dir=context.getDir("iptv",0);for(Entry e:entries()){File target=new File(dir,e.id+".m3u"),backup=new File(dir,e.id+".bak");if(backup.exists()){if(!target.exists())backup.renameTo(target);else backup.delete();}}}
 public synchronized List<Entry> entries(){List<Entry> out=new ArrayList<Entry>();try{JSONArray a=new JSONArray(prefs.getString("lists","[]"));for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);String id=j.getString("id");if(!id.matches("[a-f0-9-]{36}"))continue;Entry e=new Entry(id,j.getString("name"),j.getString("origin"));e.base=j.optString("base",e.origin);e.updated=j.optLong("updated");e.count=j.optInt("count");out.add(e);}}catch(Exception ignored){}return out;}
 private void write(List<Entry> a)throws Exception{JSONArray j=new JSONArray();for(Entry e:a)j.put(e.json());if(!prefs.edit().putString("lists",j.toString()).commit())throw new IOException("No se pudieron guardar las listas");}
 public synchronized Entry add(String name,String origin)throws Exception{List<Entry> a=entries();if(a.size()>=50)throw new IOException("Máximo 50 listas");Entry e=new Entry(UUID.randomUUID().toString(),clean(name),origin);a.add(e);write(a);return e;}
 private String clean(String s)throws Exception{s=s.trim();if(s.length()==0||s.length()>80)throw new IOException("El nombre debe tener entre 1 y 80 caracteres");return s;}
 public synchronized void rename(Entry e,String name)throws Exception{List<Entry> a=entries();for(Entry x:a)if(x.id.equals(e.id)){x.name=clean(name);e.name=x.name;write(a);return;}throw new IOException("La lista ya no existe");}
 public synchronized void remove(Entry e)throws Exception{List<Entry> a=entries();for(Iterator<Entry> i=a.iterator();i.hasNext();)if(i.next().id.equals(e.id))i.remove();write(a);prefs.edit().remove("fav:"+e.id).remove("hidden:"+e.id).commit();new File(dir,e.id+".m3u").delete();}
 public boolean cached(Entry e){return new File(dir,e.id+".m3u").isFile();}
 public IptvCatalog cachedCatalog(Entry e)throws Exception{FileInputStream in=new FileInputStream(new File(dir,e.id+".m3u"));try{return IptvCatalog.parse(new InputStreamReader(in,"UTF-8"),e.remote()?e.base:null);}finally{in.close();}}
 public IptvCatalog refresh(Entry e,Uri replacement)throws Exception{
  HttpURLConnection connection=null;InputStream in=null;File temp=new File(dir,e.id+".tmp");String origin=replacement==null?e.origin:replacement.toString();boolean remote=origin.startsWith("http://")||origin.startsWith("https://");
  try{if(remote){connection=Net.open(origin,null);in=connection.getInputStream();}else in=context.getContentResolver().openInputStream(Uri.parse(origin));if(in==null)throw new IOException("No se puede leer el archivo");FileOutputStream out=new FileOutputStream(temp);try{byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Carga cancelada");total+=n;if(total>IptvCatalog.MAX_BYTES)throw new IOException("Máximo 8 MiB por lista");out.write(b,0,n);}out.getFD().sync();}finally{out.close();}
   String base=remote?(connection==null?origin:connection.getURL().toString()):null;FileInputStream check=new FileInputStream(temp);IptvCatalog catalog;try{catalog=IptvCatalog.parse(new InputStreamReader(check,"UTF-8"),base);}finally{check.close();}
   synchronized(this){List<Entry> all=entries();Entry found=null;for(Entry x:all)if(x.id.equals(e.id))found=x;if(found==null)throw new IOException("La lista fue eliminada");if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Carga cancelada");File target=new File(dir,e.id+".m3u"),backup=new File(dir,e.id+".bak");boolean had=target.exists();if(had&&!target.renameTo(backup))throw new IOException("No se pudo proteger la copia anterior");if(!temp.renameTo(target)){if(had)backup.renameTo(target);throw new IOException("No se pudo guardar la copia local");}found.origin=origin;found.base=base;found.count=catalog.channels.size();found.updated=System.currentTimeMillis();try{write(all);}catch(Exception failed){target.delete();if(had)backup.renameTo(target);throw failed;}backup.delete();e.origin=origin;e.base=base;e.count=found.count;e.updated=found.updated;}
   return catalog;
  }finally{if(in!=null)try{in.close();}catch(Exception ignored){}if(connection!=null)connection.disconnect();temp.delete();}
 }
 public synchronized Set<String> favorites(Entry e){Set<String> out=new HashSet<String>();try{JSONArray a=new JSONArray(prefs.getString("fav:"+e.id,"[]"));for(int i=0;i<a.length();i++)out.add(a.getString(i));}catch(Exception ignored){}return out;}
 public synchronized void toggle(Entry e,IptvCatalog.Channel c)throws Exception{Set<String> f=favorites(e);String key=c.key();if(!f.remove(key))f.add(key);JSONArray a=new JSONArray();for(String s:f)a.put(s);if(!prefs.edit().putString("fav:"+e.id,a.toString()).commit())throw new IOException("No se pudo guardar el favorito");}
 public synchronized Set<String> hidden(Entry e){Set<String> out=new HashSet<String>();try{JSONArray a=new JSONArray(prefs.getString("hidden:"+e.id,"[]"));for(int i=0;i<a.length();i++)out.add(a.getString(i));}catch(Exception ignored){}return out;}
 public synchronized void setHidden(Entry e,String key,boolean hide)throws Exception{Set<String> values=hidden(e);if(hide)values.add(key);else values.remove(key);saveHidden(e,values);}
 public synchronized void restoreAll(Entry e)throws Exception{saveHidden(e,new HashSet<String>());}
 private void saveHidden(Entry e,Set<String> values)throws Exception{JSONArray a=new JSONArray();for(String key:values)a.put(key);if(!prefs.edit().putString("hidden:"+e.id,a.toString()).commit())throw new IOException("No se pudo guardar la marca local");}
 public synchronized void migrate(Store legacy){if(prefs.getBoolean("migrated",false))return;try{for(String s:legacy.sources())if(s.startsWith("M|")){boolean exists=false;for(Entry e:entries())if(e.origin.equals(s.substring(2)))exists=true;if(!exists)add("Lista importada "+(entries().size()+1),s.substring(2));}prefs.edit().putBoolean("migrated",true).commit();}catch(Exception ignored){}}
}
