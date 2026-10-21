package local.m13.home;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONObject;

public final class LibreTvApi {
    private final SharedPreferences prefs;
    private final Context context;
    public LibreTvApi(Context c){context=c.getApplicationContext();prefs=c.getSharedPreferences("libretv",0);}
    public boolean standalone(){return prefs.getBoolean("standalone",true);}
    private void prepareIndex(){try{LibreTvTitleIndex.get(context).refresh();}catch(Exception e){android.util.Log.w("M13Index","Title index unavailable; preserve regular search",e);}}
    public static String validateServer(String value)throws Exception{
        URI u=new URI(value.trim());
        if(!("http".equals(u.getScheme())||"https".equals(u.getScheme()))||u.getHost()==null||u.getRawUserInfo()!=null||u.getRawQuery()!=null||u.getRawFragment()!=null||!(u.getPath()==null||u.getPath().isEmpty()||u.getPath().equals("/")))throw new Exception("请输入完整的服务地址，例如 http://127.0.0.1:8081");
        return u.getScheme()+"://"+u.getRawAuthority();
    }
    public String server(){return prefs.getString("server","http://127.0.0.1:8081");}
    public String cookie(){return prefs.getString("cookie","");}
    public static String enc(String value)throws Exception{return URLEncoder.encode(value,"UTF-8");}
    public String proxy(String target)throws Exception{return server()+"/api/proxy?url="+enc(target);}
    public JSONObject request(String path,JSONObject body)throws Exception{
        if(standalone()){LibreTvNet.init(context);if(path.equals("/api/search")){prepareIndex();return LibreTvStandalone.search(body);}if(path.startsWith("/api/detail?"))return LibreTvStandalone.detail(path);throw new Exception("不支持的本地请求");}
        return call(path,body,true);
    }
    public JSONObject search(JSONObject body,LibreTvStandalone.SearchProgress progress)throws Exception{
        if(standalone()){LibreTvNet.init(context);prepareIndex();return LibreTvStandalone.search(body,progress);}
        return request("/api/search",body);
    }
    private JSONObject call(String path,JSONObject body,boolean retry)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(server()+path).openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(35000);c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Accept","application/json");
        if(!cookie().isEmpty())c.setRequestProperty("Cookie",cookie());
        try{
            if(body!=null){byte[] data=body.toString().getBytes("UTF-8");c.setRequestMethod("POST");c.setRequestProperty("Content-Type","application/json");c.setDoOutput(true);c.setFixedLengthStreamingMode(data.length);c.getOutputStream().write(data);c.getOutputStream().close();}
            int status=c.getResponseCode();
            if(status==401&&retry&&!path.equals("/api/auth")){login();return call(path,body,false);}
            InputStream in=status>=400?c.getErrorStream():c.getInputStream();
            if(in==null)throw new Exception("服务暂时没有响应（"+status+"）");
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
            try{while((n=in.read(b))!=-1){if(out.size()+n>4*1024*1024)throw new Exception("返回内容过大");out.write(b,0,n);}}finally{in.close();}
            JSONObject result=new JSONObject(new String(out.toByteArray(),"UTF-8"));
            if(status>=400)throw new Exception(result.optString("error","请求失败（"+status+"）"));
            String set=c.getHeaderField("Set-Cookie");
            if(path.equals("/api/auth")&&set!=null)prefs.edit().putString("cookie",set.split(";",2)[0]).commit();
            return result;
        }finally{c.disconnect();}
    }
    public synchronized void login()throws Exception{
        String password=prefs.getString("password","");
        if(password.isEmpty())throw new Exception("请在连接设置中输入 LibreTV 密码");
        call("/api/auth",new JSONObject().put("password",password),false);
    }
    public JSONObject status()throws Exception{
        if(standalone()){LibreTvNet.init(context);prepareIndex();return new JSONObject().put("verified",true).put("defaultSources",LibreTvStandalone.sources());}
        JSONObject d=request("/api/status",null);
        if(!d.optBoolean("verified")){login();d=request("/api/status",null);}return d;
    }
}
