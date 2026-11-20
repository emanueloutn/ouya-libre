package org.ouyalibre.tv;

import java.util.*;

/** Query expansion is literal and bounded; it never invents titles or results. */
public final class SearchQuery {
 private static final Set<String> NOISE=new HashSet<String>(Arrays.asList("el","la","de","en","un","al","los","las","del","por","para","con","una","ver","online","pelicula","peliculas","completa","completo","latino","latina","espanol","espanola","gratis","audio","serie","series","subtitulado","subtitulada","subtitulos","castellano","hd","full","mp4","h264","x264","the","of","and"));
 private static final Set<String> CATEGORIES=new HashSet<String>(Arrays.asList("documental","documentales","anime","animacion","cine","pelicula","peliculas","serie","series","videos","video"));
 private SearchQuery(){}
 private static boolean year(String w){return w.matches("(?:19|20)[0-9]{2}");}
 private static boolean format(String w){return w.matches("[0-9]{3,4}p")||w.equals("1080")||w.equals("720")||w.equals("480");}
 public static List<String> terms(String query){
  List<String> words=new ArrayList<String>();for(String w:OkSource.normalize(query).split(" +"))if(w.length()>=2&&!NOISE.contains(w)&&!CATEGORIES.contains(w)&&!format(w))words.add(w);
  boolean hasTitle=false;for(String w:words)if(!year(w))hasTitle=true;
  if(hasTitle)for(Iterator<String> it=words.iterator();it.hasNext();)if(year(it.next()))it.remove();
  return words;
 }
 public static boolean matches(String title,String query){List<String> words=terms(query);String text=" "+OkSource.normalize(title)+" ";if(words.isEmpty()){for(String word:OkSource.normalize(query).split(" +"))if(!NOISE.contains(word)&&!CATEGORIES.contains(word)&&!format(word))return text.contains(" "+OkSource.normalize(query)+" ");return true;}for(String word:words)if(!text.contains(" "+word+" "))return false;return true;}
 public static String title(String query){List<String> words=terms(query);if(words.isEmpty())return query.trim();StringBuilder b=new StringBuilder();for(String word:words){if(b.length()>0)b.append(' ');b.append(word);}return b.toString();}
 public static boolean wantsSpanish(String query){String q=" "+OkSource.normalize(query)+" ";for(String w:new String[]{"latino","latina","espanol","espanola","castellano"})if(q.contains(" "+w+" "))return true;return false;}
 public static boolean acceptable(Item item,String query){return !item.related()&&matches(item.title,query)&&(!wantsSpanish(query)||item.spanish());}
 public static List<String> webVariants(String query){LinkedHashSet<String> variants=new LinkedHashSet<String>();String original=query.trim();variants.add(original);String title=title(query);if(!OkSource.normalize(original).equals(OkSource.normalize(title)))variants.add(wantsSpanish(query)?title+" español":title);else variants.add(title+" español latino");return new ArrayList<String>(variants);}
 public static List<String> catalogVariants(String query){LinkedHashSet<String> out=new LinkedHashSet<String>();out.add(query.trim());out.add(wantsSpanish(query)?title(query)+" español":title(query));return new ArrayList<String>(out);}
 public static String identity(Item item){if(item.source.startsWith("PeerTube · "))return item.url.replaceAll("/+$","")+"#video="+item.id;return item.url;}
}
