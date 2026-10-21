package local.m13.home;

import android.app.Service;
import android.app.Notification;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class HomeKeyService extends Service {
    private final Handler handler=new Handler();
    private final Runnable show=new Runnable(){public void run(){openHome();}};
    private final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())&&"homekey".equals(intent.getStringExtra("reason"))){
            Log.i("M13Home","Home event received");handler.removeCallbacks(show);handler.postDelayed(show,350);
        }
    }};
    public void onCreate(){
        super.onCreate();registerReceiver(receiver,new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        Notification notification=new Notification.Builder(this).setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("简洁桌面").setContentText("主页键服务已开启").setOngoing(true).build();
        startForeground(13,notification);
    }
    public int onStartCommand(Intent intent,int flags,int id){
        if(!getSharedPreferences("home",MODE_PRIVATE).getBoolean("home_key_enabled",false)){stopSelf();return START_NOT_STICKY;}
        return START_STICKY;
    }
    private void openHome(){
        if(!getSharedPreferences("home",MODE_PRIVATE).getBoolean("home_key_enabled",false))return;
        try{
            ActivityManager manager=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
            if(!manager.getAppTasks().isEmpty()){manager.getAppTasks().get(0).moveToFront();Log.i("M13Home","Own task move requested");}
        }catch(RuntimeException e){Log.i("M13Home","Own task unavailable; opening home");}
        Intent intent=new Intent(this,MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(intent);
    }
    public IBinder onBind(Intent intent){return null;}
    public void onDestroy(){handler.removeCallbacks(show);unregisterReceiver(receiver);stopForeground(true);super.onDestroy();}
}
