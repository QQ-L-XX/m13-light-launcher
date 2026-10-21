package local.m13.home;

import java.util.Calendar;
import java.util.Locale;

/** City-level sunrise/sunset estimates using NOAA's General Solar Position equations.
 * Zenith 90.833 degrees includes the usual solar-disk/refraction correction.
 * No network/location permission; this does not account for local terrain or buildings.
 * Reference: https://gml.noaa.gov/grad/solcalc/solareqns.PDF
 */
public final class CrayonSolar {
    public static final double LATITUDE=26.0745,LONGITUDE=119.2965;
    private static Times cached;
    public static final class Times {
        public final String date;
        public final long sunrise,sunset;
        public final int sunriseMinute,sunsetMinute;
        Times(String date,long midnight,int rise,int set){this.date=date;sunriseMinute=rise;sunsetMinute=set;sunrise=midnight+rise*60000L;sunset=midnight+set*60000L;}
        public boolean night(long now){return now<sunrise||now>=sunset;}
    }
    public static synchronized Times forTime(long now){
        Calendar c=Calendar.getInstance(CrayonTheme.ZONE,Locale.CHINA);c.setTimeInMillis(now);
        String key=String.format(Locale.US,"%04d-%02d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH));
        if(cached!=null&&key.equals(cached.date))return cached;
        int day=c.get(Calendar.DAY_OF_YEAR),days=c.getActualMaximum(Calendar.DAY_OF_YEAR);
        double offset=CrayonTheme.ZONE.getOffset(now)/60000.0;
        int rise=event(day,days,offset,true),set=event(day,days,offset,false);
        if(rise<0||set>=1440||rise>=set)throw new IllegalStateException("福州日出日落计算结果无效");
        c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
        cached=new Times(key,c.getTimeInMillis(),rise,set);return cached;
    }
    private static int event(int day,int days,double offset,boolean sunrise){
        double minute=720,latitude=Math.toRadians(LATITUDE);
        for(int i=0;i<5;i++){
            double gamma=2*Math.PI/days*(day-1+(minute/60-12)/24);
            double eq=229.18*(0.000075+0.001868*Math.cos(gamma)-0.032077*Math.sin(gamma)-0.014615*Math.cos(2*gamma)-0.040849*Math.sin(2*gamma));
            double decl=0.006918-0.399912*Math.cos(gamma)+0.070257*Math.sin(gamma)-0.006758*Math.cos(2*gamma)+0.000907*Math.sin(2*gamma)-0.002697*Math.cos(3*gamma)+0.00148*Math.sin(3*gamma);
            double cosine=Math.cos(Math.toRadians(90.833))/(Math.cos(latitude)*Math.cos(decl))-Math.tan(latitude)*Math.tan(decl);
            if(Double.isNaN(cosine)||cosine<=-1||cosine>=1)throw new IllegalStateException("福州太阳高度计算结果无效");
            double angle=Math.toDegrees(Math.acos(cosine));
            minute=720-4*(LONGITUDE+(sunrise?angle:-angle))-eq+offset;
        }
        return (int)Math.round(minute);
    }
}
