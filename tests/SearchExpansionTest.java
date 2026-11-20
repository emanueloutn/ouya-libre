package org.ouyalibre.tv;
import java.io.*;import java.util.*;
/** Runs real network searches on API16; fixture checks are not catalog results. */
public final class SearchExpansionTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);}
 public static void main(String[] args)throws Exception{
 Net.init(new FileInputStream("/data/local/tmp/search-cacert.pem"));
 check(SearchQuery.title("Dexter latino").equals("dexter"),"language separated from title");
 check(SearchQuery.matches("Dexter Temporada 1", "Dexter latino"),"title accepted without redundant language tag");
 check(!SearchQuery.matches("Dexter review", "Dexterino latino"),"unrelated title rejected");
 check(SearchQuery.title("1984").equals("1984"),"numeric title preserved");
 check(SearchQuery.matches("N.N. - El graduado","n.n.")&&!SearchQuery.matches("Otra obra","n.n."),"punctuated title respected");
 Item a=new Item("PeerTube · example.org","a","a","","","https://example.org"),b=new Item("PeerTube · example.org","b","b","","","https://example.org");
 check(!SearchQuery.identity(a).equals(SearchQuery.identity(b)),"two videos from same server stay distinct");
 check(SearchQuery.webVariants("Dexter latino").size()==2,"bounded two-query expansion");
 local();
 check(!SearchQuery.acceptable(new Item("x","x","Dexter music","","en","https://example.org"),"Dexter latino"),"explicit Spanish query excludes unrelated-language fallback");
 if(args.length>0&&args[0].equals("--online")){
  String query=args.length>1?args[1]:"Dexter latino";
  for(Source s:new Source[]{new OkSource(),new WebSource("Bing"),new SepiaSource()}){
   if(args.length>2&&!s.name().contains(args[2]))continue;
   try{List<Item> items=s.search(query);System.out.println("REAL "+s.name()+" candidates="+items.size()+" query="+query);int checked=0,valid=0;
    for(Item item:items){if(!SearchQuery.acceptable(item,query))continue;if(checked++>=6)break;boolean ok=false;String error="";Net.beginProbeBudget();try{ok=Playable.item(s,item);}catch(Exception e){error=e.toString();}finally{Net.endProbeBudget();}if(ok)valid++;System.out.println("MEDIA "+ok+" "+item.title+" | "+item.url+" | "+error);}
    System.out.println("RESULT "+s.name()+" validated="+valid+" probed="+Math.min(checked,6));
   }catch(Exception e){System.out.println("SOURCE ERROR "+s.name()+" "+e);}
  }
 }
 System.out.println("DONE");System.exit(0);
 }
 static void local()throws Exception{
  final java.net.ServerSocket server=new java.net.ServerSocket(0,8,java.net.InetAddress.getByName("127.0.0.1"));
  Thread t=new Thread(new Runnable(){public void run(){while(!server.isClosed())try{java.net.Socket socket=server.accept();try{
   BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),"US-ASCII"));String request=in.readLine();if(request==null)continue;String path=request.split(" ")[1],line;while((line=in.readLine())!=null&&line.length()>0){}
   String body="<html>Access required</html>",type="text/html",status="200 OK",extra="";
   if(path.equals("/master")){body="#EXTM3U\n#EXT-X-STREAM-INF:RESOLUTION=3840x2160\n/missing\n#EXT-X-STREAM-INF:RESOLUTION=1280x720\n/missing\n#EXT-X-STREAM-INF:RESOLUTION=1920x1080\n/media\n";type="application/vnd.apple.mpegurl";}
   if(path.equals("/live")){body="#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:6,\n/missing\n#EXTINF:6,\n/media\n";type="application/vnd.apple.mpegurl";}
   if(path.equals("/redirect")){status="302 Found";extra="Location: /folder/list\r\n";}
   if(path.equals("/folder/list")){body="#EXTM3U\n#EXTINF:6,\nmedia\n#EXT-X-ENDLIST\n";type="application/vnd.apple.mpegurl";}
   byte[] bytes=body.getBytes("UTF-8");if(path.equals("/media")||path.equals("/folder/media")){bytes=new byte[376];bytes[0]=0x47;bytes[188]=0x47;type="video/mp2t";}
   if(path.equals("/missing"))status="404 Not Found";if(path.equals("/denied"))status="401 Unauthorized";
   OutputStream out=socket.getOutputStream();out.write(("HTTP/1.1 "+status+"\r\n"+extra+"Content-Type: "+type+"\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));out.write(bytes);out.flush();
  }finally{socket.close();}}catch(Exception ignored){}}});t.setDaemon(true);t.start();
  try{String base="http://127.0.0.1:"+server.getLocalPort();check(Playable.probe(base+"/master",0),"HLS tries another <=1080 variant after missing media");check(Playable.probe(base+"/live",0),"live HLS checks current segment");check(Playable.probe(base+"/redirect",0),"HLS relative media follows redirected manifest origin");check(!Playable.probe(base+"/html",0),"HTML page rejected as media");boolean denied=false;try{Playable.probe(base+"/denied",0);}catch(IOException expected){denied=true;}check(denied,"access requirement rejected");
   Net.beginProbeBudget();check(Net.boundedTimeout(20000)<=6000,"probe socket time bounded");Net.endProbeBudget();check(Net.boundedTimeout(20000)==20000,"probe budget does not affect normal playback requests");
  }finally{Net.endProbeBudget();server.close();}
 }

}
