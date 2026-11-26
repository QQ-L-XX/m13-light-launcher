package local.m13.home;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.pm.*;
import android.widget.Toast;
import java.util.*;
import org.json.*;

/** Reversible removal from the user environment, limited to three optional UI apps. */
public final class PreinstalledApps {
    public static final String[] PACKAGES={"com.yunos.tv.edu","com.yunos.music","com.yunos.app.photoplayer"};
    private static final String[] LABELS={"少儿大厅","原厂音乐","原厂照片"};
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("optional-system-apps",0);}
    public static Set<String> selected(Context c){return new HashSet<String>(prefs(c).getStringSet("selected",Collections.<String>emptySet()));}
    public static synchronized void configure(Context c,Set<String> wanted)throws Exception{
        if(!HomePolicy.isOwner(c))throw new SecurityException("需要轻桌面的本地设备管理权限");
        Set<String> known=new HashSet<String>(Arrays.asList(PACKAGES));if(!known.containsAll(wanted))throw new IllegalArgumentException("只允许管理这三项可选应用");
        DevicePolicyManager manager=(DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);ComponentName admin=new ComponentName(c,HomeAdmin.class);SharedPreferences p=prefs(c);Set<String> old=selected(c);Map<String,Boolean> before=new LinkedHashMap<String,Boolean>();SharedPreferences.Editor edit=p.edit();
        for(String name:PACKAGES)if(old.contains(name)||wanted.contains(name)){ApplicationInfo info=c.getPackageManager().getApplicationInfo(name,PackageManager.GET_UNINSTALLED_PACKAGES);if(info.uid<10000)throw new IllegalArgumentException("不能处理共享系统身份的应用");before.put(name,manager.isApplicationHidden(admin,name));if(wanted.contains(name)&&!old.contains(name))edit.putBoolean("before:"+name,before.get(name));}
        try{for(String name:before.keySet()){boolean hidden=wanted.contains(name)||p.getBoolean("before:"+name,false);manager.setApplicationHidden(admin,name,hidden);if(manager.isApplicationHidden(admin,name)!=hidden)throw new IllegalStateException("应用状态未保存");if(!wanted.contains(name))edit.remove("before:"+name);}if(!edit.putStringSet("selected",new HashSet<String>(wanted)).commit())throw new IllegalStateException("清单未保存");}
        catch(Exception e){for(Map.Entry<String,Boolean> entry:before.entrySet())manager.setApplicationHidden(admin,entry.getKey(),entry.getValue());throw e;}
    }
    public static void restoreAll(Context c){if(selected(c).isEmpty())return;try{configure(c,Collections.<String>emptySet());}catch(Exception e){throw new IllegalStateException("可选预装应用尚未恢复",e);}}
    public static JSONObject status(Context c)throws Exception{JSONObject result=new JSONObject().put("owner",HomePolicy.isOwner(c));JSONArray apps=new JSONArray();DevicePolicyManager manager=(DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);for(int n=0;n<PACKAGES.length;n++)apps.put(new JSONObject().put("package",PACKAGES[n]).put("name",LABELS[n]).put("selected",selected(c).contains(PACKAGES[n])).put("hidden",manager.isApplicationHidden(new ComponentName(c,HomeAdmin.class),PACKAGES[n])));return result.put("apps",apps).put("firmwareDeleted",false);}
    public static void menu(final Activity a){final Set<String> wanted=selected(a);boolean[] checked=new boolean[PACKAGES.length];for(int n=0;n<checked.length;n++)checked[n]=wanted.contains(PACKAGES[n]);new AlertDialog.Builder(a).setTitle("精简预装应用").setMultiChoiceItems(new String[]{"停用少儿大厅","停用原厂音乐","停用原厂照片"},checked,new DialogInterface.OnMultiChoiceClickListener(){public void onClick(DialogInterface d,int n,boolean on){if(on)wanted.add(PACKAGES[n]);else wanted.remove(PACKAGES[n]);}}).setPositiveButton("保存",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int n){try{configure(a,wanted);Toast.makeText(a,"已保存；取消勾选即可恢复",1).show();}catch(Exception e){new AlertDialog.Builder(a).setMessage(e.getMessage()).setPositiveButton("确定",null).show();}}}).setNegativeButton("返回",null).setNeutralButton("说明",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int n){new AlertDialog.Builder(a).setMessage("勾选后应用入口和后台被停用，数据保留。取消勾选可恢复。原厂 APK 仍在系统分区，因此不会腾出系统分区空间。轻音乐、LibreTV、投屏、输入法及网络组件不在此清单内。").setPositiveButton("确定",null).show();}}).show();}
}
