package local.m13.home;

import java.io.*;
import java.text.Normalizer;
import java.util.*;

/** Offline Unicode readings (MIT pinyin-data); bounded polyphonic alternatives. */
public final class LibreTvPinyin {
    private static volatile Map<Integer,String[]> dictionary=Collections.emptyMap();
    public static synchronized void load(InputStream in)throws IOException{
        if(!dictionary.isEmpty()){in.close();return;}
        Map<Integer,String[]> out=new HashMap<Integer,String[]>();
        BufferedReader reader=new BufferedReader(new InputStreamReader(in,"UTF-8"));
        try{String line;while((line=reader.readLine())!=null){String[] p=line.split("\t",2);if(p.length==2)out.put(Integer.parseInt(p[0],16),p[1].split(","));}}
        finally{reader.close();}dictionary=out;
    }
    public static String latin(String word){
        String text=Normalizer.normalize(word==null?"":word,Normalizer.Form.NFKC).toLowerCase(Locale.US);
        text=Normalizer.normalize(text,Normalizer.Form.NFD).replace("u\u0308","v");
        StringBuilder b=new StringBuilder();
        for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c>='a'&&c<='z'||c>='0'&&c<='9')b.append(c);else if(Character.getType(c)!=Character.NON_SPACING_MARK&&!Character.isWhitespace(c)&&Character.isLetter(c))return "";}
        return b.toString();
    }
    public static boolean query(String value){String q=latin(value);return q.length()>=2&&q.length()<=100&&q.matches(".*[a-z].*");}
    public static boolean chinese(String value){for(int i=0;i<value.length();){int c=value.codePointAt(i);if(c>=0x3400&&c<=0x9fff||c>=0x20000&&c<=0x3134f)return true;i+=Character.charCount(c);}return false;}
    public static final class Key {public final String full,initials;Key(String f,String i){full=f;initials=i;}}
    public static List<Key> keys(String title){
        List<Key> keys=new ArrayList<Key>();keys.add(new Key("",""));
        String text=Normalizer.normalize(title,Normalizer.Form.NFKC).toLowerCase(Locale.US);int length=0;
        for(int i=0;i<text.length()&&length<100;length++){
            int c=text.codePointAt(i);i+=Character.charCount(c);String[] readings=dictionary.get(c);
            if(readings==null){if(c>='a'&&c<='z'||c>='0'&&c<='9')readings=new String[]{new String(Character.toChars(c))};else if(Character.isLetterOrDigit(c))return Collections.emptyList();else continue;}
            List<Key> next=new ArrayList<Key>();Set<String> seen=new HashSet<String>();
            // Reading-first order preserves a useful spread when alternatives are capped.
            for(String syllable:readings){for(Key key:keys){String f=key.full+syllable,n=key.initials+syllable.charAt(0);if(seen.add(f+"\t"+n))next.add(new Key(f,n));if(next.size()>=16)break;}if(next.size()>=16)break;}keys=next;
        }
        return keys;
    }
    public static int keyScore(String full,String initials,String q){
        if(full.equals(q))return 970;if(initials.equals(q))return 960;
        if(full.startsWith(q))return 870;if(initials.startsWith(q))return 860;
        if(q.length()>=4){if(full.contains(q))return 780;if(initials.contains(q))return 770;}return -1;
    }
    private static String span(String title,String q,boolean initials){
        String text=Normalizer.normalize(title,Normalizer.Form.NFKC).toLowerCase(Locale.US);
        for(int start=0;start<text.length();){int first=text.codePointAt(start),nextStart=start+Character.charCount(first);
            if(!Character.isLetterOrDigit(first)){start=nextStart;continue;}
            Set<Integer> positions=new HashSet<Integer>();positions.add(0);
            for(int end=start;end<text.length();){int c=text.codePointAt(end);end+=Character.charCount(c);String[] readings=dictionary.get(c);
                if(readings==null){if(c>='a'&&c<='z'||c>='0'&&c<='9')readings=new String[]{new String(Character.toChars(c))};else if(Character.isLetterOrDigit(c))break;else continue;}
                Set<Integer> next=new HashSet<Integer>();
                for(int at:positions)for(String reading:readings){String term=initials?reading.substring(0,1):reading;if(q.startsWith(term,at)){int n=at+term.length();if(n==q.length())return text.substring(start,end);next.add(n);}}
                if(next.isEmpty())break;positions=next;
            }start=nextStart;
        }return "";
    }
    /** Recover the Chinese span, avoiding provider-specific prefixes/punctuation. */
    public static String anchor(String title,String word){String q=latin(word);if(!query(word))return "";String full=span(title,q,false);return full.isEmpty()?span(title,q,true):full;}
    public static int score(String title,String word){
        if(!chinese(title)||!query(word))return -1;String q=latin(word);int best=-1;
        for(Key key:keys(title))best=Math.max(best,keyScore(key.full,key.initials,q));return best;
    }
    public static String kind(int score){return score==970||score==870||score==780?"pinyin":score==960||score==860||score==770?"initials":"";}
}
