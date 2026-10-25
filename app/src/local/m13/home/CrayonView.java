package local.m13.home;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.*;

/** A quiet, locally drawn crayon calendar. The paper/calendar layer is cached. */
public final class CrayonView extends View {
    private CrayonTheme.Palette colors=CrayonTheme.DAY;
    public long paletteChanges;private long appearanceChecked,timetableRevision=-1;
    private int INK,ACCENT,BLUE,CORAL,YELLOW;
    public String appearance(){return colors.night?"night":"day";}
    private void refreshAppearance(){long time=SystemClock.elapsedRealtime();if(appearanceChecked!=0&&time-appearanceChecked<500)return;appearanceChecked=time;CrayonTheme.Palette next=CrayonAppearance.isNight(getContext())?CrayonTheme.NIGHT:CrayonTheme.DAY;
        long revision=CrayonTimetableStore.revision(getContext());if(timetableRevision!=revision){timetableRevision=revision;paper=null;}
        if(paper==null||colors!=next){colors=next;INK=colors.ink;ACCENT=colors.accent;BLUE=colors.blue;CORAL=colors.coral;YELLOW=colors.yellow;paper=null;sunImage=null;moonImage=null;cloudImage=null;forecastImage=null;paletteChanges++;setBackgroundColor(colors.background);}}
    private final Handler handler=new Handler();private final Paint paint=new Paint(3);
    private final Typeface hand,body=Typeface.create("sans-serif",Typeface.NORMAL);
    private final TimeZone zone=TimeZone.getTimeZone("Asia/Shanghai");
    private Bitmap paper,sunImage,moonImage,cloudImage;private int cloudColor;
    private final Path strokePath=new Path(),cloudPath=new Path();
    private String paperDate="";private boolean running;private long started=SystemClock.elapsedRealtime();
    private CrayonWeather.Snapshot forecastSnapshot;private String forecastDate="",todayRange="",tomorrowText="明天 · 预报更新中";private boolean forecastFresh;
    private Bitmap forecastImage;private int tomorrowCode=-1,forecastImageCode=-1;private final RectF forecastBounds=new RectF();
    public int tomorrowIconCode(){return tomorrowCode;}
    public long frames;
    public CrayonView(Context c){super(c);Typeface f;try{f=Typeface.createFromAsset(c.getAssets(),"PatrickHand-Regular.ttf");}catch(Exception e){f=Typeface.create("casual",0);}hand=f;setFocusable(true);setBackgroundColor(0xfff5f8ff);}
    private final Runnable tick=new Runnable(){public void run(){if(!running)return;invalidate();handler.postDelayed(this,66);}};
    protected void onAttachedToWindow(){super.onAttachedToWindow();running=true;CrayonWeather.refresh(getContext());handler.post(tick);}
    protected void onDetachedFromWindow(){running=false;handler.removeCallbacksAndMessages(null);paper=null;sunImage=null;moonImage=null;cloudImage=null;forecastImage=null;super.onDetachedFromWindow();}
    private Calendar now(){return Calendar.getInstance(zone,Locale.CHINA);}
    private void text(Canvas c,String value,float x,float y,float size,int color,Typeface font,Paint.Align align){paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTypeface(font);paint.setTextSize(size);paint.setTextAlign(align);paint.setStrokeWidth(1);c.drawText(value,x,y,paint);}
    private void line(Canvas c,float x1,float y1,float x2,float y2,int color,float width,long seed){
        Random r=new Random(seed);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        for(int pass=0;pass<4;pass++){paint.setColor(color);paint.setAlpha(pass==0?115:42);paint.setStrokeWidth(width*(pass==0?1f:.6f));Path p=strokePath;p.rewind();int steps=Math.max(3,(int)(Math.hypot(x2-x1,y2-y1)/12));for(int i=0;i<=steps;i++){float t=i/(float)steps,jx=(r.nextFloat()-.5f)*width*.45f,jy=(r.nextFloat()-.5f)*width*.45f;float x=x1+(x2-x1)*t+jx,y=y1+(y2-y1)*t+jy;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}c.drawPath(p,paint);}paint.setAlpha(255);
    }
    private void circle(Canvas c,float cx,float cy,float radius,int color,float width,long seed){
        Random r=new Random(seed);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
        for(int pass=0;pass<5;pass++){paint.setColor(color);paint.setAlpha(pass==0?130:42);paint.setStrokeWidth(width*(pass==0?1:.55f));Path p=strokePath;p.rewind();for(int i=0;i<=64;i++){double a=i*Math.PI*2/64;float rad=radius+(r.nextFloat()-.5f)*width*.7f;float x=cx+(float)Math.cos(a)*rad,y=cy+(float)Math.sin(a)*rad;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);}c.drawPath(p,paint);}paint.setAlpha(255);
    }
    private void disc(Canvas c,float cx,float cy,float radius,int color,long seed){
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setAlpha(110);c.drawCircle(cx,cy,radius,paint);paint.setAlpha(255);
        Random r=new Random(seed);for(int i=0;i<35;i++){float yy=(r.nextFloat()*2-1)*radius,xx=(float)Math.sqrt(Math.max(0,radius*radius-yy*yy));line(c,cx-xx*.94f,cy+yy,cx+xx*.94f,cy+yy+2,color,1.5f,i+seed);}circle(c,cx,cy,radius,color,3,seed);
    }
    private void paper(Calendar date){
        paper=Bitmap.createBitmap(1280,720,Bitmap.Config.RGB_565);Canvas c=new Canvas(paper);c.drawColor(colors.background);
        Random random=new Random(1858);paint.setStrokeWidth(.6f);for(int n=0;n<2800;n++){paint.setColor(n%2==0?colors.grain:(colors.night?0x047e9bc2:0x18ffffff));float x=random.nextInt(1280),y=random.nextInt(720);c.drawLine(x,y,x+random.nextInt(7)+1,y+(random.nextFloat()-.5f)*2,paint);}
        text(c,"轻桌面 · 蜡笔日历",70,57,14,colors.muted,body,Paint.Align.LEFT);text(c,"福州",1208,57,18,INK,body,Paint.Align.RIGHT);
        int calendarLayer=c.save();c.translate(66,86);c.scale(.90f,.90f);c.translate(-66,-114);
        // Uneven paper edges, loose binding and wax strokes rather than a UI card.
        paint.setColor(colors.shadow);paint.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(72,123,675,656),13,13,paint);
        paint.setColor(colors.card);c.drawRoundRect(new RectF(66,114,668,647),11,11,paint);
        line(c,69,118,665,116,ACCENT,2.5f,31);line(c,666,118,668,644,ACCENT,2.5f,32);line(c,668,645,67,647,ACCENT,2.5f,33);line(c,68,645,67,117,ACCENT,2.5f,34);
        for(int x:new int[]{157,575}){circle(c,x,119,11,colors.binding,3,98+x);paint.setStyle(Paint.Style.FILL);paint.setColor(colors.background);c.drawCircle(x,113,4,paint);}
        int year=date.get(Calendar.YEAR),month=date.get(Calendar.MONTH)+1,today=date.get(Calendar.DAY_OF_MONTH);
        text(c,String.valueOf(month),111,237,101,INK,hand,Paint.Align.LEFT);float numberWidth;paint.setTextSize(101);paint.setTypeface(hand);numberWidth=paint.measureText(String.valueOf(month));text(c,"月",121+numberWidth,230,26,INK,body,Paint.Align.LEFT);
        text(c,String.valueOf(year),622,231,29,ACCENT,hand,Paint.Align.RIGHT);line(c,108,261,625,260,ACCENT,3,561);
        String[] weekdays={"一","二","三","四","五","六","日"};float start=143,step=74;
        for(int i=0;i<7;i++)text(c,weekdays[i],start+i*step,297,15,i>=5?CORAL:colors.muted,body,Paint.Align.CENTER);
        Calendar first=(Calendar)date.clone();first.set(Calendar.DAY_OF_MONTH,1);int offset=(first.get(Calendar.DAY_OF_WEEK)+5)%7,max=first.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar cell=(Calendar)first.clone();cell.add(Calendar.DAY_OF_MONTH,-offset);
        for(int n=0;n<42;n++){int d=cell.get(Calendar.DAY_OF_MONTH);boolean current=cell.get(Calendar.MONTH)==date.get(Calendar.MONTH);float x=start+(n%7)*step,y=344+(n/7)*49;
            if(current&&d==today){disc(c,x,y-9,21,colors.today,200);circle(c,x,y-9,22,CORAL,3.5f,201);}
            text(c,String.valueOf(d),x,y,29,current?(n%7>=5?CORAL:INK):colors.faint,hand,Paint.Align.CENTER);cell.add(Calendar.DAY_OF_MONTH,1);
        }
        // A little crayon garden along the bottom of the calendar.
        for(int n=0;n<8;n++){float x=122+n*66,y=627;line(c,x,y,x-2,y-13-n%3*4,ACCENT,2.5f,500+n);line(c,x,y-4,x-9,y-11,ACCENT,2.5f,530+n);line(c,x,y-6,x+8,y-16,ACCENT,2.5f,550+n);}
        c.restoreToCount(calendarLayer);drawTimetable(c,date);
        text(c,"按任意键返回",71,694,12,colors.muted,body,Paint.Align.LEFT);
        SimpleDateFormat key=new SimpleDateFormat("yyyyMMdd",Locale.US);key.setTimeZone(zone);paperDate=key.format(date.getTime());
    }
    protected void onDraw(Canvas canvas){super.onDraw(canvas);frames++;refreshAppearance();Calendar date=now();SimpleDateFormat dayKey=new SimpleDateFormat("yyyyMMdd",Locale.US);dayKey.setTimeZone(zone);if(paper==null||!paperDate.equals(dayKey.format(date.getTime())))paper(date);
        float scale=Math.min(getWidth()/1280f,getHeight()/720f)*(1f-TvUi.margin(getContext())*.025f);int saved=canvas.save();canvas.translate((getWidth()-1280*scale)/2,(getHeight()-720*scale)/2);canvas.scale(scale,scale);
        double t=(SystemClock.elapsedRealtime()-started)/1000d;canvas.translate((float)Math.sin(t/43)*4,(float)Math.cos(t/53)*2);
        paint.setAlpha(255);canvas.drawBitmap(paper,0,0,paint);
        int weatherLayer=canvas.save();canvas.translate(965,-10);canvas.scale(.90f,.86f);canvas.translate(-965,0);
        SimpleDateFormat clock=new SimpleDateFormat("HH:mm",Locale.US);clock.setTimeZone(zone);text(canvas,clock.format(date.getTime()),965,179,84,INK,hand,Paint.Align.CENTER);
        String[] days={"星期日","星期一","星期二","星期三","星期四","星期五","星期六"};text(canvas,days[date.get(Calendar.DAY_OF_WEEK)-1],965,214,17,colors.muted,body,Paint.Align.CENTER);
        CrayonWeather.Snapshot weather=CrayonWeather.get();boolean available=weather!=null;int code=available?weather.code:3;boolean daytime=!available||weather.day;
        weather(canvas,965,342,code,daytime,t);
        String temperature=available?Math.round(weather.temperature)+"°":"--°";text(canvas,temperature,965,521,101,INK,hand,Paint.Align.CENTER);
        text(canvas,available?CrayonWeather.label(code)+(weather.fresh()?"":" · 缓存"):"联网后获取天气",965,558,23,INK,body,Paint.Align.CENTER);
        updateForecast(weather,date);
        if(available&&!todayRange.isEmpty())text(canvas,"今天 "+todayRange,965,596,25,ACCENT,hand,Paint.Align.CENTER);
        plainLine(canvas,839,617,1091,617,colors.binding,1);
        drawTomorrow(canvas);
        canvas.restoreToCount(weatherLayer);
        String stamp=weather==null?"天气 open-meteo.com · 等待联网更新":"天气 open-meteo.com · "+weather.asOf.replace('T',' ')+(!weather.fresh()?" · 缓存 / 待更新":CrayonWeather.error().isEmpty()?"":" · 更新暂不可用");text(canvas,stamp,1208,693,11,colors.muted,body,Paint.Align.RIGHT);
        canvas.restoreToCount(saved);
        if(frames%900==1){CrayonWeather.refresh(getContext());AutostartPolicy.applyIfIdle(getContext());}
    }
    private void drawTimetable(Canvas c,Calendar date){
        CrayonTimetable table=CrayonTimetableStore.read(getContext());if(table==null)return;CrayonTimetable.Day day=table.display(date.getTimeInMillis());String[] courses=day.courses();
        text(c,day.label,70,591,18,INK,body,Paint.Align.LEFT);text(c,day.weekday+" · "+day.date.substring(5).replace('-','/'),193,591,14,ACCENT,hand,Paint.Align.LEFT);
        if(courses.length==0){line(c,70,612,1208,612,ACCENT,2,8100);text(c,day.isoDay>=6?"周末 · 暂无常规课程":"明天暂无常规课程",639,651,20,colors.muted,body,Paint.Align.CENTER);return;}
        text(c,"上午 1—4 节  /  下午 5—7 节",1208,591,12,colors.muted,body,Paint.Align.RIGHT);
        float gap=10,width=(1138-gap*6-10)/7;
        for(int n=0;n<7;n++){float left=70+n*(width+gap)+(n>=4?10:0),center=left+width/2;int color=n<4?BLUE:CORAL;
            paint.setStyle(Paint.Style.FILL);paint.setColor(colors.card);paint.setAlpha(220);c.drawRoundRect(new RectF(left,605,left+width,675),7,7,paint);paint.setAlpha(255);line(c,left+8,608,left+width-8,608,color,2.3f,8200+n);
            text(c,String.format(Locale.US,"%02d",n+1)+" · "+(n<4?"上午":"下午"),center,625,11,colors.muted,body,Paint.Align.CENTER);
            String value=courses[n];paint.setTypeface(body);paint.setTextSize(17);if(paint.measureText(value)<=width-14){text(c,value,center,653,17,INK,body,Paint.Align.CENTER);}else{int split=value.indexOf('（');if(split<=0){split=value.length()/2;}String first=value.substring(0,split),second=value.substring(split);fitCourse(c,first,center,646,width-14);fitCourse(c,second,center,666,width-14);}
        }
    }
    private void fitCourse(Canvas c,String value,float x,float y,float width){paint.setTypeface(body);paint.setTextSize(15);float size=Math.min(15,15*width/Math.max(1,paint.measureText(value)));text(c,value,x,y,size,INK,body,Paint.Align.CENTER);}
    private void updateForecast(CrayonWeather.Snapshot weather,Calendar date){
        boolean fresh=weather!=null&&weather.forecastFresh();
        if(forecastSnapshot==weather&&forecastDate.equals(paperDate)&&forecastFresh==fresh)return;
        forecastSnapshot=weather;forecastDate=paperDate;forecastFresh=fresh;todayRange="";tomorrowCode=-1;tomorrowText=weather==null?"明天 · 预报更新中":"明天 · 预报待更新";
        if(weather!=null){CrayonWeather.Forecast today=weather.forDate(CrayonWeather.dateOffset(date.getTimeInMillis(),0)),tomorrow=weather.forDate(CrayonWeather.dateOffset(date.getTimeInMillis(),1));if(today!=null)todayRange=today.range();if(tomorrow!=null){tomorrowText=tomorrow.summary()+(fresh?"":" · 缓存");tomorrowCode=tomorrow.code;}}
    }
    private void drawTomorrow(Canvas c){
        if(tomorrowCode<0){text(c,tomorrowText,965,649,18,colors.muted,body,Paint.Align.CENTER);return;}
        if(forecastImage==null||forecastImageCode!=tomorrowCode){
            forecastImageCode=tomorrowCode;forecastImage=Bitmap.createBitmap(96,80,Bitmap.Config.ARGB_8888);Canvas icon=new Canvas(forecastImage);icon.translate(48,32);icon.scale(.34f,.34f);
            // Daily forecast uses a sun symbol even with the night palette. Cache once,
            // so differing current/forecast cloud colors never churn bitmaps per frame.
            weather(icon,0,0,tomorrowCode,true,0);
        }
        paint.setTypeface(body);paint.setTextSize(18);float width=paint.measureText(tomorrowText),left=965-(58+width)/2;
        forecastBounds.set(left,621,left+48,661);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);c.drawBitmap(forecastImage,null,forecastBounds,paint);
        text(c,tomorrowText,left+58,649,18,colors.muted,body,Paint.Align.LEFT);
    }
    private void weather(Canvas c,float x,float y,int code,boolean day,double t){
        boolean sun=code==0||code==1||code==2,wet=code>=51&&code<=67||code>=80&&code<=82||code>=95,snow=code>=71&&code<=77||code==85||code==86;
        cacheWeather(wet?colors.wet:BLUE);
        if(sun){float sx=x+(code==2?-35:0),sy=y+(code==2?-25:0);
            if(day){int save=c.save();c.rotate((float)Math.sin(t/11)*3,sx,sy);paint.setAlpha(255);c.drawBitmap(sunImage,sx-110,sy-110,paint);c.restoreToCount(save);}
            else{paint.setAlpha(255);c.drawBitmap(moonImage,sx-110,sy-110,paint);for(int n=0;n<4;n++){float xx=sx-68+n*46,yy=sy-65+(n%2)*20;float r=3+(float)Math.sin(t+n)*1.2f;plainLine(c,xx-r,yy,xx+r,yy,colors.star,2);plainLine(c,xx,yy-r,xx,yy+r,colors.star,2);}}
        }
        if(code!=0&&code!=1){float drift=(float)Math.sin(t/5)*8;paint.setAlpha(255);c.drawBitmap(cloudImage,x+drift+(code==2?22:0)-140,y+(code==2?24:2)-105,paint);
            if(wet)for(int n=0;n<7;n++){float xx=x-70+n*23,yy=y+58+(float)((t*29+n*13)%40);plainLine(c,xx+3,yy-5,xx,yy+5,BLUE,3);}
            if(snow)for(int n=0;n<6;n++){float xx=x-65+n*25+(float)Math.sin(t+n)*5,yy=y+61+(float)((t*14+n*11)%40);plainLine(c,xx-3,yy,xx+3,yy,BLUE,2);plainLine(c,xx,yy-3,xx,yy+3,BLUE,2);}
            if(code==45||code==48)for(int n=0;n<3;n++)plainLine(c,x-75,y+55+n*11,x+65,y+55+n*11,colors.muted,3);
            if(code>=95){plainLine(c,x+6,y+45,x-7,y+65,YELLOW,6);plainLine(c,x-7,y+65,x+9,y+62,YELLOW,6);plainLine(c,x+9,y+62,x-3,y+84,YELLOW,6);}
        }
    }
    private void plainLine(Canvas c,float x1,float y1,float x2,float y2,int color,float width){paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x1,y1,x2,y2,paint);}
    private void cacheWeather(int color){
        if(sunImage==null){sunImage=Bitmap.createBitmap(220,220,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(sunImage);float sx=110,sy=110;disc(c,sx,sy,47,YELLOW,74);
            for(int ray=0;ray<12;ray++){double a=ray*Math.PI/6;line(c,sx+(float)Math.cos(a)*62,sy+(float)Math.sin(a)*62,sx+(float)Math.cos(a)*76,sy+(float)Math.sin(a)*76,YELLOW,4,750+ray);}
            line(c,sx-16,sy-5,sx-15,sy-2,INK,2,99);line(c,sx+15,sy-5,sx+16,sy-2,INK,2,98);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(INK);c.drawArc(new RectF(sx-10,sy-5,sx+10,sy+12),10,155,false,paint);
        }
        if(moonImage==null){moonImage=Bitmap.createBitmap(220,220,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(moonImage);disc(c,110,110,42,colors.moon,52);paint.setStyle(Paint.Style.FILL);paint.setColor(colors.background);c.drawCircle(129,98,39,paint);}
        if(cloudImage==null||cloudColor!=color){cloudColor=color;cloudImage=Bitmap.createBitmap(280,190,Bitmap.Config.ARGB_8888);cloud(new Canvas(cloudImage),140,105,color,1);}
    }
    private void cloud(Canvas c,float x,float y,int color,float scale){
        int save=c.save();c.translate(x,y);c.scale(scale,scale);Path cloud=cloudPath;cloud.rewind();cloud.moveTo(-77,29);cloud.cubicTo(-112,20,-104,-26,-70,-29);cloud.cubicTo(-68,-65,-18,-77,3,-44);cloud.cubicTo(33,-63,69,-45,67,-20);cloud.cubicTo(105,-22,114,25,79,33);cloud.cubicTo(42,39,-43,38,-77,29);cloud.close();
        paint.setStyle(Paint.Style.FILL);paint.setColor(colors.cloudFill);c.drawPath(cloud,paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(3);c.drawPath(cloud,paint);
        int texture=c.save();c.clipPath(cloud);
        for(int n=0;n<55;n++){float yy=-69+n*2;line(c,-109,yy,111,yy+7,color,1.2f,2000+n);}c.restoreToCount(texture);c.restoreToCount(save);
    }
}
