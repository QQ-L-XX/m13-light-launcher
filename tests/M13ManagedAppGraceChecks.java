import local.m13.home.ManagedAppGrace;
public class M13ManagedAppGraceChecks {
 static int count;static void check(long actual,long expected){count++;if(actual!=expected)throw new AssertionError(actual+" != "+expected);}
 public static void main(String[] args){String bili="com.xiaodianshi.tv.yst",qiyi="com.gitvdideo.aliapp";
 check(ManagedAppGrace.remaining(bili,1000,0,120999),1);
 check(ManagedAppGrace.remaining(bili,1000,0,121000),0);
 check(ManagedAppGrace.remaining(bili,1000,500000,500001),119999);
 check(ManagedAppGrace.remaining(bili,0,0,500001),0);
 check(ManagedAppGrace.remaining(bili,500000,0,100),0);
 check(ManagedAppGrace.remaining(bili,0,500000,100),0);
 check(ManagedAppGrace.remaining(qiyi,1000,0,20999),1);
 check(ManagedAppGrace.remaining(qiyi,1000,0,21000),0);
 check(ManagedAppGrace.remaining(bili,1000,500000,1000000),0);
 System.out.println("Managed-app retention checks PASS: "+count);}
}