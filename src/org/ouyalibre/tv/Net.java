package org.ouyalibre.tv;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;
import java.util.*;

public final class Net {
 private static SSLSocketFactory factory;
 private static volatile X509TrustManager trustManager;
 private static volatile java.util.concurrent.CountDownLatch ready;
 public static void prepare(final InputStream roots){ready=new java.util.concurrent.CountDownLatch(1);Thread t=new Thread(new Runnable(){public void run(){try{init(roots);}catch(Exception e){try{init(null);}catch(Exception ignored){}}finally{ready.countDown();}}},"certificate-store");t.setDaemon(true);t.start();}
 public static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
 public static void init(InputStream extraRoot)throws Exception{
  TrustManagerFactory tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init((KeyStore)null);
  final X509TrustManager system=(X509TrustManager)tm.getTrustManagers()[0];
  if(extraRoot==null){trustManager=system;factory=new TLSFactory((SSLSocketFactory)SSLSocketFactory.getDefault());return;}
  KeyStore ks=KeyStore.getInstance(KeyStore.getDefaultType());ks.load(null,null);
  try{ByteArrayOutputStream pem=new ByteArrayOutputStream();byte[] block=new byte[8192];int n;while((n=extraRoot.read(block))!=-1){if(pem.size()+n>1024*1024)throw new IOException("Almacén de certificados demasiado grande");pem.write(block,0,n);}java.util.regex.Matcher certs=java.util.regex.Pattern.compile("-----BEGIN CERTIFICATE-----[\\s\\S]*?-----END CERTIFICATE-----").matcher(pem.toString("US-ASCII"));int index=0;CertificateFactory cf=CertificateFactory.getInstance("X.509");while(certs.find())try{ks.setCertificateEntry("ca"+(index++),cf.generateCertificate(new ByteArrayInputStream((certs.group()+"\n").getBytes("US-ASCII"))));}catch(CertificateException unsupported){}if(index==0)throw new CertificateException("No hay certificados utilizables");}finally{extraRoot.close();}
  tm.init(ks);final X509TrustManager extra=(X509TrustManager)tm.getTrustManagers()[0];
  X509TrustManager trust=new X509TrustManager(){
   public X509Certificate[] getAcceptedIssuers(){return system.getAcceptedIssuers();}
   public void checkClientTrusted(X509Certificate[] c,String a)throws CertificateException{system.checkClientTrusted(c,a);}
   public void checkServerTrusted(X509Certificate[] c,String a)throws CertificateException{try{system.checkServerTrusted(c,a);}catch(CertificateException e){extra.checkServerTrusted(c,a);}}
  };
  trustManager=trust;
  SSLContext ctx=SSLContext.getInstance("TLS");ctx.init(null,new TrustManager[]{trust},null);factory=new TLSFactory(ctx.getSocketFactory());
 }
 public static URL safe(String raw)throws Exception{
  URL u=new URL(raw);if(!u.getProtocol().equals("https")&&!u.getProtocol().equals("http"))throw new IOException("Solo enlaces HTTP o HTTPS públicos");
  if(u.getUserInfo()!=null)throw new IOException("No se admiten credenciales en el enlace");return u;
 }
 public static HttpURLConnection open(String raw,String range)throws Exception{
  if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Solicitud cancelada");
  java.util.concurrent.CountDownLatch latch=ready;if(latch!=null&&!latch.await(30,java.util.concurrent.TimeUnit.SECONDS))throw new IOException("La configuración de certificados no terminó a tiempo");
  URL u=safe(raw);
  for(int i=0;i<7;i++){
   if(trustManager==null)init(null);HttpURLConnection c=u.getProtocol().equals("https")?new ModernHttps(u,trustManager):(HttpURLConnection)u.openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);
   c.setRequestProperty("User-Agent","OuyaLibre/0.1 (public video search)");c.setRequestProperty("Accept-Encoding","identity");if(range!=null)c.setRequestProperty("Range",range);
   if(c instanceof HttpsURLConnection && factory!=null)((HttpsURLConnection)c).setSSLSocketFactory(factory);
   int status=c.getResponseCode();
   if(status==301||status==302||status==303||status==307||status==308){String loc=c.getHeaderField("Location");c.disconnect();if(loc==null)throw new IOException("Redirección vacía");u=safe(new URL(u,loc).toString());continue;}
   if(status!=200&&status!=206){c.disconnect();throw new IOException("HTTP "+status+" en "+u.getHost());}
   return c;
  }throw new IOException("Demasiadas redirecciones");
 }
 public static byte[] bytes(String url,int max)throws Exception{
  HttpURLConnection c=open(url,null);try{InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
   while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Lectura cancelada");if(out.size()+n>max)throw new IOException("Respuesta demasiado grande");out.write(b,0,n);}return out.toByteArray();
  }finally{c.disconnect();}
 }
 public static String text(String url)throws Exception{return new String(bytes(url,4*1024*1024),"UTF-8");}
 private static final class TLSFactory extends SSLSocketFactory{
  private final SSLSocketFactory f;TLSFactory(SSLSocketFactory x){f=x;}
  private Socket enable(Socket s){if(s instanceof SSLSocket){SSLSocket ssl=(SSLSocket)s;List<String> p=new ArrayList<String>();for(String v:ssl.getSupportedProtocols())if(v.equals("TLSv1.2")||v.equals("TLSv1.3"))p.add(v);if(!p.isEmpty())ssl.setEnabledProtocols(p.toArray(new String[0]));}return s;}
  public String[] getDefaultCipherSuites(){return f.getDefaultCipherSuites();} public String[] getSupportedCipherSuites(){return f.getSupportedCipherSuites();}
  public Socket createSocket()throws IOException{return enable(f.createSocket());}
  public Socket createSocket(Socket s,String h,int p,boolean a)throws IOException{return enable(f.createSocket(s,h,p,a));}
  public Socket createSocket(String h,int p)throws IOException{return enable(f.createSocket(h,p));}
  public Socket createSocket(String h,int p,InetAddress l,int q)throws IOException{return enable(f.createSocket(h,p,l,q));}
  public Socket createSocket(InetAddress h,int p)throws IOException{return enable(f.createSocket(h,p));}
  public Socket createSocket(InetAddress h,int p,InetAddress l,int q)throws IOException{return enable(f.createSocket(h,p,l,q));}
 }
}
