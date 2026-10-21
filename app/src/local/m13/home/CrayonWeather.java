package local.m13.home;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** City-level public weather; a cache is always labelled with its observation time. */
public final class CrayonWeather {
    public static final String CITY="福州";
    private static final String URL="https://api.open-meteo.com/v1/forecast?latitude=26.0745&longitude=119.2965&current=temperature_2m,is_day,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=2&timezone=Asia%2FShanghai";
    private static final ExecutorService worker=Executors.newSingleThreadExecutor();
    private static volatile Snapshot snapshot;
    private static volatile String error="";
    private static long lastAttempt;
    private static boolean loaded,busy;
    public static final class Snapshot {
        final String asOf;final long time,fetched;final int code;final boolean day;
        final double temperature,wind;
        final Forecast[] forecasts;
        Snapshot(JSONObject root,long fetched)throws Exception{
            JSONObject current=root.getJSONObject("current");
            temperature=current.getDouble("temperature_2m");
            if(Double.isNaN(temperature)||temperature<-90||temperature>65)throw new Exception("天气数据不完整");
            asOf=current.getString("time");SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm",Locale.US);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));f.setLenient(false);time=f.parse(asOf).getTime();
            this.fetched=fetched;code=current.getInt("weather_code");day=current.optInt("is_day",1)==1;wind=current.optDouble("wind_speed_10m",Double.NaN);
            JSONObject daily=root.optJSONObject("daily");
            JSONArray dates=daily==null?null:daily.optJSONArray("time");
            forecasts=new Forecast[dates==null?0:Math.min(dates.length(),14)];
            for(int n=0;n<forecasts.length;n++){
                JSONArray lo=daily.optJSONArray("temperature_2m_min"),hi=daily.optJSONArray("temperature_2m_max"),codes=daily.optJSONArray("weather_code");
                double min=number(lo,n),max=number(hi,n);int dailyCode=codes==null||codes.isNull(n)?-1:codes.optInt(n,-1);
                String date=dates.optString(n,"");
                if(date.matches("\\d{4}-\\d{2}-\\d{2}")&&!Double.isNaN(min)&&!Double.isNaN(max)&&min>=-90&&max<=65&&min<=max)forecasts[n]=new Forecast(date,dailyCode,min,max);
            }
        }
        private static double number(JSONArray a,int n){return a==null||a.isNull(n)?Double.NaN:a.optDouble(n,Double.NaN);}
        Forecast forDate(String date){for(Forecast f:forecasts)if(f!=null&&date.equals(f.date))return f;return null;}
        boolean forecastFresh(){return fetched>0&&Math.abs(System.currentTimeMillis()-fetched)<18*60*60*1000L;}
        boolean fresh(){return Math.abs(System.currentTimeMillis()-time)<3*60*60*1000L;}
    }
    public static final class Forecast {
        final String date;final int code;final double min,max;
        Forecast(String date,int code,double min,double max){this.date=date;this.code=code;this.min=min;this.max=max;}
        String range(){return Math.round(min)+"° / "+Math.round(max)+"°";}
        String summary(){return "明天 · "+(code>=0?label(code)+"  ":"")+range();}
        JSONObject json()throws Exception{return new JSONObject().put("date",date).put("weatherCode",code).put("minC",min).put("maxC",max);}
    }
    static String dateOffset(long time,int days){Calendar date=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"),Locale.CHINA);date.setTimeInMillis(time);date.add(Calendar.DAY_OF_MONTH,days);SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setTimeZone(date.getTimeZone());return f.format(date.getTime());}
    public static Snapshot get(){return snapshot;}
    public static String error(){return error;}
    public static synchronized void refresh(Context context){
        final Context app=context.getApplicationContext();final SharedPreferences prefs=app.getSharedPreferences("crayon_weather",0);
        if(!loaded){loaded=true;try{String raw=prefs.getString("json","");if(!raw.isEmpty())snapshot=new Snapshot(new JSONObject(raw),prefs.getLong("fetched",0));}catch(Exception ignored){}}
        long now=System.currentTimeMillis();if(busy||now-lastAttempt<15*60*1000L)return;
        Forecast nextDay=snapshot==null?null:snapshot.forDate(dateOffset(now,1));
        if(snapshot!=null&&snapshot.fresh()&&now-snapshot.fetched<15*60*1000L&&nextDay!=null&&nextDay.code>=0)return;
        lastAttempt=now;busy=true;
        worker.submit(new Runnable(){public void run(){try{
            LibreTvNet.init(app);String raw=LibreTvNet.get(URL);long fetched=System.currentTimeMillis();Snapshot next=new Snapshot(new JSONObject(raw),fetched);
            if(Math.abs(fetched-next.time)>6*60*60*1000L)throw new Exception("天气时间需要更新");
            prefs.edit().putString("json",raw).putLong("fetched",fetched).apply();snapshot=next;error="";
        }catch(Exception e){error="天气更新暂时不可用";}finally{synchronized(CrayonWeather.class){busy=false;}}}});
    }
    public static String label(int code){
        if(code==0)return "晴";if(code==1)return "晴间多云";if(code==2)return "多云";if(code==3)return "阴";
        if(code==45||code==48)return "雾";if(code>=51&&code<=57)return "细雨";if(code>=61&&code<=67)return "雨";
        if(code>=71&&code<=77)return "雪";if(code>=80&&code<=82)return "阵雨";if(code==85||code==86)return "阵雪";if(code>=95)return "雷阵雨";return "天气";
    }
    public static String diagnostics(){try{Snapshot s=snapshot;JSONObject d=new JSONObject().put("city",CITY).put("busy",busy).put("error",error).put("available",s!=null);if(s!=null){d.put("asOf",s.asOf).put("temperatureC",s.temperature).put("weatherCode",s.code).put("fresh",s.fresh()).put("fetchedAt",s.fetched).put("forecastFresh",s.forecastFresh());Forecast today=s.forDate(dateOffset(System.currentTimeMillis(),0)),tomorrow=s.forDate(dateOffset(System.currentTimeMillis(),1));if(today!=null)d.put("today",today.json());if(tomorrow!=null)d.put("tomorrow",tomorrow.json()).put("tomorrowText",s.forecastFresh()?tomorrow.summary():"明天 · 预报待更新");}return d.toString();}catch(Exception e){return "{}";}}
}
