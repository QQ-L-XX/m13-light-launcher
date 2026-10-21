package local.m13.home;

import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import java.util.*;
import org.json.*;

/** Reversible Device Owner app availability; no root or component-permission bypass. */
public final class AutostartPolicy {
    public static final String[] PACKAGES={"me.efesser.flauncher","com.xiaodianshi.tv.yst","com.gitvdideo.aliapp"};
    public static final String[] LABELS={"旧 FLauncher 桌面","云视听小电视","银河奇异果"};
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("manual_start_apps",0);}
    private static DevicePolicyManager manager(Context c){return (DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);}
    private static ComponentName admin(Context c){return new ComponentName(c,HomeAdmin.class);}
    private static boolean known(String name){for(String p:PACKAGES)if(p.equals(name))return true;return false;}
    public static Set<String> selected(Context c){return new HashSet<String>(prefs(c).getStringSet("selected",Collections.<String>emptySet()));}
    public static boolean managed(Context c,String name){return selected(c).contains(name);}
    public static Intent intent(Context c,String name){
        Intent launch=c.getPackageManager().getLaunchIntentForPackage(name);if(launch!=null)return launch;
        String saved=prefs(c).getString("launch:"+name,"");ComponentName component=ComponentName.unflattenFromString(saved);
        return component==null?null:new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    public static boolean mediaActive(Context c){
        AudioManager a=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        if(a==null||a.isMusicActive()||a.getMode()!=AudioManager.MODE_NORMAL)return true;
        try{JSONObject cast=new JSONObject(CastPlaybackActivity.diagnostics()),libre=new JSONObject(LibreTvActivity.diagnostics());return cast.optBoolean("playing")||libre.optBoolean("playing");}catch(Exception unavailable){return true;}
    }
    public static void configure(Context c,Set<String> wanted){
        if(!HomePolicy.isOwner(c))throw new IllegalStateException("尚未启用桌面管理");
        for(String name:wanted)if(!known(name))throw new IllegalArgumentException("只能设置已列出的应用");
        SharedPreferences p=prefs(c);Set<String> old=selected(c);DevicePolicyManager d=manager(c);SharedPreferences.Editor edit=p.edit();
        for(String name:wanted)if(!old.contains(name)){
            try{c.getPackageManager().getApplicationInfo(name,PackageManager.GET_UNINSTALLED_PACKAGES);}catch(PackageManager.NameNotFoundException e){throw new IllegalArgumentException("应用未安装");}
            Intent launch=c.getPackageManager().getLaunchIntentForPackage(name);
            if(launch==null||launch.getComponent()==null)throw new IllegalStateException("无法记录应用打开入口");
            edit.putBoolean("was_hidden:"+name,d.isApplicationHidden(admin(c),name)).putString("launch:"+name,launch.getComponent().flattenToString());
        }
        // Restore removed targets before forgetting their previous state.
        for(String name:old)if(!wanted.contains(name)){
            boolean before=p.getBoolean("was_hidden:"+name,false);d.setApplicationHidden(admin(c),name,before);
            if(d.isApplicationHidden(admin(c),name)!=before)throw new IllegalStateException("应用恢复未完成");
            edit.remove("was_hidden:"+name).remove("opened:"+name);
        }
        if(!edit.putStringSet("selected",new HashSet<String>(wanted)).commit())throw new IllegalStateException("设置未保存");
        applyIfIdle(c);
    }
    public static void applyIfIdle(Context c){
        if(!HomePolicy.isOwner(c)||selected(c).isEmpty()||mediaActive(c))return;
        DevicePolicyManager d=manager(c);long now=System.currentTimeMillis();String error="";
        for(String name:selected(c))try{
            if(Math.abs(now-prefs(c).getLong("opened:"+name,0))<20000)continue;
            if(!d.isApplicationHidden(admin(c),name))d.setApplicationHidden(admin(c),name,true);
            if(!d.isApplicationHidden(admin(c),name))throw new IllegalStateException("后台限制未生效");
        }catch(RuntimeException e){error=name+": "+e.getMessage();}
        prefs(c).edit().putString("error",error).apply();
    }
    public static void boot(Context c){
        SharedPreferences.Editor edit=prefs(c).edit();for(String name:selected(c))edit.remove("opened:"+name);edit.commit();applyIfIdle(c);
    }
    public static void launch(Context c,String name){
        if(!known(name)||!managed(c,name))throw new IllegalArgumentException("应用未设为按需启动");
        Intent launch=intent(c,name);if(launch==null)throw new IllegalStateException("找不到应用入口");
        DevicePolicyManager d=manager(c);d.setApplicationHidden(admin(c),name,false);
        if(d.isApplicationHidden(admin(c),name))throw new IllegalStateException("应用启用未完成");
        prefs(c).edit().putLong("opened:"+name,System.currentTimeMillis()).commit();
        ScreenPolicy.awaken(c);c.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    public static void restoreAll(Context c){if(!selected(c).isEmpty())configure(c,Collections.<String>emptySet());}
    public static String diagnostics(Context c){try{JSONObject result=new JSONObject().put("owner",HomePolicy.isOwner(c)).put("mediaProtected",mediaActive(c)).put("error",prefs(c).getString("error","")).put("selected",new JSONArray(selected(c)));JSONArray targets=new JSONArray();DevicePolicyManager d=manager(c);for(int n=0;n<PACKAGES.length;n++){String name=PACKAGES[n];targets.put(new JSONObject().put("package",name).put("name",LABELS[n]).put("managed",managed(c,name)).put("hidden",d.isApplicationHidden(admin(c),name)).put("launch",prefs(c).getString("launch:"+name,"")));}return result.put("targets",targets).toString();}catch(Exception e){return "{}";}}
}
