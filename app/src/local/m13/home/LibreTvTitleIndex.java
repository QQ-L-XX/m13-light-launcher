package local.m13.home;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.*;
import org.json.*;

/** Private SQLite title index. Seed ships in APK; lookup and refresh run on the box. */
public final class LibreTvTitleIndex {
    private static LibreTvTitleIndex instance;
    private final Context context;private final SQLiteDatabase db;private volatile boolean refreshing;
    private String error="";private int count;
    public static synchronized LibreTvTitleIndex get(Context c)throws Exception{if(instance==null)instance=new LibreTvTitleIndex(c.getApplicationContext());return instance;}
    public static synchronized LibreTvTitleIndex current(){return instance;}
    private LibreTvTitleIndex(Context c)throws Exception{
        context=c;LibreTvPinyin.load(c.getAssets().open("pinyin.tsv"));
        File target=c.getDatabasePath("libretv-titles-v1.db");
        if(!target.exists()){
            target.getParentFile().mkdirs();File temp=new File(target.getPath()+".tmp");
            // aapt stores foo.gz as a ZIP-compressed asset named foo.
            InputStream in=c.getAssets().open("title-index.db",android.content.res.AssetManager.ACCESS_STREAMING);OutputStream out=new FileOutputStream(temp);
            try{byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}finally{try{in.close();}finally{out.close();}}
            if(!temp.renameTo(target))throw new IOException("片名索引初始化未完成，请重试");
        }
        db=SQLiteDatabase.openDatabase(target.getPath(),null,SQLiteDatabase.OPEN_READWRITE);db.enableWriteAheadLogging();
        Cursor cursor=db.rawQuery("SELECT COUNT(*) FROM titles",null);try{cursor.moveToFirst();count=cursor.getInt(0);}finally{cursor.close();}
    }
    public synchronized List<String> resolve(String word){
        final String q=LibreTvPinyin.latin(word);if(!LibreTvPinyin.query(word))return Collections.emptyList();
        final LinkedHashMap<String,Integer> matches=new LinkedHashMap<String,Integer>();
        collect(matches,"py=? OR initials=?",new String[]{q,q},q,3000);
        collect(matches,"py LIKE ? OR initials LIKE ?",new String[]{q+"%",q+"%"},q,6000);
        if(matches.isEmpty()&&q.length()>=4)collect(matches,"py LIKE ? OR initials LIKE ?",new String[]{"%"+q+"%","%"+q+"%"},q,6000);
        List<String> names=new ArrayList<String>(matches.keySet());Collections.sort(names,new Comparator<String>(){public int compare(String a,String b){int r=Integer.compare(matches.get(b),matches.get(a));if(r!=0)return r;r=Integer.compare(a.length(),b.length());return r!=0?r:a.compareTo(b);}});
        return new ArrayList<String>(names.subList(0,Math.min(128,names.size())));
    }
    private void collect(Map<String,Integer> out,String where,String[] args,String query,int limit){
        Cursor c=db.rawQuery("SELECT n.name,k.py,k.initials FROM title_keys k JOIN titles n ON n.id=k.title_id WHERE "+where+" ORDER BY length(n.name),n.name LIMIT "+limit,args);
        try{while(c.moveToNext()){String name=c.getString(0);int score=LibreTvPinyin.keyScore(c.getString(1),c.getString(2),query);Integer old=out.get(name);if(old==null||old<score)out.put(name,score);}}finally{c.close();}
    }
    public synchronized void learn(JSONArray rows){
        if(rows==null)return;db.beginTransaction();
        try{for(int i=0;i<rows.length();i++){JSONObject v=rows.optJSONObject(i);if(v==null||!LibreTvStandalone.allowed(v.optString("type_name")))continue;String name=v.optString("vod_name").trim();if(name.isEmpty()||name.length()>200||!LibreTvPinyin.chinese(name))continue;
            ContentValues title=new ContentValues();title.put("name",name);long id=db.insertWithOnConflict("titles",null,title,SQLiteDatabase.CONFLICT_IGNORE);if(id<0)continue;
            for(LibreTvPinyin.Key key:LibreTvPinyin.keys(name)){ContentValues values=new ContentValues();values.put("title_id",id);values.put("py",key.full);values.put("initials",key.initials);db.insertWithOnConflict("title_keys",null,values,SQLiteDatabase.CONFLICT_IGNORE);}count++;
        }db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    public synchronized void refresh(){
        final SharedPreferences prefs=context.getSharedPreferences("title-index",0);long last=prefs.getLong("updated",0);
        if(refreshing||System.currentTimeMillis()-last<6*60*60*1000L)return;refreshing=true;
        new Thread(new Runnable(){public void run(){try{
            JSONArray sources=LibreTvStandalone.sources();
            for(int s=0;s<sources.length();s++){JSONObject source=sources.getJSONObject(s);String key=source.getString("key");int cursor=prefs.getInt(key+"-page",9);
                for(int n=0;n<12;n++){int page=n<8?n+1:cursor;JSONObject d=new JSONObject(LibreTvNet.get(source.getString("url")+"?ac=list&pg="+page));JSONArray rows=d.optJSONArray("list");if(rows==null)throw new IOException("片名索引暂时无法更新");learn(rows);if(n>=8){cursor=page>=d.optInt("pagecount",page)?9:page+1;prefs.edit().putInt(key+"-page",cursor).apply();}}
            }prefs.edit().putLong("updated",System.currentTimeMillis()).apply();synchronized(LibreTvTitleIndex.this){error="";}
        }catch(Exception e){synchronized(LibreTvTitleIndex.this){error="片名索引暂未更新，已有片名仍可搜索";}}finally{refreshing=false;}}},"title-index-refresh").start();
    }
    public synchronized JSONObject status()throws JSONException{return new JSONObject().put("titles",count).put("refreshing",refreshing).put("updated",context.getSharedPreferences("title-index",0).getLong("updated",0)).put("error",error).put("pcRequired",false).put("maxResolvedTitles",128);}
}
