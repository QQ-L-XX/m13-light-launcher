package local.m13.home;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import java.util.Calendar;
import java.util.TimeZone;

public final class NewsSchedule extends BroadcastReceiver {
    static final String STOP = "local.m13.home.NEWS_STOP";
    private static final String TICK = "local.m13.home.NEWS_TICK";
    private static final int START = 7 * 60 + 10;
    private static final int END = 7 * 60 + 50;
    private static int launchedDay;
    static boolean enabled(Context c) {
        return c.getSharedPreferences("news",0).getBoolean("enabled",true);
    }
    static Calendar now() { return Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")); }
    static boolean inWindow(Calendar t) {
        int minute=t.get(Calendar.HOUR_OF_DAY)*60+t.get(Calendar.MINUTE);
        return minute>=START && minute<END;
    }
    static void schedule(Context c) {
        AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        for(int minute:new int[]{10,50}) {
            Intent i=new Intent(c,NewsSchedule.class).setAction(minute==10?TICK:STOP);
            PendingIntent pi=PendingIntent.getBroadcast(c,minute,i,PendingIntent.FLAG_UPDATE_CURRENT);
            if(!enabled(c)){ alarms.cancel(pi); continue; }
            Calendar t=now();
            int time=minute==10?START:END;
            t.set(Calendar.HOUR_OF_DAY,time/60); t.set(Calendar.MINUTE,time%60);
            t.set(Calendar.SECOND,0); t.set(Calendar.MILLISECOND,0);
            if(t.getTimeInMillis()<=System.currentTimeMillis()) t.add(Calendar.DATE,1);
            alarms.setExact(AlarmManager.RTC_WAKEUP,t.getTimeInMillis(),pi);
        }
    }
    private static int day(Calendar t) {
        return t.get(Calendar.YEAR)*1000+t.get(Calendar.DAY_OF_YEAR);
    }
    static void userStopped(Context c) {
        Calendar t=now();
        if(inWindow(t)) c.getSharedPreferences("news",0).edit().putInt("stoppedDay",day(t)).commit();
    }
    static void userResumed(Context c) {
        SharedPreferences prefs=c.getSharedPreferences("news",0);
        if(prefs.contains("stoppedDay")) prefs.edit().remove("stoppedDay").commit();
    }
    static boolean reconcile(Context c, boolean boot) {
        schedule(c);
        Calendar t=now();
        if(!enabled(c)||!inWindow(t)) return false;
        SharedPreferences prefs=c.getSharedPreferences("news",0);
        int today=day(t);
        if(prefs.getInt("stoppedDay",0)==today)return false;
        if(launchedDay==today && (!boot || NewsActivity.scheduledActive()))return NewsActivity.scheduledActive();
        c.startActivity(new Intent(c,NewsActivity.class).putExtra("scheduled",true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        launchedDay=today;
        return true;
    }
    public void onReceive(Context c,Intent i){
        if(STOP.equals(i.getAction())){
            NewsActivity.stopAtEnd(); schedule(c);
        }else {
            if(!inWindow(now())) NewsActivity.stopScheduledOnly();
            reconcile(c,false);
        }
    }
}
