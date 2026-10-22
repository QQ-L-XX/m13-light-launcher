package local.m13.home;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;
import io.nayuki.qrcodegen.QrCode;

/** QR image is generated entirely on-device; no login token goes to an image API. */
public final class NeteaseLoginDialog {
    private static java.lang.ref.WeakReference<NeteaseLoginDialog> active=new java.lang.ref.WeakReference<NeteaseLoginDialog>(null);
    public static void closeFor(Activity a){NeteaseLoginDialog d=active.get();if(d!=null&&d.activity==a&&d.dialog!=null)d.dialog.dismiss();}
    private final Activity activity;private final Handler ui=new Handler();private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private AlertDialog dialog;private ImageView image;private TextView status;private int generation,attempts;private boolean closed;private boolean verificationShown;private int failures;private NeteaseAccount.Login pending;
    public NeteaseLoginDialog(Activity a){activity=a;}
    private int dp(int n){return(int)(activity.getResources().getDisplayMetrics().density*n+.5f);}
    public static void menu(final Activity a){NeteaseAccount.init(a);final boolean authorized=NeteaseAccount.logged();new AlertDialog.Builder(a).setTitle("网易云音乐账号").setMessage(authorized?"此盒子已保存网易云登录授权。播放时会使用该账号的实际权限；单独付费、地区或版权限制仍由网易云决定。":"扫码登录后，盒子将使用你的网易云账号权限获取播放地址。无需电脑服务，也无需在聊天中提供密码。").setPositiveButton(authorized?"重新扫码":"扫码登录",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){new NeteaseLoginDialog(a).show();}}).setNeutralButton(authorized?"退出此盒子":"取消",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){if(authorized){NeteaseAccount.logout();MusicActivity.refreshAccountLabels();Toast.makeText(a,"已清除此盒子的网易云授权",Toast.LENGTH_SHORT).show();}}}).setNegativeButton("返回",null).show();}
    public void show(){
        LinearLayout body=new LinearLayout(activity);body.setOrientation(1);body.setGravity(Gravity.CENTER);body.setPadding(dp(24),dp(8),dp(24),dp(8));
        image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setBackgroundColor(Color.WHITE);body.addView(image,new LinearLayout.LayoutParams(dp(260),dp(260)));
        status=new TextView(activity);status.setTextSize(16);status.setGravity(Gravity.CENTER);status.setPadding(0,dp(12),0,dp(8));body.addView(status,new LinearLayout.LayoutParams(-1,dp(68)));
        TextView help=new TextView(activity);help.setText("用手机网易云音乐 App 扫码，并在手机确认登录。\n授权仅保存在盒子应用私有区；二维码过期后可刷新。");help.setTextSize(13);help.setGravity(Gravity.CENTER);body.addView(help);
        dialog=new AlertDialog.Builder(activity).setTitle("网易云 · 扫码授权此盒子").setView(body).setPositiveButton("刷新二维码",null).setNegativeButton("关闭",null).create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){public void onDismiss(DialogInterface d){closed=true;generation++;ui.removeCallbacksAndMessages(null);worker.shutdownNow();}});active=new java.lang.ref.WeakReference<NeteaseLoginDialog>(this);dialog.show();dialog.getWindow().setLayout(dp(620),-2);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){public void onClick(View v){begin();}});begin();
    }
    private static Bitmap encode(String text){QrCode code=QrCode.encodeText(text,QrCode.Ecc.MEDIUM);int border=4,scale=8,size=(code.size+border*2)*scale;int[] pixels=new int[size*size];java.util.Arrays.fill(pixels,Color.WHITE);for(int y=0;y<code.size;y++)for(int x=0;x<code.size;x++)if(code.getModule(x,y))for(int dy=0;dy<scale;dy++)for(int dx=0;dx<scale;dx++)pixels[((y+border)*scale+dy)*size+(x+border)*scale+dx]=Color.BLACK;return Bitmap.createBitmap(pixels,size,size,Bitmap.Config.ARGB_8888);}
    private void begin(){final int request=++generation;attempts=0;failures=0;verificationShown=false;image.setImageDrawable(null);status.setText("正在向网易云申请二维码…");final NeteaseAccount.Login login=new NeteaseAccount.Login();pending=login;
        worker.submit(new Runnable(){public void run(){try{final Bitmap qr=encode(login.begin());ui.post(new Runnable(){public void run(){if(closed||request!=generation){qr.recycle();return;}image.setImageBitmap(qr);if(image.getDrawable() instanceof android.graphics.drawable.BitmapDrawable)((android.graphics.drawable.BitmapDrawable)image.getDrawable()).setFilterBitmap(false);status.setText("等待手机网易云扫码");poll(request,login);}});}catch(Exception e){ui.post(new Runnable(){public void run(){if(!closed&&request==generation)status.setText("二维码获取失败，请刷新或检查盒子网络");}});}}});
    }
    private void poll(final int request,final NeteaseAccount.Login login){if(closed||request!=generation)return;if(++attempts>100){status.setText("二维码已超时，请刷新");return;}
        ui.postDelayed(new Runnable(){public void run(){if(closed||request!=generation)return;worker.submit(new Runnable(){public void run(){try{final int code=login.check();final Bitmap verification=code==8821&&!verificationShown?encode(login.verificationQr()):null;ui.post(new Runnable(){public void run(){if(closed||request!=generation){if(verification!=null)verification.recycle();return;}failures=0;if(code==8821){if(verification!=null){image.setImageBitmap(verification);verificationShown=true;}status.setText("还需安全验证：请用网易云再次扫码，按手机提示完成验证");poll(request,login);return;}if(code==803){try{login.commit();MusicActivity.refreshAccountLabels();status.setText("授权成功，已保存到盒子");image.setImageDrawable(null);Toast.makeText(activity,"网易云已登录，可重试会员歌曲",Toast.LENGTH_LONG).show();dialog.dismiss();}catch(Exception e){status.setText("授权保存失败，请重新扫码");}return;}if(code==800){status.setText("二维码已失效，请刷新");return;}if(code==802)status.setText("已扫码，请在手机网易云确认登录");else if(code==801)status.setText("等待手机网易云扫码");else{status.setText("网易云未完成授权（状态 "+code+"），请稍后重试或改用官方客户端");image.setImageDrawable(null);return;}poll(request,login);}});}catch(Exception e){ui.post(new Runnable(){public void run(){if(!closed&&request==generation){org.json.JSONObject diagnostic=NeteaseAccount.diagnostics();if("missing_metadata".equals(diagnostic.optString("verificationStage"))){status.setText("网易云拒绝此登录方式（8821），未提供验证入口。请稍后再试或使用官方客户端。");image.setImageDrawable(null);return;}if(++failures>=3){status.setText("授权暂未完成，已停止自动重试。请稍后刷新二维码。");image.setImageDrawable(null);return;}status.setText(diagnostic.optInt("lastCode")==8821?"验证入口暂不可用，有限重试中…":"连接暂时中断，正在重试…");poll(request,login);}}});}}});}},3000);
    }
}
