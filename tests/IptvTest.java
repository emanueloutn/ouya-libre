package org.ouyalibre.tv;
import java.io.*;import java.util.*;
public class IptvTest {
 static int passed;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);passed++;System.out.println("PASS "+name);}
 static IptvCatalog parse(String s)throws Exception{return IptvCatalog.parse(new StringReader(s),"https://example.org/lists/list.m3u");}
 public static void main(String[] args)throws Exception{
  IptvCatalog a=parse("\ufeff#EXTM3U\n#EXTINF:-1 group-title=\"Cine, Latino\" tvg-logo=\"https://example.org/logo.png\",Canal Uno\n../one.m3u8\n#EXTINF:-1 tvg-name='Noticias',\n#EXTGRP:Información\nhttps://example.org/two.m3u8\nhttps://example.org/plain\n");
  check(a.channels.size()==3,"Import every channel, including bare URLs");
  check(a.channels.get(0).title.equals("Canal Uno")&&a.channels.get(0).group.equals("Cine, Latino"),"Quoted attribute commas do not break channel titles");
  check(a.channels.get(0).url.equals("https://example.org/one.m3u8"),"Resolve relative stream against playlist URL");
  check(a.channels.get(1).title.equals("Noticias")&&a.channels.get(1).group.equals("Información"),"tvg-name fallback and EXTGRP");
  check(a.channels.get(2).group.equals(IptvCatalog.UNGROUPED),"Missing metadata does not inherit previous group");
  check(a.groups().size()==3,"Distinct groups");
  check(a.filter("Cine, Latino","uno",Collections.<String>emptySet(),false).size()==1,"Group and search stay inside the selected catalog");
  IptvCatalog b=parse("#EXTM3U\n#EXTINF:-1 group-title=\"Otro\",Canal Dos\nhttps://example.org/two\n");
  check(b.channels.size()==1&&b.filter(null,"uno",Collections.<String>emptySet(),false).isEmpty(),"Second playlist does not mix channels from the first");
  Set<String> fav=new HashSet<String>();fav.add(a.channels.get(0).key());
  check(a.filter(null,"",fav,true).size()==1&&b.filter(null,"",fav,true).isEmpty(),"Favorites use stable per-channel keys and remain isolated");
  check(a.channels.get(0).key().length()==64&&a.channels.get(0).key().equals(a.channels.get(0).key()),"Stable bounded channel identity");
  IptvCatalog c=parse("#EXTM3U\n#EXTINF:-1,No\nudp://239.1.1.1:1234\n#EXTINF:-1,Yes\nhttps://example.org/a\n");check(c.channels.size()==1&&c.skipped==1,"Unsupported protocols reported rather than played");
  boolean denied=false;try{parse("#EXTM3U\n#EXT-X-TARGETDURATION:6\na.ts\n");}catch(IOException e){denied=true;}check(denied,"Individual HLS is not mistaken for a channel catalog");
  denied=false;try{parse("#EXTM3U\nhttps://user:password@example.org/a\n");}catch(IOException e){denied=true;}check(denied,"Embedded basic-auth credentials rejected");
  StringBuilder large=new StringBuilder("#EXTM3U\n");for(int i=0;i<500;i++)large.append("#EXTINF:-1 group-title=\"Grupo\",Canal ").append(i).append("\nhttps://example.org/").append(i).append("\n");check(parse(large.toString()).channels.size()==500,"Lists are not truncated to the previous 100-channel limit");
  denied=false;try{parse("<html>Login</html>");}catch(IOException e){denied=true;}check(denied,"HTML login page cannot overwrite a catalog");
  check(a.filter("cine, latino","",Collections.<String>emptySet(),false).size()==1,"Group case variants remain accessible");
  System.out.println("TOTAL "+passed);
 }
}
