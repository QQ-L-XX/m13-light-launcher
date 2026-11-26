package local.m13.home;

/** Monotonic retention windows; a clock reset never produces indefinite retention. */
public final class ManagedAppGrace {
    public static long duration(String name){return "com.xiaodianshi.tv.yst".equals(name)?120000:20000;}
    public static long remaining(String name,long opened,long returned,long now){long start=returned>0?returned:opened;if(start<=0||now<start)return 0;long elapsed=now-start;return Math.max(0,duration(name)-elapsed);}
}
