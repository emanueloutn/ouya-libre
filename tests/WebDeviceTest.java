package org.ouyalibre.tv;
import java.io.*;import java.util.*;
/** Actual-device integration checks. Fixture HTML is outside the APK/catalog. */
public final class WebDeviceTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);System.out.println("PASS "+message);}
 static String file(String name)throws Exception{FileInputStream in=new FileInputStream("/data/local/tmp/"+name);ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];int n;try{while((n=in.read(x))>0)b.write(x,0,n);}finally{in.close();}return new String(b.toByteArray(),"UTF-8");}
 public static void main(String[] args)throws Exception{
  Net.init(new FileInputStream("/data/local/tmp/isrgrootx1.pem"));
  if(args.length>0&&args[0].equals("--accept")){java.lang.reflect.Field f=Net.class.getDeclaredField("trustManager");f.setAccessible(true);ModernHttps c=new ModernHttps(Net.safe(args[1]),(javax.net.ssl.X509TrustManager)f.get(null));c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","OuyaLibre/0.1 (public video search)");c.setRequestProperty("Accept","*/*");c.setRequestProperty("Accept-Encoding","identity");try{int status=c.getResponseCode();System.out.println("ACCEPT STATUS "+status);if(status==200){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=c.getInputStream().read(b))>0&&out.size()<4*1024*1024)out.write(b,0,n);System.out.println("WEB CANDIDATES "+WebSource.parseBrave(out.toString("UTF-8"),"el club de la pelea").size());}}finally{c.disconnect();}System.exit(0);}
  if(args.length>0&&args[0].equals("--fetch")){try{String text=Net.text(args[1]);System.out.println("FETCH "+text.length()+" bytes; web candidates "+WebSource.parseBrave(text,"el club de la pelea").size());}catch(Exception e){System.out.println("FETCH ERROR "+e);}System.exit(0);}
  List<Item> web=WebSource.parseBrave(file("brave-search.html"),"el club de la pelea");boolean nonOk=false,paid=false;
  for(Item i:web){if(i.url.contains("tokyvideo"))nonOk=true;if(i.url.contains("netflix.com")||i.url.contains("primevideo.com")||i.url.contains("apple.com"))paid=true;}
  check(nonOk,"web parses candidates from another host");check(!paid,"paid platforms excluded");check(WebSource.parseBrave(file("brave-search.html"),"zzzxxyy-no-such-title").isEmpty(),"unrelated results excluded");
  check(!OkSource.parseSearch(file("ok-search4.html"),"el club de la pelea").isEmpty(),"real anonymous OK search response parsed");
  boolean mismatch=false;try{OkSource.parseSearch(file("ok-search4.html"),"unrelated query");}catch(Exception expected){mismatch=true;}check(mismatch,"OK query echo must match");
  check(!OkSource.parseStreams(file("ok-example.html")).isEmpty(),"real OK metadata parsed");
  check(OkSource.accepts("https://ok.ru/videoembed/3785097939699"),"embedded OK URL accepted");check(!OkSource.accepts("https://ok.ru.evil.invalid/video/3785097939699"),"lookalike host rejected");
  check(OkSource.matches("N.N. - El graduado","n.n."),"short punctuated titles match");check(!OkSource.matches("Arthur Temporada 1 Español","el club de la pelea"),"metadata-only title matches excluded");
  local();
  if(args.length>0&&args[0].equals("--online")){
   List<Item> live=new OkSource().search("el club de la pelea");check(!live.isEmpty(),"live OK search returns actual results");
   Item original=new Item("OK.ru · vídeo público","3785097939699","User URL","","","https://ok.ru/video/3785097939699");
   check(Playable.item(new OkSource(),original),"user OK link returns media bytes anonymously");
   Item toky=new Item("Enlace público","toky","TokyVideo","","","https://www.tokyvideo.com/es/video/el-club-de-la-pelea-pelicula-en-latino");
   boolean removed=false;try{removed=!Playable.item(new LinkSource(),toky);}catch(Exception unavailable){removed=true;}check(removed,"removed second-host video must be hidden");
   List<Item> actual=new WebSource().search("el club de la pelea");check(!actual.isEmpty(),"live general web search returns candidates");
  }
 }
 static void local()throws Exception{
  final java.net.ServerSocket server=new java.net.ServerSocket(0,4,java.net.InetAddress.getByName("127.0.0.1"));
  Thread thread=new Thread(new Runnable(){public void run(){while(!server.isClosed())try{
   java.net.Socket socket=server.accept();try{BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),"US-ASCII"));String line=in.readLine();if(line==null)continue;String path=line.split(" ")[1];while((line=in.readLine())!=null&&line.length()>0){}
    String body=path.equals("/outer")?"<iframe src='/inner'></iframe>":path.equals("/inner")?"<video><source src='/film.mp4'></video>":"<html>Login required</html>";byte[] data=body.getBytes("UTF-8");String type="text/html",status="200 OK";
    if(path.equals("/film.mp4")){data=new byte[]{0,0,0,16,'f','t','y','p','i','s','o','m',0,0,0,0};type="video/mp4";}if(path.equals("/denied"))status="401 Unauthorized";
    OutputStream out=socket.getOutputStream();if(path.equals("/cut")||path.equals("/cutbad")){data=(path.equals("/cut")?"<complete>public metadata</complete>":"<complete>unfinished").getBytes("UTF-8");out.write(("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n"+Integer.toHexString(data.length)+"\r\n").getBytes("US-ASCII"));out.write(data);out.write("\r\n".getBytes("US-ASCII"));}else{out.write(("HTTP/1.1 "+status+"\r\nContent-Type: "+type+"\r\nContent-Length: "+data.length+"\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));out.write(data);}out.flush();
   }finally{socket.close();}
  }catch(Exception ignored){}}},"web-test-fixture");thread.setDaemon(true);thread.start();
  try{String base="http://127.0.0.1:"+server.getLocalPort();Item i=new Item("Enlace público","test","test","","",base+"/outer");List<Stream> streams=new LinkSource().resolve(i);
   check(streams.size()==1&&streams.get(0).url.equals(base+"/film.mp4"),"nested public iframe resolves");check(Playable.item(new LinkSource(),i),"resolved iframe media signature checked");
   check(!Playable.probe(base+"/login",0),"HTML access page is not media");boolean denied=false;try{Playable.probe(base+"/denied",0);}catch(IOException expected){denied=true;}check(denied,"401 access requirement rejected");
   OkSource.Parser<String> parser=new OkSource.Parser<String>(){public String parse(String html)throws Exception{if(!html.contains("</complete>"))throw new IOException("Incomplete metadata");return "complete";}};
   check(OkSource.payload(base+"/cut",parser).equals("complete"),"complete payload survives unrelated page-tail truncation");boolean incomplete=false;try{OkSource.payload(base+"/cutbad",parser);}catch(IOException expected){incomplete=true;}check(incomplete,"truncated payload is never accepted");
  }finally{server.close();}
 }
}
