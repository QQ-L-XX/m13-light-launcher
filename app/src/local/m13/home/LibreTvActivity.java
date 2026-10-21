package local.m13.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class LibreTvActivity extends Activity {
    private static java.lang.ref.WeakReference<LibreTvActivity> active=new java.lang.ref.WeakReference<LibreTvActivity>(null);
    private String sourceName="",playState="idle";
    private int videoWidth,videoHeight;
    private LibreTvLocalProxy localProxy;
    public static String diagnostics(){try{LibreTvActivity a=active.get();if(a==null)return "{}";JSONObject d=new JSONObject().put("state",a.playState).put("title",a.videoTitle).put("source",a.sourceName).put("videoWidth",a.videoWidth).put("videoHeight",a.videoHeight).put("standalone",a.api.standalone()).put("boxLocalProxy",a.localProxy!=null);if(a.localProxy!=null)d.put("localRequests",a.localProxy.requestCount()).put("upstreamHosts",a.localProxy.upstreamHosts()).put("localError",a.localProxy.lastError());if(a.video!=null)d.put("playing",a.video.isPlaying()).put("positionMs",a.video.getCurrentPosition()).put("durationMs",a.video.getDuration());return d.toString();}catch(Exception e){return "{}";}}
    private final Handler ui=new Handler();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LibreTvApi api;
    private LinearLayout root,list,searchBar;
    private TextView status;
    private EditText query;
    private VideoView video;
    private JSONArray sources=new JSONArray();
    private JSONObject detail;
    private String searchText="",videoTitle="";
    private boolean playing,destroyed;
    private boolean stoppedPlayback;
    private boolean libraryLaunch,resuming;
    private JSONObject historyItem;
    private long resumePosition;
    private int screenGeneration,episodeIndex;
    private final Runnable progress=new Runnable(){public void run(){
        if(playing&&video!=null){saveProgress();Log.i("M13LibreTV","playback playing="+video.isPlaying()+" position="+video.getCurrentPosition()+" duration="+video.getDuration()+" size="+video.getWidth()+"x"+video.getHeight());ui.postDelayed(this,10000);}
    }};
    public void onCreate(Bundle b){super.onCreate(b);active=new java.lang.ref.WeakReference<LibreTvActivity>(this);api=new LibreTvApi(this);String requested=getIntent().getStringExtra("query");if(requested!=null)searchText=requested;getWindow().setFlags(1024,1024);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);browse();
        String incoming=getIntent().getStringExtra("libraryDetail");if(incoming!=null)try{libraryLaunch=true;detail=new JSONObject(incoming);historyItem=new JSONObject(getIntent().getStringExtra("libraryItem"));sourceName=historyItem.optString("sourceName");resumePosition=getIntent().getLongExtra("libraryPosition",0);final int episode=getIntent().getIntExtra("libraryEpisode",0);status.setText("正在准备播放…");task(new Job(){public Object run()throws Exception{return api.status();}},new Done(){public void accept(Object o){play(episode);}});return;}catch(Exception e){libraryLaunch=false;}
        connect();}
    private int dp(float x){return(int)(getResources().getDisplayMetrics().density*x+0.5f);}
    private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;}
    private TextView button(String s,final Runnable action){
        final TextView b=label(s,17,0xffdde4ed);b.setGravity(Gravity.CENTER_VERTICAL);b.setPadding(dp(20),dp(12),dp(20),dp(12));b.setBackground(bg(0xff202a38));b.setFocusable(true);b.setClickable(true);b.setMinHeight(dp(52));
        b.setOnFocusChangeListener(new View.OnFocusChangeListener(){public void onFocusChange(View v,boolean f){b.setBackground(bg(f?0xffe6edf7:0xff202a38));b.setTextColor(f?0xff101722:0xffdde4ed);}});
        b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){action.run();}});return b;
    }
    private void addButton(String s,Runnable r){TextView b=button(s,r);b.setId(View.generateViewId());if(list.getChildCount()>0){View previous=list.getChildAt(list.getChildCount()-1);previous.setNextFocusDownId(b.getId());b.setNextFocusUpId(previous.getId());}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);list.addView(b,p);}
    private void browse(){
        screenGeneration++;playing=false;video=null;
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(42),dp(24),dp(42),dp(18));root.setBackgroundColor(0xff0a1019);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("LibreTV",32,0xfff7f9fd);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,dp(54),1));
        header.addView(button(api.standalone()?"播放设置":"连接设置",new Runnable(){public void run(){settings();}}));root.addView(header);
        root.addView(label("搜一部喜欢的，开始今晚的放松时光",15,0xff98a8bd),new LinearLayout.LayoutParams(-1,dp(35)));
        searchBar=new LinearLayout(this);searchBar.setGravity(Gravity.CENTER_VERTICAL);
        query=new EditText(this);query.setSingleLine(true);query.setTextColor(0xfff4f7fc);query.setHintTextColor(0xff8292a9);query.setTextSize(20);query.setHint("输入片名");query.setText(searchText);query.setInputType(InputType.TYPE_CLASS_TEXT);
        searchBar.addView(query,new LinearLayout.LayoutParams(0,dp(54),1));searchBar.addView(button("搜索",new Runnable(){public void run(){search();}}));root.addView(searchBar);
        query.setOnEditorActionListener(new TextView.OnEditorActionListener(){public boolean onEditorAction(TextView v,int id,KeyEvent event){if(event==null||event.getAction()==KeyEvent.ACTION_UP){search();return true;}return false;}});
        status=label("正在连接…",14,0xffa5b5ca);root.addView(status,new LinearLayout.LayoutParams(-1,dp(42)));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=label("方向键选择 · 确认键打开 · 返回键回到上一级    |    "+(api.standalone()?"盒子独立运行":"使用时请保持电脑开机"),12,0xff70839c);root.addView(hint,new LinearLayout.LayoutParams(-1,dp(26)));setContentView(root);
    }
    private interface Job{Object run()throws Exception;}
    private interface Done{void accept(Object value)throws Exception;}
    private void task(final Job job,final Done done){final int generation=screenGeneration;
        worker.submit(new Runnable(){public void run(){try{final Object value=job.run();ui.post(new Runnable(){public void run(){if(destroyed||generation!=screenGeneration)return;try{done.accept(value);}catch(Exception e){status.setText("处理失败，请重试");}}});}
        catch(final Exception e){ui.post(new Runnable(){public void run(){if(!destroyed&&generation==screenGeneration)status.setText("连接未完成："+friendly(e));}});}}});
    }
    private String friendly(Exception e){String m=e.getMessage();if(e instanceof java.net.SocketTimeoutException)return "请求超时，请重试";if(e instanceof java.net.ConnectException||e instanceof java.net.NoRouteToHostException)return api.standalone()?"请检查盒子的网络连接，或稍后更换来源重试":"请确认电脑开机、LibreTV 已启动，且盒子与电脑在同一网络";return m==null?"网络暂时不可用":m;}
    private void connect(){task(new Job(){public Object run()throws Exception{return api.status();}},new Done(){public void accept(Object value)throws Exception{
        sources=((JSONObject)value).optJSONArray("defaultSources");if(sources==null)sources=new JSONArray();status.setText((api.standalone()?"独立运行":"已连接")+" · "+sources.length()+" 个来源 · 输入片名开始搜索");history();if(!searchText.isEmpty())search();
    }});}
    private void history(){
        final SharedPreferences p=getSharedPreferences("libretv",0);final String last=p.getString("last_detail","");
        if(!last.isEmpty())try{final JSONObject saved=new JSONObject(last);addButton("上次观看 · "+saved.optString("title",saved.optString("name")),new Runnable(){public void run(){detail=saved;episodes();}});}catch(Exception ignored){}
    }
    private void search(){
        searchText=query.getText().toString().trim();if(searchText.isEmpty()){query.requestFocus();return;}
        screenGeneration++;detail=null;list.removeAllViews();status.setText("正在搜索「"+searchText+"」…");
        ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(),0);
        final String word=searchText;
        task(new Job(){public Object run()throws Exception{if(sources.length()==0){JSONObject s=api.status();sources=s.optJSONArray("defaultSources");if(sources==null)sources=new JSONArray();}return api.request("/api/search",new JSONObject().put("wd",word).put("sources",sources).put("filterAdult",true));}},new Done(){public void accept(Object o)throws Exception{
            JSONArray results=((JSONObject)o).optJSONArray("list");int count=results==null?0:results.length();status.setText(count==0?"没有找到结果，请换个片名或稍后重试":"找到 "+count+" 个结果"+(count>120?" · 显示前 120 项，可细化片名":""));
            java.util.List<JSONObject> ordered=new java.util.ArrayList<JSONObject>();for(int i=0;i<count;i++)ordered.add(results.getJSONObject(i));
            java.util.Collections.sort(ordered,new java.util.Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){boolean ax=a.optString("name").equals(word),bx=b.optString("name").equals(word);if(ax!=bx)return ax?-1:1;if(ax)return a.optString("sourceKey").compareTo(b.optString("sourceKey"));return 0;}});
            for(int i=0;i<Math.min(count,120);i++){final JSONObject item=ordered.get(i);addButton(item.optString("name")+"    "+item.optString("year")+"  ·  "+item.optString("sourceName"),new Runnable(){public void run(){loadDetail(item);}});}
            Log.i("M13LibreTV","search results="+count);if(list.getChildCount()>0)list.getChildAt(0).requestFocus();
        }});
    }
    private void loadDetail(final JSONObject item){screenGeneration++;sourceName=item.optString("sourceName");status.setText("正在读取剧集…");task(new Job(){public Object run()throws Exception{
        JSONObject source=null;for(int i=0;i<sources.length();i++)if(sources.getJSONObject(i).optString("key").equals(item.optString("sourceKey")))source=sources.getJSONObject(i);
        if(source==null)throw new Exception("来源已变化，请重新搜索");JSONObject data=api.request("/api/detail?id="+LibreTvApi.enc(item.getString("vodId"))+"&source="+LibreTvApi.enc(source.toString()),null);
        JSONObject info=data.optJSONObject("videoInfo");data.put("title",info==null?item.optString("name"):info.optString("title",item.optString("name")));return data;
    }},new Done(){public void accept(Object o){detail=(JSONObject)o;episodes();}});}
    private void episodes(){
        list.removeAllViews();JSONArray eps=detail.optJSONArray("episodes");int count=eps==null?0:eps.length();status.setText(detail.optString("title",detail.optString("name"))+" · "+count+" 集");
        for(int i=0;i<count;i++){final int n=i;addButton(count==1?"立即播放":"第 "+(i+1)+" 集",new Runnable(){public void run(){play(n);}});}
        addButton("返回搜索",new Runnable(){public void run(){detail=null;search();}});if(list.getChildCount()>0)list.getChildAt(0).requestFocus();
    }
    private void play(final int n){
        try{
            final JSONObject current=detail;final String target=current.getJSONArray("episodes").getString(n);final String url;
            if(api.standalone()){if(localProxy!=null)localProxy.close();localProxy=new LibreTvLocalProxy();url=localProxy.url(target);}else url=api.proxy(target);
            episodeIndex=n;videoTitle=current.optString("title",current.optString("name"));
            getSharedPreferences("libretv",0).edit().putString("last_detail",current.toString()).putInt("last_episode",n).apply();
            screenGeneration++;playing=true;playState="loading";videoWidth=videoHeight=0;
            FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(0xff000000);video=new VideoView(this);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);frame.addView(video,p);
            final TextView overlay=label("正在载入 · "+videoTitle,18,0xffeeeeee);overlay.setGravity(Gravity.CENTER);overlay.setBackgroundColor(0xc0000000);FrameLayout.LayoutParams op=new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM);frame.addView(overlay,op);setContentView(frame);
            MediaController controls=new MediaController(this);controls.setAnchorView(video);video.setMediaController(controls);
            video.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){public void onPrepared(MediaPlayer mp){playState="prepared";videoWidth=mp.getVideoWidth();videoHeight=mp.getVideoHeight();overlay.setText(videoTitle+"  ·  确认键暂停 / 继续，左右键快退 / 快进");ui.postDelayed(new Runnable(){public void run(){overlay.setVisibility(View.GONE);}},3500);resuming=resumePosition>0;
                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener(){public void onSeekComplete(MediaPlayer p){resuming=false;saveProgress();}});mp.start();if(resuming)mp.seekTo((int)Math.min(resumePosition,Math.max(0,mp.getDuration()-3000)));else saveProgress();Log.i("M13LibreTV","prepared duration="+mp.getDuration()+" video="+mp.getVideoWidth()+"x"+mp.getVideoHeight());ui.post(progress);}});
            video.setOnInfoListener(new MediaPlayer.OnInfoListener(){public boolean onInfo(MediaPlayer mp,int what,int extra){if(what==MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START){playState="rendering";videoWidth=mp.getVideoWidth();videoHeight=mp.getVideoHeight();Log.i("M13LibreTV","first video frame rendered");}return false;}});
            video.setOnErrorListener(new MediaPlayer.OnErrorListener(){public boolean onError(MediaPlayer mp,int what,int extra){playState="error "+what+"/"+extra;Log.e("M13LibreTV","playback error "+what+"/"+extra);if(libraryLaunch){saveProgress();new AlertDialog.Builder(LibreTvActivity.this).setMessage("这个片源暂时无法播放（"+what+"/"+extra+"），请返回后换一个来源。").setPositiveButton("返回",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){finish();}}).show();}else{leaveVideo();new AlertDialog.Builder(LibreTvActivity.this).setMessage("这个片源暂时无法播放（"+what+"/"+extra+"），请重试或换一个来源。").setPositiveButton("确定",null).show();}return true;}});
            video.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){public void onCompletion(MediaPlayer mp){saveProgress();if(libraryLaunch)finish();else leaveVideo();}});
            HashMap<String,String> headers=new HashMap<String,String>();
            if(!api.standalone()){java.net.CookieManager manager=new java.net.CookieManager();java.net.HttpCookie cookie=java.net.HttpCookie.parse(api.cookie()).get(0);cookie.setPath("/");manager.getCookieStore().add(new java.net.URI(api.server()),cookie);java.net.CookieHandler.setDefault(manager);headers.put("Cookie",api.cookie());}
            video.setVideoURI(Uri.parse(url),headers);video.requestFocus();
        }catch(Exception e){if(playing)leaveVideo();status.setText("播放未完成："+friendly(e));}
    }
    private void saveProgress(){if(historyItem!=null&&video!=null&&!resuming)try{LibreTvHistory.get(this).save(historyItem,episodeIndex,video.getCurrentPosition(),video.getDuration());}catch(RuntimeException e){Log.e("M13LibreTV","History write failed",e);}}
    private void leaveVideo(){saveProgress();ui.removeCallbacks(progress);if(video!=null)video.stopPlayback();if(localProxy!=null){localProxy.close();localProxy=null;}playing=false;browse();if(detail!=null)episodes();}
    public boolean dispatchKeyEvent(KeyEvent e){int key=e.getKeyCode();if(playing&&video!=null){boolean toggle=key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;boolean seek=key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT;if(toggle||seek){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){if(toggle){if(video.isPlaying())video.pause();else video.start();}else video.seekTo(Math.min(video.getDuration(),Math.max(0,video.getCurrentPosition()+(key==KeyEvent.KEYCODE_DPAD_RIGHT?10000:-10000))));}return true;}}return super.dispatchKeyEvent(e);}
    public void onBackPressed(){if(libraryLaunch){saveProgress();finish();return;}if(playing){leaveVideo();return;}if(detail!=null){detail=null;search();return;}super.onBackPressed();}
    protected void onResume(){super.onResume();if(stoppedPlayback){stoppedPlayback=false;browse();if(detail!=null)episodes();else connect();}}
    protected void onPause(){saveProgress();ui.removeCallbacks(progress);if(video!=null)video.pause();super.onPause();}
    protected void onStop(){if(playing){saveProgress();ui.removeCallbacks(progress);if(video!=null)video.stopPlayback();if(localProxy!=null){localProxy.close();localProxy=null;}video=null;playing=false;playState="idle";stoppedPlayback=true;}super.onStop();}
    protected void onDestroy(){destroyed=true;ui.removeCallbacksAndMessages(null);if(video!=null)video.stopPlayback();if(localProxy!=null){localProxy.close();localProxy=null;}worker.shutdownNow();super.onDestroy();}
    private void settings(){
        new AlertDialog.Builder(this).setTitle("运行方式").setSingleChoiceItems(new String[]{"盒子独立运行 · 无需电脑","电脑中转 · 使用原 LibreTV 服务"},api.standalone()?0:1,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){d.dismiss();if(which==0){getSharedPreferences("libretv",0).edit().putBoolean("standalone",true).apply();sources=new JSONArray();detail=null;browse();connect();}else serverSettings();}}).setNegativeButton("取消",null).show();
    }
    private void serverSettings(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(24),dp(8),dp(24),dp(8));
        final EditText address=new EditText(this);address.setSingleLine(true);address.setText(api.server());address.setHint("LibreTV 服务地址");form.addView(address);
        final EditText password=new EditText(this);password.setSingleLine(true);password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);password.setHint("访问密码（留空保留原密码）");form.addView(password);
        new AlertDialog.Builder(this).setTitle("连接电脑上的 LibreTV").setMessage("电脑需保持开机，并与盒子连接同一个路由器。").setView(form).setPositiveButton("连接",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{
            String server=LibreTvApi.validateServer(address.getText().toString());SharedPreferences.Editor edit=getSharedPreferences("libretv",0).edit();
            if(!server.equals(api.server()))edit.remove("password");edit.putBoolean("standalone",false).putString("server",server).remove("cookie");if(password.length()>0)edit.putString("password",password.getText().toString());edit.commit();sources=new JSONArray();detail=null;browse();connect();
        }catch(Exception ex){status.setText(ex.getMessage());}}}).setNegativeButton("取消",null).show();
    }
}
