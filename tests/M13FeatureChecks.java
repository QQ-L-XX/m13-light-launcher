import local.m13.home.*;
import java.util.*;
import java.text.*;

public final class M13FeatureChecks {
    private static int checks;
    private static void ok(boolean value){checks++;if(!value)throw new AssertionError("Feature check "+checks);}
    private static long at(String date)throws Exception{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));return f.parse(date).getTime();}
    public static void main(String[] args)throws Exception{
        Random random=new Random(1);
        ok(MusicQueuePolicy.next(0,0,1,true,0,random)==-1);
        ok(MusicQueuePolicy.next(2,3,1,true,0,random)==-1);
        ok(MusicQueuePolicy.next(2,3,1,false,0,random)==0);
        ok(MusicQueuePolicy.next(0,3,-1,false,0,random)==2);
        ok(MusicQueuePolicy.next(1,3,1,true,1,random)==1);
        ok(MusicQueuePolicy.next(1,3,1,false,1,random)==2);
        ok(MusicQueuePolicy.next(0,1,1,true,2,random)==0);
        for(int count=2;count<30;count++)for(int index=0;index<count;index++){int next=MusicQueuePolicy.next(index,count,1,true,2,random);ok(next>=0&&next<count&&next!=index);}
        ok(UpdateVersion.newer("v2.33","2.32"));ok(UpdateVersion.newer("v2.10","2.9"));ok(!UpdateVersion.newer("v2.30","2.33"));ok(!UpdateVersion.newer("2.33.0","2.33"));ok(!UpdateVersion.newer("nightly","2.33"));ok(!UpdateVersion.newer("2.34-beta","2.33"));ok(!UpdateVersion.newer("99999999999999.0","2.33"));
        String[][] week=new String[7][];for(int n=0;n<7;n++)week[n]=new String[]{"语文","数学","英语","科学","美术","信息科技","自习"};
        Set<String> holidays=new HashSet<String>(Arrays.asList("2026-10-26","2026-10-27"));Map<String,String[]> changed=new HashMap<String,String[]>();changed.put("2026-10-24",new String[]{"体育","体育","体育","体育","体育","体育","体育"});
        CrayonTimetable t=new CrayonTimetable(week,holidays,changed);
        ok(t.display(at("2026-10-23 22:00")).date.equals("2026-10-24"));ok(t.display(at("2026-10-23 22:00")).label.equals("调课安排"));
        ok(t.display(at("2026-10-25 12:00")).date.equals("2026-10-28"));ok(t.display(at("2026-10-25 12:00")).label.equals("下次上课"));
        holidays.clear();changed.get("2026-10-24")[0]="mutated";ok(t.display(at("2026-10-23 12:00")).courses()[0].equals("体育"));ok(t.display(at("2026-10-25 12:00")).date.equals("2026-10-28"));
        for(String invalid:new String[]{"2026-02-30","2026-1-01","no-date","2026-13-01"})try{CrayonTimetable.validateDate(invalid);ok(false);}catch(IllegalArgumentException expected){ok(true);}
        System.out.println("Queue, holiday and update checks PASS: "+checks);
    }
}
