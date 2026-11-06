package org.ouyalibre.tv;

import android.app.*;
import android.os.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import java.io.*;
import java.util.concurrent.*;

public final class PlayerActivity extends Activity {
 private VideoView video;private TextView status,subtitle;private Button toggle;private Store store;private Item item;
 private String stream,subUrl;private LocalProxy proxy;private boolean prepared=false,closed=false,subtitles=true,wantPlay=true,buffering=false;private int generation=0,lastPosition=-1,lastObserved=-1,pendingSeek=-1,stalledTicks=0,retries=0;
 private final Handler handler=new Handler();private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Runnable tick=new Runnable(){public void run(){if(prepared){try{int position=pendingSeek>=0?pendingSeek:video.getCurrentPosition();if(wantPlay&&position==lastObserved)stalledTicks++;else stalledTicks=0;lastObserved=position;if(position>0||lastPosition<0)lastPosition=position;status.setText(item.title+"   "+time(position)+" / "+time(video.getDuration())+(buffering||stalledTicks>2?" · Reproducción detenida / cargando…"+(proxy==null?"":"\n"+proxy.diagnostics()):""));save();toggle.setText(video.isPlaying()?"Pausar":"Reproducir");if(stalledTicks>=15){if(retries<1){retries++;release();begin();return;}status.setText("La fuente dejó de avanzar. Puedes reintentar desde "+time(position)+" o volver y elegir otro archivo.");}}catch(Exception e){status.setText("El reproductor perdió la señal. Usa Reintentar o Volver.");}}if(!closed)handler.postDelayed(this,3000);}};
 public static String time(int ms){int s=Math.max(0,ms)/1000;return String.format(java.util.Locale.US,"%02d:%02d:%02d",s/3600,s/60%60,s%60);}
 public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_FULLSCREEN);store=new Store(this);
  try{item=Item.from(new org.json.JSONObject(getIntent().getStringExtra("item")));stream=getIntent().getStringExtra("stream");Net.safe(stream);subUrl=getIntent().getStringExtra("subtitle");}catch(Exception e){finish();return;}
  FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);video=new VideoView(this);FrameLayout.LayoutParams vp=new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);root.addView(video,vp);
  LinearLayout overlay=new LinearLayout(this);overlay.setOrientation(1);overlay.setPadding(20,10,20,10);overlay.setBackgroundColor(0xdd0d141f);status=Ui.text(this,"Preparando vídeo…",17);overlay.addView(status);
  LinearLayout controls=new LinearLayout(this);toggle=button(controls,"Pausar",new Runnable(){public void run(){if(!prepared)return;try{if(video.isPlaying()){video.pause();wantPlay=false;}else{video.start();wantPlay=true;}toggle.setText(wantPlay?"Pausar":"Reproducir");}catch(Exception e){status.setText("La reproducción se interrumpió. Usa Reintentar.");}}});
  button(controls,"−30 s",new Runnable(){public void run(){seek(-30000);}});button(controls,"+30 s",new Runnable(){public void run(){seek(30000);}});
  button(controls,"Desde inicio",new Runnable(){public void run(){if(prepared)try{pendingSeek=0;lastPosition=0;video.seekTo(0);store.position(item,0);}catch(Exception e){pendingSeek=-1;status.setText("No se puede buscar en esta señal.");}}});
  button(controls,"Subtítulos",new Runnable(){public void run(){subtitles=!subtitles;subtitle.setVisibility(subtitles?View.VISIBLE:View.INVISIBLE);}});
  button(controls,"Reintentar",new Runnable(){public void run(){retries=0;wantPlay=true;release();begin();}});button(controls,"Volver",new Runnable(){public void run(){finish();}});overlay.addView(controls);
  FrameLayout.LayoutParams op=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);root.addView(overlay,op);
  subtitle=Ui.text(this,"",23);subtitle.setGravity(Gravity.CENTER);subtitle.setShadowLayer(3,1,1,Color.BLACK);FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);sp.bottomMargin=Ui.dp(this,132);root.addView(subtitle,sp);
  video.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){public void onPrepared(final MediaPlayer mp){try{prepared=true;mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener(){public void onSeekComplete(MediaPlayer p){if(prepared){pendingSeek=-1;try{lastPosition=p.getCurrentPosition();}catch(Exception ignored){}}}});mp.setOnInfoListener(new MediaPlayer.OnInfoListener(){public boolean onInfo(MediaPlayer p,int what,int extra){if(what==701)buffering=true;if(what==702)buffering=false;return false;}});store.remember(item);int pos=store.position(item);if(pos>0&&(video.getDuration()<=0||pos<video.getDuration()-3000)){pendingSeek=pos;lastPosition=pos;video.seekTo(pos);}if(wantPlay)video.start();status.setText(item.title);handler.removeCallbacks(tick);handler.post(tick);loadSubtitle(mp);}catch(Exception e){prepared=false;status.setText("El archivo no pudo iniciarse. Prueba otra calidad o fuente.");}}});
  video.setOnErrorListener(new MediaPlayer.OnErrorListener(){public boolean onError(MediaPlayer mp,int what,int extra){prepared=false;status.setText("No se pudo reproducir ("+what+" / "+extra+"). Prueba otro archivo/calidad o Reintentar.");return true;}});
  video.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){public void onCompletion(MediaPlayer mp){prepared=false;wantPlay=false;store.position(item,0);status.setText("Vídeo finalizado · "+item.title);}});
  setContentView(root);toggle.requestFocus();
 }
 private Button button(LinearLayout row,String label,final Runnable r){Button b=Ui.button(this,label,new View.OnClickListener(){public void onClick(View v){r.run();}});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1);p.setMargins(2,2,2,2);row.addView(b,p);return b;}
 protected void onStart(){super.onStart();if(item!=null)begin();}
 private void begin(){final int gen=++generation;lastPosition=-1;lastObserved=-1;pendingSeek=-1;stalledTicks=0;buffering=false;status.setText("Conectando con la fuente pública…");worker.submit(new Runnable(){public void run(){LocalProxy p=null;try{p=new LocalProxy();final LocalProxy ready=p;final String local=p.link(stream);handler.post(new Runnable(){public void run(){if(closed||gen!=generation){ready.close();return;}try{proxy=ready;video.setVideoURI(Uri.parse(local));video.requestFocus();toggle.requestFocus();}catch(Exception e){ready.close();status.setText("El reproductor no pudo abrir este archivo.");}}});}catch(final Exception e){if(p!=null)p.close();handler.post(new Runnable(){public void run(){if(!closed&&gen==generation)status.setText("No se pudo conectar: "+e.getMessage());}});}}});}
 private void seek(int delta){if(!prepared)return;try{int duration=video.getDuration(),pos=Math.max(0,video.getCurrentPosition()+delta);if(duration>0)pos=Math.min(pos,duration-1000);pendingSeek=pos;lastPosition=pos;video.seekTo(pos);}catch(Exception e){pendingSeek=-1;status.setText("No se puede buscar en esta señal.");}}
 private void save(){if(prepared&&item!=null)try{int p=pendingSeek>=0?pendingSeek:video.getCurrentPosition();if(p>0||lastPosition<0)lastPosition=Math.max(0,p);store.position(item,Math.max(0,lastPosition));}catch(Exception e){if(lastPosition>=0)store.position(item,lastPosition);}}
 private void release(){generation++;save();prepared=false;handler.removeCallbacks(tick);if(video!=null)try{video.stopPlayback();}catch(Exception ignored){}if(proxy!=null){proxy.close();proxy=null;}}
 protected void onStop(){release();super.onStop();}
 protected void onDestroy(){closed=true;release();worker.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy();}
 private void loadSubtitle(final MediaPlayer mp){if(subUrl==null||subUrl.length()==0)return;final int gen=generation;worker.submit(new Runnable(){public void run(){try{final byte[] bytes=Net.bytes(subUrl,1024*1024);final File f=new File(getCacheDir(),"subtitle.srt");FileOutputStream out=new FileOutputStream(f);try{out.write(bytes);}finally{out.close();}
   handler.post(new Runnable(){public void run(){if(closed||!prepared||gen!=generation)return;try{mp.addTimedTextSource(f.getAbsolutePath(),MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP);MediaPlayer.TrackInfo[] tracks=mp.getTrackInfo();for(int i=0;i<tracks.length;i++)if(tracks[i].getTrackType()==MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT){mp.selectTrack(i);break;}mp.setOnTimedTextListener(new MediaPlayer.OnTimedTextListener(){public void onTimedText(MediaPlayer p,TimedText t){subtitle.setText(t==null?"":t.getText());}});}catch(Exception e){status.setText("Vídeo listo; subtítulos no compatibles en este archivo.");}}});
  }catch(Exception e){handler.post(new Runnable(){public void run(){if(!closed&&gen==generation)status.setText("Vídeo listo; no se pudieron descargar los subtítulos.");}});}}});}
 public boolean dispatchKeyEvent(KeyEvent e){int c=e.getKeyCode();if(c==KeyEvent.KEYCODE_BUTTON_A)return super.dispatchKeyEvent(new KeyEvent(e.getDownTime(),e.getEventTime(),e.getAction(),KeyEvent.KEYCODE_DPAD_CENTER,0));if(c==KeyEvent.KEYCODE_BUTTON_B){if(e.getAction()==KeyEvent.ACTION_UP)finish();return true;}if(c==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE||c==KeyEvent.KEYCODE_BUTTON_X){if(e.getAction()==KeyEvent.ACTION_UP)toggle.performClick();return true;}return super.dispatchKeyEvent(e);}
}
