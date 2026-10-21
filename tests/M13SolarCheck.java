import local.m13.home.CrayonSolar;
import local.m13.home.CrayonTheme;
import java.text.SimpleDateFormat;
import java.util.*;

public class M13SolarCheck {
    private static int checks;
    private static void ok(boolean yes,String what){checks++;if(!yes)throw new AssertionError(what);}
    private static long parse(String text)throws Exception{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm",Locale.US);f.setTimeZone(CrayonTheme.ZONE);f.setLenient(false);return f.parse(text).getTime();}
    public static void main(String[] args)throws Exception{
        for(int year:new int[]{2024,2026}){
            Calendar c=Calendar.getInstance(CrayonTheme.ZONE);c.clear();c.set(year,0,1,12,0);
            while(c.get(Calendar.YEAR)==year){long noon=c.getTimeInMillis();CrayonSolar.Times t=CrayonSolar.forTime(noon);
                ok(t.sunriseMinute>270&&t.sunriseMinute<450&&t.sunsetMinute>990&&t.sunsetMinute<1200,"Fuzhou annual range "+t.date);
                ok(t.sunset-t.sunrise>9*3600000L&&t.sunset-t.sunrise<15*3600000L,"Day length "+t.date);
                ok(t.night(t.sunrise-1)&&!t.night(t.sunrise),"Sunrise boundary "+t.date);
                ok(!t.night(t.sunset-1)&&t.night(t.sunset),"Sunset boundary "+t.date);
                ok(!t.night(noon),"Noon day "+t.date);
                c.add(Calendar.DAY_OF_MONTH,1);
            }
        }
        long now=parse("2026-10-21T12:00");CrayonSolar.Times original=CrayonSolar.forTime(now);
        TimeZone old=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
        CrayonSolar.forTime(parse("2026-10-22T12:00"));CrayonSolar.Times other=CrayonSolar.forTime(now);
        ok(original.sunrise==other.sunrise&&original.sunset==other.sunset,"Device timezone independence");TimeZone.setDefault(old);
        ok(CrayonSolar.forTime(parse("2024-02-29T12:00")).date.equals("2024-02-29"),"Leap day");
        ok(CrayonSolar.forTime(parse("2026-10-21T23:59")).date.equals("2026-10-21")&&CrayonSolar.forTime(parse("2026-10-22T00:00")).date.equals("2026-10-22"),"Midnight recalculation");
        ok(CrayonTheme.night("night",420,1140,720)&&!CrayonTheme.night("day",420,1140,0),"Manual modes");
        ok(!CrayonTheme.night("auto",420,1140,420)&&CrayonTheme.night("auto",420,1140,1140),"Fixed schedule boundaries");
        if(args.length==2){long rise=parse(args[0]),set=parse(args[1]);CrayonSolar.Times t=CrayonSolar.forTime(rise+3600000L);
            ok(Math.abs(t.sunrise-rise)<=5*60000L,"Independent sunrise reference within 5 minutes");
            ok(Math.abs(t.sunset-set)<=5*60000L,"Independent sunset reference within 5 minutes");
            System.out.println("{\"referenceSunriseDeltaSeconds\":"+(t.sunrise-rise)/1000+",\"referenceSunsetDeltaSeconds\":"+(t.sunset-set)/1000+"}");
        }
        CrayonSolar.Times t=CrayonSolar.forTime(now);
        System.out.println("{\"status\":\"PASS\",\"checks\":"+checks+",\"date\":\""+t.date+"\",\"sunrise\":\""+CrayonTheme.time(t.sunriseMinute)+"\",\"sunset\":\""+CrayonTheme.time(t.sunsetMinute)+"\",\"networkRequired\":false}");
    }
}
