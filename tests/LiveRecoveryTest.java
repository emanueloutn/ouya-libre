package org.ouyalibre.tv;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Finite real TS media served as a deliberately ending IPTV fixture. No catalog entries. */
public final class LiveRecoveryTest extends Instrumentation {
 private Activity activity;
 private ServerSocket server;
 private volatile boolean running=true;
 private final AtomicInteger playlists=new AtomicInteger();
 private boolean cancel;
 private int passed;
 public void onCreate(Bundle args){super.onCreate(args);cancel=args!=null&&"true".equals(args.getString("cancel"));start();}
 private Object field(Object owner,String name)throws Exception{java.lang.reflect.Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 private void check(boolean ok,String label){if(!ok)throw new AssertionError(label);passed++;Bundle b=new Bundle();b.putString("stream","PASS "+label+"\n");sendStatus(0,b);}
 public void onStart(){Bundle result=new Bundle();int code=0;try{
  final File sample=new File("/sdcard/Download/OuyaLibre-prueba-MP2.ts");check(sample.isFile()&&sample.length()>100000,"Actual local TS fixture exists");
  server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));Thread accept=new Thread(new Runnable(){public void run(){while(running)try{final Socket s=server.accept();new Thread(new Runnable(){public void run(){try{BufferedReader reader=new BufferedReader(new InputStreamReader(s.getInputStream(),"US-ASCII"));String request=reader.readLine(),line;while((line=reader.readLine())!=null&&!line.isEmpty()){}boolean manifest=request!=null&&request.contains("/live.m3u8");byte[] body=null;long size;if(manifest){playlists.incrementAndGet();body=("#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:10\n#EXT-X-MEDIA-SEQUENCE:0\n#EXTINF:6.0,\nchunk.ts\n#EXT-X-ENDLIST\n").getBytes("US-ASCII");size=body.length;}else size=sample.length();OutputStream out=s.getOutputStream();out.write(("HTTP/1.1 200 OK\r\nContent-Type: "+(manifest?"application/vnd.apple.mpegurl":"video/mp2t")+"\r\nContent-Length: "+size+"\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));if(manifest)out.write(body);else{FileInputStream in=new FileInputStream(sample);try{byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}finally{in.close();}}out.flush();}catch(Exception ignored){}finally{try{s.close();}catch(Exception ignored){}}}}).start();}catch(Exception ignored){}}});accept.setDaemon(true);accept.start();
  String url="http://127.0.0.1:"+server.getLocalPort()+"/live.m3u8";
  Item item=new Item("Instrumented recovery","finite-real-ts","Local recovery test","Real encoded media; deliberately finite HLS","",url);
  Intent intent=new Intent(getTargetContext(),PlayerActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);intent.putExtra("item",item.json().toString());intent.putExtra("stream",url);intent.putExtra("live",true);intent.putExtra("audioCompatible",true);activity=startActivitySync(intent);
  final EngineVideoView view=(EngineVideoView)field(activity,"video");final TextView status=(TextView)field(activity,"status");long deadline=SystemClock.elapsedRealtime()+20000;while(!status.getText().toString().contains("Reintentando")&&SystemClock.elapsedRealtime()<deadline)Thread.sleep(100);
  check(status.getText().toString().contains("Reintentando"),"Unexpected live HLS end offers automatic recovery");
  check(playlists.get()==1,"First session ends before reconnect");
  check(((View)field(activity,"overlay")).getVisibility()==View.VISIBLE,"Recovery notice and controls stay visible");
  if(cancel){runOnMainSync(new Runnable(){public void run(){activity.finish();}});Thread.sleep(7000);check(playlists.get()==1,"Leaving player cancels delayed reconnect");}
  else{deadline=SystemClock.elapsedRealtime()+30000;while(playlists.get()<3&&SystemClock.elapsedRealtime()<deadline)Thread.sleep(100);check(playlists.get()==3,"Two automatic reconnects reopen the actual HLS stream");Thread.sleep(11000);check(playlists.get()==3,"Reconnect budget is bounded to two attempts");check(status.getText().toString().startsWith("La señal terminó"),"Exhausted recovery returns to usable final state");}
 }catch(Throwable e){code=1;result.putString("stream","FAILED "+e+"\n");}finally{if(activity!=null)try{runOnMainSync(new Runnable(){public void run(){activity.finish();}});Thread.sleep(2500);}catch(Exception ignored){}running=false;if(server!=null)try{server.close();}catch(Exception ignored){}}
  if(code==0)result.putString("stream","TOTAL "+passed+" real-media recovery checks passed\n");finish(code,result);
 }
}
