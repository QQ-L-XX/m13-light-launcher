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
    private final Handler ui=new Handler();private final File directory;private volatile boolean closed;
    public final AtomicInteger loaded=new AtomicInteger();
    public LibreTvPosters(Context c){directory=new File(c.getCacheDir(),"posters");directory.mkdirs();}
    private File file(String key)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(key.getBytes("UTF-8"));StringBuilder n=new StringBuilder();for(byte b:bytes)n.append(String.format(Locale.US,"%02x",b&255));return new File(directory,n.toString());}
    private Bitmap decode(byte[] bytes){BitmapFactory.Options b=new BitmapFactory.Options();b.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,b);if(b.outWidth<=0||b.outHeight<=0)return null;b.inSampleSize=1;while(b.outWidth/b.inSampleSize>440||b.outHeight/b.inSampleSize>640)b.inSampleSize*=2;b.inJustDecodeBounds=false;b.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,b);}
    public void bind(final ImageView view,final String url){view.setTag(url);view.setImageDrawable(null);if(url==null||url.isEmpty())return;Bitmap hit=memory.get(url);if(hit!=null){view.setImageBitmap(hit);return;}if(closed)return;
        workers.execute(new Runnable(){public void run(){try{
            Bitmap cached=memory.get(url);if(cached==null){File f=file(url);byte[] bytes;
                if(f.exists()){FileInputStream in=new FileInputStream(f);try{bytes=LibreTvNet.read(in,4*1024*1024);}finally{in.close();}f.setLastModified(System.currentTimeMillis());}
                else{HttpURLConnection h=LibreTvNet.open(url,null);try{if(h.getResponseCode()!=200)return;bytes=LibreTvNet.read(h.getInputStream(),4*1024*1024);}finally{h.disconnect();}FileOutputStream out=new FileOutputStream(f);try{out.write(bytes);}finally{out.close();}trim();}
                cached=decode(bytes);if(cached==null){f.delete();return;}memory.put(url,cached);loaded.incrementAndGet();}
            final Bitmap image=cached;ui.post(new Runnable(){public void run(){if(!closed&&url.equals(view.getTag()))view.setImageBitmap(image);}});
        }catch(Exception ignored){}}});
    }
    private synchronized void trim(){File[] a=directory.listFiles();if(a==null)return;long size=0;for(File f:a)size+=f.length();if(size<24*1024*1024)return;Arrays.sort(a,new Comparator<File>(){public int compare(File a,File b){return Long.compare(a.lastModified(),b.lastModified());}});for(File f:a){if(size<18*1024*1024)break;size-=f.length();f.delete();}}
    public void clear(){memory.evictAll();File[] a=directory.listFiles();if(a!=null)for(File f:a)f.delete();}
    public void close(){closed=true;workers.shutdownNow();ui.removeCallbacksAndMessages(null);memory.evictAll();}
}
