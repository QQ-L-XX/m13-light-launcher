package local.m13.home;

import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** TV-first lightweight adaptation: direct providers replace Music-TV's LX API. */
public final class MusicActivity extends Activity {
    private static java.lang.ref.WeakReference<MusicActivity> live=new java.lang.ref.WeakReference<MusicActivity>(null);
    private final Handler ui=new Handler();private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ArrayList<JSONObject> rows=new ArrayList<JSONObject>();private final Set<String> seen=new HashSet<String>();
    private String source="wy",mode="songs",word="纯音乐",playlistId="",error="",coverKey="";private int page,total,generation;private boolean busy,destroyed,dragging;
    private String lyricTrackKey="";private int lyricDelayMs;
    private MusicStore store;private LibreTvPosters posters;private EditText query;private TextView status,title,artist,lyricPrev,lyric,lyricNext,clock,transport,favorite,more,origin;private ImageView cover;private SeekBar seek;private ListView list;private Songs adapter;
    private final Runnable tick=new Runnable(){public void run(){renderPlayer();ui.postDelayed(this,500);}};
    private final Runnable lyricTick=new Runnable(){public void run(){MusicService m=MusicService.current();if(m!=null)renderLyrics(m,m.position());ui.postDelayed(this,100);}};
    public void onCreate(Bundle saved){super.onCreate(saved);live=new java.lang.ref.WeakReference<MusicActivity>(this);getWindow().setFlags(1024,1024);ScreenPolicy.applyWindow(this);NeteaseAccount.init(this);store=new MusicStore(this);posters=new LibreTvPosters(this);build();String requested=getIntent().getStringExtra("query");if(requested!=null&&!requested.trim().isEmpty()){word=requested;query.setText(word);}if(saved!=null){source=saved.getString("source","wy");word=saved.getString("word","纯音乐");query.setText(word);}search(true);}
    protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("source",source);b.putString("word",word);}
    protected void onResume(){super.onResume();TvUi.inset(this);ScreenPolicy.awaken(this);ScreenPolicy.applyWindow(this);applyLyricFont();ui.removeCallbacks(tick);ui.post(tick);ui.removeCallbacks(lyricTick);ui.post(lyricTick);}
    protected void onPause(){ui.removeCallbacks(lyricTick);ui.removeCallbacks(tick);super.onPause();}
    protected void onDestroy(){NeteaseLoginDialog.closeFor(this);destroyed=true;generation++;worker.shutdownNow();ui.removeCallbacksAndMessages(null);posters.close();if(live.get()==this)live.clear();super.onDestroy();}
    private int dp(float n){return(int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView button(LinearLayout bar,String label,final Runnable action){final TextView b=text(label,13,BlueRedTheme.TEXT);b.setGravity(Gravity.CENTER);b.setPadding(dp(12),0,dp(12),0);b.setFocusable(true);b.setClickable(true);b.setBackground(bg(0xff24446b));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(38));p.rightMargin=dp(8);bar.addView(b,p);b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){action.run();}});b.setOnFocusChangeListener(new View.OnFocusChangeListener(){public void onFocusChange(View v,boolean focus){b.setBackground(focus?BlueRedTheme.focus(dp(12)):bg(0xff24446b));b.setTextColor(focus?BlueRedTheme.DARK:BlueRedTheme.TEXT);}});return b;}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(38),dp(20),dp(38),dp(18));root.setBackground(BlueRedTheme.background(0));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);TextView brand=new BlueRedTheme.BrandText(this);brand.setText("轻音乐");brand.setTextSize(28);header.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));
        button(header,"全屏播放",new Runnable(){public void run(){startActivity(new Intent(MusicActivity.this,MusicPlayerActivity.class));}});button(header,"网易云账号",new Runnable(){public void run(){NeteaseLoginDialog.menu(MusicActivity.this);}});button(header,"发现",new Runnable(){public void run(){showDiscover();}});button(header,"收藏",new Runnable(){public void run(){saved("favorites","我的收藏");}});button(header,"最近",new Runnable(){public void run(){saved("history","最近播放");}});button(header,"续听",new Runnable(){public void run(){resumeLast();}});button(header,"返回桌面",new Runnable(){public void run(){finish();}});root.addView(header);
        TextView intro=text("歌曲与歌词直接由盒子获取 · 无需电脑服务",12,BlueRedTheme.MUTED);root.addView(intro,new LinearLayout.LayoutParams(-1,dp(26)));
        LinearLayout search=new LinearLayout(this);search.setGravity(Gravity.CENTER_VERTICAL);query=new EditText(this);query.setSingleLine(true);query.setText(word);query.setTextSize(17);query.setTextColor(BlueRedTheme.TEXT);query.setHintTextColor(BlueRedTheme.FAINT);query.setHint("输入歌名或歌手");query.setPadding(dp(12),0,dp(12),0);query.setBackground(bg(0xff102d50));search.addView(query,new LinearLayout.LayoutParams(0,dp(42),1));
        query.setOnEditorActionListener(new TextView.OnEditorActionListener(){public boolean onEditorAction(TextView v,int action,KeyEvent e){if(action==3||action==6||(e!=null&&e.getKeyCode()==66&&e.getAction()==1)){begin(false);return true;}return false;}});
        button(search,"搜歌曲",new Runnable(){public void run(){begin(false);}});button(search,"搜歌单",new Runnable(){public void run(){begin(true);}});origin=button(search,"来源：网易",new Runnable(){public void run(){source=source.equals("wy")?"kw":"wy";origin.setText(source.equals("wy")?"来源：网易":"来源：酷我");mode="songs";word=query.getText().toString().trim();search(true);}});root.addView(search,new LinearLayout.LayoutParams(-1,dp(54)));
        LinearLayout content=new LinearLayout(this);content.setPadding(0,dp(8),0,0);
        LinearLayout left=new LinearLayout(this);left.setOrientation(1);status=text("准备音乐库…",12,BlueRedTheme.MUTED);left.addView(status,new LinearLayout.LayoutParams(-1,dp(32)));list=new ListView(this);list.setDividerHeight(dp(5));list.setCacheColorHint(0);adapter=new Songs();list.setAdapter(adapter);list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener(){public void onItemClick(android.widget.AdapterView<?> a,View v,int index,long id){choose(index);}});left.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=new LinearLayout(this);more=button(footer,"加载更多",new Runnable(){public void run(){search(false);}});button(footer,"播放设置",new Runnable(){public void run(){MusicUi.options(MusicActivity.this);}});button(footer,"重试搜索",new Runnable(){public void run(){search(page==0);}});button(footer,"说明",new Runnable(){public void run(){new AlertDialog.Builder(MusicActivity.this).setTitle("轻音乐 · 盒子独立版").setMessage("参考 Music-TV 的电视交互与行歌词解析，针对 M13 移植为轻量 Java 模块。\n\n无需 LX Server、电脑或 NAS。搜索、歌单、歌词和播放由盒子直接联网完成，收藏和历史保存在本机。\n\n网易与酷我接口可能变化；会员、登录限制和试听片段不会作为完整音乐播放。不是所有歌曲都能免费收听。\n\n发现歌单目前使用网易。酷我歌词暂未接入。\n\nMusic-TV: Apache-2.0；轻桌面: AGPL-3.0。") .setPositiveButton("知道了",null).show();}});left.addView(footer,new LinearLayout.LayoutParams(-1,dp(44)));
        content.addView(left,new LinearLayout.LayoutParams(0,-1,1.3f));
        LinearLayout right=new LinearLayout(this);right.setOrientation(1);right.setGravity(Gravity.CENTER_HORIZONTAL);right.setPadding(dp(22),dp(14),dp(18),dp(12));right.setBackground(bg(0xff112c4e));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-1,1);rp.leftMargin=dp(24);content.addView(right,rp);
        cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackground(BlueRedTheme.focus(dp(16)));right.addView(cover,new LinearLayout.LayoutParams(dp(126),dp(126)));
        title=text("把喜欢的歌，留在客厅",22,BlueRedTheme.TEXT);title.setGravity(Gravity.CENTER);title.setMaxLines(2);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);right.addView(title,new LinearLayout.LayoutParams(-1,dp(58)));
        artist=text("从左侧选择歌曲",12,BlueRedTheme.MUTED);artist.setGravity(Gravity.CENTER);right.addView(artist,new LinearLayout.LayoutParams(-1,dp(25)));
        lyricPrev=text("",12,BlueRedTheme.FAINT);lyricPrev.setGravity(Gravity.CENTER);right.addView(lyricPrev,new LinearLayout.LayoutParams(-1,dp(30)));
        lyric=text("此刻，只听音乐",19,BlueRedTheme.BLUE);lyric.setGravity(Gravity.CENTER);lyric.setMaxLines(2);right.addView(lyric,new LinearLayout.LayoutParams(-1,0,1));
        lyricNext=text("",12,BlueRedTheme.FAINT);lyricNext.setGravity(Gravity.CENTER);right.addView(lyricNext,new LinearLayout.LayoutParams(-1,dp(30)));
        seek=new SeekBar(this);seek.setMax(1000);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){if(user&&!dragging){MusicService m=MusicService.current();if(m!=null)m.seek((int)((long)m.duration()*p/1000));}}public void onStartTrackingTouch(SeekBar s){dragging=true;}public void onStopTrackingTouch(SeekBar s){MusicService m=MusicService.current();if(m!=null)m.seek((int)((long)m.duration()*s.getProgress()/1000));dragging=false;}});right.addView(seek,new LinearLayout.LayoutParams(-1,dp(28)));
        clock=text("00:00 / 00:00",12,BlueRedTheme.MUTED);clock.setGravity(Gravity.CENTER);right.addView(clock,new LinearLayout.LayoutParams(-1,dp(24)));
        LinearLayout control=new LinearLayout(this);control.setGravity(Gravity.CENTER);button(control,"上一首",new Runnable(){public void run(){MusicService.command(MusicActivity.this,"previous");}});transport=button(control,"播放",new Runnable(){public void run(){MusicService.command(MusicActivity.this,"toggle");}});button(control,"下一首",new Runnable(){public void run(){MusicService.command(MusicActivity.this,"next");}});right.addView(control,new LinearLayout.LayoutParams(-1,dp(46)));
        LinearLayout extra=new LinearLayout(this);extra.setGravity(Gravity.CENTER);favorite=button(extra,"收藏歌曲",new Runnable(){public void run(){MusicService m=MusicService.current();if(m==null||m.song().optString("id").isEmpty())return;try{boolean on=store.toggle(m.song());toast(on?"已收藏":"已取消收藏");renderPlayer();}catch(Exception e){toast("收藏未保存");}}});button(extra,"歌词校准",new Runnable(){public void run(){calibrateLyrics();}});button(extra,"歌词字号",new Runnable(){public void run(){MusicUi.chooseSize(MusicActivity.this,new Runnable(){public void run(){applyLyricFont();}});}});button(extra,"停止",new Runnable(){public void run(){MusicService.command(MusicActivity.this,"stop");}});right.addView(extra,new LinearLayout.LayoutParams(-1,dp(42)));
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);TvUi.inset(this);applyLyricFont();list.requestFocus();
    }
    private void applyLyricFont(){int size=MusicUi.lyricSize(this,false);if(Math.abs(lyric.getTextSize()-size*getResources().getDisplayMetrics().scaledDensity)>.1f)lyric.setTextSize(size);lyricPrev.setSingleLine(true);lyricNext.setSingleLine(true);lyricPrev.setEllipsize(android.text.TextUtils.TruncateAt.END);lyricNext.setEllipsize(android.text.TextUtils.TruncateAt.END);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void begin(boolean playlists){mode=playlists?"playlists":"songs";if(playlists){source="wy";origin.setText("来源：网易");}playlistId="";word=query.getText().toString().trim();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(),0);search(true);list.requestFocus();}
    private void search(final boolean reset){
        if(busy&&!reset)return;if(mode.equals("favorites")||mode.equals("history"))return;
        final int request=++generation,requestedPage=reset?1:page+1;final String selectedMode=mode,selectedSource=source,selectedWord=word,id=playlistId;
        if(reset){rows.clear();seen.clear();page=0;adapter.notifyDataSetChanged();}busy=true;error="";status.setText("正在由盒子直接获取…");more.setEnabled(false);
        worker.submit(new Runnable(){public void run(){try{LibreTvNet.init(MusicActivity.this);final MusicApi.Page found=selectedMode.equals("playlist")?MusicApi.playlist(id,requestedPage):MusicApi.search(selectedSource,selectedWord,requestedPage,selectedMode.equals("playlists"));ui.post(new Runnable(){public void run(){if(destroyed||request!=generation)return;busy=false;for(int i=0;i<found.rows.length();i++){JSONObject row=found.rows.optJSONObject(i);if(row!=null&&seen.add(MusicStore.key(row)))rows.add(row);}page=requestedPage;total=found.total;more.setEnabled(found.rows.length()>0&&page*MusicApi.PAGE<total);adapter.notifyDataSetChanged();status.setText((selectedMode.equals("playlists")?"歌单":selectedMode.equals("playlist")?"歌单歌曲":"搜索 · "+selectedWord)+" · 已显示 "+rows.size()+" / "+total+" · "+(selectedSource.equals("kw")?"酷我":"网易"));}});}catch(final Exception e){ui.post(new Runnable(){public void run(){if(destroyed||request!=generation)return;busy=false;error=e.getMessage()==null?"获取失败":e.getMessage();status.setText("获取失败 · 请选择重试搜索："+error);more.setEnabled(true);}});}}});
    }
    private void saved(String kind,String label){generation++;busy=false;mode=kind;rows.clear();seen.clear();JSONArray saved=store.read(kind);for(int i=0;i<saved.length();i++)if(saved.optJSONObject(i)!=null)rows.add(saved.optJSONObject(i));total=rows.size();adapter.notifyDataSetChanged();status.setText(label+" · "+rows.size()+" 首 · 本机保存");more.setEnabled(false);list.requestFocus();}
    private void showDiscover(){new AlertDialog.Builder(this).setTitle("发现音乐").setItems(new String[]{"纯音乐","轻音乐","华语经典","舒眠音乐","热歌榜","飙升榜","新歌榜"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){source="wy";origin.setText("来源：网易");if(which<4){String[] words={"纯音乐","轻音乐","华语经典","舒眠音乐"};word=words[which];query.setText(word);mode="songs";playlistId="";}else{String[] ids={"3778678","19723756","3779629"};playlistId=ids[which-4];mode="playlist";}search(true);}}).show();}
    private void resumeLast(){try{android.content.SharedPreferences p=getSharedPreferences("music-local-v1",0);String raw=p.getString("resumeSong","");if(raw.isEmpty()){toast("还没有上次播放记录");return;}MusicService.play(this,new JSONArray().put(new JSONObject(raw)),0,p.getInt("resumePosition",0));}catch(Exception e){toast("续听记录暂不可用");}}
    void choose(int index){if(index<0||index>=rows.size())return;JSONObject song=rows.get(index);if(song.optBoolean("playlist")){source="wy";playlistId=song.optString("id");mode="playlist";search(true);return;}JSONArray queue=new JSONArray();for(JSONObject row:rows)queue.put(row);MusicService.play(this,queue,index,0);}
    private String time(int ms){int seconds=ms/1000;return String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60);}
    private void renderPlayer(){
        applyLyricFont();
        MusicService m=MusicService.current();if(m==null){transport.setText("播放");lyric.setText("此刻，只听音乐");lyricPrev.setText("");lyricNext.setText("");clock.setText("00:00 / 00:00");seek.setProgress(0);return;}
        JSONObject song=m.song();title.setText(song.optString("name","轻音乐"));artist.setText(song.optString("artist")+" · "+("kw".equals(song.optString("source"))?"酷我":"网易"));
        String key=MusicStore.key(song);if(!key.equals(coverKey)){coverKey=key;String image=song.optString("cover");if(image.isEmpty()){cover.setImageDrawable(new MinimalIcon("music",BlueRedTheme.DARK));}else posters.bind(cover,image);}
        transport.setText(m.state().equals("playing")?"暂停":m.state().equals("loading")?"连接中":"播放");favorite.setText(store.favorite(song)?"已收藏":"收藏歌曲");int position=m.position(),duration=m.duration();clock.setText(time(position)+" / "+time(duration));if(!dragging)seek.setProgress(duration>0?(int)((long)position*1000/duration):0);
        renderLyrics(m,position);
    }
    private void lyricText(TextView view,String value){if(!android.text.TextUtils.equals(view.getText(),value))view.setText(value);}
    private void renderLyrics(MusicService m,int position){
        String key=MusicStore.key(m.song());if(!key.equals(lyricTrackKey)){lyricTrackKey=key;lyricDelayMs=store.lyricDelay(m.song());}
        List<MusicLyrics.Line> lines=m.lyrics();int current=MusicLyrics.current(lines,position,lyricDelayMs);
        if(m.state().equals("error")){lyricText(lyric,m.error());lyricText(lyricPrev,"");lyricText(lyricNext,"播放设置 → 重试当前歌曲，或切换来源");}
        else if(m.state().equals("loading")){lyricText(lyric,"正在连接音乐源…");lyricText(lyricPrev,"");lyricText(lyricNext,"");}
        else if(lines.isEmpty()){lyricText(lyric,m.state().equals("paused")?"已暂停":"随音乐，慢下来");lyricText(lyricPrev,"");lyricText(lyricNext,"当前来源暂无同步歌词");}
        else{lyricText(lyricPrev,current>0?lines.get(current-1).text:"");lyricText(lyric,current>=0?lines.get(current).text:"前奏 · 即将开始");lyricText(lyricNext,current+1<lines.size()?lines.get(current+1).text:"");}
    }
    private String lyricDelayDescription(int delay){return delay==0?"原始时间（0 秒）":String.format(Locale.CHINA,"%s %.2f 秒",delay<0?"提前":"延后",Math.abs(delay)/1000f);}
    private void calibrateLyrics(){
        final MusicService m=MusicService.current();if(m==null||m.song().optString("id").isEmpty()){toast("请先播放歌曲");return;}
        final JSONObject song=m.song();final String key=MusicStore.key(song);
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("歌词校准 · "+song.optString("name")).setMessage("").setNegativeButton("提前 0.25 秒",null).setPositiveButton("延后 0.25 秒",null).setNeutralButton("恢复原始",null).create();dialog.show();
        final Runnable refresh=new Runnable(){public void run(){dialog.setMessage("歌词慢了就选提前，歌词快了就选延后。只保存这首歌，不影响音乐速度。按返回关闭。\n\n当前："+lyricDelayDescription(store.lyricDelay(song)));}};refresh.run();
        int[] buttons={AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEUTRAL};final int[] changes={-250,250,0};for(int i=0;i<buttons.length;i++){final int delta=changes[i];dialog.getButton(buttons[i]).setOnClickListener(new View.OnClickListener(){public void onClick(View v){MusicService now=MusicService.current();if(now==null||!key.equals(MusicStore.key(now.song()))){dialog.dismiss();toast("歌曲已切换，请重新打开校准");return;}int value=delta==0?0:store.lyricDelay(song)+delta;store.lyricDelay(song,value);lyricTrackKey=key;lyricDelayMs=store.lyricDelay(song);renderLyrics(now,now.position());refresh.run();}});}
    }
    public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){MusicService.command(this,"toggle");return true;}if(key==KeyEvent.KEYCODE_MEDIA_NEXT){MusicService.command(this,"next");return true;}if(key==KeyEvent.KEYCODE_MEDIA_PREVIOUS){MusicService.command(this,"previous");return true;}return super.onKeyDown(key,event);}
    private final class Songs extends BaseAdapter {
        public int getCount(){return rows.size();}public Object getItem(int i){return rows.get(i);}public long getItemId(int i){return i;}
        public View getView(int index,View recycled,android.view.ViewGroup parent){LinearLayout box;if(recycled==null){box=new LinearLayout(MusicActivity.this);box.setOrientation(1);box.setPadding(dp(16),dp(9),dp(12),dp(9));box.addView(text("",17,BlueRedTheme.TEXT));box.addView(text("",11,BlueRedTheme.MUTED));android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();states.addState(new int[]{android.R.attr.state_pressed},bg(0xff3e607e));states.addState(new int[]{android.R.attr.state_selected},bg(0xff3e607e));states.addState(new int[]{},bg(0xff183957));box.setBackground(states);}else box=(LinearLayout)recycled;
            JSONObject song=rows.get(index);String restricted=song.optBoolean("restricted")||(!NeteaseAccount.logged()&&(song.optInt("fee")==1||song.optInt("fee")==4))?" · 权限受限":"";((TextView)box.getChildAt(0)).setText((index+1)+"  "+song.optString("name")+restricted);((TextView)box.getChildAt(1)).setText(song.optString("artist")+(song.optBoolean("playlist")?" · 歌单":" · "+time((int)song.optLong("duration"))));return box;
        }
    }
    public static JSONObject diagnostics(){try{MusicActivity a=live.get();if(a==null)return new JSONObject();JSONArray result=new JSONArray();for(int i=0;i<Math.min(35,a.rows.size());i++)result.put(a.rows.get(i));return new JSONObject().put("busy",a.busy).put("mode",a.mode).put("source",a.source).put("query",a.word).put("page",a.page).put("count",a.rows.size()).put("total",a.total).put("error",a.error).put("rows",result).put("lyricDelayMs",a.lyricDelayMs).put("lyricRefreshMs",100).put("lyricSize",MusicUi.lyricSize(a,false)).put("lyricIndex",MusicService.current()==null?-1:MusicLyrics.current(MusicService.current().lyrics(),MusicService.current().position(),a.lyricDelayMs));}catch(Exception e){return new JSONObject();}}
    static boolean testSelect(int index){MusicActivity a=live.get();if(a==null||index<0||index>=a.rows.size())return false;a.choose(index);return true;}
    static void refreshAccountLabels(){MusicActivity a=live.get();if(a!=null)a.adapter.notifyDataSetChanged();}
    static boolean testSearch(String query,String source,boolean playlists){MusicActivity a=live.get();if(a==null)return false;a.source="kw".equals(source)?"kw":"wy";a.origin.setText(a.source.equals("wy")?"来源：网易":"来源：酷我");a.query.setText(query==null?"纯音乐":query);a.begin(playlists);return true;}
}
