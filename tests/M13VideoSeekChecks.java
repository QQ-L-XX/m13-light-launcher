import local.m13.home.VideoSeek;
public final class M13VideoSeekChecks {
    private static int checks;
    private static void ok(boolean value){checks++;if(!value)throw new AssertionError("Seek check "+checks);}
    public static void main(String[] args){VideoSeek s=new VideoSeek();
        ok(s.step(30000,0,10000)==-1);ok(!s.pending());
        ok(s.step(30000,120000,10000)==40000);ok(s.step(30000,120000,10000)==50000);ok(s.step(30000,120000,-10000)==40000);
        s.applied(50000);ok(s.pending());s.applied(40000);ok(!s.pending());ok(s.step(41000,120000,10000)==51000);
        ok(s.select(-100000,120000)==0);ok(s.select(Long.MAX_VALUE,120000)==119000);ok(s.select(0,500)==0);
        ok(VideoSeek.clock(0).equals("00:00"));ok(VideoSeek.clock(65000).equals("01:05"));ok(VideoSeek.clock(3601000).equals("1:00:01"));
        for(int n=0;n<100;n++){int target=s.step(0,60000,10000);ok(target>=0&&target<=59000);}s.select(20000,60000);for(int n=0;n<100;n++){int target=s.step(20000,60000,-10000);ok(target>=0&&target<=59000);}
        System.out.println("Remote seek accumulation and boundary checks PASS: "+checks);
    }
}
