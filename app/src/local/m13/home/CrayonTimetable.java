package local.m13.home;

import java.util.*;

/** Local weekly timetable, ISO weekday order, using the screen's China timezone. */
public final class CrayonTimetable {
    private final String[][] week;
    public static final class Day {
        public final String date,weekday;public final int isoDay;
        private final String[] courses;
        private Day(Calendar c,String[] values){isoDay=(c.get(Calendar.DAY_OF_WEEK)+5)%7+1;weekday=new String[]{"周一","周二","周三","周四","周五","周六","周日"}[isoDay-1];date=String.format(Locale.US,"%04d-%02d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));courses=values.clone();}
        public String[] courses(){return courses.clone();}
    }
    public CrayonTimetable(String[][] values){
        if(values==null||values.length!=7)throw new IllegalArgumentException("课程表需要星期一至星期日七组数据");week=new String[7][];
        for(int d=0;d<7;d++){if(values[d]==null||(values[d].length!=0&&values[d].length!=7))throw new IllegalArgumentException("每天应为七节课程，休息日用空数组");week[d]=values[d].clone();for(int n=0;n<week[d].length;n++){String v=week[d][n];if(v==null||v.trim().isEmpty()||v.length()>32||v.indexOf('\n')>=0||v.indexOf('\r')>=0)throw new IllegalArgumentException("课程名称无效");week[d][n]=v.trim();}}
    }
    public Day tomorrow(long time){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"),Locale.CHINA);c.setTimeInMillis(time);c.add(Calendar.DAY_OF_MONTH,1);int day=(c.get(Calendar.DAY_OF_WEEK)+5)%7;return new Day(c,week[day]);}
}
