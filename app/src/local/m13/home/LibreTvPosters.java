package local.m13.home;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.*;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Only visible GridView cells request covers; both queues and decoded memory are bounded. */
public final class LibreTvPosters {
    private final LruCache<String,Bitmap> memory=new LruCache<String,Bitmap>(6*1024*1024){protected int sizeOf(String k,Bitmap v){return v.getByteCount();}};
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,10,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(40),new ThreadPoolExecutor.DiscardOldestPolicy());
    private final Handler ui=new Handler();private final File directory;private volatile boolean closed,paused;private final Object state=new Object();private final AtomicInteger generation=new AtomicInteger();
    private final Set<HttpURLConnection> connections=Collections.synchronizedSet(new HashSet<HttpURLConnection>());
    private final Set<ImageView> views=Collections.newSetFromMap(new WeakHashMap<ImageView,Boolean>());
    private static final Map<LibreTvPosters,Boolean> instances=new WeakHashMap<LibreTvPosters,Boolean>();
    public final AtomicInteger loaded=new AtomicInteger();
    public LibreTvPosters(Context c){synchronized(instances){instances.put(this,true);}directory=new File(c.getCacheDir(),"posters");directory.mkdirs();}
    private File file(String key)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(key.getBytes("UTF-8"));StringBuilder n=new StringBuilder();for(byte b:bytes)n.append(String.format(Locale.US,"%02x",b&255));return new File(directory,n.toString());}
    private Bitmap decode(byte[] bytes){BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,b);if(b.outWidth<=0||b.outHeight<=0)return null;b.inSampleSize=1;while(b.outWidth/b.inSampleSize>440||b.outHeight/b.inSampleSize>640)b.inSampleSize*=2;b.inJustDecodeBounds=false;b.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,b);}
    private boolean valid(int token){return !closed&&!paused&&token==generation.get();}
    public boolean isPaused(){return paused;}
    public void bind(final ImageView view,final String url){views.add(view);view.setTag(url);view.setImageDrawable(null);if(url==null||url.isEmpty()||closed||paused)return;Bitmap hit=memory.get(url);if(hit!=null){view.setImageBitmap(hit);return;}final int token=generation.get();
        workers.execute(new Runnable(){public void run(){if(!valid(token))return;final LibreTvNet.RequestControl control=new LibreTvNet.RequestControl(){public boolean cancelled(){return !valid(token);}public void opened(HttpURLConnection h){connections.add(h);if(!valid(token))h.disconnect();}public void finished(HttpURLConnection h){connections.remove(h);}};
          try{Bitmap cached=memory.get(url);if(cached==null){File f=file(url);byte[] bytes;
            if(f.exists()){FileInputStream in=new FileInputStream(f);try{bytes=LibreTvNet.read(in,4*1024*1024);}finally{in.close();}f.setLastModified(System.currentTimeMillis());}
            else{HttpURLConnection h=LibreTvNet.openPoster(url,control);try{if(h.getResponseCode()!=200||!valid(token))return;InputStream in=h.getInputStream();try{bytes=LibreTvNet.read(in,4*1024*1024);}finally{in.close();}}finally{control.finished(h);h.disconnect();}if(!valid(token))return;FileOutputStream out=new FileOutputStream(f);try{out.write(bytes);}finally{out.close();}trim();}
            if(!valid(token))return;cached=decode(bytes);if(cached==null){f.delete();return;}synchronized(state){if(!valid(token))return;memory.put(url,cached);loaded.incrementAndGet();}}
            final Bitmap image=cached;ui.post(new Runnable(){public void run(){if(valid(token)&&url.equals(view.getTag()))view.setImageBitmap(image);}});
          }catch(Exception ignored){}}});
    }
    public void pause(){synchronized(state){if(closed||paused)return;paused=true;generation.incrementAndGet();workers.getQueue().clear();memory.evictAll();}ui.removeCallbacksAndMessages(null);for(ImageView view:views)view.setImageDrawable(null);final HttpURLConnection[] active;synchronized(connections){active=connections.toArray(new HttpURLConnection[0]);}if(active.length>0)new Thread(new Runnable(){public void run(){for(HttpURLConnection h:active){h.disconnect();connections.remove(h);}}},"poster-pause").start();}
    public void resume(){synchronized(state){if(!closed&&paused){generation.incrementAndGet();paused=false;}}}
    public static org.json.JSONArray diagnostics()throws Exception{org.json.JSONArray result=new org.json.JSONArray();synchronized(instances){for(LibreTvPosters p:instances.keySet())if(!p.closed)result.put(new org.json.JSONObject().put("paused",p.paused).put("activeWorkers",p.workers.getActiveCount()).put("queued",p.workers.getQueue().size()).put("connections",p.connections.size()).put("cacheBytes",p.memory.size()).put("loaded",p.loaded.get()));}return result;}
    private synchronized void trim(){File[] a=directory.listFiles();if(a==null)return;long size=0;for(File f:a)size+=f.length();if(size<24*1024*1024)return;Arrays.sort(a,new Comparator<File>(){public int compare(File a,File b){return Long.compare(a.lastModified(),b.lastModified());}});for(File f:a){if(size<18*1024*1024)break;size-=f.length();f.delete();}}
    public void clear(){memory.evictAll();File[] a=directory.listFiles();if(a!=null)for(File f:a)f.delete();}
    public void close(){pause();closed=true;workers.shutdownNow();ui.removeCallbacksAndMessages(null);memory.evictAll();}
}
