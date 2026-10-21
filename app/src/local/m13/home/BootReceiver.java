package local.m13.home;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class BootReceiver extends BroadcastReceiver {
    public void onReceive(final Context context,Intent intent){
        if(!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))return;
        if(HomePolicy.active(context)){
            HomePolicy.applyIfOwner(context);AutostartPolicy.boot(context);
            Intent home=new Intent(context,MainActivity.class);home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);context.startActivity(home);return;
        }
        if(!context.getSharedPreferences("home",Context.MODE_PRIVATE).getBoolean("home_key_enabled",false))return;
        context.startService(new Intent(context,HomeKeyService.class));
        final PendingResult pending=goAsync();
        new Handler(context.getMainLooper()).postDelayed(new Runnable(){public void run(){
            try{Intent home=new Intent(context,MainActivity.class);home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);context.startActivity(home);}finally{pending.finish();}
        }},5000);
    }
}
