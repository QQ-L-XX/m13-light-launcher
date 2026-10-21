package local.m13.home;

import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.os.PowerManager;
import android.view.WindowManager;
import org.json.JSONObject;

public final class ScreenPolicy {
    private static final String[] KEYS={"screensaver_components","screensaver_enabled","screensaver_activate_on_sleep","screensaver_activate_on_dock"};
    private static String component(Context c){return new ComponentName(c,CrayonDreamService.class).flattenToString();}
    public static boolean enabled(Context c){return component(c).equals(Settings.Secure.getString(c.getContentResolver(),KEYS[0]))&&Settings.Secure.getInt(c.getContentResolver(),KEYS[1],0)==1;}
    public static void applyWindow(Activity a){if(enabled(a))a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    public static void install(Context c)throws Exception{
        if(c.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("屏保配置权限尚未授予");
        SharedPreferences p=c.getSharedPreferences("crayon_screen",0);
        if(!p.getBoolean("backup_saved",false)){JSONObject backup=new JSONObject();for(String key:KEYS){String value=Settings.Secure.getString(c.getContentResolver(),key);backup.put(key,value==null?JSONObject.NULL:value);}p.edit().putString("backup",backup.toString()).putBoolean("backup_saved",true).commit();}
        put(c,KEYS[0],component(c));put(c,KEYS[1],"1");put(c,KEYS[2],"1");put(c,KEYS[3],"1");
        if(!enabled(c))throw new IllegalStateException("系统屏保选择未生效");CrayonWeather.refresh(c);
    }
    private static void put(Context c,String key,String value){if(!Settings.Secure.putString(c.getContentResolver(),key,value))throw new IllegalStateException("无法保存屏保设置");}
    public static void setEnabled(Context c,boolean on)throws Exception{if(on)install(c);else if(component(c).equals(Settings.Secure.getString(c.getContentResolver(),KEYS[0])))put(c,KEYS[1],"0");}
    public static void restore(Context c){SharedPreferences p=c.getSharedPreferences("crayon_screen",0);if(!p.getBoolean("backup_saved",false))return;try{JSONObject backup=new JSONObject(p.getString("backup","{}"));if(component(c).equals(Settings.Secure.getString(c.getContentResolver(),KEYS[0])))for(String key:KEYS)put(c,key,backup.isNull(key)?null:backup.optString(key,null));p.edit().remove("backup").remove("backup_saved").commit();}catch(Exception e){throw new IllegalStateException("原屏保设置恢复未完成",e);}}
    @SuppressWarnings("deprecation")
    public static void awaken(Context c){
        PowerManager power=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
        // On this vendor Android, ending a dream alone immediately enters sleep.
        // Wake before dismissing it, and bridge the handoff to the player's window.
        if(power!=null&&(CrayonDreamService.showing()||!power.isInteractive())){
            PowerManager.WakeLock bridge=power.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.ON_AFTER_RELEASE,"m13:cast-wakeup");
            bridge.setReferenceCounted(false);bridge.acquire(10000);
        }
        CrayonDreamService.awaken();CrayonActivity.awaken();
    }
    public static String diagnostics(Context c){try{JSONObject d=new JSONObject().put("enabled",enabled(c)).put("selected",Settings.Secure.getString(c.getContentResolver(),KEYS[0])).put("dreamShowing",CrayonDreamService.showing()).put("dreamFrames",CrayonDreamService.frames()).put("previewFrames",CrayonActivity.frames()).put("appearance",CrayonAppearance.diagnostics(c)).put("dreamAppearance",CrayonDreamService.appearance()).put("previewAppearance",CrayonActivity.appearance()).put("dreamPaletteChanges",CrayonDreamService.paletteChanges()).put("previewPaletteChanges",CrayonActivity.paletteChanges()).put("dreamTomorrowIconCode",CrayonDreamService.tomorrowIconCode()).put("previewTomorrowIconCode",CrayonActivity.tomorrowIconCode()).put("weather",new JSONObject(CrayonWeather.diagnostics()));return d.toString();}catch(Exception e){return "{}";}}
}
