package local.m13.home;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;

public final class NewsActivity extends Activity {
    private static WeakReference<NewsActivity> current=new WeakReference<>(null);
    private final ExecutorService io=Executors.newFixedThreadPool(4);
    private final Handler handler=new Handler();
    private final List<NewsProgram> queue=new ArrayList<>();
    private VideoView player;
    private MediaPlayer audioPlayer;
    private TextView status;
    private TextView timer,nextButton,audioTab,videoTab,fullButton,detail,listTitle;
    private LinearLayout root,header,bar,listing;
    private FrameLayout screen;
    private GridView guide;
    private BaseAdapter guideAdapter;
    private boolean fullScreen;
    private LibreTvLocalProxy proxy;
    private volatile int generation;
    private int index;
    private long loadedAt;
    private NewsProgram active;
    private android.content.SharedPreferences history;
    private final Map<String,Long> failedUntil=new HashMap<>();
    private boolean video,scheduled,audioStarted;
    private static final long STALL_MS=20000;
    private static final String LOG="M13News";

    public void onCreate(Bundle b){
        super.onCreate(b);
        current=new WeakReference<>(this);
        history=getSharedPreferences("news-played",MODE_PRIVATE);
        String cutoff=NewsProgram.day(14);
        android.content.SharedPreferences.Editor edit=history.edit();
        for(Map.Entry<String,?> e:history.getAll().entrySet()){
            if(e.getValue() instanceof String&&((String)e.getValue()).compareTo(cutoff)<0)edit.remove(e.getKey());
        }
        edit.apply();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildPage();
        accept(getIntent());
    }

    private int dp(int value){
        return Math.round(getResources().getDisplayMetrics().density*value);
    }
    private TextView label(String value,int size,int color){
        TextView t=new TextView(this);
        t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }
    private GradientDrawable panel(int fill,int border){
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(fill);shape.setCornerRadius(dp(10));
        if(border!=0)shape.setStroke(dp(2),border);
        return shape;
    }
    private void styleControl(TextView t){
        boolean focused=t.hasFocus();
        t.setBackground(focused?BlueRedTheme.focus(dp(10)):panel(t.isSelected()?0xff28597c:0xff234c7c,t.isSelected()?BlueRedTheme.BLUE:0));
        t.setTextColor(focused?BlueRedTheme.DARK:BlueRedTheme.TEXT);
        t.setAlpha(t.isEnabled()?1f:.48f);
    }
    private TextView control(LinearLayout row,String title,Runnable action){
        TextView t=label(title,15,BlueRedTheme.TEXT);
        t.setGravity(Gravity.CENTER);t.setPadding(dp(14),0,dp(14),0);
        t.setFocusable(true);t.setClickable(true);
        t.setOnClickListener(new View.OnClickListener(){public void onClick(View v){action.run();}});
        t.setOnFocusChangeListener(new View.OnFocusChangeListener(){public void onFocusChange(View v,boolean focused){styleControl(t);}});
        styleControl(t);
        LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(-2,dp(38));
        size.rightMargin=dp(8);row.addView(t,size);
        return t;
    }
    private void buildPage(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(BlueRedTheme.background(0));
        setContentView(root);
        header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header);
        TextView brand=new BlueRedTheme.BrandText(this);brand.setText("新闻播报");brand.setTextSize(27);
        brand.setTextColor(BlueRedTheme.TEXT);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        brand.setGravity(Gravity.CENTER_VERTICAL);header.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));
        timer=control(header,"",new Runnable(){public void run(){
            boolean on=!NewsSchedule.enabled(NewsActivity.this);
            getSharedPreferences("news",0).edit().putBoolean("enabled",on).apply();
            NewsSchedule.schedule(NewsActivity.this);showTimer();
        }});
        showTimer();
        bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);root.addView(bar,new LinearLayout.LayoutParams(-1,dp(50)));
        audioTab=control(bar,"听新闻",new Runnable(){public void run(){playManually(false);}});
        videoTab=control(bar,"看视频新闻",new Runnable(){public void run(){playManually(true);}});
        nextButton=control(bar,"下一条",new Runnable(){public void run(){next(generation);}});
        control(bar,"停止",new Runnable(){public void run(){
            NewsSchedule.userStopped(NewsActivity.this);invalidatePlayback();detail.setText("");status.setText("已停止");
        }});
        fullButton=control(bar,"全屏播放",new Runnable(){public void run(){setFullScreen(true);}});
        detail=label("",14,BlueRedTheme.MUTED);root.addView(detail,new LinearLayout.LayoutParams(-1,dp(24)));
        status=label("",23,BlueRedTheme.TEXT);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        status.setMaxLines(2);status.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(status,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout body=new LinearLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        screen=new FrameLayout(this);screen.setBackgroundColor(0xff000000);
        body.addView(screen,new LinearLayout.LayoutParams(0,-1,1.6f));
        player=new VideoView(this);player.setFocusable(false);player.setFocusableInTouchMode(false);
        screen.addView(player,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        listing=new LinearLayout(this);listing.setOrientation(LinearLayout.VERTICAL);
        body.addView(listing,new LinearLayout.LayoutParams(0,-1,1));
        listTitle=label("待播节目",15,BlueRedTheme.MUTED);
        listing.addView(listTitle,new LinearLayout.LayoutParams(-1,dp(36)));
        guide=new GridView(this);guide.setHorizontalSpacing(dp(10));guide.setVerticalSpacing(dp(9));
        guide.setPadding(dp(3),dp(3),dp(3),dp(6));guide.setClipToPadding(false);
        guide.setSelector(panel(0,BlueRedTheme.BLUE));guide.setDrawSelectorOnTop(true);
        guideAdapter=new BaseAdapter(){
            public int getCount(){return Math.max(0,queue.size()-index);}
            public NewsProgram getItem(int pos){return queue.get(index+pos);}
            public long getItemId(int pos){return pos;}
            public View getView(int pos,View reused,ViewGroup parent){
                LinearLayout card=(LinearLayout)reused;
                if(card==null){
                    card=new LinearLayout(NewsActivity.this);card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(dp(14),dp(8),dp(14),dp(8));card.setBackground(panel(0xff203f66,0));
                    card.setLayoutParams(new AbsListView.LayoutParams(-1,dp(92)));
                    card.addView(label("",12,BlueRedTheme.MUTED));
                    TextView headline=label("",16,BlueRedTheme.TEXT);
                    headline.setMaxLines(2);headline.setEllipsize(TextUtils.TruncateAt.END);
                    card.addView(headline,new LinearLayout.LayoutParams(-1,0,1));
                }
                NewsProgram item=getItem(pos);
                ((TextView)card.getChildAt(0)).setText(item.displayDate()+" · "+item.topicLabel());
                ((TextView)card.getChildAt(1)).setText(item.title);
                return card;
            }
        };
        guide.setAdapter(guideAdapter);
        guide.setOnItemClickListener(new AdapterView.OnItemClickListener(){public void onItemClick(AdapterView<?> parent,View view,int pos,long id){
            int picked=index+pos;
            if(picked>=index&&picked<queue.size()){
                NewsProgram choice=queue.remove(picked);
                queue.add(index,choice);
                // A deliberate selection should play even if the list is due for a refresh.
                loadedAt=SystemClock.elapsedRealtime();next(generation);
            }
        }});
        listing.addView(guide,new LinearLayout.LayoutParams(-1,0,1));
        audioTab.requestFocus();setFullScreen(false);
    }
    private void updateGuide(){
        guideAdapter.notifyDataSetChanged();
        int count=guideAdapter.getCount();
        listTitle.setText(count==0?"待播节目":"待播节目 · "+count);
        guide.setFocusable(count>0);
    }
    private void setFullScreen(boolean on){
        fullScreen=on;
        for(View v:new View[]{header,bar,detail,status,listing})v.setVisibility(on?View.GONE:View.VISIBLE);
        root.setPadding(on?0:dp(32),on?0:dp(14),on?0:dp(32),on?0:dp(14));
        if(!on&&video&&fullButton.isShown())fullButton.requestFocus();
    }
    private void showMode(){
        audioTab.setSelected(!video);videoTab.setSelected(video);
        styleControl(audioTab);styleControl(videoTab);
        screen.setVisibility(video?View.VISIBLE:View.GONE);
        fullButton.setVisibility(video?View.VISIBLE:View.GONE);
        listing.setPadding(video?dp(14):0,0,0,0);
        guide.setNumColumns(video?1:3);
    }
    private void showTimer(){timer.setText("07:10—07:50 · "+(NewsSchedule.enabled(this)?"已开启":"已关闭"));}
    protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);accept(i);}
    private void accept(Intent i){scheduled=i.getBooleanExtra("scheduled",false);if(scheduled&&!NewsSchedule.inWindow(NewsSchedule.now())){finish();return;}if(scheduled)load(false);else playManually(false);}
    private void playManually(boolean withVideo){NewsSchedule.userResumed(this);load(withVideo);}
    static void stopAtEnd(){NewsActivity a=current.get();if(a!=null){a.invalidatePlayback();a.finish();}}
    static void stopScheduledOnly(){NewsActivity a=current.get();if(a!=null&&a.scheduled)stopAtEnd();}
    static boolean scheduledActive(){NewsActivity a=current.get();return a!=null&&a.scheduled&&!a.isFinishing();}
    public void onBackPressed(){
        if(fullScreen){setFullScreen(false);return;}
        NewsSchedule.userStopped(this);super.onBackPressed();
    }
    private void releasePlayer(){
        if(active!=null&&position()>0)recordPlayed();
        active=null;
        handler.removeCallbacksAndMessages(null);
        audioStarted=false;
        if(audioPlayer!=null){
            audioPlayer.setOnPreparedListener(null);audioPlayer.setOnCompletionListener(null);audioPlayer.setOnErrorListener(null);
            audioPlayer.release();audioPlayer=null;
        }
        player.stopPlayback();
        if(proxy!=null){proxy.close();proxy=null;}
    }
    private void invalidatePlayback(){generation++;queue.clear();index=0;nextButton.setEnabled(false);styleControl(nextButton);releasePlayer();updateGuide();}
    private void load(final boolean withVideo){
        if(scheduled&&!NewsSchedule.inWindow(NewsSchedule.now())){stopAtEnd();return;}
        invalidatePlayback();video=withVideo;
        final int run=generation;
        status.setText("正在读取国内新闻…");
        detail.setText(video?"视频新闻":"听新闻");showMode();
        final NewsProgram.Source[] sources=video?new NewsProgram.Source[]{NewsProgram.Source.CHINANEWS}:NewsProgram.Source.values();
        final List<NewsProgram> candidates=new ArrayList<>();
        final int[] remaining={sources.length};
        final Runnable finishLoad=new Runnable(){public void run(){
            if(run!=generation)return;
            ++generation;
            handler.removeCallbacks(this);
            String today=NewsProgram.day(0), oldest=NewsProgram.day(6);
            Set<String> seen=new HashSet<>();
            Collections.sort(candidates);
            for(NewsProgram item:candidates){
                if(item.day.compareTo(today)<=0&&item.day.compareTo(oldest)>=0&&seen.add(item.id)&&eligible(item))queue.add(item);
            }
            loadedAt=SystemClock.elapsedRealtime();
            android.util.Log.i(LOG,"ready mode="+(video?"video":"audio")+" candidates="+candidates.size()+" eligible="+queue.size());
            if(!queue.isEmpty()){next(generation);return;}
            if(fullScreen)setFullScreen(false);
            status.setText(candidates.isEmpty()?"新闻源暂时未响应，稍后重试":"暂无可播的新节目，稍后再查");
            final int retry=generation;
            handler.postDelayed(new Runnable(){public void run(){if(retry==generation)load(video);}},candidates.isEmpty()?15000:60000);
        }};
        handler.postDelayed(finishLoad,18000);
        for(final NewsProgram.Source source:sources)io.execute(new Runnable(){public void run(){
            if(run!=generation)return;
            final List<NewsProgram> list=new ArrayList<>();
            try{LibreTvNet.init(getApplicationContext());list.addAll(NewsProgram.read(source));}
            catch(Exception e){android.util.Log.w(LOG,source+" feed "+e.getClass().getSimpleName());}
            runOnUiThread(new Runnable(){public void run(){
                if(run!=generation)return;
                candidates.addAll(list);
                if(--remaining[0]==0)finishLoad.run();
            }});
        }});
    }
    private boolean eligible(NewsProgram item){
        Long until=failedUntil.get(item.id);
        return !history.contains(item.id)&&(until==null||until<=SystemClock.elapsedRealtime());
    }
    private int position(){
        try{
            if(video&&player.isPlaying())return player.getCurrentPosition();
            if(audioStarted&&audioPlayer!=null)return audioPlayer.getCurrentPosition();
        }catch(IllegalStateException ignored){}
        return 0;
    }
    private void recordPlayed(){
        if(active==null||history.contains(active.id))return;
        history.edit().putString(active.id,active.day).apply();
        android.util.Log.i(LOG,"played "+active.id+" date="+active.day+" topic="+active.topic()+" video="+video);
    }
    private void next(final int previous){
        if(previous!=generation)return;
        if(scheduled&&!NewsSchedule.inWindow(NewsSchedule.now())){stopAtEnd();return;}
        final int run=++generation;releasePlayer();
        while(index<queue.size()&&!eligible(queue.get(index)))index++;
        if(index>=queue.size()||SystemClock.elapsedRealtime()-loadedAt>180000){load(video);return;}
        final NewsProgram item=queue.get(index++);
        active=item;
        nextButton.setEnabled(true);styleControl(nextButton);updateGuide();
        detail.setText(item.displayDate()+" · "+item.topicLabel()+" · "+(video?"视频新闻":"听新闻"));
        status.setText(item.title);
        watchProgress(run);
        io.execute(new Runnable(){public void run(){try{
            if(run!=generation)return;
            final NewsProgram resolved=item.resolve();
            runOnUiThread(new Runnable(){public void run(){if(run!=generation)return;try{
                proxy=new LibreTvLocalProxy();
                String local=proxy.url(resolved.url);
                MediaPlayer.OnCompletionListener complete=new MediaPlayer.OnCompletionListener(){public void onCompletion(MediaPlayer p){if(run==generation){recordPlayed();next(run);}}};
                MediaPlayer.OnErrorListener error=new MediaPlayer.OnErrorListener(){public boolean onError(MediaPlayer p,int what,int extra){skip(run,"player "+what+"/"+extra);return true;}};
                if(video){
                    player.setOnCompletionListener(complete);player.setOnErrorListener(error);
                    player.setVideoURI(Uri.parse(local));player.start();
                }else{
                    audioPlayer=new MediaPlayer();
                    audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    audioPlayer.setOnCompletionListener(complete);audioPlayer.setOnErrorListener(error);
                    audioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){public void onPrepared(MediaPlayer p){
                        if(run==generation&&p==audioPlayer)try{p.start();audioStarted=true;}catch(IllegalStateException e){skip(run,"start failed");}
                    }});
                    audioPlayer.setDataSource(local);audioPlayer.prepareAsync();
                }
                status.setText(resolved.title);
            }catch(Exception e){skip(run,e.getClass().getSimpleName());}}});
        }catch(final Exception e){runOnUiThread(new Runnable(){public void run(){skip(run,e.getClass().getSimpleName());}});}}});
    }
    private void watchProgress(final int run){
        handler.postDelayed(new Runnable(){
            int position=0;
            long lastProgress=SystemClock.elapsedRealtime();
            public void run(){
                if(run!=generation)return;
                if(scheduled&&!NewsSchedule.inWindow(NewsSchedule.now())){stopAtEnd();return;}
                int current=position();
                long now=SystemClock.elapsedRealtime();
                if(current>position){
                    recordPlayed();position=current;lastProgress=now;
                }else if(now-lastProgress>=STALL_MS){skip(run,"no playback progress");return;}
                handler.postDelayed(this,2000);
            }
        },2000);
    }
    private void skip(int run,String reason){
        if(run!=generation)return;
        android.util.Log.w(LOG,"item unavailable "+(active==null?"":active.id)+" "+reason);
        if(active!=null)failedUntil.put(active.id,SystemClock.elapsedRealtime()+180000);
        final int nextRun=++generation;
        releasePlayer();
        status.setText("此条暂时无法播放，正在切换");
        handler.postDelayed(new Runnable(){public void run(){next(nextRun);}},1000);
    }
    protected void onStop(){super.onStop();if(!isFinishing()){invalidatePlayback();finish();}}
    protected void onDestroy(){invalidatePlayback();io.shutdownNow();if(current.get()==this)current.clear();super.onDestroy();}
}
