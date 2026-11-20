package org.ouyalibre.tv;
import java.io.*;import java.net.*;import java.util.*;
/** Device tests: local parser cases never become search catalog entries. */
public final class DriveDeviceTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);}
 public static void main(String[] args)throws Exception{
  Net.init(new FileInputStream("/data/local/tmp/search-cacert.pem"));
  String id="1U0EOhVUNXuRdDy07qNPRAk76_C_VgUCh",base="https://drive.google.com/file/d/"+id+"/view";
  check(DriveSource.accepts(base+"?usp=sharing"),"public share link recognized");
  check(DriveSource.id("https://drive.google.com/open?id="+id).equals(id),"open link recognized");
  check(DriveSource.canonical(base+"?usp=sharing&resourcekey=0-Ab_cd-12").endsWith("?resourcekey=0-Ab_cd-12"),"published resource key preserved; tracking stripped");
  check(DriveSource.download(base+"?resourcekey=0-Ab_cd-12").contains("resourcekey=0-Ab_cd-12"),"resource key forwarded to download");
  check(!DriveSource.accepts("https://drive.google.com.evil.invalid/file/d/"+id+"/view"),"lookalike host rejected");
  check(!DriveSource.accepts("https://drive.google.com/drive/folders/"+id),"folder is not misrepresented as playable file");
  check(!DriveSource.accepts("https://drive.google.com/uc?id="+id+"&id=anotherfile123"),"ambiguous file ID rejected");
  URL end=new URL("https://drive.usercontent.google.com/download?id="+id);
  check(DriveSource.mediaResponse(end,"video/mp4; charset=binary",null),"MP4 MIME recognized");
  check(DriveSource.mediaResponse(end,"application/octet-stream","attachment; filename=\"Sample video.mp4\""),"binary download filename recognized");
  check(!DriveSource.mediaResponse(end,"text/html",null),"confirmation and access pages rejected");
  check(!DriveSource.mediaResponse(end,"application/octet-stream","attachment; filename=\"example.webm\""),"unsupported file format rejected");
  check(!DriveSource.mediaResponse(new URL("https://accounts.google.com/signin"),"video/mp4",null),"sign-in redirect rejected");
  List<Item> items=WebSource.parseGoogle("<a href='unused'></a><a href=\""+base+"\"><h3>Prueba de vídeo</h3></a><a href=\"https://accounts.google.com/test\"><h3>Prueba de vídeo</h3></a>","prueba");
  check(items.size()==1&&DriveSource.accepts(items.get(0).url),"web parser admits Drive file and rejects Google sign-in pages");
  if(args.length>0&&args[0].equals("--online")){
   Item item=new Item("Google Drive · prueba pública",id,"Vídeo de demostración publicado por RGUKT","","",base);
   check(Playable.item(new DriveSource(),item),"REAL anonymous Drive MP4 yields media bytes");
   check(!new LinkSource().resolve(item).isEmpty(),"manual public link routes through Drive resolver");
   LocalProxy proxy=new LocalProxy();try{HttpURLConnection c=Net.open(proxy.link(DriveSource.download(base)),"bytes=0-31");try{check(c.getResponseCode()==206&&c.getHeaderField("Content-Range")!=null,"REAL byte-range seeking preserved through on-device relay");byte[] data=new byte[12];InputStream in=c.getInputStream();int offset=0,n;while(offset<data.length&&(n=in.read(data,offset,data.length-offset))>0)offset+=n;check(offset==12&&data[4]=='f'&&data[5]=='t'&&data[6]=='y'&&data[7]=='p',"REAL MP4 bytes preserved by relay");}finally{c.disconnect();}}finally{proxy.close();}
   boolean rejected=false;try{new DriveSource().resolve(new Item("test","blocked","blocked","","","https://drive.google.com/file/d/1Itku_rKPeybHHRj7XG9ug_Y_ne92B70f/view"));}catch(Exception expected){rejected=true;}check(rejected,"REAL sign-in protected file refused");
   rejected=false;try{new DriveSource().resolve(new Item("test","warning","warning","","","https://drive.google.com/file/d/0ByeS4oOUV-49Zzh4R1J6R09zazQ/view"));}catch(Exception expected){rejected=true;}check(rejected,"REAL confirmation form not auto-accepted");
   try{List<Item> found=new DriveSource().search("Indiana Jones latino");System.out.println("REAL DRIVE SEARCH candidates="+found.size());for(Item f:found)check(DriveSource.accepts(f.url),"search restricted to real Drive file links");}catch(Exception e){System.out.println("REAL DRIVE SEARCH UNAVAILABLE "+e);}
  }
  System.out.println("DONE");System.exit(0);
 }
}
