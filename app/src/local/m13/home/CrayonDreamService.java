package local.m13.home;

import android.service.dreams.DreamService;
import java.lang.ref.WeakReference;

/** The replacement system dream; the original advertising package remains hidden. */
public class CrayonDreamService extends DreamService {
    private static WeakReference<CrayonDreamService> active=new WeakReference<CrayonDreamService>(null);
    private CrayonView view;
    public void onAttachedToWindow(){super.onAttachedToWindow();active=new WeakReference<CrayonDreamService>(this);setInteractive(false);setFullscreen(true);setScreenBright(true);view=new CrayonView(this);setContentView(view);}
    public void onDetachedFromWindow(){if(active.get()==this)active.clear();view=null;super.onDetachedFromWindow();}
    public static void awaken(){CrayonDreamService service=active.get();if(service!=null)service.wakeUp();}
    public static boolean showing(){return active.get()!=null;}
    public static String appearance(){CrayonDreamService service=active.get();return service==null||service.view==null?"":service.view.appearance();}
    public static long paletteChanges(){CrayonDreamService service=active.get();return service==null||service.view==null?0:service.view.paletteChanges;}
    public static int tomorrowIconCode(){CrayonDreamService service=active.get();return service==null||service.view==null?-1:service.view.tomorrowIconCode();}
    public static long frames(){CrayonDreamService service=active.get();return service==null||service.view==null?0:service.view.frames;}
}
