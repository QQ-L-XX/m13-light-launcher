package local.m13.home;

import android.content.*;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Xml;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

/** User-enabled, LAN-only UPnP AV receiver. No media passes through a PC. */
public final class CastServer implements Closeable {
 public static final String NAME="轻桌面 M13";
 private static CastServer current;
 public static synchronized CastServer get(){return current;}
 public static synchronized CastServer start(Context c)throws Exception{if(current==null||current.closed)current=new CastServer(c);return current;}
 public static synchronized void stop(){if(current!=null)current.close();current=null;}
 public static String status(){try{CastServer s=get();return s==null?new JSONObject().put("enabled",false).toString():new JSONObject().put("enabled",!s.closed).put("name",NAME).put("ip",s.ip).put("port",s.http.getLocalPort()).put("state",s.state).put("positionMs",s.position).put("durationMs",s.duration).put("title",s.title).put("error",s.error).put("searchResponses",s.responses).put("controlRequests",s.controls).put("subscriptions",s.subs.size()).toString();}catch(Exception e){return "{}";}}
 final Context context;final String ip,uuid;private final int prefix;private final InetAddress address;
 private final NetworkInterface network;private final ServerSocket http;private final MulticastSocket ssdp;
 private WifiManager.MulticastLock multicast;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ThreadPoolExecutor workers=new ThreadPoolExecutor(4,4,20,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(24));
 private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();
 private final Map<String,Sub> subs=Collections.synchronizedMap(new HashMap<String,Sub>());
 volatile boolean closed,muted;volatile String uri="",metadata="",title="手机投屏",state="NO_MEDIA_PRESENT",error="";volatile int position,duration,responses,controls;
 private static final String AV="urn:schemas-upnp-org:service:AVTransport:1",RC="urn:schemas-upnp-org:service:RenderingControl:1",CM="urn:schemas-upnp-org:service:ConnectionManager:1";
 private static final String PROTOCOLS="http-get:*:video/mp4:*,http-get:*:video/mpeg:*,http-get:*:video/mp2t:*,http-get:*:application/vnd.apple.mpegurl:*,http-get:*:application/x-mpegURL:*,http-get:*:audio/mpeg:*";
 private static class Sub{String id,path;URL url;long expires;int seq;}
 private CastServer(Context c)throws Exception{
  context=c.getApplicationContext();InetAddress selected=null;NetworkInterface net=null;int mask=24;
  for(NetworkInterface n:Collections.list(NetworkInterface.getNetworkInterfaces())){if(!n.isUp()||n.isLoopback())continue;for(InterfaceAddress a:n.getInterfaceAddresses())if(a.getAddress() instanceof Inet4Address&&a.getAddress().isSiteLocalAddress()){selected=a.getAddress();net=n;mask=a.getNetworkPrefixLength();if(n.getName().startsWith("wlan")||n.getName().startsWith("eth"))break;}if(net!=null)break;}
  if(selected==null)throw new IOException("请先连接家庭 Wi-Fi 或有线网络");address=selected;network=net;prefix=mask;ip=address.getHostAddress();
  SharedPreferences p=context.getSharedPreferences("cast",0);String id=p.getString("uuid","");if(id.isEmpty()){id=UUID.randomUUID().toString();p.edit().putString("uuid",id).commit();}uuid="uuid:"+id;
  ServerSocket h=null;MulticastSocket m=null;try{h=new ServerSocket(0,8,address);m=new MulticastSocket(null);m.setReuseAddress(true);m.bind(new InetSocketAddress(1900));m.setNetworkInterface(network);m.setTimeToLive(2);m.joinGroup(InetAddress.getByName("239.255.255.250"));
   WifiManager wifi=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);if(wifi!=null){multicast=wifi.createMulticastLock("m13-cast");multicast.setReferenceCounted(false);multicast.acquire();}
  }catch(Exception e){if(h!=null)try{h.close();}catch(Exception ignored){}if(m!=null)m.close();if(multicast!=null&&multicast.isHeld())multicast.release();workers.shutdownNow();timer.shutdownNow();throw e;}
  http=h;ssdp=m;
  new Thread(new Runnable(){public void run(){accept();}},"M13-cast-http").start();
  new Thread(new Runnable(){public void run(){discover();}},"M13-cast-ssdp").start();
  timer.scheduleAtFixedRate(new Runnable(){public void run(){advertise("ssdp:alive");}},0,60,TimeUnit.SECONDS);
 }
 boolean local(InetAddress a){byte[] x=address.getAddress(),y=a.getAddress();if(y.length!=4||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isMulticastAddress())return false;for(int bit=0;bit<prefix;bit++)if((x[bit/8]&(128>>(bit%8)))!=(y[bit/8]&(128>>(bit%8))))return false;return true;}
 static String escape(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
 static String time(int ms){int s=Math.max(0,ms/1000);return String.format(Locale.US,"%02d:%02d:%02d",s/3600,s/60%60,s%60);}
 private String location(){return "http://"+ip+":"+http.getLocalPort()+"/device.xml";}
 private String[] types(){return new String[]{"upnp:rootdevice",uuid,"urn:schemas-upnp-org:device:MediaRenderer:1",AV,RC,CM};}
 private String usn(String type){return uuid+(type.equals(uuid)?"":"::"+type);}
 private void advertise(String nts){for(String type:types())try{String s="NOTIFY * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nCACHE-CONTROL: max-age=180\r\nLOCATION: "+location()+"\r\nNT: "+type+"\r\nNTS: "+nts+"\r\nUSN: "+usn(type)+"\r\nSERVER: Android/5.1 UPnP/1.0 M13Home/2.18\r\n\r\n";byte[] b=s.getBytes("UTF-8");ssdp.send(new DatagramPacket(b,b.length,InetAddress.getByName("239.255.255.250"),1900));}catch(Exception ignored){}}
 private void discover(){byte[] buffer=new byte[8192];long last=0;while(!closed)try{DatagramPacket p=new DatagramPacket(buffer,buffer.length);ssdp.receive(p);if(!local(p.getAddress()))continue;String request=new String(p.getData(),p.getOffset(),p.getLength(),"UTF-8");if(!request.startsWith("M-SEARCH ")||!request.toLowerCase(Locale.US).contains("ssdp:discover"))continue;Map<String,String> h=headers(request);String st=h.get("st");if(st==null||System.currentTimeMillis()-last<200)continue;last=System.currentTimeMillis();for(String type:types())if(st.equals("ssdp:all")||st.equals(type)){String s="HTTP/1.1 200 OK\r\nCACHE-CONTROL: max-age=180\r\nEXT:\r\nLOCATION: "+location()+"\r\nSERVER: Android/5.1 UPnP/1.0 M13Home/2.18\r\nST: "+type+"\r\nUSN: "+usn(type)+"\r\n\r\n";byte[] b=s.getBytes("UTF-8");ssdp.send(new DatagramPacket(b,b.length,p.getAddress(),p.getPort()));responses++;}}catch(Exception e){if(!closed)error="设备发现暂时中断";}}
 private static Map<String,String> headers(String text){Map<String,String> h=new HashMap<String,String>();for(String row:text.split("\r?\n")){int n=row.indexOf(':');if(n>0)h.put(row.substring(0,n).trim().toLowerCase(Locale.US),row.substring(n+1).trim());}return h;}
 private void accept(){while(!closed)try{final Socket s=http.accept();if(!local(s.getInetAddress())){s.close();continue;}try{workers.execute(new Runnable(){public void run(){serve(s);}});}catch(RejectedExecutionException e){s.close();}}catch(Exception e){if(!closed)error="接收服务暂时中断";}}
 private String line(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();int c;while((c=in.read())!=-1&&c!=10){if(b.size()>8192)throw new IOException("Header too long");if(c!=13)b.write(c);}return b.toString("UTF-8");}
 private void reply(OutputStream out,int status,String content,String extra)throws Exception{byte[] b=content.getBytes("UTF-8");out.write(("HTTP/1.1 "+status+" "+(status<400?"OK":"Error")+"\r\nContent-Type: text/xml; charset=\"utf-8\"\r\nContent-Length: "+b.length+"\r\nConnection: close\r\nSERVER: Android/5.1 UPnP/1.0 M13Home/2.18\r\nEXT:\r\n"+extra+"\r\n").getBytes("US-ASCII"));out.write(b);out.flush();}
 private void serve(Socket socket){try{socket.setSoTimeout(5000);InputStream in=socket.getInputStream();OutputStream out=socket.getOutputStream();String[] first=line(in).split(" ");if(first.length!=3)return;String method=first[0],path=first[1];StringBuilder lines=new StringBuilder();for(int n=0;n<32;n++){String row=line(in);if(row.isEmpty())break;lines.append(row).append('\n');if(n==31)throw new IOException("Too many headers");}Map<String,String> h=headers(lines.toString());int length=Integer.parseInt(h.containsKey("content-length")?h.get("content-length"):"0");if(length<0||length>65536){reply(out,413,"","");return;}byte[] body=new byte[length];for(int p=0;p<length;){int n=in.read(body,p,length-p);if(n<0)throw new EOFException();p+=n;}
  if(method.equals("GET")){if(path.equals("/device.xml"))reply(out,200,device(),"");else if(path.matches("/(avtransport|renderingcontrol|connectionmanager)\\.xml")){InputStream asset=context.getAssets().open("cast"+path);try{reply(out,200,new String(LibreTvNet.read(asset,128*1024),"UTF-8"),"");}finally{asset.close();}}else reply(out,404,"","");}
  else if(method.equals("POST")&&path.matches("/control/(avtransport|renderingcontrol|connectionmanager)")){controls++;String action=h.get("soapaction");if(action==null||!action.contains("#")){reply(out,400,"","");return;}action=action.substring(action.indexOf('#')+1).replace("\"","");String service=path.endsWith("avtransport")?AV:path.endsWith("renderingcontrol")?RC:CM;try{Map<String,String> args=parse(new String(body,"UTF-8"));String result=action(service,action,args);reply(out,200,"<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:"+action+"Response xmlns:u=\""+service+"\">"+result+"</u:"+action+"Response></s:Body></s:Envelope>","");}catch(Exception e){reply(out,500,"<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><s:Fault><faultcode>s:Client</faultcode><faultstring>UPnPError</faultstring><detail><UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\"><errorCode>501</errorCode><errorDescription>Action Failed</errorDescription></UPnPError></detail></s:Fault></s:Body></s:Envelope>","");}}
  else if(method.equals("SUBSCRIBE")&&path.matches("/event/(avtransport|renderingcontrol|connectionmanager)")){subscribe(out,path,h);}
  else if(method.equals("UNSUBSCRIBE")){subs.remove(h.get("sid"));reply(out,200,"","");}else reply(out,405,"","");
 }catch(Exception ignored){}finally{try{socket.close();}catch(Exception ignored){}}}
 private static Map<String,String> parse(String xml)throws Exception{if(xml.contains("<!DOCTYPE")||xml.contains("<!ENTITY"))throw new IOException("DTD disabled");Map<String,String> values=new HashMap<String,String>();XmlPullParser p=Xml.newPullParser();p.setInput(new StringReader(xml));String tag="";int nodes=0;for(int e=p.getEventType();e!=XmlPullParser.END_DOCUMENT;e=p.next()){if(++nodes>2000)throw new IOException("Too many XML nodes");if(e==XmlPullParser.START_TAG)tag=p.getName();if(e==XmlPullParser.TEXT)values.put(tag,(values.containsKey(tag)?values.get(tag):"")+p.getText());}return values;}
 private static String tag(String key,String val){return "<"+key+">"+escape(val)+"</"+key+">";}
 private static String value(Map<String,String> a,String key,String fallback){return a.containsKey(key)?a.get(key):fallback;}
 private String action(String service,String action,Map<String,String> a)throws Exception{
  if(!service.equals(CM)&&!value(a,"InstanceID","0").equals("0"))throw new IOException("Instance not supported");
  if(service.equals(AV)){
   if(action.equals("SetAVTransportURI")){String next=value(a,"CurrentURI","");validateMedia(next);main.post(new Runnable(){public void run(){CastPlaybackActivity.command(context,"Unload",0);}});uri=next;metadata=value(a,"CurrentURIMetaData","");title="手机投屏";try{title=value(parse(metadata),"title",title);}catch(Exception ignored){}if(title.length()>100)title=title.substring(0,100);position=duration=0;state="STOPPED";error="";event();return "";}
   if(action.equals("Play")){if(uri.isEmpty())throw new IOException("No media");if(!value(a,"Speed","1").equals("1"))throw new IOException("Speed unsupported");state="TRANSITIONING";main.post(new Runnable(){public void run(){CastPlaybackActivity.command(context,"Play",0);}});event();return "";}
   if(action.equals("Pause")||action.equals("Stop")||action.equals("Seek")){final String cmd=action;final int seek=action.equals("Seek")?parseTime(value(a,"Target","0:0:0")):0;if(action.equals("Seek")&&!value(a,"Unit","REL_TIME").equals("REL_TIME"))throw new IOException("Seek mode unsupported");if(action.equals("Pause"))state="PAUSED_PLAYBACK";if(action.equals("Stop")){state="STOPPED";position=0;}main.post(new Runnable(){public void run(){CastPlaybackActivity.command(context,cmd,seek);}});event();return "";}
   if(action.equals("GetTransportInfo"))return tag("CurrentTransportState",state)+tag("CurrentTransportStatus",error.isEmpty()?"OK":"ERROR_OCCURRED")+tag("CurrentSpeed","1");
   if(action.equals("GetPositionInfo"))return tag("Track",uri.isEmpty()?"0":"1")+tag("TrackDuration",time(duration))+tag("TrackMetaData",metadata)+tag("TrackURI",uri)+tag("RelTime",time(position))+tag("AbsTime",time(position))+tag("RelCount","2147483647")+tag("AbsCount","2147483647");
   if(action.equals("GetMediaInfo"))return tag("NrTracks",uri.isEmpty()?"0":"1")+tag("MediaDuration",time(duration))+tag("CurrentURI",uri)+tag("CurrentURIMetaData",metadata)+tag("NextURI","")+tag("NextURIMetaData","")+tag("PlayMedium","NETWORK")+tag("RecordMedium","NOT_IMPLEMENTED")+tag("WriteStatus","NOT_IMPLEMENTED");
   if(action.equals("GetDeviceCapabilities"))return tag("PlayMedia","NETWORK")+tag("RecMedia","NOT_IMPLEMENTED")+tag("RecQualityModes","NOT_IMPLEMENTED");
   if(action.equals("GetTransportSettings"))return tag("PlayMode","NORMAL")+tag("RecQualityMode","NOT_IMPLEMENTED");
   if(action.equals("GetCurrentTransportActions"))return tag("Actions",uri.isEmpty()?"":"Play,Pause,Stop,Seek");
  }
  if(service.equals(RC)){
   AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
   if(action.equals("GetVolume"))return tag("CurrentVolume",String.valueOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)*100/Math.max(1,max)));
   if(action.equals("SetVolume")){int n=Math.max(0,Math.min(100,Integer.parseInt(value(a,"DesiredVolume","0"))));audio.setStreamVolume(AudioManager.STREAM_MUSIC,n*max/100,0);event();return "";}
   if(action.equals("GetMute"))return tag("CurrentMute",muted||audio.getStreamVolume(AudioManager.STREAM_MUSIC)==0?"1":"0");
   if(action.equals("SetMute")){boolean next=value(a,"DesiredMute","0").equals("1");if(next!=muted){audio.setStreamMute(AudioManager.STREAM_MUSIC,next);muted=next;}event();return "";}
   if(action.equals("ListPresets"))return tag("CurrentPresetNameList","FactoryDefaults");if(action.equals("SelectPreset"))return "";
  }
  if(service.equals(CM)){if(action.equals("GetProtocolInfo"))return tag("Source","")+tag("Sink",PROTOCOLS);if(action.equals("GetCurrentConnectionIDs"))return tag("ConnectionIDs","0");if(action.equals("GetCurrentConnectionInfo"))return tag("RcsID","0")+tag("AVTransportID","0")+tag("ProtocolInfo",PROTOCOLS.split(",")[0])+tag("PeerConnectionManager","")+tag("PeerConnectionID","-1")+tag("Direction","Input")+tag("Status","OK");}
  throw new IOException("Action unsupported");
 }
 private void validateMedia(String value)throws Exception{if(value.length()>8192)throw new IOException("URI too long");URL u=new URL(value);if(!(u.getProtocol().equals("http")||u.getProtocol().equals("https"))||u.getUserInfo()!=null)throw new IOException("Unsupported URI");for(InetAddress a:InetAddress.getAllByName(u.getHost()))if(a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isMulticastAddress()||(a.isSiteLocalAddress()&&!local(a)))throw new IOException("Media host not allowed");}
 private static int parseTime(String t)throws Exception{String[] parts=t.split(":");if(parts.length!=3)throw new IOException("Invalid time");long n=(Long.parseLong(parts[0])*3600+Long.parseLong(parts[1])*60+(long)Double.parseDouble(parts[2]))*1000;if(n<0||n>Integer.MAX_VALUE)throw new IOException("Invalid time");return (int)n;}
 private String device(){StringBuilder s=new StringBuilder("<?xml version=\"1.0\"?><root xmlns=\"urn:schemas-upnp-org:device-1-0\"><specVersion><major>1</major><minor>0</minor></specVersion><device><deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType><friendlyName>"+NAME+"</friendlyName><manufacturer>Local Home</manufacturer><modelName>M13 Minimal Home</modelName><modelNumber>2.7</modelNumber><UDN>"+uuid+"</UDN><serviceList>");String[] names={"avtransport","renderingcontrol","connectionmanager"},types={AV,RC,CM},ids={"AVTransport","RenderingControl","ConnectionManager"};for(int n=0;n<3;n++)s.append("<service><serviceType>").append(types[n]).append("</serviceType><serviceId>urn:upnp-org:serviceId:").append(ids[n]).append("</serviceId><SCPDURL>/").append(names[n]).append(".xml</SCPDURL><controlURL>/control/").append(names[n]).append("</controlURL><eventSubURL>/event/").append(names[n]).append("</eventSubURL></service>");return s.append("</serviceList></device></root>").toString();}
 private void subscribe(OutputStream out,String path,Map<String,String> h)throws Exception{Sub s;String sid=h.get("sid");if(sid!=null){s=subs.get(sid);if(s==null){reply(out,412,"","");return;}}else{String callback=h.get("callback");if(callback==null||!callback.startsWith("<")||!callback.endsWith(">")||!"upnp:event".equalsIgnoreCase(h.get("nt"))){reply(out,412,"","");return;}URL url=new URL(callback.substring(1,callback.length()-1));if(!url.getProtocol().equals("http")||url.getUserInfo()!=null)throw new IOException("Callback not allowed");for(InetAddress a:InetAddress.getAllByName(url.getHost()))if(!local(a))throw new IOException("Callback outside LAN");s=new Sub();s.id="uuid:"+UUID.randomUUID();s.path=path;s.url=url; synchronized(subs){Iterator<Sub> it=subs.values().iterator();while(it.hasNext())if(it.next().expires<System.currentTimeMillis())it.remove();if(subs.size()>=16){reply(out,503,"","");return;}subs.put(s.id,s);}}
  s.expires=System.currentTimeMillis()+1800000;reply(out,200,"","SID: "+s.id+"\r\nTIMEOUT: Second-1800\r\n");notifySub(s);
 }
 void event(){synchronized(subs){for(Sub s:new ArrayList<Sub>(subs.values()))if(s.expires>System.currentTimeMillis())notifySub(s);else subs.remove(s.id);}}
 private void notifySub(final Sub s){
  try{workers.execute(new Runnable(){public void run(){Socket socket=null;try{
   String props;
   if(s.path.endsWith("avtransport")){
    String last="<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/AVT/\"><InstanceID val=\"0\"><TransportState val=\""+state+"\"/><TransportStatus val=\""+(error.isEmpty()?"OK":"ERROR_OCCURRED")+"\"/><CurrentTransportActions val=\"Play,Pause,Stop,Seek\"/><AVTransportURI val=\""+escape(uri)+"\"/><CurrentTrackURI val=\""+escape(uri)+"\"/><CurrentTrackDuration val=\""+time(duration)+"\"/></InstanceID></Event>";
    props="<e:property>"+tag("LastChange",last)+"</e:property>";
   }else if(s.path.endsWith("renderingcontrol")){
    AudioManager a=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    String last="<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/RCS/\"><InstanceID val=\"0\"><Volume channel=\"Master\" val=\""+(a.getStreamVolume(3)*100/Math.max(1,a.getStreamMaxVolume(3)))+"\"/><Mute channel=\"Master\" val=\""+(muted||a.getStreamVolume(3)==0?1:0)+"\"/></InstanceID></Event>";
    props="<e:property>"+tag("LastChange",last)+"</e:property>";
   }else props="<e:property>"+tag("SourceProtocolInfo","")+"</e:property><e:property>"+tag("SinkProtocolInfo",PROTOCOLS)+"</e:property><e:property>"+tag("CurrentConnectionIDs","0")+"</e:property>";
   byte[] data=("<?xml version=\"1.0\"?><e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">"+props+"</e:propertyset>").getBytes("UTF-8");
   InetAddress peer=InetAddress.getByName(s.url.getHost());if(!local(peer))return;
   int port=s.url.getPort()<0?80:s.url.getPort();socket=new Socket();socket.connect(new InetSocketAddress(peer,port),2000);socket.setSoTimeout(2000);
   String path=s.url.getFile().isEmpty()?"/":s.url.getFile();int seq;synchronized(s){seq=s.seq++;}
   String headers="NOTIFY "+path+" HTTP/1.1\r\nHOST: "+s.url.getHost()+":"+port+"\r\nCONTENT-TYPE: text/xml; charset=\"utf-8\"\r\nNT: upnp:event\r\nNTS: upnp:propchange\r\nSID: "+s.id+"\r\nSEQ: "+seq+"\r\nCONTENT-LENGTH: "+data.length+"\r\nCONNECTION: close\r\n\r\n";
   OutputStream output=socket.getOutputStream();output.write(headers.getBytes("UTF-8"));output.write(data);output.flush();line(socket.getInputStream());
  }catch(Exception ignored){}finally{if(socket!=null)try{socket.close();}catch(Exception ignored){}}}});
  }catch(RejectedExecutionException ignored){}
 }
 public void close(){if(closed)return;advertise("ssdp:byebye");closed=true;timer.shutdownNow();ssdp.close();try{http.close();}catch(Exception ignored){}workers.shutdownNow();subs.clear();if(muted){((AudioManager)context.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_MUSIC,false);muted=false;}if(multicast!=null&&multicast.isHeld())multicast.release();main.post(new Runnable(){public void run(){CastPlaybackActivity.command(context,"Stop",0);}});}
}
