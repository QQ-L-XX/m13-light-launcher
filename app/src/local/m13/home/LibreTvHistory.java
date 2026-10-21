package local.m13.home;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;

/** Private, durable watch progress. URLs are refreshed from the source when resuming. */
public final class LibreTvHistory extends SQLiteOpenHelper {
    private static LibreTvHistory instance;
    public static synchronized LibreTvHistory get(Context c){if(instance==null)instance=new LibreTvHistory(c.getApplicationContext());return instance;}
    private LibreTvHistory(Context c){super(c,"libretv-history.db",null,1);}
    public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE history (entry_key TEXT PRIMARY KEY, item TEXT NOT NULL, episode INTEGER NOT NULL, position INTEGER NOT NULL, duration INTEGER NOT NULL, updated INTEGER NOT NULL)");}
    public void onUpgrade(SQLiteDatabase db,int old,int next){}
    public static String key(JSONObject item){return item.optString("sourceKey")+":"+item.optString("vodId",item.optString("name"));}
    public synchronized void save(JSONObject item,int episode,long position,long duration){
        if(item==null||item.optString("vodId").isEmpty()||duration<=0)return;
        ContentValues v=new ContentValues();v.put("entry_key",key(item));v.put("item",item.toString());v.put("episode",episode);v.put("position",Math.max(0,position));v.put("duration",duration);v.put("updated",System.currentTimeMillis());
        SQLiteDatabase db=getWritableDatabase();db.insertWithOnConflict("history",null,v,SQLiteDatabase.CONFLICT_REPLACE);
        db.execSQL("DELETE FROM history WHERE entry_key NOT IN (SELECT entry_key FROM history ORDER BY updated DESC LIMIT 200)");
    }
    public synchronized JSONArray entries(){
        JSONArray a=new JSONArray();Cursor c=getReadableDatabase().query("history",null,null,null,null,null,"updated DESC","200");
        try{while(c.moveToNext())try{JSONObject v=new JSONObject(c.getString(c.getColumnIndexOrThrow("item")));v.put("historyKey",c.getString(c.getColumnIndexOrThrow("entry_key"))).put("episode",c.getInt(c.getColumnIndexOrThrow("episode"))).put("positionMs",c.getLong(c.getColumnIndexOrThrow("position"))).put("durationMs",c.getLong(c.getColumnIndexOrThrow("duration"))).put("watchedAt",c.getLong(c.getColumnIndexOrThrow("updated")));a.put(v);}catch(JSONException ignored){}}finally{c.close();}return a;
    }
    public synchronized void remove(String key){getWritableDatabase().delete("history","entry_key=?",new String[]{key});}
    public synchronized void clear(){getWritableDatabase().delete("history",null,null);}
    public static String time(long millis){long s=Math.max(0,millis)/1000;return String.format(java.util.Locale.US,"%d:%02d",s/60,s%60);}
}
