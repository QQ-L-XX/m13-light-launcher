package local.m13.home;

import java.text.Normalizer;
import java.util.*;
import java.io.*;

/** On-device literal, pinyin, initials and bounded approximate title matcher. */
public final class LibreTvSearch {
    private static volatile Map<String,String> numberedAliases=Collections.emptyMap();
    private static final java.util.regex.Pattern NUMBERED=java.util.regex.Pattern.compile("^([^0-9]{2,})([0-9]{1,2})$");
    private static final java.util.regex.Pattern SEASON=java.util.regex.Pattern.compile("^(.{2,})第([零一二三四五六七八九十0-9]+)季$");
    public static synchronized void loadAliases(InputStream source)throws IOException{
        if(!numberedAliases.isEmpty()){source.close();return;}Map<String,String> aliases=new HashMap<String,String>();DataInputStream in=new DataInputStream(new BufferedInputStream(source,32768));
        try{int count=in.readInt();if(count<1||count>200000)throw new IOException("片名编号索引大小错误");for(int n=0;n<count;n++)aliases.put(normalize(in.readUTF()),in.readUTF());}finally{in.close();}numberedAliases=aliases;
    }
    public static String canonicalQuery(String value){String alias=numberedAliases.get(normalize(value));return alias==null?value:alias;}
    public static String numberedStem(String word){java.util.regex.Matcher m=NUMBERED.matcher(normalize(canonicalQuery(word)));return m.matches()?m.group(1):"";}
    private static final java.util.regex.Pattern VERSION=java.util.regex.Pattern.compile("((?:19|20)[0-9]{2}|[0-9]{2}(?=年?版$)|[零〇一二三四五六七八九]{2,4}(?=年?版$))(?:年?版)?$");
    public static String baseTitle(String word){String q=normalize(word);java.util.regex.Matcher m=VERSION.matcher(q);return m.find()&&m.start()>=2?q.substring(0,m.start()):q;}
    public static int versionYear(String word){
        java.util.regex.Matcher m=VERSION.matcher(normalize(word));if(!m.find()||m.start()<2)return 0;String raw=m.group(1),digits="";
        for(int i=0;i<raw.length();i++){char c=raw.charAt(i);int n="零一二三四五六七八九".indexOf(c);digits+=c=='〇'?"0":n>=0?String.valueOf(n):String.valueOf(c);}
        try{int year=Integer.parseInt(digits);return digits.length()==2?(year>=30?1900:2000)+year:year;}catch(Exception ignored){return 0;}
    }
    public static int score(String title,String word,String year){
        int wanted=versionYear(word);if(wanted>0&&baseTitle(title).equals(baseTitle(word))){
            int actual=versionYear(title);if(actual==0)try{actual=Integer.parseInt(year);}catch(Exception ignored){}
            return actual==wanted?980:actual>0?500:700;
        }return score(title,word);
    }
    public static String normalize(String value){
        String text=Normalizer.normalize(value==null?"":value,Normalizer.Form.NFKC).toLowerCase(Locale.US);
        StringBuilder result=new StringBuilder();
        for(int i=0;i<text.length();){int c=text.codePointAt(i);if(Character.isLetterOrDigit(c))result.appendCodePoint(c);i+=Character.charCount(c);}
        return result.toString();
    }
    private static int[] points(String value){int[] out=new int[value.codePointCount(0,value.length())];int n=0;for(int i=0;i<value.length();){int c=value.codePointAt(i);out[n++]=c;i+=Character.charCount(c);}return out;}
    private static String head(String value,int n){return value.substring(0,value.offsetByCodePoints(0,Math.min(n,value.codePointCount(0,value.length()))));}
    private static String tail(String value,int n){return value.substring(value.offsetByCodePoints(0,Math.max(0,value.codePointCount(0,value.length())-n)));}
    private static boolean han(String value){int[] a=points(value);if(a.length==0)return false;for(int c:a)if(!((c>=0x3400&&c<=0x9fff)||(c>=0x20000&&c<=0x3134f)))return false;return true;}
    public static List<String> tokens(String word){
        List<String> out=new ArrayList<String>();
        for(String part:Normalizer.normalize(word,Normalizer.Form.NFKC).split("[\\s\\p{P}\\p{S}]+")){String n=normalize(part);if(n.codePointCount(0,n.length())>=2&&!out.contains(n))out.add(n);}
        return out;
    }
    /** Upstream APIs often only do literal substring search: also ask for anchors. */
    public static List<String> queries(String word){
        LinkedHashSet<String> terms=new LinkedHashSet<String>();String raw=Normalizer.normalize(canonicalQuery(word).trim(),Normalizer.Form.NFKC),normal=normalize(raw);terms.add(raw);
        String stem=numberedStem(raw);if(!stem.isEmpty())terms.add(stem);
        if(versionYear(raw)>0){terms.clear();terms.add(baseTitle(raw));terms.add(raw);}
        int length=normal.codePointCount(0,normal.length());
        if(!normal.equals(raw.toLowerCase(Locale.US)))terms.add(normal);
        for(String token:tokens(raw)){if(terms.size()>=5)break;terms.add(token);}
        if(length>=4){int longer=Math.min(8,length-1);terms.add(head(normal,longer));terms.add(tail(normal,longer));}
        if(length>=3){int anchor=han(normal)?2:Math.max(3,(length+1)/2);if(anchor<length){terms.add(head(normal,anchor));terms.add(tail(normal,anchor));}}
        // A three-character Chinese title can have its middle character mistyped.
        if(length==3&&han(normal))terms.add(head(normal,1));
        List<String> out=new ArrayList<String>();for(String term:terms)if(!term.isEmpty()&&out.size()<5)out.add(term);
        return out;
    }
    /** Semi-global edit distance: allow title prefixes/suffixes and adjacent swaps. */
    private static int distance(String query,String title){
        int[] a=points(query),b=points(title),older=new int[b.length+1],previous=new int[b.length+1],row=new int[b.length+1];
        for(int i=1;i<=a.length;i++){
            row[0]=i;
            for(int j=1;j<=b.length;j++){
                int value=Math.min(previous[j]+1,Math.min(row[j-1]+1,previous[j-1]+(a[i-1]==b[j-1]?0:1)));
                if(i>1&&j>1&&a[i-1]==b[j-2]&&a[i-2]==b[j-1])value=Math.min(value,older[j-2]+1);
                row[j]=value;
            }
            int[] swap=older;older=previous;previous=row;row=swap;
        }
        int best=a.length;for(int value:previous)best=Math.min(best,value);return best;
    }
    public static int score(String title,String word){
        String t=normalize(canonicalQuery(title)),q=normalize(canonicalQuery(word));if(q.isEmpty()||t.isEmpty())return -1;
        java.util.regex.Matcher season=SEASON.matcher(q);
        if(season.matches()&&t.startsWith(season.group(1))&&!t.startsWith(q))return -1;
        java.util.regex.Matcher wanted=NUMBERED.matcher(q);
        if(wanted.matches()&&t.startsWith(wanted.group(1))){
            String rest=t.substring(wanted.group(1).length());java.util.regex.Matcher actual=java.util.regex.Pattern.compile("^([0-9]+)").matcher(rest);
            if(!actual.find()||!actual.group(1).equals(wanted.group(2)))return -1;
        }
        if(t.equals(q))return 1000;
        if(versionYear(word)>0&&baseTitle(title).equals(baseTitle(word))){int actual=versionYear(title);return actual==versionYear(word)?980:actual>0?500:700;}
        if(t.startsWith(q))return 900;
        if(t.contains(q))return 800;
        List<String> parts=tokens(word);if(parts.size()>1){boolean all=true;for(String part:parts)all&=t.contains(part);if(all)return 750;}
        int phonetic=LibreTvPinyin.score(title,word);if(phonetic>=0)return phonetic;
        int length=q.codePointCount(0,q.length());boolean chinese=han(q);
        if(length<(chinese?3:5)||length>100)return -1;
        // One typo for short titles, at most two for longer titles; reject weak matches.
        if(t.codePointCount(0,t.length())>256)t=head(t,256);
        int limit=length>=8?2:1,d=distance(q,t);
        return d<=limit?600-d*100:-1;
    }
    public static String kind(int score){String phonetic=LibreTvPinyin.kind(score);return !phonetic.isEmpty()?phonetic:score>=1000?"exact":score>=800?"contains":score>=750?"keywords":"fuzzy";}
}
