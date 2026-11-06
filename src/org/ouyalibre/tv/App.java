package org.ouyalibre.tv;
import android.app.Application;
import java.io.*;

public final class App extends Application {
 public void onCreate(){super.onCreate();try{Net.prepare(getAssets().open("cacert.pem"));}catch(Exception e){Net.prepare(null);}final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){public void uncaughtException(Thread thread,Throwable e){try{StringWriter text=new StringWriter();e.printStackTrace(new PrintWriter(text));String s=new java.util.Date()+"\n"+text.toString();if(s.length()>20000)s=s.substring(0,20000);FileOutputStream out=openFileOutput("last-crash.txt",MODE_PRIVATE);out.write(s.getBytes("UTF-8"));out.close();}catch(Throwable ignored){}if(previous!=null)previous.uncaughtException(thread,e);else android.os.Process.killProcess(android.os.Process.myPid());}});}
}
