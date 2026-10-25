package local.m13.home;

import android.content.*;
import org.json.*;
import java.util.*;
import java.io.*;

/** Deliberate whitelist: account cookies, device policy and service secrets never leave the app. */
public final class LauncherBackup {
    private static final String[] STORES={"home","music-ui","crayon_screen","crayon-timetable"};
    private static boolean allowed(String store,String key){
        if(store.equals("home"))return key.equals("theme")||key.equals("safe-margin")||key.equals("home_order_v1")||key.startsWith("hidden:")||key.startsWith("favorite:");
        if(store.equals("music-ui"))return key.equals("lyric-size")||key.equals("play-mode");
        if(store.equals("crayon_screen"))return key.equals("appearance")||key.equals("day_start")||key.equals("night_start");
        return store.equals("crayon-timetable")&&key.equals("week");
    }
    public static JSONObject export(Context c)throws Exception{JSONObject data=new JSONObject();for(String name:STORES){JSONObject values=new JSONObject();for(Map.Entry<String,?> e:c.getSharedPreferences(name,0).getAll().entrySet())if(allowed(name,e.getKey()))values.put(e.getKey(),e.getValue());data.put(name,values);}return new JSONObject().put("format","m13-settings").put("schema",1).put("createdAt",System.currentTimeMillis()).put("settings",data);}
    static void validate(JSONObject root)throws Exception{
        if(!"m13-settings".equals(root.optString("format"))||root.optInt("schema")!=1)throw new IllegalArgumentException("不是可用的轻桌面设置备份");
        JSONObject all=root.getJSONObject("settings");if(all.length()!=STORES.length)throw new IllegalArgumentException("备份内容不完整");
        for(String name:STORES){JSONObject values=all.getJSONObject(name);if(values.length()>300)throw new IllegalArgumentException("备份项目过多");Iterator<String> keys=values.keys();while(keys.hasNext()){String key=keys.next();Object value=values.get(key);if(key.length()>180||!allowed(name,key))throw new IllegalArgumentException("备份包含不支持的设置");
            if(key.startsWith("hidden:")||key.startsWith("favorite:")){if(!(value instanceof Boolean))throw new IllegalArgumentException("应用设置格式错误");}
            else if(key.equals("week")){if(!(value instanceof String)||((String)value).length()>65536)throw new IllegalArgumentException("课程格式错误");CrayonTimetableStore.parse((String)value);}
            else if(key.equals("home_order_v1")){JSONArray order=new JSONArray((String)value);if(order.length()>200)throw new IllegalArgumentException("排序项目过多");for(int n=0;n<order.length();n++)if(order.getString(n).length()>180)throw new IllegalArgumentException("应用标识过长");}
            else if(key.equals("appearance")){if(!(value instanceof String)||!Arrays.asList("solar","auto","day","night").contains(value))throw new IllegalArgumentException("日夜模式格式错误");}
            else{if(!(value instanceof Integer))throw new IllegalArgumentException("显示设置格式错误");int n=(Integer)value;int max=key.equals("theme")||key.equals("play-mode")?2:key.equals("day_start")||key.equals("night_start")?1439:3;if(n<0||n>max)throw new IllegalArgumentException("显示设置超出范围");}
        }JSONObject screen=all.getJSONObject("crayon_screen");CrayonTheme.validate(screen.optInt("day_start",CrayonTheme.DAY_START),screen.optInt("night_start",CrayonTheme.NIGHT_START));}
    }
    private static void apply(Context c,JSONObject root)throws Exception{JSONObject all=root.getJSONObject("settings");for(String name:STORES){SharedPreferences p=c.getSharedPreferences(name,0);SharedPreferences.Editor edit=p.edit();for(String key:p.getAll().keySet())if(allowed(name,key))edit.remove(key);JSONObject values=all.getJSONObject(name);Iterator<String> keys=values.keys();while(keys.hasNext()){String key=keys.next();Object value=values.get(key);if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(value instanceof Integer)edit.putInt(key,(Integer)value);else edit.putString(key,(String)value);}if(name.equals("crayon-timetable"))edit.putLong("revision",p.getLong("revision",0)+1);if(!edit.commit())throw new IOException("设置保存失败");}}
    public static void restore(Context c,JSONObject root)throws Exception{validate(root);JSONObject before=export(c);File rollback=new File(c.getFilesDir(),"settings-before-restore.json");write(rollback,before.toString(2));try{apply(c,root);}catch(Exception e){apply(c,before);throw e;}}
    public static void write(File file,String content)throws Exception{File temp=new File(file.getParentFile(),file.getName()+".tmp");FileOutputStream out=new FileOutputStream(temp);try{out.write(content.getBytes("UTF-8"));out.getFD().sync();}finally{out.close();}if(!temp.renameTo(file))throw new IOException("备份保存失败");}
    public static JSONObject read(InputStream in)throws Exception{return new JSONObject(new String(LibreTvNet.read(in,192*1024),"UTF-8"));}
}
