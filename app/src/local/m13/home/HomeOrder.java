package local.m13.home;
import java.util.*;

/** Stable ordering: retain known IDs, ignore removed/duplicate IDs, append new apps. */
public final class HomeOrder {
    public static List<String> arrange(List<String> present,List<String> saved){LinkedHashSet<String> remaining=new LinkedHashSet<String>(present);List<String> result=new ArrayList<String>();for(String id:saved)if(remaining.remove(id))result.add(id);result.addAll(remaining);return result;}
    public static List<String> move(List<String> order,String id,int delta,boolean first){List<String> result=new ArrayList<String>(order);int old=result.indexOf(id);if(old<0)return result;int target=first?0:Math.max(0,Math.min(result.size()-1,old+delta));result.remove(old);result.add(target,id);return result;}
}
