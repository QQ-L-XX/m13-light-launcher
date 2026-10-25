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
    public static void choose(final Activity a,final Runnable done){new AlertDialog.Builder(a).setTitle("电视画面边距").setSingleChoiceItems(new String[]{"标准","加宽 · 适合轻微裁边","更宽","最大"},margin(a),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int n){a.getSharedPreferences("home",0).edit().putInt("safe-margin",n).apply();d.dismiss();done.run();}}).setNegativeButton("返回",null).show();}
}
