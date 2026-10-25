package local.m13.home;

import android.content.*;
import org.json.*;

/** Personal timetable stays in app-private storage; no names or photo assets. */
public final class CrayonTimetableStore {
    private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("crayon-timetable",0);}
    public static JSONObject config(Context c)throws Exception{String raw=prefs(c).getString("week","");if(!raw.isEmpty())return new JSONObject(raw);JSONArray week=new JSONArray();for(int n=0;n<7;n++)week.put(new JSONArray());return new JSONObject().put("week",week);}
    public static long revision(Context c){return prefs(c).getLong("revision",0);}
    static CrayonTimetable parse(String raw)throws Exception{
        JSONArray all=new JSONObject(raw).getJSONArray("week");if(all.length()!=7)throw new IllegalArgumentException("需要七天课程");String[][] week=new String[7][];
        for(int d=0;d<7;d++){JSONArray day=all.getJSONArray(d);week[d]=new String[day.length()];for(int n=0;n<day.length();n++)week[d][n]=day.getString(n);}JSONObject root=new JSONObject(raw);java.util.Set<String> holidays=new java.util.HashSet<String>();JSONArray off=root.optJSONArray("holidays");if(off!=null){if(off.length()>180)throw new IllegalArgumentException("休息日期过多");for(int n=0;n<off.length();n++)holidays.add(off.getString(n));}java.util.Map<String,String[]> changes=new java.util.HashMap<String,String[]>();JSONObject special=root.optJSONObject("overrides");if(special!=null){if(special.length()>180)throw new IllegalArgumentException("调课日期过多");java.util.Iterator<String> keys=special.keys();while(keys.hasNext()){String key=keys.next();JSONArray day=special.getJSONArray(key);String[] values=new String[day.length()];for(int n=0;n<values.length;n++)values[n]=day.getString(n);changes.put(key,values);}}return new CrayonTimetable(week,holidays,changes);
    }
    public static CrayonTimetable read(Context c){try{String raw=prefs(c).getString("week","");return raw.isEmpty()?null:parse(raw);}catch(Exception e){return null;}}
    public static void configure(Context c,String raw)throws Exception{if(raw==null||raw.length()>65536)throw new IllegalArgumentException("课程表数据无效");parse(raw);if(!prefs(c).edit().putString("week",raw).putLong("revision",revision(c)+1).commit())throw new IllegalStateException("课程表未保存");}
    public static JSONObject status(Context c,long time)throws Exception{CrayonTimetable table=read(c);JSONObject result=new JSONObject().put("configured",table!=null).put("timezone","Asia/Shanghai").put("revision",revision(c));if(table!=null){CrayonTimetable.Day day=table.tomorrow(time);CrayonTimetable.Day display=table.display(time);result.put("display",new JSONObject().put("date",display.date).put("weekday",display.weekday).put("label",display.label).put("courses",new JSONArray(java.util.Arrays.asList(display.courses()))));result.put("date",day.date).put("weekday",day.weekday).put("courses",new JSONArray(java.util.Arrays.asList(day.courses())));}return result;}
}
