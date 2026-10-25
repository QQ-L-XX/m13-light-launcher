package local.m13.home;

import android.content.*;
import org.json.*;

/** Explicit shell-only diagnostics; not a network control endpoint. */
public final class MusicDiagnostics extends BroadcastReceiver {
    public void onReceive(Context c,Intent i){try{
        NeteaseAccount.init(c);String action=i.getStringExtra("command");
        if("sleep".equals(action)){MusicService m=MusicService.current();if(m!=null)m.setSleep(i.getIntExtra("minutes",0));}
        else if("select".equals(action))MusicActivity.testSelect(i.getIntExtra("index",0));
        else if("search".equals(action))MusicActivity.testSearch(i.getStringExtra("query"),i.getStringExtra("source"),i.getBooleanExtra("playlists",false));
        else if("seek".equals(action)){MusicService m=MusicService.current();if(m!=null)m.seek(i.getIntExtra("position",0));}
        else if("toggle".equals(action)||"next".equals(action)||"previous".equals(action)||"stop".equals(action))MusicService.command(c,action);
        else if("favorite".equals(action)){MusicService m=MusicService.current();if(m!=null)new MusicStore(c).toggle(m.song());}
        JSONObject result=new JSONObject().put("neteaseAccount",NeteaseAccount.diagnostics()).put("player",MusicService.diagnostics()).put("library",MusicActivity.diagnostics()).put("favorites",new MusicStore(c).read("favorites").length()).put("history",new MusicStore(c).read("history").length());setResultCode(-1);setResultData(result.toString());
    }catch(Exception e){setResultCode(0);setResultData(e.getClass().getSimpleName());}}
}
