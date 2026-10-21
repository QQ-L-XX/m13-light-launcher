package local.m13.home;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import java.util.*;
import java.io.*;
import org.json.*;

/** One explicit user action; uses the platform background-process API, without root. */
public final class BackgroundCleaner {
    private static volatile boolean busy;
    private static volatile String last="{}";
    public interface Callback {void finished(String message);}
    public static boolean busy(){return busy;}
    public static String status(){try{return new JSONObject(last).put("busy",busy).toString();}catch(Exception e){return "{}";}}
    public static synchronized void start(Context context,final Callback callback){
        if(busy)return;busy=true;final Context app=context.getApplicationContext();
        new Thread(new Runnable(){public void run(){String message;
            try{JSONObject result=clean(app);last=result.toString();message=result.getString("message");}
            catch(Exception e){message="清理暂未完成，请稍后再试";try{last=new JSONObject().put("error",e.toString()).put("message",message).toString();}catch(Exception ignored){}}
            final String text=message;busy=false;new Handler(Looper.getMainLooper()).post(new Runnable(){public void run(){callback.finished(text);}});
        }},"m13-background-clean").start();
    }
    private static JSONObject clean(Context c)throws Exception{
        ActivityManager am=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
        if(am==null)throw new IllegalStateException("Activity manager unavailable");
        AudioManager audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        JSONObject result=new JSONObject().put("at",System.currentTimeMillis()).put("ownPid",Process.myPid()).put("rootUsed",false);
        // On vendor Android we cannot reliably attribute another app's audio session.
        // Keep all media-related background work when audio is active or in a call.
        if(audio==null||audio.isMusicActive()||audio.getMode()!=AudioManager.MODE_NORMAL)return result.put("mediaProtected",true).put("requested",new JSONArray()).put("confirmedStopped",new JSONArray()).put("message","播放相关后台已保留，暂不清理");
        PackageManager pm=c.getPackageManager();Set<String> launchable=new HashSet<String>();
        for(String category:new String[]{Intent.CATEGORY_LAUNCHER,"android.intent.category.LEANBACK_LAUNCHER"}){
            Intent query=new Intent(Intent.ACTION_MAIN).addCategory(category);
            for(ResolveInfo info:pm.queryIntentActivities(query,0))launchable.add(info.activityInfo.packageName);
        }
        List<ActivityManager.RunningAppProcessInfo> processes=am.getRunningAppProcesses();
        if(processes==null)throw new IllegalStateException("Running processes unavailable");
        Set<Integer> protectedUids=new HashSet<Integer>();protectedUids.add(Process.myUid());
        for(ActivityManager.RunningAppProcessInfo p:processes)if(p.importance<ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE)protectedUids.add(p.uid);
        List<ActivityManager.RunningServiceInfo> services=am.getRunningServices(200);
        if(services!=null)for(ActivityManager.RunningServiceInfo service:services)if(service.foreground)protectedUids.add(service.uid);
        // This Android 5.1 build limits the public process list to our UID. Its
        // readable /proc status files still expose process UIDs, without root.
        Set<Integer> observed=processUids(processes);Set<String> candidates=new LinkedHashSet<String>();
        result.put("apiProcessCount",processes.size()).put("observedUidCount",observed.size());
        for(Integer uid:observed){
            if(uid<10000||protectedUids.contains(uid))continue;
            String[] uidPackages=pm.getPackagesForUid(uid);
            // Shared/system UIDs can contain unrelated critical components: skip the UID.
            if(uidPackages==null||uidPackages.length!=1)continue;
            String name=uidPackages[0];if(!launchable.contains(name)||name.equals(c.getPackageName()))continue;
            ApplicationInfo info;try{info=pm.getApplicationInfo(name,0);}catch(PackageManager.NameNotFoundException missing){continue;}
            if((info.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP|ApplicationInfo.FLAG_PERSISTENT))!=0)continue;
            candidates.add(name);
        }
        JSONArray requested=new JSONArray(),failed=new JSONArray();
        for(String name:candidates){
            // Recheck visibility and audio immediately before each platform request.
            if(audio.isMusicActive()||audio.getMode()!=AudioManager.MODE_NORMAL)break;
            List<ActivityManager.RunningAppProcessInfo> current=am.getRunningAppProcesses();if(current==null)break;
            boolean visible=false;for(ActivityManager.RunningAppProcessInfo p:current)if(p.pkgList!=null&&Arrays.asList(p.pkgList).contains(name)&&p.importance<ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE)visible=true;
            if(visible)continue;
            try{am.killBackgroundProcesses(name);requested.put(name);}catch(SecurityException denied){failed.put(name);}
        }
        if(requested.length()>0)Thread.sleep(1500);
        List<ActivityManager.RunningAppProcessInfo> after=am.getRunningAppProcesses();
        Set<Integer> afterUids=after==null?null:processUids(after);
        JSONArray confirmed=new JSONArray(),remaining=new JSONArray();
        if(afterUids!=null)for(int n=0;n<requested.length();n++){String name=requested.getString(n);boolean alive=afterUids.contains(pm.getApplicationInfo(name,0).uid);if(alive)remaining.put(name);else confirmed.put(name);}
        String message=confirmed.length()>0?"已清理 "+confirmed.length()+" 个闲置应用":requested.length()>0?"后台已整理，部分应用仍在运行":failed.length()>0?"系统暂未允许清理":"后台无需清理";
        return result.put("mediaProtected",false).put("requested",requested).put("confirmedStopped",confirmed).put("remaining",remaining).put("failed",failed).put("message",message);
    }
    private static Set<Integer> processUids(List<ActivityManager.RunningAppProcessInfo> known){
        Set<Integer> uids=new HashSet<Integer>();for(ActivityManager.RunningAppProcessInfo p:known)uids.add(p.uid);
        File[] entries=new File("/proc").listFiles();if(entries==null)return uids;
        for(File entry:entries){String name=entry.getName();if(name.isEmpty()||name.charAt(0)<'0'||name.charAt(0)>'9')continue;
            try{BufferedReader reader=new BufferedReader(new FileReader(new File(entry,"status")));try{String line;while((line=reader.readLine())!=null)if(line.startsWith("Uid:")){uids.add(Integer.parseInt(line.substring(4).trim().split("\\s+")[0]));break;}}finally{reader.close();}}catch(Exception unavailable){}
        }return uids;
    }
}
