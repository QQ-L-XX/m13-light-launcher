package local.m13.home;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Conservative work identity, independent of fuzzy/pinyin search relevance. */
public final class LibreTvFilmGroups<T> {
    private static final Pattern YEAR=Pattern.compile("(?:18|19|20)\\d{2}");
    private static final String TAG="(?:国语|国語|粤语|粵語|普通话|普通話|国粤双语|国粤雙語|英语|英語|原声|原聲|双语|雙語|日语|日語|韩语|韓語|中字|中英双字|HD|BD|DVD|4K|1080[Pp]|720[Pp]|高清|蓝光|蓝光修复|修复|导演剪辑|加长|完整版)(?:版)?";
    private static final Pattern BRACKET_TAG=Pattern.compile("(?i)(?:\\s*[\\[（(【]\\s*"+TAG+"\\s*[\\]）)】]|\\s+"+TAG+"|(?:国语|国語|粤语|粵語|国粤双语|国粤雙語)(?:版)?|(?:普通话|普通話|英语|英語|原声|原聲|双语|雙語|日语|日語|韩语|韓語|导演剪辑|加长|完整)版)$");
    public static String canonicalTitle(String title,String year){
        String value=Normalizer.normalize(title==null?"":title,Normalizer.Form.NFKC).trim();
        while(!value.isEmpty()){
            String next=BRACKET_TAG.matcher(value).replaceFirst("").trim();
            if(!year.isEmpty())next=next.replaceFirst("\\s*[\\[（(【]\\s*"+year+"\\s*[\\]）)】]$","").trim();
            if(next.isEmpty()||next.equals(value))break;value=next;
        }return value;
    }
    private static String normalize(String value){
        String text=Normalizer.normalize(value,Normalizer.Form.NFKC).toLowerCase(Locale.US);StringBuilder b=new StringBuilder();
        for(int i=0;i<text.length();){int c=text.codePointAt(i);if(Character.isLetterOrDigit(c))b.appendCodePoint(c);i+=Character.charCount(c);}return b.toString();
    }
    public static String family(String title,String type){
        if((title+type).matches(".*(电影解说|電影解說|影视解说|影視解說|解说|解說).*"))return "commentary";
        if((title+type).contains("预告")||(title+type).contains("預告"))return "trailer";
        if(type.matches(".*(纪录|记录|紀錄|記錄).*"))return "documentary";
        if(type.contains("综艺")||type.contains("綜藝"))return "show";
        if(type.contains("动漫")||type.contains("動漫"))return "anime-series";
        if(type.matches(".*(连续剧|連續劇|电视剧|電視劇|国产剧|香港剧|台湾剧|韩国剧|欧美剧|美国剧|日本剧|海外剧|泰剧|泰国剧|短剧|内地剧|日剧|韩剧|美剧).*"))return "tv";
        if(type.matches(".*(片|电影|電影|动作|喜剧|爱情|科幻|恐怖|剧情|战争|悬疑|惊悚|犯罪|奇幻|冒险|动画|邵氏).*"))return "movie";
        return "unknown:"+normalize(type);
    }
    public static final class Identity {
        public final String title,name,year,family,id,bucket;
        public Identity(String title,String year,String type,String id){
            Matcher m=YEAR.matcher(year==null?"":year);this.year=m.find()?m.group():"";
            this.title=canonicalTitle(title,this.year);name=normalize(this.title);family=LibreTvFilmGroups.family(title,type==null?"":type);
            this.id=id!=null&&id.matches("[1-9]\\d{2,11}")?id:"";bucket=family+"|"+name;
        }
        boolean conflicts(Identity other){return !bucket.equals(other.bucket)||(!year.isEmpty()&&!other.year.isEmpty()&&!year.equals(other.year))||(!id.isEmpty()&&!other.id.isEmpty()&&!id.equals(other.id));}
        boolean matches(Identity other){
            if(name.isEmpty()||conflicts(other))return false;
            if(year.isEmpty()||other.year.isEmpty())return !id.isEmpty()&&id.equals(other.id);
            return true;
        }
    }
    public static final class Entry<T> {
        public final String key,source;public final Identity identity;public final T value;
        public Entry(String key,String source,Identity identity,T value){this.key=key;this.source=source;this.identity=identity;this.value=value;}
    }
    public static final class Group<T> {
        public final String key;public final List<Entry<T>> entries=new ArrayList<Entry<T>>();
        Group(String key){this.key=key;}
        boolean accepts(Identity identity){boolean match=false;for(Entry<T> e:entries){if(e.identity.conflicts(identity))return false;match|=e.identity.matches(identity);}return match;}
        public int sourceCount(){Set<String> sources=new HashSet<String>();for(Entry<T> e:entries)sources.add(e.source);return sources.size();}
    }
    private final Map<String,List<Group<T>>> buckets=new HashMap<String,List<Group<T>>>();
    private final Set<String> seen=new HashSet<String>();private int serial;
    public int versionCount(){return seen.size();}
    public void clear(){buckets.clear();seen.clear();serial=0;}
    /** Returns the changed group, or null for an already seen provider/video ID. */
    public Group<T> add(Entry<T> entry){
        if(!seen.add(entry.key))return null;
        List<Group<T>> candidates=buckets.get(entry.identity.bucket);if(candidates==null){candidates=new ArrayList<Group<T>>();buckets.put(entry.identity.bucket,candidates);}
        Group<T> selected=null;int matches=0;
        for(Group<T> group:candidates)if(group.accepts(entry.identity)){selected=group;matches++;}
        // Missing metadata must not bridge two explicitly conflicting works.
        if(matches!=1){selected=new Group<T>("film:"+(++serial));candidates.add(selected);}
        selected.entries.add(entry);return selected;
    }
}
