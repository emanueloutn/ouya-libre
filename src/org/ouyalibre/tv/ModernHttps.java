package org.ouyalibre.tv;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.security.cert.*;
import java.util.*;
import javax.net.ssl.X509TrustManager;
import org.ouyalibre.crypto.tls.*;
import org.ouyalibre.crypto.tls.crypto.TlsCertificate;
import org.ouyalibre.crypto.tls.crypto.impl.bc.BcTlsCrypto;

/** TLS 1.2 with modern ciphers, independent of Android 4.1's HTTP/TLS downgrade logic. */
final class ModernHttps extends HttpURLConnection {
 private final X509TrustManager trust;
 private final Map<String,String> request=new LinkedHashMap<String,String>();
 private final Map<String,List<String>> headers=new LinkedHashMap<String,List<String>>();
 private Socket socket;private InputStream input,body;private TlsClientProtocol protocol;
 ModernHttps(URL u,X509TrustManager t){super(u);trust=t;}
 public void setRequestProperty(String key,String value){if(connected)throw new IllegalStateException("Conexión iniciada");if(!key.matches("[A-Za-z0-9-]+")||value.indexOf('\r')>=0||value.indexOf('\n')>=0)throw new IllegalArgumentException("Cabecera no válida");request.put(key,value);}
 public void connect()throws IOException{
  if(connected)return;final String host=url.getHost();socket=new Socket();
  try{
   socket.connect(new InetSocketAddress(host,url.getPort()<0?443:url.getPort()),getConnectTimeout());socket.setSoTimeout(getReadTimeout());
   protocol=new TlsClientProtocol(socket.getInputStream(),socket.getOutputStream());
   protocol.connect(new DefaultTlsClient(new BcTlsCrypto(new SecureRandom())){
    protected ProtocolVersion[] getSupportedVersions(){return ProtocolVersion.TLSv12.only();}
    public Hashtable getClientExtensions()throws IOException{Hashtable ext=super.getClientExtensions();if(ext==null)ext=new Hashtable();if(!host.matches("[0-9.]+")&&host.indexOf(':')<0){Vector names=new Vector();names.addElement(new ServerName(NameType.host_name,host.getBytes("US-ASCII")));TlsExtensionsUtils.addServerNameExtensionClient(ext,names);}return ext;}
    public TlsAuthentication getAuthentication(){return new TlsAuthentication(){
     public void notifyServerCertificate(TlsServerCertificate server)throws IOException{
      try{TlsCertificate[] chain=server.getCertificate().getCertificateList();if(chain.length==0)throw new CertificateException("Certificado vacío");X509Certificate[] certs=new X509Certificate[chain.length];CertificateFactory factory=CertificateFactory.getInstance("X.509");for(int i=0;i<chain.length;i++)certs[i]=(X509Certificate)factory.generateCertificate(new ByteArrayInputStream(chain[i].getEncoded()));String type=certs[0].getPublicKey().getAlgorithm().equals("EC")?"ECDHE_ECDSA":"ECDHE_RSA";trust.checkServerTrusted(certs,type);verifyHost(host,certs[0]);}
      catch(Exception e){throw new IOException("No se pudo validar el certificado de "+host,e);}
     }
     public TlsCredentials getClientCredentials(CertificateRequest request){return null;}
    };}
   });
   String path;try{URI ascii=new URI(url.toURI().toASCIIString());path=ascii.getRawPath();if(path==null||path.length()==0)path="/";if(ascii.getRawQuery()!=null)path+="?"+ascii.getRawQuery();}catch(Exception e){throw new IOException("URL no válida",e);}
   String hostHeader=host+(url.getPort()>0&&url.getPort()!=443?":"+url.getPort():"");StringBuilder r=new StringBuilder("GET ").append(path).append(" HTTP/1.1\r\nHost: ").append(hostHeader).append("\r\nConnection: close\r\n");for(Map.Entry<String,String> h:request.entrySet())r.append(h.getKey()).append(": ").append(h.getValue()).append("\r\n");r.append("\r\n");protocol.getOutputStream().write(r.toString().getBytes("US-ASCII"));protocol.getOutputStream().flush();input=new BufferedInputStream(protocol.getInputStream(),16*1024);
   String first=line(input);if(first==null||!first.startsWith("HTTP/1."))throw new IOException("Respuesta HTTP no válida");String[] status=first.split(" ",3);if(status.length<2)throw new IOException("Estado HTTP vacío");responseCode=Integer.parseInt(status[1]);responseMessage=status.length>2?status[2]:"";
   String l;int count=0;while((l=line(input))!=null&&l.length()>0){if(++count>100)throw new IOException("Demasiadas cabeceras");int colon=l.indexOf(':');if(colon>0){String key=l.substring(0,colon).toLowerCase(Locale.US);List<String> values=headers.get(key);if(values==null){values=new ArrayList<String>();headers.put(key,values);}values.add(l.substring(colon+1).trim());}}
   connected=true;String transfer=getHeaderField("Transfer-Encoding"),length=getHeaderField("Content-Length");if(transfer!=null&&transfer.toLowerCase(Locale.US).contains("chunked"))body=new Chunked(input);else if(length!=null)body=new Limited(input,Long.parseLong(length));else body=input;
  }catch(Exception e){disconnect();if(e instanceof IOException)throw (IOException)e;throw new IOException("Error de conexión HTTPS",e);}
 }
 public int getResponseCode()throws IOException{connect();return responseCode;}
 public String getHeaderField(String key){try{connect();}catch(IOException e){return null;}List<String> values=headers.get(key.toLowerCase(Locale.US));return values==null||values.isEmpty()?null:values.get(values.size()-1);}
 public Map<String,List<String>> getHeaderFields(){try{connect();}catch(IOException e){}return Collections.unmodifiableMap(headers);}
 public InputStream getInputStream()throws IOException{connect();if(responseCode>=400)throw new IOException("HTTP "+responseCode);return body;}
 public void disconnect(){if(socket!=null)try{socket.close();}catch(IOException ignored){}socket=null;}
 public boolean usingProxy(){return false;}
 private static String line(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();int c;while((c=in.read())!=-1){if(c=='\n')break;if(c!='\r')b.write(c);if(b.size()>16384)throw new IOException("Cabecera demasiado larga");}return c==-1&&b.size()==0?null:b.toString("US-ASCII");}
 static void verifyHost(String host,X509Certificate cert)throws Exception{
  boolean ip=host.matches("[0-9.]+")||host.indexOf(':')>=0;Collection<List<?>> names=cert.getSubjectAlternativeNames();String normalized=host.toLowerCase(Locale.US);if(normalized.endsWith("."))normalized=normalized.substring(0,normalized.length()-1);
  if(names!=null)for(List<?> entry:names){int type=((Integer)entry.get(0)).intValue();Object value=entry.get(1);if(ip&&type==7&&value instanceof String&&InetAddress.getByName(host).equals(InetAddress.getByName((String)value)))return;if(!ip&&type==2&&value instanceof String){String pattern=((String)value).toLowerCase(Locale.US);if(normalized.equals(pattern))return;if(pattern.startsWith("*.")){String suffix=pattern.substring(1);if(normalized.endsWith(suffix)){String left=normalized.substring(0,normalized.length()-suffix.length());if(left.length()>0&&left.indexOf('.')<0)return;}}}}
  throw new CertificateException("El certificado no corresponde al servidor "+host);
 }
 private static final class Limited extends FilterInputStream{
  private long left;Limited(InputStream in,long size){super(in);left=size;}
  public int read()throws IOException{byte[] b=new byte[1];int n=read(b,0,1);return n<0?-1:b[0]&255;}
  public int read(byte[] b,int off,int len)throws IOException{if(len==0)return 0;if(left<=0)return -1;int n=in.read(b,off,(int)Math.min((long)len,left));if(n<0)throw new EOFException("La fuente cerró el vídeo antes de tiempo");left-=n;return n;}
 }
 private static final class Chunked extends FilterInputStream{
  private long left=0;private boolean done=false,first=true;Chunked(InputStream in){super(in);}
  public int read()throws IOException{byte[] b=new byte[1];int n=read(b,0,1);return n<0?-1:b[0]&255;}
  public int read(byte[] b,int off,int len)throws IOException{if(len==0)return 0;if(done)return -1;if(left==0){if(!first){String end=line(in);if(end==null||end.length()!=0)throw new IOException("Fin de bloque HTTP no válido");}first=false;String size=line(in);if(size==null)throw new EOFException("Bloque HTTP incompleto");int sep=size.indexOf(';');try{left=Long.parseLong((sep<0?size:size.substring(0,sep)).trim(),16);}catch(Exception e){throw new IOException("Tamaño de bloque no válido",e);}if(left<0)throw new IOException("Bloque negativo");if(left==0){done=true;int count=0;String trailer;while((trailer=line(in))!=null&&trailer.length()>0)if(++count>100)throw new IOException("Demasiadas cabeceras de cierre");return -1;}}
   int n=in.read(b,off,(int)Math.min((long)len,left));if(n<0)throw new EOFException("Bloque HTTP truncado");left-=n;return n;
  }
 }
}
