package local.m13.home;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;

/** In-frame TV controls, always visible during seeking and pause. No floating system window. */
public final class VideoControls {
    private final Activity activity;private final VideoView video;private final Handler ui=new Handler();
    private final LinearLayout panel;private final TextView title,time,hint,state;private final SeekBar bar;
    private final VideoSeek seek=new VideoSeek();private boolean ready,closed,dragging,inFlight,buffering;private int sent;private long showUntil;
    private int dp(int n){return TvUi.dp(activity,n);}
    private TextView text(String value,int size,int color){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    public VideoControls(final Activity a,FrameLayout frame,VideoView v,String name){activity=a;video=v;panel=new LinearLayout(a);panel.setOrientation(1);panel.setPadding(dp(40),dp(16),dp(40),dp(20));GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xe6091428,0xf50d1b32});panel.setBackground(background);
        LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);title=text(name,19,BlueRedTheme.TEXT);title.setSingleLine();title.setEllipsize(android.text.TextUtils.TruncateAt.END);heading.addView(title,new LinearLayout.LayoutParams(0,dp(34),1));state=text("正在载入…",15,BlueRedTheme.BLUE);heading.addView(state);panel.addView(heading);
        bar=new SeekBar(a);bar.setMax(1000);bar.setFocusable(false);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(BlueRedTheme.BLUE));bar.setThumbTintList(android.content.res.ColorStateList.valueOf(BlueRedTheme.RED));bar.setEnabled(false);panel.addView(bar,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout footer=new LinearLayout(a);footer.setGravity(Gravity.CENTER_VERTICAL);time=text("00:00 / --:--",17,BlueRedTheme.TEXT);footer.addView(time,new LinearLayout.LayoutParams(0,dp(32),1));hint=text("左右键快退 / 快进 · 确认键暂停 / 继续",13,BlueRedTheme.MUTED);footer.addView(hint);panel.addView(footer);FrameLayout.LayoutParams layout=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);int edge=dp(20+TvUi.margin(a)*12);layout.setMargins(edge,0,edge,edge);frame.addView(panel,layout);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){dragging=true;show();}public void onProgressChanged(SeekBar b,int value,boolean user){if(user&&ready){seek.select((long)duration()*value/1000,duration());show();}}public void onStopTrackingTouch(SeekBar b){dragging=false;flush();show();}});
        video.setOnTouchListener(new View.OnTouchListener(){public boolean onTouch(View v,android.view.MotionEvent e){if(e.getAction()==android.view.MotionEvent.ACTION_UP)show();return true;}});state.setOnClickListener(new View.OnClickListener(){public void onClick(View view){toggle();}});show();ui.post(tick);
    }
    private int duration(){try{return Math.max(0,video.getDuration());}catch(Exception e){return 0;}}
    private int position(){try{return Math.max(0,video.getCurrentPosition());}catch(Exception e){return 0;}}
    public void prepared(){ready=true;bar.setEnabled(duration()>0);show();}
    public void show(){if(closed)return;showUntil=SystemClock.elapsedRealtime()+4500;panel.setVisibility(View.VISIBLE);render();}
    public void step(int direction,int repeat){if(!ready)return;if(seek.step(position(),duration(),direction*(repeat>=5?30000:10000))<0){show();return;}show();ui.removeCallbacks(send);ui.postDelayed(send,180);}
    public void flush(){ui.removeCallbacks(send);send.run();}
    private final Runnable send=new Runnable(){public void run(){if(closed||!ready||inFlight||!seek.pending())return;sent=seek.target();inFlight=true;video.seekTo(sent);}};
    public void seekComplete(){if(inFlight){inFlight=false;seek.applied(sent);if(seek.pending())ui.post(send);}render();}
    public void toggle(){if(!ready)return;if(video.isPlaying())video.pause();else video.start();show();}
    public void buffering(boolean waiting){buffering=waiting;show();}
    private void render(){if(closed)return;int duration=duration(),current=position(),shown=seek.pending()?seek.target():current;state.setText(!ready?"正在载入…":seek.pending()?"正在跳转":buffering?"正在缓冲…":video.isPlaying()?"播放中":"已暂停");time.setText((seek.pending()?"跳转到 ":"")+VideoSeek.clock(shown)+" / "+(duration>0?VideoSeek.clock(duration):"--:--"));if(!dragging)bar.setProgress(duration>0?(int)((long)shown*1000/duration):0);}
    private final Runnable tick=new Runnable(){public void run(){if(closed)return;if(panel.getVisibility()==View.VISIBLE)render();if(ready&&video.isPlaying()&&!seek.pending()&&!dragging&&SystemClock.elapsedRealtime()>showUntil)panel.setVisibility(View.GONE);ui.postDelayed(this,250);}};
    public void close(){closed=true;ui.removeCallbacksAndMessages(null);}
    public JSONObject diagnostics()throws Exception{return new JSONObject().put("visible",panel.getVisibility()==View.VISIBLE).put("positionMs",position()).put("durationMs",duration()).put("seekPending",seek.pending()).put("seekTargetMs",seek.target()).put("clock",time.getText().toString()).put("ready",ready);}
}
