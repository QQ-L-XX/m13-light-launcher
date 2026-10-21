package local.m13.home;

import android.util.Base64;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Per-playback loopback server: no LAN listener, bounded workers, no PC dependency. */
public final class LibreTvLocalProxy implements Closeable {
    private final ServerSocket server;
    private final String secret=UUID.randomUUID().toString();
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(4,4,20,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(12));
    private final Set<Socket> clients=Collections.synchronizedSet(new HashSet<Socket>());
    private final Set<HttpURLConnection> upstreams=Collections.synchronizedSet(new HashSet<HttpURLConnection>());
    private volatile boolean closed;
    private final java.util.concurrent.atomic.AtomicInteger requests=new java.util.concurrent.atomic.AtomicInteger();
    private final Set<String> hosts=Collections.synchronizedSet(new HashSet<String>());
    private volatile String lastError="";
    public int requestCount(){return requests.get();}
    public String upstreamHosts(){synchronized(hosts){return hosts.toString();}}
    public String lastError(){return lastError;}
    private long length(HttpURLConnection h){try{return Long.parseLong(h.getHeaderField("Content-Length"));}catch(Exception e){return -1;}}
    public LibreTvLocalProxy()throws IOException{
        server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));
        Thread accept=new Thread(new Runnable(){public void run(){while(!closed)try{final Socket s=server.accept();clients.add(s);try{workers.execute(new Runnable(){public void run(){serve(s);}});}catch(RejectedExecutionException e){clients.remove(s);s.close();}}catch(IOException e){if(!closed)close();}}},"M13-local-media");accept.setDaemon(true);accept.start();
    }
    public String url(String target)throws Exception{return "http://127.0.0.1:"+server.getLocalPort()+"/"+secret+"/"+Base64.encodeToString(target.getBytes("UTF-8"),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);}
    private void headers(OutputStream out,int status,String type,long length)throws IOException{
        String h="HTTP/1.1 "+status+" "+(status<400?"OK":"Error")+"\r\nContent-Type: "+type+"\r\nConnection: close\r\nCache-Control: no-store\r\n"+(length>=0?"Content-Length: "+length+"\r\n":"")+"\r\n";out.write(h.getBytes("US-ASCII"));
    }
    private String line(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();int c;while((c=in.read())!=-1&&c!=10){if(b.size()>=8192)throw new IOException("Header too long");if(c!=13)b.write(c);}return b.toString("US-ASCII");}
    private void serve(Socket s){HttpURLConnection h=null;boolean sent=false;try{
        s.setSoTimeout(12000);InputStream input=s.getInputStream();OutputStream out=s.getOutputStream();String[] first=line(input).split(" ");if(first.length!=3||!(first[0].equals("GET")||first[0].equals("HEAD")))throw new IOException("Invalid method");
        String prefix="/"+secret+"/";if(!first[1].startsWith(prefix)){headers(out,403,"text/plain",0);return;}
        String range=null;int count=0;for(String x;(x=line(input)).length()>0;){if(++count>50)throw new IOException("Too many headers");if(x.toLowerCase(Locale.US).startsWith("range:"))range=x.substring(6).trim();}
        String target=new String(Base64.decode(first[1].substring(prefix.length()),Base64.URL_SAFE),"UTF-8");h=LibreTvNet.open(target,range);upstreams.add(h);requests.incrementAndGet();hosts.add(h.getURL().getHost());
        int status=h.getResponseCode();if(status!=200&&status!=206){headers(out,status,"text/plain",0);sent=true;return;}
        String type=h.getContentType();if(type==null)type="application/octet-stream";
        boolean playlist=type.toLowerCase(Locale.US).contains("mpegurl")||h.getURL().getPath().toLowerCase(Locale.US).endsWith(".m3u8");
        if(first[0].equals("HEAD")){headers(out,status,type,length(h));sent=true;return;}
        InputStream body=h.getInputStream();
        if(playlist){
            String text=new String(LibreTvNet.read(body,4*1024*1024),"UTF-8");if(!text.trim().startsWith("#EXTM3U"))throw new IOException("Invalid playlist");StringBuilder rewritten=new StringBuilder();
            Pattern uri=Pattern.compile("URI=\"([^\"]+)\"");
            for(String row:text.split("\\r?\\n")){
                if(row.startsWith("#")){Matcher m=uri.matcher(row);StringBuffer mapped=new StringBuffer();while(m.find())m.appendReplacement(mapped,Matcher.quoteReplacement("URI=\""+url(new URL(h.getURL(),m.group(1)).toString())+"\""));m.appendTail(mapped);rewritten.append(mapped);}
                else if(!row.trim().isEmpty())rewritten.append(url(new URL(h.getURL(),row.trim()).toString()));
                rewritten.append('\n');
            }
            byte[] bytes=rewritten.toString().getBytes("UTF-8");headers(out,200,"application/vnd.apple.mpegurl",bytes.length);sent=true;out.write(bytes);
        }else{
            byte[] prefixBytes=new byte[65536];int n=0,r;while(n<prefixBytes.length&&(r=body.read(prefixBytes,n,prefixBytes.length-n))!=-1)n+=r;
            int offset=0;if(type.startsWith("image/")){int found=-1;for(int i=0;i+376<n;i++)if(prefixBytes[i]==71&&prefixBytes[i+188]==71&&prefixBytes[i+376]==71){found=i;break;}if(found<0)throw new IOException("片源不是可播放的视频分片");offset=found;type="video/mp2t";}
            if(type.startsWith("text/html")||type.contains("javascript"))throw new IOException("片源返回了网页");
            long size=length(h);headers(out,status,type,size<0?-1:size-offset);sent=true;out.write(prefixBytes,offset,n-offset);byte[] buf=new byte[32768];while((r=body.read(buf))!=-1)out.write(buf,0,r);
        }out.flush();
    }catch(Exception e){if(!closed){lastError=e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()).replaceAll("https?://[^\\s]+","[source]");android.util.Log.e("M13LocalMedia",lastError);}if(!sent)try{headers(s.getOutputStream(),502,"text/plain",0);}catch(Exception ignored){}}
    finally{if(h!=null){upstreams.remove(h);h.disconnect();}clients.remove(s);try{s.close();}catch(IOException ignored){}}}
    public void close(){if(closed)return;closed=true;try{server.close();}catch(IOException ignored){}synchronized(clients){for(Socket s:clients)try{s.close();}catch(IOException ignored){}clients.clear();}workers.shutdownNow();final HttpURLConnection[] pending;synchronized(upstreams){pending=upstreams.toArray(new HttpURLConnection[0]);upstreams.clear();}Thread cleanup=new Thread(new Runnable(){public void run(){for(HttpURLConnection h:pending)h.disconnect();}},"M13-media-cleanup");cleanup.setDaemon(true);cleanup.start();}
}
