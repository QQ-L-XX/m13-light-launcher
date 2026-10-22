package local.m13.home;

import java.util.*;
import java.util.regex.*;

/** LRC line parsing adapted from Music-TV's LyricParser (Apache-2.0).
 * Java port for API 21; this module implements line timing, not karaoke words.
 */
public final class MusicLyrics {
    private static final Pattern TIME=Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?\\]");
    private static final Pattern OFFSET=Pattern.compile("\\[offset:([+-]?\\d+)\\]",Pattern.CASE_INSENSITIVE);
    public static final class Line {
        public final long time;public final String text;
        public Line(long at,String value){time=at;text=value;}
    }
    public static List<Line> parse(String content){
        List<Line> result=new ArrayList<Line>();if(content==null)return result;
        long offset=0;Matcher om=OFFSET.matcher(content);if(om.find())try{offset=Long.parseLong(om.group(1));}catch(Exception ignored){}
        for(String raw:content.split("\\r?\\n")){
            Matcher m=TIME.matcher(raw);String value=m.replaceAll("").trim();m.reset();
            while(m.find()){
                long min=Long.parseLong(m.group(1)),sec=Long.parseLong(m.group(2));if(sec>=60)continue;
                String fraction=m.group(3);long ms=fraction==null?0:Long.parseLong((fraction+"000").substring(0,3));
                result.add(new Line(Math.max(0,(min*60+sec)*1000+ms+offset),value));
            }
        }
        Collections.sort(result,new Comparator<Line>(){public int compare(Line a,Line b){return Long.compare(a.time,b.time);}});return result;
    }
    /** Positive delay shows lyrics later; negative delay advances them. */
    public static int current(List<Line> lines,long position,int delayMs){return current(lines,position-delayMs);}
    public static int current(List<Line> lines,long position){int lo=0,hi=lines.size()-1,found=-1;while(lo<=hi){int mid=(lo+hi)>>>1;if(lines.get(mid).time<=position){found=mid;lo=mid+1;}else hi=mid-1;}return found;}
}
