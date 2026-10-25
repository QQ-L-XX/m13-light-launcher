package local.m13.home;
import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.net.*;
import java.util.concurrent.*;
import org.json.JSONObject;

public class CastPlaybackActivity extends Activity {
 private static java.lang.ref.WeakReference<CastPlaybackActivity> active=new java.lang.ref.WeakReference<CastPlaybackActivity>(null);
 private final Handler ui=new Handler();private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private VideoView video;private TextView label;private LibreTvLocalProxy relay;private CastServer server;private String loaded="";private boolean prepared,destroyed;private int width,height,generation;
 public static void command(Context c,String action,int position){CastPlaybackActivity a=active.get();CastServer s=CastServer.get();if(action.equals("Play")){if(s==null)return;ScreenPolicy.awaken(c);if(a!=null&&!a.isFinishing()){if(!s.uri.equals(a.loaded))a.load();else if(a.video!=null){a.video.start();s.state=a.prepared?"PLAYING":"TRANSITIONING";s.event();}}else c.startActivity(new Intent(c,CastPlaybackActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return;}if(a==null||a.video==null)return;if(action.equals("Pause")){a.video.pause();if(s!=null)s.state="PAUSED_PLAYBACK";}if(action.equals("Seek")){a.video.seekTo(position);if(s!=null)s.position=position;}if(action.equals("Unload")){a.finish();return;}if(action.equals("Stop")){if(s!=null){s.state="STOPPED";s.position=0;}a.finish();}}
 public static String diagnostics(){try{CastPlaybackActivity a=active.get();return a==null?"{}":new JSONObject().put("prepared",a.prepared).put("videoWidth",a.width).put("videoHeight",a.height).put("playing",a.video!=null&&a.video.isPlaying()).put("positionMs",a.video==null?0:a.video.getCurrentPosition()).put("localRelay",a.relay!=null).put("relayError",a.relay==null?"":a.relay.lastError()).put("relayRequests",a.relay==null?0:a.relay.requestCount()).put("upstreamHosts",a.relay==null?"":a.relay.upstreamHosts()).toString();}catch(Exception e){return "{}";}}
 private final Runnable tick=new Runnable(){public void run(){if(destroyed)return;if(server!=null&&video!=null&&prepared&&server.uri.equals(loaded)&&!server.state.equals("STOPPED")){server.position=video.getCurrentPosition();server.duration=video.getDuration();}ui.postDelayed(this,1000);}};
 public void onCreate(Bundle b){super.onCreate(b);active=new java.lang.ref.WeakReference<CastPlaybackActivity>(this);getWindow().setFlags(1024,1024);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);video=new VideoView(this);FrameLayout.LayoutParams vp=new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);root.addView(video,vp);label=new TextView(this);label.setTextSize(16);label.setTextColor(0xffedf0f5);label.setPadding(36,24,36,24);label.setBackgroundColor(0x99000000);root.addView(label,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));setContentView(root);load();ui.post(tick);}
 private void load(){server=CastServer.get();if(server==null||server.closed){finish();return;}prepared=false;loaded=server.uri;label.setText("正在播放 · "+server.title);label.setVisibility(View.VISIBLE);video.stopPlayback();if(relay!=null){relay.close();relay=null;}final int token=++generation;final String uri=loaded;final int resume=server.position;
  video.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){public void onPrepared(MediaPlayer p){prepared=true;width=p.getVideoWidth();height=p.getVideoHeight();server.duration=p.getDuration();p.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener(){public void onSeekComplete(MediaPlayer p){if(server!=null){server.position=p.getCurrentPosition();server.event();}}});if(resume>0)p.seekTo(Math.min(resume,Math.max(0,p.getDuration()-1000)));if(server.state.equals("PAUSED_PLAYBACK"))video.pause();else{video.start();server.state="PLAYING";}server.error="";server.event();label.setText(server.title+"  ·  确认键暂停 / 继续");ui.postDelayed(new Runnable(){public void run(){label.setVisibility(View.GONE);}},3500);}});
  video.setOnErrorListener(new MediaPlayer.OnErrorListener(){public boolean onError(MediaPlayer p,int what,int extra){if(server!=null){server.error="这个视频格式或地址暂时无法播放";server.state="STOPPED";server.event();}label.setText("这个视频暂时无法播放，请在手机上换一个视频。\n按返回键回到投屏前的页面");label.setVisibility(View.VISIBLE);return true;}});
  video.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){public void onCompletion(MediaPlayer p){if(server!=null){server.state="STOPPED";server.position=server.duration;server.event();}finish();}});
  worker.submit(new Runnable(){
   public void run(){
    LibreTvLocalProxy proxy=null;
    try{
     LibreTvNet.init(CastPlaybackActivity.this);URL u=new URL(uri);boolean local=false;
     for(InetAddress a:InetAddress.getAllByName(u.getHost()))local|=a.isSiteLocalAddress();
     String target=uri;
     if(!local){proxy=new LibreTvLocalProxy();target=proxy.url(uri);}
     final LibreTvLocalProxy selected=proxy;final String url=target;
     ui.post(new Runnable(){public void run(){
      if(destroyed||token!=generation){if(selected!=null)selected.close();return;}
      relay=selected;video.setVideoURI(Uri.parse(url));video.requestFocus();video.start();
     }});
    }catch(Exception e){
     if(proxy!=null)proxy.close();
     ui.post(new Runnable(){public void run(){
      if(destroyed||token!=generation)return;
      server.error="视频地址暂时无法连接";server.state="STOPPED";server.event();
      label.setText("视频地址暂时无法连接，请在手机上重试。\n按返回键回到投屏前的页面");
     }});
    }
   }
  });
 }
 public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN&&video!=null&&prepared){int k=e.getKeyCode();if(k==23||k==66||k==85){if(video.isPlaying()){video.pause();server.state="PAUSED_PLAYBACK";}else{video.start();server.state="PLAYING";}server.event();return true;}if(k==21||k==22){video.seekTo(Math.max(0,video.getCurrentPosition()+(k==22?10000:-10000)));return true;}}return super.dispatchKeyEvent(e);}
 public void onBackPressed(){if(server!=null){server.state="STOPPED";server.position=0;server.event();}finish();}
 protected void onPause(){if(video!=null&&prepared&&server!=null&&server.uri.equals(loaded)){if(!server.state.equals("STOPPED")){server.position=video.getCurrentPosition();server.state="PAUSED_PLAYBACK";}video.pause();server.event();}super.onPause();}
 protected void onStop(){super.onStop();if(video!=null)video.stopPlayback();prepared=false;if(relay!=null){relay.close();relay=null;}if(!isFinishing())finish();}
 protected void onDestroy(){destroyed=true;generation++;ui.removeCallbacksAndMessages(null);worker.shutdownNow();if(active.get()==this)active.clear();super.onDestroy();}
}
