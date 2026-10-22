package local.m13.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import org.json.JSONObject;

/** Local installer channel; DUMP permission restricts callers to shell/system. */
public class LibreTvProvision extends BroadcastReceiver {
    public void onReceive(Context c,Intent i){
        try{
            if("local.m13.home.SCREEN_TIMETABLE_CONFIG".equals(i.getAction())){CrayonTimetableStore.configure(c,new String(Base64.decode(i.getStringExtra("config"),Base64.DEFAULT),"UTF-8"));setResultCode(-1);setResultData(CrayonTimetableStore.status(c,System.currentTimeMillis()).toString());return;}
            if("local.m13.home.SCREEN_TIMETABLE_STATUS".equals(i.getAction())){setResultCode(-1);setResultData(CrayonTimetableStore.status(c,i.getLongExtra("at",System.currentTimeMillis())).toString());return;}
            if("local.m13.home.AUTOSTART_STATUS".equals(i.getAction())){setResultCode(-1);setResultData(AutostartPolicy.diagnostics(c));return;}
            if("local.m13.home.AUTOSTART_CONFIG".equals(i.getAction())){java.util.Set<String> selected=new java.util.HashSet<String>();String names=i.getStringExtra("packages");if(names==null)throw new IllegalArgumentException("缺少应用列表");for(String name:names.split(","))if(!name.isEmpty())selected.add(name);AutostartPolicy.configure(c,selected);setResultCode(-1);setResultData(AutostartPolicy.diagnostics(c));return;}
            if("local.m13.home.AUTOSTART_LAUNCH".equals(i.getAction())){AutostartPolicy.launch(c,i.getStringExtra("package"));setResultCode(-1);setResultData(AutostartPolicy.diagnostics(c));return;}
            if("local.m13.home.CLEAN_STATUS".equals(i.getAction())){setResultCode(-1);setResultData(BackgroundCleaner.status());return;}
            if("local.m13.home.SCREEN_STATUS".equals(i.getAction())){setResultCode(-1);setResultData(ScreenPolicy.diagnostics(c));return;}
            if("local.m13.home.SCREEN_APPEARANCE_CHECK".equals(i.getAction())){setResultCode(-1);setResultData(CrayonAppearance.diagnostics(c,i.getLongExtra("at",System.currentTimeMillis())).toString());return;}
            if("local.m13.home.SCREEN_CONFIG".equals(i.getAction())){ScreenPolicy.install(c);setResultCode(-1);setResultData(ScreenPolicy.diagnostics(c));return;}
            if("local.m13.home.SCREEN_APPEARANCE".equals(i.getAction())){CrayonAppearance.set(c,i.getStringExtra("mode"),i.getIntExtra("day",CrayonAppearance.day(c)),i.getIntExtra("night",CrayonAppearance.night(c)));setResultCode(-1);setResultData(ScreenPolicy.diagnostics(c));return;}
            if("local.m13.home.SCREEN_PREVIEW".equals(i.getAction())){c.startActivity(new Intent(c,CrayonActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));setResultCode(-1);setResultData("Preview opened");return;}
            if("local.m13.home.CAST_STATUS".equals(i.getAction())){setResultCode(-1);setResultData(new JSONObject().put("receiver",new JSONObject(CastServer.status())).put("player",new JSONObject(CastPlaybackActivity.diagnostics())).toString());return;}
            if("local.m13.home.LIBRE_STATUS".equals(i.getAction())){setResultCode(-1);setResultData(LibreTvActivity.diagnostics());return;}
            if("local.m13.home.INDEX_STATUS".equals(i.getAction())){LibreTvTitleIndex index=LibreTvTitleIndex.current();setResultCode(-1);setResultData(index==null?"{}":index.status().toString());return;}
            if("local.m13.home.LIBRARY_STATUS".equals(i.getAction())){setResultCode(-1);setResultData(LibreTvLibraryActivity.diagnostics());return;}
            if("local.m13.home.HISTORY_STATUS".equals(i.getAction())){org.json.JSONArray all=LibreTvHistory.get(c).entries();org.json.JSONArray result=new org.json.JSONArray();for(int n=0;n<all.length();n++){org.json.JSONObject v=all.getJSONObject(n);result.put(new org.json.JSONObject().put("title",v.optString("name")).put("sourceKey",v.optString("sourceKey")).put("vodId",v.optString("vodId")).put("episode",v.optInt("episode")).put("positionMs",v.optLong("positionMs")).put("durationMs",v.optLong("durationMs")));}setResultCode(-1);setResultData(result.toString());return;}
            JSONObject config=new JSONObject(new String(Base64.decode(i.getStringExtra("config"),Base64.DEFAULT),"UTF-8"));
            String server=LibreTvApi.validateServer(config.getString("server"));
            c.getSharedPreferences("libretv",0).edit().putString("server",server)
                .putString("password",config.optString("password")).remove("cookie").commit();
            setResultCode(-1);setResultData("Configured");
        }catch(Exception e){setResultCode(0);setResultData(i.getAction()!=null&&i.getAction().startsWith("local.m13.home.SCREEN_")?e.toString():"Invalid configuration");}
    }
}
