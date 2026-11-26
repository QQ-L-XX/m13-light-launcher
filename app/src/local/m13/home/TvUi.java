package local.m13.home;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;

/** TV overscan is a display preference, independent from the media surface. */
public final class TvUi {
    public static int margin(Context c){return Math.max(0,Math.min(3,c.getSharedPreferences("home",0).getInt("safe-margin",0)));}
    public static int dp(Context c,float value){return (int)(c.getResources().getDisplayMetrics().density*value+.5f);}
    public static void inset(Activity a){
        ViewGroup content=(ViewGroup)a.findViewById(android.R.id.content);float scale=1f-margin(a)*.025f;content.setBackgroundColor(0xff10233e);if(content.getChildCount()>0){View page=content.getChildAt(0);page.setScaleX(scale);page.setScaleY(scale);}
    }
    private static final String[] ANIMATIONS={"window_animation_scale","transition_animation_scale","animator_duration_scale"};
    public static void motion(Context c,boolean fast)throws Exception{android.content.SharedPreferences prefs=c.getSharedPreferences("home",0);if(c.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("此系统尚未授予动画设置权限");org.json.JSONObject before=new org.json.JSONObject();for(String key:ANIMATIONS){String value=android.provider.Settings.Global.getString(c.getContentResolver(),key);before.put(key,value==null?org.json.JSONObject.NULL:value);}if(fast&&!prefs.contains("animation-backup"))if(!prefs.edit().putString("animation-backup",before.toString()).commit())throw new java.io.IOException("原动画设置未保存");org.json.JSONObject restore=new org.json.JSONObject(prefs.getString("animation-backup",before.toString()));try{for(String key:ANIMATIONS){String value=fast?"0.5":restore.isNull(key)?null:restore.getString(key);if(!android.provider.Settings.Global.putString(c.getContentResolver(),key,value))throw new java.io.IOException("动画设置未生效");}}catch(Exception e){for(String key:ANIMATIONS)android.provider.Settings.Global.putString(c.getContentResolver(),key,before.isNull(key)?null:before.getString(key));throw e;}prefs.edit().putBoolean("fast-motion",fast).apply();}
    public static void chooseMotion(final Activity a){new AlertDialog.Builder(a).setTitle("系统切换动画").setItems(new String[]{"快速 · 动画时长减半","恢复原来的动画"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int n){try{motion(a,n==0);android.widget.Toast.makeText(a,n==0?"已缩短切换动画":"已恢复原动画",0).show();}catch(Exception e){new AlertDialog.Builder(a).setMessage(e.getMessage()).setPositiveButton("确定",null).show();}}}).setNegativeButton("返回",null).show();}
    public static void choose(final Activity a,final Runnable done){new AlertDialog.Builder(a).setTitle("电视画面边距").setSingleChoiceItems(new String[]{"标准","加宽 · 适合轻微裁边","更宽","最大"},margin(a),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int n){a.getSharedPreferences("home",0).edit().putInt("safe-margin",n).apply();d.dismiss();done.run();}}).setNegativeButton("返回",null).show();}
}
