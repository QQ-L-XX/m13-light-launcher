import java.util.*;
import local.m13.home.HomeOrder;
public final class M13HomeOrderChecks {
    private static int checks;
    private static List<String> list(String... values){return Arrays.asList(values);}
    private static void same(List<String> actual,String... expected){checks++;if(!actual.equals(list(expected)))throw new AssertionError(actual.toString());}
    public static void main(String[] args){
        same(HomeOrder.arrange(list("a","b","c"),list()),"a","b","c");
        same(HomeOrder.arrange(list("a","b","c"),list("c","c","gone","a")),"c","a","b");
        same(HomeOrder.arrange(list("a","c","new"),list("b","c","a")),"c","a","new");
        same(HomeOrder.arrange(list("a","a","b"),list("b")),"b","a");
        same(HomeOrder.move(list("a","b","c"),"c",0,true),"c","a","b");
        same(HomeOrder.move(list("a","b","c"),"b",-1,false),"b","a","c");
        same(HomeOrder.move(list("a","b","c"),"b",1,false),"a","c","b");
        same(HomeOrder.move(list("a","b","c"),"a",-1,false),"a","b","c");
        same(HomeOrder.move(list("a","b","c"),"c",1,false),"a","b","c");
        same(HomeOrder.move(list("a","b","c"),"gone",1,false),"a","b","c");
        List<String> original=list("a","b");HomeOrder.move(original,"b",0,true);same(original,"a","b");
        same(HomeOrder.arrange(list(),list("gone")));
        System.out.println("Home ordering checks PASS: "+checks);
    }
}
