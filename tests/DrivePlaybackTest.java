package org.ouyalibre.tv;
import android.app.*;import android.os.*;import android.content.*;import java.util.*;
/** Plays actual publicly published university demo video; no simulated media. */
public final class DrivePlaybackTest extends Instrumentation {
 private Activity activity;private int passed;
 public void onCreate(Bundle b){super.onCreate(b);start();}
 static Object field(Object o,String n)throws Exception{java.lang.reflect.Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 void check(boolean b,String text){if(!b)throw new AssertionError(text);passed++;Bundle s=new Bundle();s.putString("stream","PASS "+text+"\n");sendStatus(0,s);}
 public void onStart(){Bundle result=new Bundle();int code=0;try{
  final int listsBefore=new IptvStore(getTargetContext()).entries().size();
  Item item=new Item("Google Drive · enlace público","1U0EOhVUNXuRdDy07qNPRAk76_C_VgUCh","Demostración pública de RGUKT","Ejemplo de vídeo publicado por la universidad para probar acceso público.","","https://drive.google.com/file/d/1U0EOhVUNXuRdDy07qNPRAk76_C_VgUCh/view");
  List<Stream> streams=new LinkSource().resolve(item);check(!streams.isEmpty(),"real shared Drive file resolved without session");
  Intent intent=new Intent(getTargetContext(),PlayerActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);intent.putExtra("item",item.json().toString());intent.putExtra("stream",streams.get(0).url);intent.putExtra("audioCompatible",true);activity=startActivitySync(intent);
  final EngineVideoView video=(EngineVideoView)field(activity,"video");long until=System.currentTimeMillis()+40000;while(video.getCurrentPosition()<500&&System.currentTimeMillis()<until)Thread.sleep(250);check(video.getCurrentPosition()>=500,"real Drive video starts in embedded player");int before=video.getCurrentPosition();Thread.sleep(6000);check(video.getCurrentPosition()>before+2500,"real Drive playback advances");
  org.videolan.libvlc.MediaPlayer player=(org.videolan.libvlc.MediaPlayer)field(video,"player");org.videolan.libvlc.Media media=player.getMedia();try{org.videolan.libvlc.Media.Stats stats=media.getStats();check(stats!=null&&stats.displayedPictures>0,"actual Drive video frames displayed");check(stats.decodedAudio>0,"actual Drive audio decoded");Bundle s=new Bundle();s.putString("stream","FRAMES "+stats.displayedPictures+" AUDIO "+stats.decodedAudio+"\n");sendStatus(0,s);}finally{media.release();}
  final int target=video.getCurrentPosition()+8000;runOnMainSync(new Runnable(){public void run(){video.seekTo(target);}});until=System.currentTimeMillis()+20000;while(video.getCurrentPosition()<target-1000&&System.currentTimeMillis()<until)Thread.sleep(250);check(video.getCurrentPosition()>=target-1000,"Drive playback seeks with byte ranges");
  check(new IptvStore(getTargetContext()).entries().size()==listsBefore,"saved IPTV lists preserved");result.putString("stream","TOTAL "+passed+" Drive playback checks passed\n");
 }catch(Throwable e){code=1;result.putString("stream","FAIL "+e+"\n");}finally{if(activity!=null)try{runOnMainSync(new Runnable(){public void run(){activity.finish();}});Thread.sleep(2500);}catch(Throwable ignored){}finish(code,result);}}
}
