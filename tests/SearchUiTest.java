package org.ouyalibre.tv;
import android.app.*;import android.content.*;import android.os.*;import java.lang.reflect.*;import java.util.*;
/** Device-only test of real searches and cancellation; never feeds fixture results. */
public final class SearchUiTest extends Instrumentation{
 private String query;private Activity activity;
 public void onCreate(Bundle args){super.onCreate(args);query=args==null?"Big Buck Bunny":args.getString("query","Big Buck Bunny");start();}
 static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 void say(String s){Bundle b=new Bundle();b.putString("stream",s+"\n");sendStatus(0,b);}
 public void onStart(){Bundle result=new Bundle();try{
  say("SAVED IPTV lists="+new IptvStore(getTargetContext()).entries().size()+" favorites="+new Store(getTargetContext()).items("favorites").size()+" history="+new Store(getTargetContext()).items("history").size());
  Intent intent=new Intent(getTargetContext(),MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);activity=startActivitySync(intent);
  runOnMainSync(new Runnable(){public void run(){try{Field q=MainActivity.class.getDeclaredField("query");q.setAccessible(true);q.set(activity,query);Method m=MainActivity.class.getDeclaredMethod("runSearch");m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}}});
  for(int i=0;i<12;i++){Thread.sleep(10000);final int[] pending={0};runOnMainSync(new Runnable(){public void run(){try{pending[0]=(Integer)field(activity,"remaining");say("SEARCH "+query+" count="+((List)field(activity,"results")).size()+" pending="+pending[0]);}catch(Exception e){throw new RuntimeException(e);}}});if(pending[0]==0)break;}
  runOnMainSync(new Runnable(){public void run(){try{List<Item> items=(List<Item>)field(activity,"results");if(query.equals("Big Buck Bunny")&&items.isEmpty())throw new AssertionError("No real results in UI");Set<String> keys=new HashSet<String>();for(Item item:items){if(!item.verified||!SearchQuery.acceptable(item,query)||!keys.add(SearchQuery.identity(item)))throw new AssertionError("unverified, irrelevant or duplicate result");say("VISIBLE "+item.source+" | "+item.title);}for(Object d:(List)field(activity,"diagnostics"))say("DIAGNOSTIC "+d);Method cancel=MainActivity.class.getDeclaredMethod("cancel");cancel.setAccessible(true);cancel.invoke(activity);activity.finish();}catch(Exception e){throw new RuntimeException(e);}}});Thread.sleep(2000);result.putString("stream","PASS real UI search, uniqueness and cancellation\n");finish(0,result);
 }catch(Throwable e){if(activity!=null)try{runOnMainSync(new Runnable(){public void run(){activity.finish();}});}catch(Throwable ignored){}result.putString("stream","FAIL "+e+"\n");finish(1,result);}}
}
