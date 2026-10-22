import local.m13.home.CrayonTimetable;
import java.util.*;
import java.text.*;

public final class M13TimetableChecks {
    private static int checks;
    private static void ok(boolean v){checks++;if(!v)throw new AssertionError("Timetable check "+checks);}
    private static long at(String s)throws Exception{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));return f.parse(s).getTime();}
    public static void main(String[] args)throws Exception{
        String[][] week=new String[7][];for(int d=0;d<7;d++){week[d]=d<5?new String[7]:new String[0];for(int n=0;n<week[d].length;n++)week[d][n]="D"+(d+1)+"-"+(n+1);}
        CrayonTimetable table=new CrayonTimetable(week);week[0][0]="changed";
        for(int n=0;n<7;n++){long time=at("2026-10-19 12:00")+n*86400000L;CrayonTimetable.Day day=table.tomorrow(time);int iso=(n+1)%7+1;ok(day.isoDay==iso);ok(day.courses().length==(iso<=5?7:0));if(iso<=5)ok(day.courses()[0].equals("D"+iso+"-1"));}
        ok(table.tomorrow(at("2026-10-25 23:59")).courses()[0].equals("D1-1"));
        ok(table.tomorrow(at("2026-10-22 23:59")).date.equals("2026-10-23"));
        ok(table.tomorrow(at("2026-10-23 00:00")).date.equals("2026-10-24"));
        ok(table.tomorrow(at("2026-12-31 22:00")).date.equals("2027-01-01"));
        ok(table.tomorrow(at("2028-02-28 23:59")).date.equals("2028-02-29"));
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));ok(table.tomorrow(at("2026-10-22 00:01")).date.equals("2026-10-23"));
        String[] copy=table.tomorrow(at("2026-10-25 12:00")).courses();copy[0]="changed";ok(table.tomorrow(at("2026-10-25 12:00")).courses()[0].equals("D1-1"));
        try{new CrayonTimetable(new String[6][]);ok(false);}catch(IllegalArgumentException expected){ok(true);}
        week[0]=new String[]{"only one"};try{new CrayonTimetable(week);ok(false);}catch(IllegalArgumentException expected){ok(true);}
        System.out.println("Timetable date/rollover checks PASS: "+checks);
    }
}
