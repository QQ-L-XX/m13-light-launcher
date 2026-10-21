package local.m13.home;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import java.lang.ref.WeakReference;

/** Immediate preview uses the exact same drawing and weather cache as the system dream. */
public class CrayonActivity extends Activity {
    private static WeakReference<CrayonActivity> active=new WeakReference<CrayonActivity>(null);
    private CrayonView view;
    public void onCreate(Bundle state){super.onCreate(state);active=new WeakReference<CrayonActivity>(this);getWindow().setFlags(1024,1024);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);view=new CrayonView(this);setContentView(view);}
    public boolean dispatchKeyEvent(KeyEvent event){if(event.getAction()==KeyEvent.ACTION_DOWN){finish();return true;}return true;}
    public boolean dispatchTouchEvent(MotionEvent event){if(event.getAction()==MotionEvent.ACTION_DOWN)finish();return true;}
    protected void onPause(){super.onPause();if(!isFinishing())finish();}
    protected void onDestroy(){if(active.get()==this)active.clear();view=null;super.onDestroy();}
    public static void awaken(){CrayonActivity a=active.get();if(a!=null)a.finish();}
    public static String appearance(){CrayonActivity service=active.get();return service==null||service.view==null?"":service.view.appearance();}
    public static long paletteChanges(){CrayonActivity service=active.get();return service==null||service.view==null?0:service.view.paletteChanges;}
    public static int tomorrowIconCode(){CrayonActivity service=active.get();return service==null||service.view==null?-1:service.view.tomorrowIconCode();}
    public static long frames(){CrayonActivity a=active.get();return a==null||a.view==null?0:a.view.frames;}
}
