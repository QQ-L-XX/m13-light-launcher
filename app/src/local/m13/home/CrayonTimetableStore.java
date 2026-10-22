package local.m13.home;

import android.content.*;
import org.json.*;

/** Personal timetable stays in app-private storage; no names or photo assets. */
public final class CrayonTimetableStore {
    private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("crayon-timetable",0);}
    public static long revision(Context c){return prefs(c).getLong("revision",0);}
    private static CrayonTimetable parse(String raw)throws Exception{
        JSONArray all=new JSONObject(raw).getJSONArray("week");if(all.length()!=7)throw new IllegalArgumentException("需要七天课程");String[][] week=new String[7][];
        for(int d=0;d<7;d++){JSONArray day=all.getJSONArray(d);week[d]=new String[day.length()];for(int n=0;n<day.length();n++)week[d][n]=day.getString(n);}return new CrayonTimetable(week);
    }
    public static CrayonTimetable read(Context c){try{String raw=prefs(c).getString("week","");return raw.isEmpty()?null:parse(raw);}catch(Exception e){return null;}}
    public static void configure(Context c,String raw)throws Exception{if(raw==null||raw.length()>8192)throw new IllegalArgumentException("课程表数据无效");parse(raw);if(!prefs(c).edit().putString("week",raw).putLong("revision",revision(c)+1).commit())throw new IllegalStateException("课程表未保存");}
    public static JSONObject status(Context c,long time)throws Exception{CrayonTimetable table=read(c);JSONObject result=new JSONObject().put("configured",table!=null).put("timezone","Asia/Shanghai").put("revision",revision(c));if(table!=null){CrayonTimetable.Day day=table.tomorrow(time);result.put("date",day.date).put("weekday",day.weekday).put("courses",new JSONArray(java.util.Arrays.asList(day.courses())));}return result;}
}
