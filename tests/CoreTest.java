package org.ouyalibre.tv;

import java.util.*;
import java.io.*;
import java.net.*;

/** Real provider tests when --online is supplied; local tests never become app search results. */
public final class CoreTest {
 private static int passed=0;
 private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);System.out.println("PASS "+label);passed++;}
 private static byte[] get(String u)throws Exception{return Net.bytes(u,1024*1024);}
 public static void main(String[] args)throws Exception{
  Net.init(args.length>1?new FileInputStream(args[1]):null);
  List<Item> channels=M3uSource.parse("#EXTM3U\n#EXTINF:-1 group-title=\"Cine\",Película de prueba\nfilm.mp4\n#EXTINF:-1,Noticias\nhttps://example.org/news.m3u8\n","https://example.org/tv/list.m3u","M3U · test","cine");
  check(channels.size()==1&&channels.get(0).url.equals("https://example.org/tv/film.mp4"),"M3U filter and relative URL");
  boolean rejected=false;try{M3uSource.parse("#EXTM3U\n#EXT-X-TARGETDURATION:6\nclip.ts\n","https://example.org/a.m3u8","test","");}catch(Exception e){rejected=true;}check(rejected,"Reject HLS as catalog");
  rejected=false;try{Net.safe("https://user:password@example.org/private.mp4");}catch(Exception e){rejected=true;}check(rejected,"Reject credential URLs");
  rejected=false;try{Net.safe("file:///etc/passwd");}catch(Exception e){rejected=true;}check(rejected,"Reject non HTTP schemes");
  Item i=new Item("source","id","Título","Sinopsis","es","https://example.org");check(Item.from(i.json()).key().equals(i.key()),"Favorite/history serialization identity");check(i.spanish(),"Declared Spanish ranking");
  check(new Item("Internet Archive","id","El club de la pelea: análisis","","es","").related(),"Flag reviews instead of implying full movie");
  if(args.length>0&&args[0].equals("--online")){
   Source blender=new PeerTubeSource("https://video.blender.org");List<Item> b=blender.search("Big Buck Bunny");check(!b.isEmpty(),"LIVE PeerTube search");List<Stream> videos=blender.resolve(b.get(0));check(!videos.isEmpty(),"LIVE PeerTube MP4 resolution");
   String u=videos.get(0).url;HttpURLConnection c=Net.open(u,"bytes=0-1023");check(c.getResponseCode()==206,"LIVE video byte ranges");byte[] header=new byte[32];int n=c.getInputStream().read(header);c.disconnect();check(n>8&&new String(header,4,4,"US-ASCII").equals("ftyp"),"LIVE actual MP4 ftyp bytes");
   Source archive=new ArchiveSource();List<Item> a=archive.search("Duck and Cover");check(!a.isEmpty(),"LIVE Internet Archive search");List<Stream> af=archive.resolve(new Item("Internet Archive","ArcherProductionsInc","Duck and Cover","","eng",""));check(!af.isEmpty(),"LIVE Archive file metadata");
   Source nasa=new NasaSource();List<Item> ns=nasa.search("moon");check(!ns.isEmpty(),"LIVE NASA search");List<Stream> nv=nasa.resolve(ns.get(0));check(!nv.isEmpty(),"LIVE NASA MP4 resolution");HttpURLConnection nc=Net.open(nv.get(0).url,"bytes=0-1023");int nr=nc.getInputStream().read(header);nc.disconnect();check(nr>8&&new String(header,4,4,"US-ASCII").equals("ftyp"),"LIVE NASA actual MP4 bytes");
   RtvcSource rtvc=new RtvcSource();List<Item> rs=rtvc.search("amor");check(!rs.isEmpty()&&rs.get(0).spanish(),"LIVE RTVC Spanish search");List<Stream> episodes=rtvc.resolve(new Item(rtvc.name(),"/series-ficcion/nn","N.N.","","es","https://www.rtvcplay.co/series-ficcion/nn"));check(episodes.size()>1&&episodes.get(0).target!=null,"LIVE RTVC episode navigation");List<Stream> episode=rtvc.resolve(episodes.get(0).target);check(!episode.isEmpty()&&Net.text(episode.get(0).url).startsWith("#EXTM3U"),"LIVE RTVC real episode HLS manifest");
   List<Item> page1=((PagedSource)archive).search("bunny",1),page2=((PagedSource)archive).search("bunny",2);check(!page2.isEmpty()&&!page1.get(0).id.equals(page2.get(0).id),"LIVE Archive pagination distinct pages");
   for(String invalid:new String[]{"https://expired.badssl.com/","https://wrong.host.badssl.com/","https://self-signed.badssl.com/"}){boolean rejectedCertificate=false;try{Net.text(invalid);}catch(Exception bad){for(Throwable cause=bad;cause!=null;cause=cause.getCause())if(cause instanceof java.security.cert.CertificateException)rejectedCertificate=true;}check(rejectedCertificate,"TLS rejects invalid certificate: "+invalid);}
  }
  relayTests();System.out.println("TOTAL "+passed+" assertions passed");
 }
 private static void relayTests()throws Exception{
  final byte[] sample=new byte[1024*1024];for(int i=0;i<sample.length;i++)sample[i]=(byte)(i%251);
  final ServerSocket upstream=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));
  Thread serve=new Thread(new Runnable(){public void run(){while(!upstream.isClosed())try{Socket peer=upstream.accept();try{peer.setSoTimeout(5000);BufferedReader reader=new BufferedReader(new InputStreamReader(peer.getInputStream(),"US-ASCII"));String first=reader.readLine();if(first==null)continue;String path=first.split(" ")[1],line,range=null;while((line=reader.readLine())!=null&&line.length()>0)if(line.toLowerCase(Locale.US).startsWith("range:"))range=line.substring(6).trim();byte[] data=sample;String type="video/mp4";int status=200;String extra="";boolean big=path.equals("/big.mp4");
   if(path.equals("/index.m3u8")){data="#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"\n#EXTINF:6,\nseg.ts\n".getBytes("UTF-8");type="application/vnd.apple.mpegurl";}
   else if(path.equals("/page")){data="<video><source src='/film.mp4' type='video/mp4'></video>".getBytes("UTF-8");type="text/html";}
   else if(path.equals("/blocked")){status=403;data=new byte[0];type="text/plain";}
   if(range!=null&&range.equals("bytes=100-199")){data=Arrays.copyOfRange(sample,100,200);status=206;big=false;extra="Content-Range: bytes 100-199/"+sample.length+"\r\n";}
   OutputStream out=peer.getOutputStream();out.write(("HTTP/1.1 "+status+" Test\r\nContent-Type: "+type+"\r\nContent-Length: "+(big?5*1024*1024:data.length)+"\r\nConnection: close\r\n"+extra+"\r\n").getBytes("US-ASCII"));if(big){byte[] chunk=new byte[32768];for(int at=0;at<5*1024*1024;at+=chunk.length){for(int j=0;j<chunk.length;j++)chunk[j]=(byte)((at+j)%251);out.write(chunk);}}else out.write(data);out.flush();}finally{peer.close();}}catch(IOException ignored){}}},"fixture-http");serve.setDaemon(true);serve.start();LocalProxy proxy=null;
  try{String base="http://127.0.0.1:"+upstream.getLocalPort();proxy=new LocalProxy();String video=proxy.link(base+"/film.mp4");
   HttpURLConnection c=Net.open(video,"bytes=100-199");check(c.getResponseCode()==206,"Relay preserves HTTP 206");check("bytes 100-199/1048576".equals(c.getHeaderField("Content-Range")),"Relay preserves Content-Range");ByteArrayOutputStream b=new ByteArrayOutputStream();InputStream in=c.getInputStream();int n;while((n=in.read())!=-1)b.write(n);c.disconnect();check(Arrays.equals(Arrays.copyOfRange(sample,100,200),b.toByteArray()),"Relay preserves exact range bytes");
   String manifestUrl=proxy.link(base+"/index.m3u8");check(manifestUrl.endsWith(".m3u8"),"Legacy player recognizes HLS URL suffix");String hls=new String(get(manifestUrl),"UTF-8");check(hls.contains("#EXTM3U")&&!hls.contains("URI=\"key.bin\"")&&!hls.contains("\nseg.ts"),"HLS relative segment and key rewriting");
   String segment=hls.trim().substring(hls.trim().lastIndexOf('\n')+1);check(get(segment).length==sample.length,"HLS rewritten segment retrieval");
   HttpURLConnection large=Net.open(proxy.link(base+"/big.mp4"),null);InputStream videoInput=large.getInputStream();byte[] block=new byte[32768];int total=0,read;boolean exact=true;while((read=videoInput.read(block))!=-1){for(int j=0;j<read;j++)if(block[j]!=(byte)((total+j)%251))exact=false;total+=read;}large.disconnect();check(total==5*1024*1024&&exact,"MP4 bounded read-ahead preserves data across ring wrap");
   Item page=new Item("Enlace público","page","page","","",base+"/page");List<Stream> links=new LinkSource().resolve(page);check(links.size()==1&&links.get(0).url.equals(base+"/film.mp4"),"Public HTML video extraction");
   boolean denied=false;try{Net.text(base+"/blocked");}catch(IOException e){denied=e.getMessage().contains("403");}check(denied,"Restricted source errors propagate");
   String invalid=video.replace(video.substring(video.lastIndexOf('/')+1),"unregistered");denied=false;try{get(invalid);}catch(IOException e){denied=e.getMessage().contains("502");}check(denied,"Relay rejects unregistered resource");
  }finally{if(proxy!=null)proxy.close();upstream.close();}
 }
}
