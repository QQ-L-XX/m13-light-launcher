import local.m13.home.MusicLyrics;
import java.util.*;
public class M13MusicChecks {
    private static int count;
    private static void ok(boolean condition){count++;if(!condition)throw new AssertionError("Check "+count);}
    public static void main(String[] args){
        List<MusicLyrics.Line> a=MusicLyrics.parse("[00:03.005]three\n[00:01.2][00:02.34]repeat\n[ar:example]\n[00:61.0]invalid");
        ok(a.size()==3);ok(a.get(0).time==1200);ok(a.get(1).time==2340);ok(a.get(2).time==3005);ok(a.get(0).text.equals("repeat"));
        ok(MusicLyrics.current(a,1199)==-1);ok(MusicLyrics.current(a,1200)==0);ok(MusicLyrics.current(a,2339)==0);ok(MusicLyrics.current(a,2340)==1);ok(MusicLyrics.current(a,10000)==2);
        ok(MusicLyrics.parse(null).isEmpty());ok(MusicLyrics.current(Collections.<MusicLyrics.Line>emptyList(),0)==-1);
        List<MusicLyrics.Line> b=MusicLyrics.parse("[offset:-200]\n[00:00.1]a\n[01:02]b");ok(b.get(0).time==0);ok(b.get(1).time==61800);
        List<MusicLyrics.Line> c=MusicLyrics.parse("[offset:+125]\n[12:34.56]c\r\n[00:00]zero");ok(c.get(0).time==125);ok(c.get(1).time==754685);
        ok(MusicLyrics.current(a,1200,250)==-1);ok(MusicLyrics.current(a,1449,250)==-1);ok(MusicLyrics.current(a,1450,250)==0);
        ok(MusicLyrics.current(a,950,-250)==0);ok(MusicLyrics.current(a,949,-250)==-1);ok(MusicLyrics.current(a,1200,0)==0);
        Random random=new Random(1337);
        for(int n=0;n<1000;n++){long time=random.nextInt(900000);int expected=-1;for(int i=0;i<c.size();i++)if(c.get(i).time<=time)expected=i;ok(MusicLyrics.current(c,time)==expected);}
        System.out.println("Music lyric boundary checks PASS: "+count);
    }
}
