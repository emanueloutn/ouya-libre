package org.ouyalibre.tv;
import android.content.Context;import java.io.*;import java.util.*;import org.json.*;
/** Exact application-owned labels only; never translates provider titles or user text. */
public final class Texts {
 private static volatile boolean en=false;private static final Map<String,String> english=new HashMap<String,String>();
 public static void init(Context c){en="en".equals(c.getSharedPreferences("settings",0).getString("language","es"));try{InputStream in=c.getAssets().open("en.json");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;try{while((n=in.read(b))!=-1)out.write(b,0,n);}finally{in.close();}JSONObject data=new JSONObject(new String(out.toByteArray(),"UTF-8"));Iterator<String> keys=data.keys();while(keys.hasNext()){String key=keys.next();english.put(key,data.getString(key));}}catch(Exception e){android.util.Log.e("OuyaLibre","Language catalog could not be loaded",e);}}
 public static boolean english(){return en;}
 public static String get(String s){if(!en)return s;String value=english.get(s);return value==null?s:value;}
 public static void setLanguage(Context c,String language){if(!c.getSharedPreferences("settings",0).edit().putString("language",language).commit())throw new IllegalStateException("Could not save language");en="en".equals(language);}
 private Texts(){}
}
