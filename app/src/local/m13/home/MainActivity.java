package local.m13.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler=new Handler();
    private SharedPreferences preferences;
    private TextView clock,date,heroTitle,heroSubtitle,cleanButton;
    private boolean allApps;
    private String focusedPackage;
    private final String[] defaults={"com.xiaodianshi.tv.yst","com.gitvdideo.aliapp","com.yunos.app.videoplayer"};
    private final Runnable tick=new Runnable(){public void run(){tickClock();VoiceWarmup.request(MainActivity.this);handler.postDelayed(this,30000);}};

    private final Runnable restrictIdle=new Runnable(){public void run(){AutostartPolicy.applyIfIdle(MainActivity.this);handler.postDelayed(this,30000);}};

    public void onCreate(Bundle saved){
        super.onCreate(saved);
        preferences=getSharedPreferences("home",MODE_PRIVATE);
        if(!preferences.getBoolean("layout_24",false))preferences.edit().putBoolean("layout_24",true).putInt("theme",1).apply();
        if(!preferences.getBoolean("blue_red_29",false))preferences.edit().putBoolean("blue_red_29",true).putInt("theme",0).apply();
        HomePolicy.applyIfOwner(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ScreenPolicy.applyWindow(this);
        if(preferences.getBoolean("home_key_enabled",false))startService(new Intent(this,HomeKeyService.class));
        else stopService(new Intent(this,HomeKeyService.class));
    }
    protected void onResume(){super.onResume();ScreenPolicy.applyWindow(this);build();handler.removeCallbacks(tick);handler.post(tick);handler.removeCallbacks(restrictIdle);handler.postDelayed(restrictIdle,20000);}
    protected void onPause(){handler.removeCallbacks(restrictIdle);handler.removeCallbacks(tick);super.onPause();}
    protected void onDestroy(){handler.removeCallbacks(restrictIdle);handler.removeCallbacks(tick);super.onDestroy();}
    public void onNewIntent(Intent intent){super.onNewIntent(intent);ScreenPolicy.applyWindow(this);HomePolicy.applyIfOwner(this);allApps=false;focusedPackage=null;build();}
    public void onBackPressed(){if(allApps){allApps=false;focusedPackage=null;build();}}
    private int dp(float n){return(int)(getResources().getDisplayMetrics().density*n+0.5f);}
    private TextView text(String value,int size,int color){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;
    }
    private TextView brand(String value,int size){TextView t=new BlueRedTheme.BrandText(this);t.setText(value);t.setTextSize(size);t.setTextColor(BlueRedTheme.TEXT);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private GradientDrawable surface(int start,int end,int stroke){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});
        d.setCornerRadius(dp(15));if(stroke!=0)d.setStroke(dp(2),stroke);return d;
    }
    private static class App{
        String name,pkg;Drawable icon;Intent intent;
        App(String n,String p,Drawable d,Intent i){name=n;pkg=p;icon=d;intent=i;}
    }
    private List<App> applications(){
        PackageManager pm=getPackageManager();LinkedHashMap<String,App> map=new LinkedHashMap<String,App>();
        Intent query=new Intent(Intent.ACTION_MAIN);query.addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo r:pm.queryIntentActivities(query,0)){
            String pkg=r.activityInfo.packageName;
            if(pkg.equals(getPackageName())||pkg.equals("me.efesser.flauncher")||pkg.equals("com.yunos.tv.probe")||pkg.equals("com.taintan.tv.test")||pkg.equals("com.mipt.autotestfora8"))continue;
            Intent launch=pm.getLaunchIntentForPackage(pkg);
            if(launch!=null)map.put(pkg,new App(r.loadLabel(pm).toString(),pkg,r.loadIcon(pm),launch));
        }
        for(String pkg:defaults)if(!map.containsKey(pkg))try{
            Intent launch=AutostartPolicy.intent(this,pkg);
            android.content.pm.ApplicationInfo info=pm.getApplicationInfo(pkg,PackageManager.GET_UNINSTALLED_PACKAGES);
            if(launch!=null)map.put(pkg,new App(pm.getApplicationLabel(info).toString(),pkg,pm.getApplicationIcon(info),launch));
        }catch(Exception ignored){}
        List<App> apps=new ArrayList<App>(map.values());
        final App libre=new App("LibreTV","__libretv__",null,new Intent(this,LibreTvLibraryActivity.class));
        apps.add(libre);
        final App cast=new App("投屏","__cast__",null,new Intent(this,CastActivity.class));apps.add(cast);
        Collections.sort(apps,new Comparator<App>(){public int compare(App a,App b){return a.name.compareToIgnoreCase(b.name);}});
        if(allApps)return apps;
        List<App> home=new ArrayList<App>();
        if(!preferences.getBoolean("hidden:__libretv__",false))home.add(libre);
        for(String wanted:defaults)for(App app:apps)if(app.pkg.equals(wanted)&&!preferences.getBoolean("hidden:"+app.pkg,false))home.add(app);
        if(!preferences.getBoolean("hidden:__cast__",false))home.add(cast);
        for(App app:apps)if(preferences.getBoolean("favorite:"+app.pkg,false)&&!preferences.getBoolean("hidden:"+app.pkg,false)&&!home.contains(app))home.add(app);
        return home;
    }
    private String description(App app){
        if(AutostartPolicy.managed(this,app.pkg))return "按需启动 · 点开即可使用";
        if(app.pkg.equals("__libretv__"))return "电影 · 剧集 · 继续观看";
        if(app.pkg.equals("__cast__"))return "把手机上的喜欢，放到大屏";
        if(app.pkg.equals("__more__"))return allApps?"回到常用应用":"所有应用，都在这里";
        if(app.pkg.equals("com.xiaodianshi.tv.yst"))return "视频与兴趣";
        if(app.pkg.equals("com.gitvdideo.aliapp"))return "电影 · 剧集 · 综艺";
        if(app.pkg.equals("com.yunos.app.videoplayer"))return "本地视频与音乐";
        if(app.pkg.equals("com.android.settings"))return "网络 · 画面 · 声音";
        return "确认打开 · 长按整理";
    }
    private void build(){
        int theme=preferences.getInt("theme",0);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(56),dp(28),dp(56),dp(24));
        root.setBackground(BlueRedTheme.background(theme));root.setClipChildren(false);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=brand("轻桌面",20);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        header.addView(brand,new LinearLayout.LayoutParams(0,dp(44),1));
        addNav(header,"首页",!allApps,new Runnable(){public void run(){allApps=false;focusedPackage=null;build();}});
        addNav(header,"应用",allApps,new Runnable(){public void run(){allApps=true;focusedPackage=null;build();}});
        addNav(header,"语音搜片",false,new Runnable(){public void run(){try{startActivity(new Intent().setClassName("local.m13.voice","local.m13.voice.VoiceActivity"));}catch(Exception e){message("请先安装语音搜片组件");}}});
        addNav(header,"设置",false,new Runnable(){public void run(){showSettings();}});
        cleanButton=addNav(header,"一键清理",false,new Runnable(){public void run(){cleanBackground();}});cleanButton.setEnabled(!BackgroundCleaner.busy());
        LinearLayout timeBox=new LinearLayout(this);timeBox.setOrientation(1);timeBox.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);clock=text("",23,0xffeceef1);clock.setGravity(Gravity.RIGHT);clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));date=text("",10,BlueRedTheme.FAINT);date.setGravity(Gravity.RIGHT);timeBox.addView(clock);timeBox.addView(date);LinearLayout.LayoutParams timeParams=new LinearLayout.LayoutParams(dp(110),dp(48));timeParams.leftMargin=dp(38);header.addView(timeBox,timeParams);root.addView(header);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER_VERTICAL);
        heroTitle=brand(allApps?"全部应用":"LibreTV",42);heroTitle.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));heroTitle.setSingleLine(true);heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        heroSubtitle=text("电影 · 剧集 · 继续观看",14,BlueRedTheme.MUTED);
        hero.addView(heroTitle,new LinearLayout.LayoutParams(-1,dp(62)));hero.addView(heroSubtitle,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout.LayoutParams heroParams=new LinearLayout.LayoutParams(-1,dp(allApps?105:132));heroParams.topMargin=dp(allApps?20:62);root.addView(hero,heroParams);
        TextView section=text(allApps?"应用库":"常用",12,BlueRedTheme.FAINT);root.addView(section,new LinearLayout.LayoutParams(-1,dp(28)));
        ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);scroll.setPadding(0,dp(8),0,dp(12));scroll.setVerticalScrollBarEnabled(false);
        GridLayout grid=new GridLayout(this);grid.setColumnCount(5);grid.setClipChildren(false);grid.setClipToPadding(false);
        List<App> apps=applications();int width=(getResources().getDisplayMetrics().widthPixels-dp(112))/5-dp(16);
        View restore=null;
        for(int i=0;i<apps.size();i++){View card=addCard(grid,apps.get(i),i,width);if(apps.get(i).pkg.equals(focusedPackage))restore=card;}
        View more=addCard(grid,new App(allApps?"返回首页":"全部应用","__more__",null,null),apps.size(),width);
        if("__more__".equals(focusedPackage))restore=more;
        scroll.addView(grid,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=text("长按应用，整理首页",11,BlueRedTheme.FAINT);hint.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        root.addView(hint,new LinearLayout.LayoutParams(-1,dp(22)));setContentView(root);
        if(restore!=null)restore.requestFocus();else if(grid.getChildCount()>0)grid.getChildAt(0).requestFocus();tickClock();
    }
    private void tickClock(){if(clock!=null)clock.setText(new SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date()));if(date!=null)date.setText(new SimpleDateFormat("M月d日  EEE",Locale.CHINA).format(new Date()));}
    private TextView addNav(LinearLayout bar,String label,final boolean selected,final Runnable action){
        final TextView tab=text(label,14,selected?BlueRedTheme.BLUE:BlueRedTheme.MUTED);tab.setGravity(Gravity.CENTER);tab.setFocusable(true);tab.setClickable(true);
        tab.setBackground(surface(selected?0xff254d80:0,selected?0xff24416c:0,0));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(64),dp(34));p.rightMargin=dp(8);bar.addView(tab,p);
        tab.setOnFocusChangeListener(new View.OnFocusChangeListener(){public void onFocusChange(View v,boolean f){tab.setBackground(f?BlueRedTheme.focus(dp(15)):surface(selected?0xff254d80:0,selected?0xff24416c:0,0));tab.setTextColor(f?BlueRedTheme.DARK:selected?BlueRedTheme.BLUE:BlueRedTheme.MUTED);}});
        tab.setOnClickListener(new View.OnClickListener(){public void onClick(View v){action.run();}});
        return tab;
    }
    private void cleanBackground(){
        if(BackgroundCleaner.busy())return;
        if(cleanButton!=null){cleanButton.setEnabled(false);cleanButton.setText("清理中");}
        BackgroundCleaner.start(this,new BackgroundCleaner.Callback(){public void finished(String message){if(cleanButton!=null){cleanButton.setEnabled(true);cleanButton.setText("一键清理");}android.widget.Toast.makeText(getApplicationContext(),message,android.widget.Toast.LENGTH_LONG).show();}});
    }
    private View addCard(GridLayout grid,final App app,int index,int width){
        final LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setFocusable(true);card.setFocusableInTouchMode(true);card.setClickable(true);card.setClipChildren(false);
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=width;p.height=dp(190);p.setMargins(dp(8),dp(7),dp(8),dp(12));grid.addView(card,p);
        final boolean libre=app.pkg.equals("__libretv__");
        final LinearLayout face=new LinearLayout(this);face.setGravity(Gravity.CENTER);face.setBackground(surface(libre?0xff185995:0xff214f83,libre?0xff702e5e:0xff20395f,0));card.addView(face,new LinearLayout.LayoutParams(-1,dp(142)));
        final MinimalIcon glyph=app.icon==null?new MinimalIcon(app.pkg.equals("__libretv__")?"play":app.pkg.equals("__cast__")?"cast":allApps?"back":"grid",BlueRedTheme.BLUE):null;
        ImageView icon=new ImageView(this);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if(app.icon!=null)icon.setImageDrawable(app.icon);else icon.setImageDrawable(glyph);
        face.addView(icon,new LinearLayout.LayoutParams(dp(58),dp(58)));
        final TextView label=text(app.name,13,BlueRedTheme.MUTED);label.setGravity(Gravity.CENTER);label.setSingleLine(true);label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams=new LinearLayout.LayoutParams(-1,dp(36));nameParams.topMargin=dp(6);card.addView(label,nameParams);
        card.setOnFocusChangeListener(new View.OnFocusChangeListener(){public void onFocusChange(View v,boolean f){
            face.setBackground(f?BlueRedTheme.focus(dp(15)):surface(libre?0xff185995:0xff214f83,libre?0xff702e5e:0xff20395f,0));label.setTextColor(f?BlueRedTheme.TEXT:BlueRedTheme.MUTED);if(glyph!=null)glyph.color(f?BlueRedTheme.DARK:BlueRedTheme.BLUE);
            v.animate().scaleX(f?1.025f:1f).scaleY(f?1.025f:1f).setDuration(140).start();
            if(f){focusedPackage=app.pkg;heroTitle.setText(app.name);heroSubtitle.setText(description(app));}
        }});
        card.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            if(app.pkg.equals("__more__")){allApps=!allApps;focusedPackage=null;build();return;}
            try{if(AutostartPolicy.managed(MainActivity.this,app.pkg))AutostartPolicy.launch(MainActivity.this,app.pkg);else startActivity(app.intent);}catch(Exception e){message("暂时无法打开这个应用："+e.getMessage());}
        }});
        if(!app.pkg.equals("__more__"))card.setOnLongClickListener(new View.OnLongClickListener(){public boolean onLongClick(View v){organize(app);return true;}});return card;
    }
    private boolean isDefault(String pkg){if(pkg.equals("__libretv__")||pkg.equals("__cast__"))return true;for(String p:defaults)if(p.equals(pkg))return true;return false;}
    private void organize(final App app){
        final boolean visible=isDefault(app.pkg)?!preferences.getBoolean("hidden:"+app.pkg,false):preferences.getBoolean("favorite:"+app.pkg,false);
        new AlertDialog.Builder(this).setTitle(app.name).setItems(new String[]{visible?"从首页移除":"添加到首页","应用信息"},new android.content.DialogInterface.OnClickListener(){
            public void onClick(android.content.DialogInterface d,int which){
                if(which==0){SharedPreferences.Editor e=preferences.edit();if(isDefault(app.pkg))e.putBoolean("hidden:"+app.pkg,visible);else e.putBoolean("favorite:"+app.pkg,!visible);e.apply();focusedPackage=null;build();}
                else try{if(app.pkg.equals("__libretv__"))startActivity(new Intent(MainActivity.this,LibreTvLibraryActivity.class));else if(app.pkg.equals("__cast__"))startActivity(new Intent(MainActivity.this,CastActivity.class));else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+app.pkg)));}catch(Exception e){message("暂时无法打开应用信息");}
            }
        }).show();
    }
    private void showSettings(){
        new AlertDialog.Builder(this).setTitle("桌面设置").setItems(new String[]{"系统设置","背景颜色","蜡笔屏保","主页键设置","恢复原系统桌面","关于轻桌面","后台与自启动"},new android.content.DialogInterface.OnClickListener(){
            public void onClick(android.content.DialogInterface d,int which){
                if(which==0)try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception e){message("暂时无法打开系统设置");}
                if(which==1)new AlertDialog.Builder(MainActivity.this).setTitle("背景颜色").setSingleChoiceItems(new String[]{"蓝红","海蓝","莓红"},preferences.getInt("theme",0),new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface dialog,int item){preferences.edit().putInt("theme",item).apply();dialog.dismiss();build();}}).show();
                if(which==2)screenSettings();
                if(which==3)homeKeySettings();
                if(which==4)new AlertDialog.Builder(MainActivity.this).setTitle("恢复原系统桌面？").setMessage("重新显示原桌面包，清除轻桌面的默认主页设置，并解除本地设备管理。轻桌面和原有应用数据会保留。").setPositiveButton("恢复",new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface dialog,int item){try{HomePolicy.restore(MainActivity.this);}catch(RuntimeException e){message("恢复未完成："+e.getMessage());}}}).setNegativeButton("取消",null).show();
                if(which==6)autostartSettings();
                if(which==5)message("轻桌面 2.18\n极简首页 · LibreTV · 投屏 · 蜡笔屏保\n影视与天气由盒子独立运行，无需电脑开机。\n\n"+(HomePolicy.active(MainActivity.this)?"已固定主页并管理系统广告。可以在设置中恢复原系统桌面。":"当前尚未完成默认主页配置。")+"\n长按应用可整理首页常用应用。");
            }
        }).show();
    }
    private void autostartSettings(){
        final java.util.Set<String> selected=AutostartPolicy.selected(this);final boolean[] checked=new boolean[AutostartPolicy.PACKAGES.length];for(int n=0;n<checked.length;n++)checked[n]=selected.contains(AutostartPolicy.PACKAGES[n]);
        new AlertDialog.Builder(this).setTitle("后台与自启动").setMultiChoiceItems(new String[]{"旧 FLauncher 桌面 · 停用","云视听小电视 · 按需启动","银河奇异果 · 按需启动"},checked,new android.content.DialogInterface.OnMultiChoiceClickListener(){public void onClick(android.content.DialogInterface d,int n,boolean on){if(on)selected.add(AutostartPolicy.PACKAGES[n]);else selected.remove(AutostartPolicy.PACKAGES[n]);}})
            .setPositiveButton("保存",new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface d,int w){try{AutostartPolicy.configure(MainActivity.this,selected);build();message("已保存。选中的视频应用从轻桌面点开即可使用；回桌面且无播放后会限制后台。\n轻桌面 M13 投屏保留；视频应用自己的专用投屏需先打开对应应用。\n取消勾选可恢复原状。");}catch(Exception e){message("设置未完成："+e.getMessage());}}})
            .setNeutralButton("说明",new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface d,int w){message("仅手动打开：闲置时停用后台入口，点开应用时恢复。应用、账号和观看记录会保留。\n播放期间保留后台。轻桌面 M13 投屏正常；第三方应用自己的专用投屏需先打开对应应用。\n这台系统不提供单独的自启动开关，因此使用按需启用方式。");}}).setNegativeButton("返回",null).show();
    }
    private void screenSettings(){
        final boolean on=ScreenPolicy.enabled(this);
        new AlertDialog.Builder(this).setTitle("蜡笔日历与天气").setMessage("福州 · 日历、时钟和天气动画\n"+(on?"闲置后自动显示，按任意键返回。":"屏保已关闭。")+"\n"+CrayonAppearance.summary(this)+"\n天气每 15 分钟尝试更新，断网时标注更新时间。\n天气数据：Open-Meteo")
            .setPositiveButton("预览",new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface d,int w){startActivity(new Intent(MainActivity.this,CrayonActivity.class));}})
            .setNeutralButton("显示设置",new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface d,int w){screenDisplaySettings();}})
            .setNegativeButton("返回",null).show();
    }
    private void screenDisplaySettings(){
        final boolean on=ScreenPolicy.enabled(this);
        new AlertDialog.Builder(this).setTitle("屏保显示").setItems(new String[]{"日夜模式","切换时间",on?"关闭屏保":"开启屏保"},new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface d,int which){
            if(which==0){final String[] modes={"solar","auto","day","night"};String mode=CrayonAppearance.mode(MainActivity.this);int selected="auto".equals(mode)?1:"day".equals(mode)?2:"night".equals(mode)?3:0;
                new AlertDialog.Builder(MainActivity.this).setTitle("日夜模式").setSingleChoiceItems(new String[]{"按福州日出日落自动切换","按固定时间自动切换","始终日间","始终夜间"},selected,new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface dialog,int n){CrayonAppearance.set(MainActivity.this,modes[n],CrayonAppearance.day(MainActivity.this),CrayonAppearance.night(MainActivity.this));dialog.dismiss();screenSettings();}}).setNegativeButton("返回",null).show();
            }else if(which==1){int minute=CrayonAppearance.day(MainActivity.this);android.app.TimePickerDialog picker=new android.app.TimePickerDialog(MainActivity.this,new android.app.TimePickerDialog.OnTimeSetListener(){public void onTimeSet(android.widget.TimePicker v,int h,int m){final int day=h*60+m;int night=CrayonAppearance.night(MainActivity.this);android.app.TimePickerDialog end=new android.app.TimePickerDialog(MainActivity.this,new android.app.TimePickerDialog.OnTimeSetListener(){public void onTimeSet(android.widget.TimePicker v,int h,int m){try{CrayonAppearance.set(MainActivity.this,"auto",day,h*60+m);screenSettings();}catch(IllegalArgumentException e){message(e.getMessage());}}},night/60,night%60,true);end.setTitle("夜间开始 · 北京时间");end.show();}},minute/60,minute%60,true);picker.setTitle("日间开始 · 北京时间");picker.show();
            }else try{ScreenPolicy.setEnabled(MainActivity.this,!on);ScreenPolicy.applyWindow(MainActivity.this);screenSettings();}catch(Exception e){message("屏保设置未完成："+e.getMessage());}
        }}).setNegativeButton("返回",null).show();
    }
    private void homeKeySettings(){
        if(HomePolicy.active(this)){message("轻桌面已设为默认主页，开机和主页键都使用此桌面。无需后台按键接管服务。需要恢复时，请选择“恢复原系统桌面”。");return;}
        boolean enabled=preferences.getBoolean("home_key_enabled",false);
        new AlertDialog.Builder(this).setTitle("主页键兼容模式 · "+(enabled?"已开启":"已关闭"))
            .setMessage("开启后，按主页键仍会先显示原桌面，约 5 秒后返回轻桌面。这个模式不能替代默认桌面设置。")
            .setPositiveButton("开启",new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface d,int w){preferences.edit().putBoolean("home_key_enabled",true).apply();startService(new Intent(MainActivity.this,HomeKeyService.class));}})
            .setNegativeButton("关闭",new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface d,int w){preferences.edit().putBoolean("home_key_enabled",false).apply();stopService(new Intent(MainActivity.this,HomeKeyService.class));}}).show();
    }
    private void message(String value){new AlertDialog.Builder(this).setMessage(value).setPositiveButton("确定",null).show();}
}
