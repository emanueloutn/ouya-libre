package org.ouyalibre.tv;
import java.util.*;
import java.net.*;
import java.io.*;

/** Bounded media probe; never loads an entire movie into RAM. */
public final class Playable {
 public static boolean item(Source source,Item item)throws Exception{
  return options(source.resolve(item),0);
 }
 private static boolean options(List<Stream> streams,int depth)throws Exception{
  int count=0;for(Stream s:streams){if(Thread.currentThread().isInterrupted())throw new InterruptedException();if(++count>4)break;
   try{if(s.target!=null){if(depth<2&&options(new RtvcSource().resolve(s.target),depth+1))return true;}else if(probe(s.url,0))return true;}catch(InterruptedException e){throw e;}catch(Exception ignored){}
  }return false;
 }
 static boolean probe(String raw,int depth)throws Exception{
  HttpURLConnection c=Net.open(raw,"bytes=0-8191");
  try{c.setReadTimeout(Net.boundedTimeout(10000));InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;
   while(out.size()<8192&&(n=in.read(b,0,Math.min(b.length,8192-out.size())))>0){Net.checkBudget();out.write(b,0,n);}
   byte[] data=out.toByteArray();if(data.length<8)return false;
   String text=new String(data,"UTF-8").trim();
   if(text.startsWith("#EXTM3U")){
    if(depth>2||text.contains("SAMPLE-AES")||text.contains("KEYFORMAT="))return false;
    List<String> links=new ArrayList<String>();boolean allowed=true;for(String line:text.split("\\r?\\n")){line=line.trim();if(line.startsWith("#EXT-X-STREAM-INF:")){java.util.regex.Matcher size=java.util.regex.Pattern.compile("RESOLUTION=[0-9]+x([0-9]+)").matcher(line);allowed=!size.find()||Integer.parseInt(size.group(1))<=1080;}else if(line.length()>0&&!line.startsWith("#")){if(allowed)links.add(line);allowed=true;}}
    boolean live=text.contains("#EXTINF:")&&!text.contains("#EXT-X-ENDLIST");for(int i=0;i<Math.min(2,links.size());i++){String link=links.get(live?links.size()-1-i:i);try{if(probe(new URL(c.getURL(),link).toString(),depth+1))return true;}catch(InterruptedIOException e){throw e;}catch(Exception ignored){}}return false;
   }
   if(text.startsWith("<")||text.startsWith("{")||text.startsWith("["))return false;
   // ISO BMFF/MP4 and MPEG TS. A media probe cannot certify every codec/profile.
   return (data[4]=='f'&&data[5]=='t'&&data[6]=='y'&&data[7]=='p')||(data[0]==0x47&&data.length>=189&&data[188]==0x47);
  }finally{c.disconnect();}
 }
}
