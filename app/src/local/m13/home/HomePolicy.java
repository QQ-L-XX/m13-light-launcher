package local.m13.home;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public final class HomePolicy {
    private static final String STOCK="com.youku.taitan.tv";
    private static final String ADVERT="com.yunos.advert.service";
    private static final String SCREENSAVER="com.android.dreams.phototable";
    private static DevicePolicyManager manager(Context c){return (DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);}
    private static ComponentName admin(Context c){return new ComponentName(c,HomeAdmin.class);}
    public static boolean isOwner(Context c){DevicePolicyManager d=manager(c);return d!=null&&d.isDeviceOwnerApp(c.getPackageName());}
    public static boolean active(Context c){return isOwner(c)&&c.getSharedPreferences("home",0).getBoolean("managed_home",false);}
    public static void applyIfOwner(Context c){
        if(!isOwner(c))return;
        DevicePolicyManager d=manager(c);ComponentName a=admin(c);
        blockSystemAds(c,d,a);
        blockScreensaver(c,d,a);
        try{
            if(active(c)&&d.isApplicationHidden(a,STOCK))return;
            IntentFilter f=new IntentFilter(Intent.ACTION_MAIN);f.addCategory(Intent.CATEGORY_HOME);f.addCategory(Intent.CATEGORY_DEFAULT);
            d.addPersistentPreferredActivity(a,f,new ComponentName(c,MainActivity.class));
            // This YunOS PackageManager returns false after a successful hide.
            // Confirm the stored state instead of treating its return value as failure.
            if(!d.isApplicationHidden(a,STOCK))d.setApplicationHidden(a,STOCK,true);
            if(!d.isApplicationHidden(a,STOCK))throw new IllegalStateException("Hidden state not confirmed");
            c.getSharedPreferences("home",0).edit().putBoolean("managed_home",true).putBoolean("home_key_enabled",false).remove("policy_error").commit();
            c.stopService(new Intent(c,HomeKeyService.class));
            Log.i("M13HomePolicy","Managed Home configured; stock launcher hidden");
        }catch(RuntimeException e){
            c.getSharedPreferences("home",0).edit().putString("policy_error",e.toString()).commit();
            Log.e("M13HomePolicy","Default Home setup failed",e);
            try{d.setApplicationHidden(a,STOCK,false);d.clearPackagePersistentPreferredActivities(a,c.getPackageName());}catch(RuntimeException ignored){}
        }
    }
    private static void blockSystemAds(Context c,DevicePolicyManager d,ComponentName a){
        try{
            if(!d.isApplicationHidden(a,ADVERT))d.setApplicationHidden(a,ADVERT,true);
            if(d.isApplicationHidden(a,ADVERT)){
                c.getSharedPreferences("home",0).edit().putBoolean("advert_blocked",true).remove("advert_error").commit();
                Log.i("M13HomePolicy","System advertising package hidden");
            }else throw new IllegalStateException("Advertising service remains visible");
        }catch(RuntimeException e){c.getSharedPreferences("home",0).edit().putString("advert_error",e.toString()).commit();Log.e("M13HomePolicy","Ad service blocking failed",e);}
    }
    private static void blockScreensaver(Context c,DevicePolicyManager d,ComponentName a){
        try{if(!d.isApplicationHidden(a,SCREENSAVER)){d.setApplicationHidden(a,SCREENSAVER,true);if(d.isApplicationHidden(a,SCREENSAVER))c.getSharedPreferences("home",0).edit().putBoolean("screensaver_hidden_by_home",true).commit();}
            if(!d.isApplicationHidden(a,SCREENSAVER))throw new IllegalStateException("Screensaver remains visible");
            c.getSharedPreferences("home",0).edit().remove("screensaver_error").commit();
        }catch(RuntimeException e){c.getSharedPreferences("home",0).edit().putString("screensaver_error",e.toString()).commit();Log.e("M13HomePolicy","Screensaver blocking failed",e);}
    }
    public static void restore(Context c){
        AutostartPolicy.restoreAll(c);
        ScreenPolicy.restore(c);
        DevicePolicyManager d=manager(c);ComponentName a=admin(c);
        if(isOwner(c)){
            // A per-user uninstall preserves the system APK; reinstall it for this user first.
            d.enableSystemApp(a,STOCK);
            if(d.isApplicationHidden(a,STOCK))d.setApplicationHidden(a,STOCK,false);
            if(d.isApplicationHidden(a,STOCK))throw new IllegalStateException("Original launcher remains hidden");
            if(c.getSharedPreferences("home",0).getBoolean("advert_blocked",false)){
                d.enableSystemApp(a,ADVERT);
                if(d.isApplicationHidden(a,ADVERT))d.setApplicationHidden(a,ADVERT,false);
                if(d.isApplicationHidden(a,ADVERT))throw new IllegalStateException("Advertising service remains hidden");
            }
            if(c.getSharedPreferences("home",0).getBoolean("screensaver_hidden_by_home",false)){
                if(d.isApplicationHidden(a,SCREENSAVER))d.setApplicationHidden(a,SCREENSAVER,false);
                if(d.isApplicationHidden(a,SCREENSAVER))throw new IllegalStateException("Screensaver remains hidden");
            }
            d.clearPackagePersistentPreferredActivities(a,c.getPackageName());
            d.clearDeviceOwnerApp(c.getPackageName());
        }
        if(d.isAdminActive(a))d.removeActiveAdmin(a);
        c.getPackageManager().clearPackagePreferredActivities(c.getPackageName());
        c.getSharedPreferences("home",0).edit().putBoolean("managed_home",false).putBoolean("home_key_enabled",false).putBoolean("advert_blocked",false).putBoolean("screensaver_hidden_by_home",false).commit();
        c.stopService(new Intent(c,HomeKeyService.class));
        Intent original=new Intent();original.setComponent(new ComponentName(STOCK,"com.youku.tv.home.activity.HomeActivity"));original.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(original);
        Log.i("M13HomePolicy","Original Home restored; local device management released");
    }
}
