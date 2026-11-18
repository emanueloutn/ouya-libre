package org.ouyalibre.tv;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.*;

/** Streaming catalog parser, independent of Android and of movie search. */
public final class IptvCatalog {
 public static final int MAX_CHANNELS=10000,MAX_BYTES=8*1024*1024;
 public static final String UNGROUPED="Sin categoría";
 public static final class Channel {
  public final String title,group,url,logo;private String cachedKey;
  Channel(String t,String g,String u,String l){title=t;group=g.length()==0?UNGROUPED:g;url=u;logo=l;}
  public String key(){if(cachedKey!=null)return cachedKey;try{byte[] b=java.security.MessageDigest.getInstance("SHA-256").digest((url+"\n"+title+"\n"+group).getBytes("UTF-8"));char[] hex="0123456789abcdef".toCharArray(),out=new char[b.length*2];for(int i=0;i<b.length;i++){out[i*2]=hex[(b[i]&255)>>>4];out[i*2+1]=hex[b[i]&15];}cachedKey=new String(out);return cachedKey;}catch(Exception e){throw new IllegalStateException(e);}}
 }
 public final List<Channel> channels=new ArrayList<Channel>();
 public int skipped;
 private static final Pattern ATTR=Pattern.compile("([\\w-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s,]+))");
 private static String limited(String s,int max){return s.length()>max?s.substring(0,max):s;}
 public static String http(String s)throws Exception{for(int i=0;i<s.length();i++)if(s.charAt(i)<=32||s.charAt(i)=='<'||s.charAt(i)=='>'||s.charAt(i)=='|')throw new IOException("Dirección no compatible");URL u=new URL(s);if(u.getHost().length()==0)throw new IOException("Dirección sin servidor");if(!u.getProtocol().equals("http")&&!u.getProtocol().equals("https"))throw new IOException("Solo señales HTTP/HTTPS compatibles");if(u.getUserInfo()!=null)throw new IOException("No se admite usuario:contraseña en la dirección");return u.toExternalForm();}
 private static int comma(String line){char quote=0;for(int i=0;i<line.length();i++){char c=line.charAt(i);if((c=='\"'||c=='\'')&&(quote==0||quote==c))quote=quote==0?c:0;if(c==','&&quote==0)return i;}return -1;}
 public static IptvCatalog parse(Reader input,String base)throws Exception{
  IptvCatalog out=new IptvCatalog();BufferedReader r=new BufferedReader(input);String title="",group="",logo="",fallback="",line;int chars=0;
  while((line=r.readLine())!=null){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Carga cancelada");chars+=line.length()+1;if(chars>MAX_BYTES||line.length()>65536)throw new IOException("Lista demasiado grande");line=line.trim();if(line.startsWith("\ufeff"))line=line.substring(1);
   if(line.startsWith("<"))throw new IOException("La respuesta es una página, no una lista M3U");
   if(line.startsWith("#EXT-X-"))throw new IOException("Este archivo es una señal HLS individual, no una lista de canales. Usa Abrir enlace público.");
   if(line.startsWith("#EXTINF:")){title="";group="";logo="";fallback="";int c=comma(line);if(c>=0)title=limited(line.substring(c+1).trim(),500);Matcher m=ATTR.matcher(c>=0?line.substring(0,c):line);while(m.find()){String v=m.group(2)!=null?m.group(2):m.group(3)!=null?m.group(3):m.group(4);String k=m.group(1).toLowerCase(Locale.US);if(k.equals("group-title"))group=limited(v.trim(),200);if(k.equals("tvg-name"))fallback=limited(v.trim(),500);if(k.equals("tvg-logo"))logo=limited(v,2000);}continue;}
   if(line.startsWith("#EXTGRP:")){group=limited(line.substring(8).trim(),200);continue;}
   if(line.length()==0||line.startsWith("#"))continue;
   try{String u=http(base==null||base.length()==0?line:new URL(new URL(base),line).toExternalForm());if(u.length()>8192)throw new IOException("URL demasiado larga");String t=title.length()>0?title:fallback.length()>0?fallback:"Canal "+(out.channels.size()+1);out.channels.add(new Channel(t,group,u,logo));}catch(Exception unsupported){out.skipped++;}
   title="";group="";logo="";fallback="";
   if(out.channels.size()>MAX_CHANNELS)throw new IOException("La lista supera los "+MAX_CHANNELS+" canales admitidos. No se ha importado parcialmente.");
  }
  if(out.channels.isEmpty())throw new IOException("No hay canales HTTP/HTTPS en esta lista");return out;
 }
 public List<String> groups(){TreeSet<String> s=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);for(Channel c:channels)s.add(c.group);return new ArrayList<String>(s);}
 public List<Channel> filter(String group,String query,Set<String> favorites,boolean onlyFavorites){List<Channel> out=new ArrayList<Channel>();String q=query.toLowerCase(Locale.US);for(Channel c:channels)if((group==null||c.group.equalsIgnoreCase(group))&&(c.title+" "+c.group).toLowerCase(Locale.US).contains(q)&&(!onlyFavorites||favorites.contains(c.key())))out.add(c);return out;}
}
