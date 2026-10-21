package local.m13.home;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

public final class CrayonAppearance {
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("crayon_screen",0);}
    public static String mode(Context c){return prefs(c).getString("appearance","solar");}
    public static int day(Context c){return prefs(c).getInt("day_start",CrayonTheme.DAY_START);}
    public static int night(Context c){return prefs(c).getInt("night_start",CrayonTheme.NIGHT_START);}
    public static boolean isNight(Context c){return isNightAt(c,System.currentTimeMillis());}
    public static boolean isNightAt(Context c,long time){return "solar".equals(mode(c))?CrayonSolar.forTime(time).night(time):CrayonTheme.night(mode(c),day(c),night(c),CrayonTheme.minute(time));}
    public static void set(Context c,String mode,int day,int night){
        if(!"solar".equals(mode)&&!"auto".equals(mode)&&!"day".equals(mode)&&!"night".equals(mode))throw new IllegalArgumentException("未知显示模式");
        CrayonTheme.validate(day,night);
        if(!prefs(c).edit().putString("appearance",mode).putInt("day_start",day).putInt("night_start",night).commit())throw new IllegalStateException("显示设置未保存");
    }
    public static String summary(Context c){
        if("solar".equals(mode(c))){CrayonSolar.Times t=CrayonSolar.forTime(System.currentTimeMillis());return "按日出日落自动切换 · 福州\n今日约 "+CrayonTheme.time(t.sunriseMinute)+" 日出 / "+CrayonTheme.time(t.sunsetMinute)+" 日落\n按北京时间本地估算，每天更新，无需联网。";}
        return ("auto".equals(mode(c))?"固定时间切换 · 日间 "+CrayonTheme.time(day(c))+" / 夜间 "+CrayonTheme.time(night(c)):"day".equals(mode(c))?"始终日间":"始终夜间")+"\n按福州时间（北京时间），无需联网。";
    }
    public static JSONObject diagnostics(Context c)throws Exception{return diagnostics(c,System.currentTimeMillis());}
    public static JSONObject diagnostics(Context c,long time)throws Exception{
        CrayonSolar.Times t=CrayonSolar.forTime(time);
        return new JSONObject().put("mode",mode(c)).put("dayStart",day(c)).put("nightStart",night(c)).put("timezone","Asia/Shanghai").put("effective",isNightAt(c,time)?"night":"day").put("minute",CrayonTheme.minute(time)).put("evaluatedAt",time)
            .put("solar",new JSONObject().put("city","福州").put("date",t.date).put("latitude",CrayonSolar.LATITUDE).put("longitude",CrayonSolar.LONGITUDE).put("sunriseAt",t.sunrise).put("sunsetAt",t.sunset).put("sunrise",CrayonTheme.time(t.sunriseMinute)).put("sunset",CrayonTheme.time(t.sunsetMinute)).put("source","NOAA_local_estimate").put("networkRequired",false));
    }
}
