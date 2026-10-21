package local.m13.home;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Local clock schedule; independent of network/weather freshness and device timezone. */
public final class CrayonTheme {
    public static final TimeZone ZONE=TimeZone.getTimeZone("Asia/Shanghai");
    public static final int DAY_START=420,NIGHT_START=1140;
    public static boolean night(String mode,int dayStart,int nightStart,int minute){
        if("day".equals(mode))return false;if("night".equals(mode))return true;
        validate(dayStart,nightStart);
        boolean day=dayStart<nightStart?minute>=dayStart&&minute<nightStart:minute>=dayStart||minute<nightStart;
        return !day;
    }
    public static void validate(int day,int night){if(day<0||day>=1440||night<0||night>=1440||day==night)throw new IllegalArgumentException("日间与夜间时间需不同");}
    public static int minute(long time){Calendar c=Calendar.getInstance(ZONE,Locale.CHINA);c.setTimeInMillis(time);return c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);}
    public static String time(int minute){return String.format(Locale.US,"%02d:%02d",minute/60,minute%60);}
    public static final Palette DAY=new Palette(false),NIGHT=new Palette(true);
    public static final class Palette {
        public final boolean night;
        public final int background,card,shadow,ink,accent,blue,coral,yellow,muted,faint,today,binding,cloudFill,wet,moon,star,grain;
        private Palette(boolean n){night=n;
            background=n?0xff101a30:0xfff5f8ff;card=n?0xff182641:0xfffffafd;shadow=n?0xff0b1326:0xffdce8f7;
            ink=n?0xffadbed9:0xff233c68;accent=n?0xff668fbd:0xff4289c6;blue=n?0xff739dc8:0xff6fbcf1;coral=n?0xffc9789c:0xffed4779;yellow=n?0xffb5a06c:0xffe5bc60;
            muted=n?0xff7d92b3:0xff60799e;faint=n?0xff465b7a:0xffb0bfd5;today=n?0xff623d64:0xffffc5d9;binding=n?0xff546b8f:0xff93aacb;
            cloudFill=n?0xff2b3c58:0xffd2e0de;wet=n?0xff637e9e:0xff8eabb6;moon=n?0xff8d9dbc:0xffb8bac8;star=n?0xff9b927a:0xffb6ad89;grain=n?0x0b91abcd:0x0d9f9580;
        }
    }
}
