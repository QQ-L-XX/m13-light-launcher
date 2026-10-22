package local.m13.home;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** On-box playback; metadata queues are local, TLS relay binds loopback only. */
public final class MusicService extends Service implements AudioManager.OnAudioFocusChangeListener {
    private static MusicService live;
    public static MusicService current(){return live;}
    private final Handler ui=new Handler();private final ExecutorService network=Executors.newSingleThreadExecutor();
    private MediaPlayer player;private LibreTvLocalProxy relay;private AudioManager audio;private MediaSession session;
    private JSONArray queue=new JSONArray();private int index=-1,generation;private JSONObject song=new JSONObject();
    private List<MusicLyrics.Line> lyrics=Collections.emptyList();private String state="stopped",error="";
    private long declaredMs;private boolean prepared,resumeOnFocus;private int resumeSeek;
    private final Runnable checkpoint=new Runnable(){public void run(){if(prepared)savePosition();ui.postDelayed(this,5000);}};
    public void onCreate(){super.onCreate();NeteaseAccount.init(this);live=this;audio=(AudioManager)getSystemService(AUDIO_SERVICE);session=new MediaSession(this,"M13 Music");session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback(){public void onPlay(){resume();}public void onPause(){pause();}public void onSkipToNext(){next(1);}public void onSkipToPrevious(){next(-1);}public void onStop(){stopMusic();}public void onSeekTo(long p){seek((int)p);}});session.setActive(true);ui.post(checkpoint);
    }
    public IBinder onBind(Intent i){return null;}
    public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        String action=intent.getAction();
        if("play".equals(action))try{queue=new JSONArray(intent.getStringExtra("queue"));index=intent.getIntExtra("index",0);load(intent.getIntExtra("resume",0));}catch(Exception e){fail("歌曲列表无效");}
        else if("toggle".equals(action))toggle();else if("next".equals(action))next(1);else if("previous".equals(action))next(-1);else if("stop".equals(action))stopMusic();
        return START_NOT_STICKY;
    }
    public static void play(Context c,JSONArray queue,int index,int resume){c.startService(new Intent(c,MusicService.class).setAction("play").putExtra("queue",queue.toString()).putExtra("index",index).putExtra("resume",resume));}
    public static void command(Context c,String command){if(live!=null)c.startService(new Intent(c,MusicService.class).setAction(command));}
    private void releasePlayer(){prepared=false;if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}if(relay!=null){relay.close();relay=null;}}
    private void load(int seek){
        if(index<0||index>=queue.length())return;savePosition();releasePlayer();lyrics=Collections.emptyList();song=queue.optJSONObject(index);if(song==null){fail("歌曲信息不完整");return;}
        state="loading";error="";resumeSeek=Math.max(0,seek);final int request=++generation;final JSONObject selected=song;notifyState();
        network.submit(new Runnable(){public void run(){try{
            LibreTvNet.init(MusicService.this);final MusicApi.Stream stream=MusicApi.resolve(selected);
            ui.post(new Runnable(){public void run(){if(request!=generation)return;prepare(stream,request);}});
            try{final List<MusicLyrics.Line> lines=MusicLyrics.parse(MusicApi.lyrics(selected));ui.post(new Runnable(){public void run(){if(request==generation)lyrics=lines;}});}catch(Exception ignored){}
        }catch(final Exception e){ui.post(new Runnable(){public void run(){if(request==generation)fail(e.getMessage()==null?"歌曲暂不可播":e.getMessage());}});}}});
    }
    private void prepare(MusicApi.Stream stream,final int request){try{
        declaredMs=stream.declaredMs;relay=new LibreTvLocalProxy();final MediaPlayer fresh=new MediaPlayer();player=fresh;
        fresh.setAudioStreamType(AudioManager.STREAM_MUSIC);fresh.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
        fresh.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){public void onPrepared(MediaPlayer p){if(request!=generation||p!=player)return;prepared=true;
            // Shortened previews are never presented as full tracks.
            if(declaredMs>90000&&p.getDuration()>0&&p.getDuration()+8000<declaredMs){fail("音乐源只返回了片段，请换一首");return;}
            try{if(resumeSeek>0&&resumeSeek<p.getDuration()-2000)p.seekTo(resumeSeek);resumeSeek=0;new MusicStore(MusicService.this).played(song);resume();}catch(Exception e){fail("音乐未能开始播放");}
        }});
        fresh.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){public void onCompletion(MediaPlayer p){if(request==generation)next(1);}});
        fresh.setOnErrorListener(new MediaPlayer.OnErrorListener(){public boolean onError(MediaPlayer p,int what,int extra){if(request==generation)fail("音频播放失败（"+what+" / "+extra+"），请换一首");return true;}});
        fresh.setDataSource(relay.url(stream.url));fresh.prepareAsync();
    }catch(Exception e){fail("音乐源连接失败，请重试或换一首");}}
    public void toggle(){if("playing".equals(state))pause();else if(prepared)resume();else if(index>=0)load(0);}
    public void resume(){if(!prepared||player==null)return;int focus=audio.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);if(focus!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){state="paused";error="音频正被其他应用使用";notifyState();return;}player.setVolume(1,1);player.start();state="playing";error="";resumeOnFocus=false;notifyState();}
    public void pause(){if(prepared&&player!=null){player.pause();state="paused";savePosition();notifyState();}}
    public void seek(int milliseconds){if(prepared&&player!=null)player.seekTo(Math.max(0,Math.min(milliseconds,Math.max(0,player.getDuration()-1000))));}
    public void next(int direction){if(queue.length()==0)return;index=(index+direction+queue.length())%queue.length();load(0);}
    public void stopMusic(){savePosition();generation++;releasePlayer();state="stopped";error="";audio.abandonAudioFocus(this);stopForeground(true);stopSelf();}
    private void fail(String message){releasePlayer();state="error";error=message;audio.abandonAudioFocus(this);stopForeground(true);notifyState();}
    public void onAudioFocusChange(int value){if(value==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){if(player!=null&&prepared)player.setVolume(.18f,.18f);}else if(value==AudioManager.AUDIOFOCUS_LOSS||value==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){boolean playing="playing".equals(state);pause();resumeOnFocus=value==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT&&playing;}else if(value==AudioManager.AUDIOFOCUS_GAIN){if(prepared&&player!=null)player.setVolume(1,1);if(resumeOnFocus)resume();}}
    private PendingIntent command(String action,int id){return PendingIntent.getService(this,id,new Intent(this,MusicService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT);}
    private void notifyState(){
        session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,song.optString("name")).putString(MediaMetadata.METADATA_KEY_ARTIST,song.optString("artist")).putLong(MediaMetadata.METADATA_KEY_DURATION,duration()).build());
        int playback="playing".equals(state)?PlaybackState.STATE_PLAYING:"loading".equals(state)?PlaybackState.STATE_BUFFERING:PlaybackState.STATE_PAUSED;
        session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_STOP|PlaybackState.ACTION_SEEK_TO).setState(playback,position(),"playing".equals(state)?1:0).build());
        if("stopped".equals(state)||"error".equals(state))return;
        Notification n=new Notification.Builder(this).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(song.optString("name","轻音乐")).setContentText(song.optString("artist")+" · "+("playing".equals(state)?"播放中":"loading".equals(state)?"连接中":"已暂停")).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MusicActivity.class),PendingIntent.FLAG_UPDATE_CURRENT)).setOngoing("playing".equals(state)||"loading".equals(state)).addAction(android.R.drawable.ic_media_previous,"上一首",command("previous",1)).addAction(android.R.drawable.ic_media_pause,"播放 / 暂停",command("toggle",2)).addAction(android.R.drawable.ic_menu_close_clear_cancel,"停止",command("stop",3)).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).build();
        startForeground(121,n);
    }
    public int position(){try{return prepared?player.getCurrentPosition():0;}catch(Exception e){return 0;}}
    public int duration(){try{return prepared?player.getDuration():0;}catch(Exception e){return 0;}}
    public JSONObject song(){return song;}
    public List<MusicLyrics.Line> lyrics(){return lyrics;}
    public String state(){return state;}
    public String error(){return error;}
    private void savePosition(){if(prepared)getSharedPreferences("music-local-v1",0).edit().putInt("resumePosition",position()).putString("resumeSong",song.toString()).apply();}
    public static JSONObject diagnostics(){try{MusicService s=live;if(s==null)return new JSONObject().put("state","stopped");return new JSONObject().put("state",s.state).put("title",s.song.optString("name")).put("source",s.song.optString("source")).put("id",s.song.optString("id")).put("position",s.position()).put("duration",s.duration()).put("declaredDuration",s.declaredMs).put("lyrics",s.lyrics.size()).put("queueSize",s.queue.length()).put("index",s.index).put("relayRequests",s.relay==null?0:s.relay.requestCount()).put("upstreamHosts",s.relay==null?"":s.relay.upstreamHosts()).put("relayError",s.relay==null?"":s.relay.lastError()).put("error",s.error).put("standalone",true);}catch(Exception e){return new JSONObject();}}
    public void onDestroy(){savePosition();generation++;ui.removeCallbacksAndMessages(null);network.shutdownNow();releasePlayer();audio.abandonAudioFocus(this);session.release();live=null;super.onDestroy();}
}
