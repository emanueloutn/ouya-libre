package org.ouyalibre.tv;
public final class Stream {
 public final String label,url,subtitle;public final Item target;
 public Stream(String l,String u){this(l,u,"");}
 public Stream(String l,String u,String s){label=l;url=u;subtitle=s;target=null;}
 public Stream(String l,Item item){label=l;target=item;url="";subtitle="";}
 public String toString(){return label;}
}
