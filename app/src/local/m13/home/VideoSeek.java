package local.m13.home;

/** Keep rapid remote presses relative to the requested target, not stale decoder time. */
public final class VideoSeek {
    private int target;private boolean pending;
    public int step(int current,int duration,int delta){return select((long)(pending?target:current)+delta,duration);}
    public int select(long value,int duration){if(duration<=0)return -1;target=(int)Math.max(0,Math.min(value,Math.max(0,duration-1000L)));pending=true;return target;}
    public int target(){return target;}public boolean pending(){return pending;}
    public void applied(int sent){if(sent==target)pending=false;}
    public static String clock(int ms){int s=Math.max(0,ms)/1000;return s>=3600?String.format(java.util.Locale.US,"%d:%02d:%02d",s/3600,s/60%60,s%60):String.format(java.util.Locale.US,"%02d:%02d",s/60,s%60);}
}
