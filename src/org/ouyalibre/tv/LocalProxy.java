package org.ouyalibre.tv;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Bounded in-device relay: TLS 1.2 for legacy MediaPlayer, Range and HLS relative URLs. */
public final class LocalProxy {
 private final ServerSocket server;
 private final String token=UUID.randomUUID().toString();
 private final ConcurrentHashMap<String,String> urls=new ConcurrentHashMap<String,String>();
 private final Set<Socket> sockets=Collections.synchronizedSet(new HashSet<Socket>());
 private final ThreadPoolExecutor pool=new ThreadPoolExecutor(3,3,10,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(6));
 private volatile boolean running=true;
 private final java.util.concurrent.atomic.AtomicLong received=new java.util.concurrent.atomic.AtomicLong(),queued=new java.util.concurrent.atomic.AtomicLong();
 private volatile long lastData=0;private volatile String lastFailure="";
 private void received(int n){received.addAndGet(n);lastData=System.currentTimeMillis();}
 public String diagnostics(){long idle=lastData==0?-1:(System.currentTimeMillis()-lastData)/1000;return String.format(Locale.US,"Recibidos %.1f MiB · Búfer adicional %.1f MiB",received.get()/1048576.0,Math.max(0,queued.get())/1048576.0)+(idle<0?" · Esperando primeros datos":" · Últimos datos hace "+idle+" s")+(lastFailure.length()>0?" · Fuente: "+lastFailure:"");}
 public LocalProxy()throws IOException{
  server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));
  Thread accept=new Thread(new Runnable(){public void run(){while(running)try{final Socket s=server.accept();s.setSoTimeout(20000);sockets.add(s);try{pool.execute(new Runnable(){public void run(){serve(s);}});}catch(RejectedExecutionException e){sockets.remove(s);s.close();}}catch(IOException e){if(!running)return;}}},"video-relay");accept.setDaemon(true);accept.start();
 }
 public String link(String url)throws Exception{
  Net.safe(url);if(urls.size()>1200)throw new IOException("Demasiados recursos HLS en esta sesión");String path=new URL(url).getPath().toLowerCase(Locale.US);String id=UUID.randomUUID().toString()+(path.endsWith(".m3u8")?".m3u8":path.endsWith(".mp4")?".mp4":"");urls.put(id,url);
  return "http://127.0.0.1:"+server.getLocalPort()+"/"+token+"/"+id;
 }
 public void close(){running=false;try{server.close();}catch(Exception e){}synchronized(sockets){for(Socket s:sockets)try{s.close();}catch(Exception e){}}pool.shutdownNow();urls.clear();}
 private String line(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();int v;while((v=in.read())!=-1){if(v=='\n')break;if(v!='\r')b.write(v);if(b.size()>16384)throw new IOException("Cabecera muy larga");}return v==-1&&b.size()==0?null:b.toString("US-ASCII");}
 private void serve(Socket socket){HttpURLConnection c=null;ReadAhead ahead=null;boolean started=false;
  try{
   InputStream in=socket.getInputStream();OutputStream out=socket.getOutputStream();String first=line(in);if(first==null)return;
   String[] req=first.split(" ");if(req.length<2||(!req[0].equals("GET")&&!req[0].equals("HEAD")))throw new IOException("Método no admitido");
   String prefix="/"+token+"/";if(!req[1].startsWith(prefix))throw new IOException("Solicitud desconocida");String remote=urls.get(req[1].substring(prefix.length()));if(remote==null)throw new IOException("Recurso no registrado");
   String range=null,l;int count=0;while((l=line(in))!=null&&l.length()>0){if(++count>80)throw new IOException("Demasiadas cabeceras");if(l.toLowerCase(Locale.US).startsWith("range:"))range=l.substring(6).trim();}
   c=Net.open(remote,range);String type=c.getContentType();if(type==null)type="application/octet-stream";
   boolean hls=new URL(remote).getPath().toLowerCase(Locale.US).endsWith(".m3u8")||type.toLowerCase(Locale.US).contains("mpegurl");
   if(hls){String text=new String(read(c.getInputStream(),4*1024*1024),"UTF-8");String rewritten=rewrite(text,c.getURL());byte[] b=rewritten.getBytes("UTF-8");header(out,200,"application/vnd.apple.mpegurl",String.valueOf(b.length),null);started=true;if(req[0].equals("GET"))out.write(b);}
   else{header(out,c.getResponseCode(),type,c.getHeaderField("Content-Length"),c.getHeaderField("Content-Range"));started=true;if(req[0].equals("GET")){InputStream data=c.getInputStream();long size=0;try{size=Long.parseLong(c.getHeaderField("Content-Length"));}catch(Exception ignored){}if(size>2*1024*1024&&(type.toLowerCase(Locale.US).contains("video/mp4")||new URL(remote).getPath().toLowerCase(Locale.US).endsWith(".mp4"))){ahead=ReadAhead.create(c,data,this);if(ahead!=null){ahead.waitForInitialData();data=ahead;}}byte[] b=new byte[32*1024];int n;while(running&&(n=data.read(b))!=-1){if(ahead==null)received(n);out.write(b,0,n);}}}
   out.flush();
  }catch(Exception e){lastFailure=e.getClass().getSimpleName();android.util.Log.w("OuyaLibreVideo","Transporte interrumpido: "+lastFailure);if(!started)try{OutputStream out=socket.getOutputStream();byte[] b="Fuente no disponible".getBytes("UTF-8");header(out,502,"text/plain",String.valueOf(b.length),null);out.write(b);out.flush();}catch(Exception ignored){}}
  finally{if(ahead!=null)ahead.close();if(c!=null)c.disconnect();sockets.remove(socket);try{socket.close();}catch(Exception e){}}
 }
 private byte[] read(InputStream in,int max)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(b.size()+n>max)throw new IOException("Manifiesto muy grande");b.write(buf,0,n);}return b.toByteArray();}
 private final LinkedHashMap<String,String> recent=new LinkedHashMap<String,String>();
 private synchronized String child(URL base,String relative)throws Exception{
  String url=new URL(base,relative).toString();String found=recent.get(url);if(found!=null)return found;
  // Sliding live playlist window: prevent unbounded growth during long television sessions.
  if(recent.size()>=600){Iterator<Map.Entry<String,String>> it=recent.entrySet().iterator();Map.Entry<String,String> old=it.next();urls.remove(old.getValue().substring(old.getValue().lastIndexOf('/')+1));it.remove();}
  String result=link(url);recent.put(url,result);return result;
 }
 private String rewrite(String text,URL base)throws Exception{
  if(!text.startsWith("#EXTM3U"))throw new IOException("Manifiesto no válido");StringBuilder out=new StringBuilder();Pattern uri=Pattern.compile("URI=\"([^\"]+)\"");
  for(String raw:text.split("\n")){String l=raw.trim();if(l.length()>0&&!l.startsWith("#"))l=child(base,l);
   else if(l.startsWith("#")){Matcher m=uri.matcher(l);StringBuffer b=new StringBuffer();while(m.find())m.appendReplacement(b,Matcher.quoteReplacement("URI=\""+child(base,m.group(1))+"\""));m.appendTail(b);l=b.toString();}out.append(l).append('\n');}return out.toString();
 }
 private void header(OutputStream out,int code,String type,String len,String range)throws IOException{
  String s="HTTP/1.1 "+code+(code==206?" Partial Content":code==200?" OK":" Bad Gateway")+"\r\nConnection: close\r\nContent-Type: "+type+"\r\nAccept-Ranges: bytes\r\n";
  if(len!=null)s+="Content-Length: "+len+"\r\n";if(range!=null)s+="Content-Range: "+range+"\r\n";out.write((s+"\r\n").getBytes("US-ASCII"));
 }
 /** Two bounded read-ahead buffers: at most 8 MiB allocated for progressive MP4. */
 private static final class ReadAhead extends InputStream {
  private static final Semaphore permits=new Semaphore(2);
  private final byte[] ring=new byte[4*1024*1024];private final HttpURLConnection connection;private final InputStream source;private final Thread producer;
  private final LocalProxy owner;private int first=0,count=0;private boolean eof=false,closed=false;private IOException error;
  static ReadAhead create(HttpURLConnection c,InputStream in,LocalProxy owner){if(!permits.tryAcquire())return null;try{return new ReadAhead(c,in,owner);}catch(OutOfMemoryError lowMemory){permits.release();return null;}catch(RuntimeException e){permits.release();throw e;}}
  private ReadAhead(HttpURLConnection c,InputStream in,LocalProxy owner){this.owner=owner;connection=c;source=in;producer=new Thread(new Runnable(){public void run(){produce();}},"video-read-ahead");producer.setDaemon(true);producer.start();}
  private void produce(){byte[] b=new byte[32*1024];try{int n;while((n=source.read(b))!=-1){owner.received(n);int offset=0;while(offset<n){synchronized(this){while(count==ring.length&&!closed)wait();if(closed)return;int end=(first+count)%ring.length;int take=Math.min(n-offset,Math.min(ring.length-count,ring.length-end));System.arraycopy(b,offset,ring,end,take);count+=take;owner.queued.addAndGet(take);offset+=take;notifyAll();}}}}catch(InterruptedException cancelled){Thread.currentThread().interrupt();}catch(IOException failed){synchronized(this){if(!closed){error=failed;owner.lastFailure=failed.getClass().getSimpleName();}}}finally{synchronized(this){eof=true;notifyAll();}}}
  synchronized void waitForInitialData()throws IOException{long until=System.currentTimeMillis()+8000;while(count<1024*1024&&!eof&&!closed&&error==null){long left=until-System.currentTimeMillis();if(left<=0)break;try{wait(left);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Carga cancelada",e);}}}
  public synchronized int read(byte[] b,int offset,int length)throws IOException{if(length==0)return 0;while(count==0){if(error!=null)throw error;if(eof||closed)return -1;try{wait();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Lectura cancelada",e);}}int n=Math.min(length,Math.min(count,ring.length-first));System.arraycopy(ring,first,b,offset,n);first=(first+n)%ring.length;count-=n;owner.queued.addAndGet(-n);notifyAll();return n;}
  public int read()throws IOException{byte[] b=new byte[1];int n=read(b,0,1);return n<0?-1:b[0]&255;}
  public synchronized int available(){return count;}
  public void close(){synchronized(this){if(closed)return;closed=true;owner.queued.addAndGet(-count);count=0;notifyAll();}try{connection.disconnect();producer.interrupt();}finally{permits.release();}}
 }
}
