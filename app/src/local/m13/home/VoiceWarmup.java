package local.m13.home;
import android.content.*;
import android.os.SystemClock;
/** Prepare the offline engine while the launcher is visible; never open audio. */
public final class VoiceWarmup {
 private static long last=-30000;
 public static void request(Context c){long now=SystemClock.elapsedRealtime();if(now-last<15000)return;last=now;
  final Context app=c.getApplicationContext();new Thread(new Runnable(){public void run(){try{LibreTvNet.init(app);LibreTvTitleIndex.get(app);}catch(Exception ignored){}}},"m13-search-preparation").start();
  try{if(c.startService(new Intent().setClassName("local.m13.voice","local.m13.voice.EngineWarmService").putExtra("prewarm",true))==null)last=-30000;}catch(Exception unavailable){last=-30000;android.util.Log.w("M13VoiceWarmup","Voice preparation unavailable",unavailable);}
 }
}
