package local.m13.home;

import android.content.*;
import org.json.*;

/** Device-private track metadata only. Signed stream URLs are never persisted. */
public final class MusicStore {
    private final SharedPreferences prefs;
    public MusicStore(Context c){prefs=c.getSharedPreferences("music-local-v1",0);}
    public static String key(JSONObject song){return song.optString("source")+":"+song.optString("id");}
    public int lyricDelay(JSONObject song){return prefs.getInt("lyric-delay:"+key(song),0);}
    public void lyricDelay(JSONObject song,int ms){prefs.edit().putInt("lyric-delay:"+key(song),Math.max(-10000,Math.min(10000,ms))).apply();}
    public synchronized JSONArray read(String key){try{return new JSONArray(prefs.getString(key,"[]"));}catch(Exception e){return new JSONArray();}}
    public synchronized void save(String key,JSONArray value){prefs.edit().putString(key,value.toString()).commit();}
    public synchronized boolean favorite(JSONObject song){JSONArray list=read("favorites");for(int i=0;i<list.length();i++)if(key(song).equals(key(list.optJSONObject(i))))return true;return false;}
    public synchronized boolean toggle(JSONObject song)throws Exception{
        boolean before=favorite(song);JSONArray result=new JSONArray();if(!before)result.put(song);
        JSONArray list=read("favorites");for(int i=0;i<list.length()&&result.length()<200;i++)if(!key(song).equals(key(list.getJSONObject(i))))result.put(list.getJSONObject(i));save("favorites",result);return !before;
    }
    public synchronized void played(JSONObject song)throws Exception{JSONArray result=new JSONArray().put(song),old=read("history");for(int i=0;i<old.length()&&result.length()<100;i++)if(!key(song).equals(key(old.getJSONObject(i))))result.put(old.getJSONObject(i));save("history",result);}
}
