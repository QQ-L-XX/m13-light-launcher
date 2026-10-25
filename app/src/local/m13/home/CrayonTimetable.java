package local.m13.home;

import java.util.*;

/** Local weekly timetable, ISO weekday order, using the screen's China timezone. */
public final class CrayonTimetable {
    private final String[][] week;private final Set<String> holidays=new HashSet<String>();private final Map<String,String[]> overrides=new HashMap<String,String[]>();
    public static final class Day {
        public final String date,weekday;public final int isoDay;public String label="明日课程";
        private final String[] courses;
        private Day(Calendar c,String[] values){isoDay=(c.get(Calendar.DAY_OF_WEEK)+5)%7+1;weekday=new String[]{"周一","周二","周三","周四","周五","周六","周日"}[isoDay-1];date=String.format(Locale.US,"%04d-%02d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));courses=values.clone();}
        public String[] courses(){return courses.clone();}
    }
    public CrayonTimetable(String[][] values){
        if(values==null||values.length!=7)throw new IllegalArgumentException("课程表需要星期一至星期日七组数据");week=new String[7][];
        for(int d=0;d<7;d++){if(values[d]==null||(values[d].length!=0&&values[d].length!=7))throw new IllegalArgumentException("每天应为七节课程，休息日用空数组");week[d]=values[d].clone();for(int n=0;n<week[d].length;n++){String v=week[d][n];if(v==null||v.trim().isEmpty()||v.length()>32||v.indexOf('\n')>=0||v.indexOf('\r')>=0)throw new IllegalArgumentException("课程名称无效");week[d][n]=v.trim();}}
    }
    public CrayonTimetable(String[][] values,Set<String> off,Map<String,String[]> changed){this(values);if(off!=null)for(String date:off){validateDate(date);holidays.add(date);}if(changed!=null)for(Map.Entry<String,String[]> entry:changed.entrySet()){validateDate(entry.getKey());String[][] sample=new String[7][];for(int n=0;n<7;n++)sample[n]=entry.getValue();CrayonTimetable checked=new CrayonTimetable(sample);overrides.put(entry.getKey(),checked.week[0]);}}
    public static void validateDate(String date){try{if(date==null||!date.matches("\\d{4}-\\d{2}-\\d{2}"))throw new Exception();java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);f.parse(date);}catch(Exception e){throw new IllegalArgumentException("日期格式应为 YYYY-MM-DD");}}
    private static String key(Calendar c){return String.format(Locale.US,"%04d-%02d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));}
    private String[] values(Calendar c){String date=key(c);return holidays.contains(date)?new String[0]:overrides.containsKey(date)?overrides.get(date):week[(c.get(Calendar.DAY_OF_WEEK)+5)%7];}
    public Day tomorrow(long time){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"),Locale.CHINA);c.setTimeInMillis(time);c.add(Calendar.DAY_OF_MONTH,1);int day=(c.get(Calendar.DAY_OF_WEEK)+5)%7;return new Day(c,week[day]);}
    /** Upcoming weekday for display; raw tomorrow() remains available for configuration reads. */
    public Day display(long time){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"),Locale.CHINA);c.setTimeInMillis(time);int today=c.get(Calendar.DAY_OF_WEEK);c.add(Calendar.DAY_OF_MONTH,1);boolean weekend=false,off=false;for(int n=0;n<370;n++){String date=key(c);boolean changed=overrides.containsKey(date)&&overrides.get(date).length>0;if(holidays.contains(date)||((c.get(Calendar.DAY_OF_WEEK)==Calendar.SATURDAY||c.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY)&&!changed)||(overrides.containsKey(date)&&overrides.get(date).length==0)){off|=holidays.contains(date)||overrides.containsKey(date);weekend=true;c.add(Calendar.DAY_OF_MONTH,1);continue;}Day result=new Day(c,values(c));if(off)result.label="下次上课";else if(weekend||today==Calendar.SUNDAY)result.label="下周一课程";if(changed)result.label="调课安排";return result;}throw new IllegalStateException("休息日期过多，请检查课程设置");}
}
