package local.m13.home;

import android.content.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

/** First-party QR authorization. Cookies are confined to two explicitly allowed NetEase HTTPS hosts,
 * are never exported in diagnostics, and stay in this app's private storage.
 */
public final class NeteaseAccount {
    private static Context app;private static final String ORIGIN="https://music.163.com";
    private static volatile String phase="idle",verificationStage="idle";private static volatile int lastCode,verificationCode;private static volatile boolean hasVerifyId,hasVerifyType,hasVerifyToken;
    public static synchronized void init(Context context){app=context.getApplicationContext();}
    private static android.content.SharedPreferences prefs(){if(app==null)throw new IllegalStateException("音乐账号未初始化");return app.getSharedPreferences("netease-account-private-v1",0);}
    private static synchronized Map<String,String> saved(){Map<String,String> result=new LinkedHashMap<String,String>();try{JSONObject raw=new JSONObject(prefs().getString("session","{}"));Iterator<String> keys=raw.keys();while(keys.hasNext()){String k=keys.next();result.put(k,raw.optString(k));}}catch(Exception ignored){}return result;}
    public static boolean logged(){String cookie=saved().get("MUSIC_U");return cookie!=null&&!cookie.isEmpty();}
    public static synchronized void logout(){prefs().edit().clear().commit();phase="idle";lastCode=0;}
    public static synchronized void accept(Map<String,String> session)throws Exception{
        if(session.get("MUSIC_U")==null||session.get("MUSIC_U").isEmpty())throw new IOException("尚未取得登录授权，请重新扫码");
        JSONObject value=new JSONObject();for(Map.Entry<String,String> e:session.entrySet())value.put(e.getKey(),e.getValue());
        if(!prefs().edit().putString("session",value.toString()).putLong("authorizedAt",System.currentTimeMillis()).commit())throw new IOException("盒子未能保存登录状态");phase="authorized";
    }
    private static void capture(HttpURLConnection h,Map<String,String> jar){
        for(Map.Entry<String,List<String>> e:h.getHeaderFields().entrySet())if(e.getKey()!=null&&e.getKey().equalsIgnoreCase("Set-Cookie"))for(String raw:e.getValue())try{for(HttpCookie c:HttpCookie.parse(raw)){
            String name=c.getName();if(!(name.equals("MUSIC_U")||name.equals("MUSIC_A")||name.equals("__csrf")||name.equals("NMTID")||name.equals("MUSIC_R_T")))continue;
            String domain=c.getDomain();if(domain!=null&&!domain.isEmpty()&&!domain.equals("music.163.com")&&!domain.equals(".music.163.com"))continue;
            if(c.getMaxAge()==0)jar.remove(name);else jar.put(name,c.getValue());
        }}catch(IllegalArgumentException ignored){}
    }
    private static JSONObject request(String path,Map<String,String> jar)throws Exception{return request(path,jar,null);}
    private static JSONObject request(String path,Map<String,String> jar,String body)throws Exception{
        if(app==null)throw new IOException("请先打开轻音乐");LibreTvNet.init(app);
        URL url=LibreTvNet.checked(path.startsWith("https://")?path:ORIGIN+path);
        if(!url.getProtocol().equals("https")||!(url.getHost().equals("music.163.com")||url.getHost().equals("interface.music.163.com"))||url.getPort()!=-1)throw new IOException("账号请求地址无效");
        HttpURLConnection h=(HttpURLConnection)url.openConnection();h.setInstanceFollowRedirects(false);h.setConnectTimeout(8000);h.setReadTimeout(10000);LibreTvNet.configureTls(h);
        h.setRequestProperty("User-Agent",url.getHost().equals("interface.music.163.com")?"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.1.29.205117":LibreTvNet.UA);h.setRequestProperty("Referer",ORIGIN+"/");h.setRequestProperty("Accept","application/json");
        StringBuilder cookies=new StringBuilder();for(Map.Entry<String,String> e:jar.entrySet()){if(cookies.length()>0)cookies.append("; ");cookies.append(e.getKey()).append('=').append(e.getValue());}if(cookies.length()>0)h.setRequestProperty("Cookie",cookies.toString());
        try{if(body!=null){byte[] data=body.getBytes("UTF-8");h.setRequestMethod("POST");h.setDoOutput(true);h.setRequestProperty("Content-Type","application/x-www-form-urlencoded");h.setFixedLengthStreamingMode(data.length);OutputStream out=h.getOutputStream();try{out.write(data);}finally{out.close();}}if(h.getResponseCode()!=200)throw new IOException("网易云连接暂不可用，请稍后再试");capture(h,jar);InputStream in=h.getInputStream();try{return new JSONObject(new String(LibreTvNet.read(in,4*1024*1024),"UTF-8"));}finally{in.close();}}finally{h.disconnect();}
    }
    public static JSONObject get(String url)throws Exception{
        Map<String,String> session=saved();JSONObject result=request(url,session);
        if(session.containsKey("MUSIC_U"))accept(session);return result;
    }
    private static String aes(String value,String key)throws Exception{
        javax.crypto.Cipher c=javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(javax.crypto.Cipher.ENCRYPT_MODE,new javax.crypto.spec.SecretKeySpec(key.getBytes("UTF-8"),"AES"),new javax.crypto.spec.IvParameterSpec("0102030405060708".getBytes("UTF-8")));
        return android.util.Base64.encodeToString(c.doFinal(value.getBytes("UTF-8")),android.util.Base64.NO_WRAP);
    }
    /** Standard web-client envelope; HTTPS and normal account checks still apply. */
    private static JSONObject webPost(String path,JSONObject data,Map<String,String> jar)throws Exception{
        data.put("csrf_token",jar.containsKey("__csrf")?jar.get("__csrf"):"");
        String alphabet="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";java.security.SecureRandom random=new java.security.SecureRandom();StringBuilder secret=new StringBuilder();for(int i=0;i<16;i++)secret.append(alphabet.charAt(random.nextInt(alphabet.length())));
        String encoded=aes(aes(data.toString(),"0CoJUm6Qyw8W8jud"),secret.toString());
        String der="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDgtQn2JZ34ZC28NWYpAUd98iZ37BUrX/aKzmFbt7clFSs6sXqHauqKWqdtLkF2KexO40H1YTX8z2lSgBBOAxLsvaklV8k4cBFK9snQXE9/DDaFt6Rr7iVZMldczhC0JNgTz+SHXT6CBHuX3e9SdB1Ua44oncaTWz7OBGLbCiK45wIDAQAB";
        java.security.interfaces.RSAPublicKey pub=(java.security.interfaces.RSAPublicKey)java.security.KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(android.util.Base64.decode(der,android.util.Base64.DEFAULT)));
        String rsa=new java.math.BigInteger(1,secret.reverse().toString().getBytes("UTF-8")).modPow(pub.getPublicExponent(),pub.getModulus()).toString(16);while(rsa.length()<256)rsa="0"+rsa;
        return request(path,jar,"params="+URLEncoder.encode(encoded,"UTF-8")+"&encSecKey="+rsa);
    }
    public static final class Login {
        final Map<String,String> jar=new LinkedHashMap<String,String>();private String key,riskUrl;private JSONObject risk;
        public String begin()throws Exception{verificationStage="idle";verificationCode=0;hasVerifyId=hasVerifyType=hasVerifyToken=false;JSONObject reply=request("https://interface.music.163.com/api/login/qrcode/unikey",jar,"type=3");lastCode=reply.optInt("code");key=reply.optString("unikey");if(key.isEmpty()&&reply.optJSONObject("data")!=null)key=reply.getJSONObject("data").optString("unikey");if(lastCode!=200||key.isEmpty())throw new IOException("网易云暂未提供二维码，请刷新");phase="waiting";return "https://music.163.com/login?codekey="+URLEncoder.encode(key,"UTF-8");}
        public int check()throws Exception{JSONObject reply=request("https://interface.music.163.com/api/login/qrcode/client/login",jar,"type=3&key="+URLEncoder.encode(key,"UTF-8"));int code=reply.optInt("code");lastCode=code;phase=code==8821?"verification_required":code==802?"scanned":code==803?"approved":code==800?"expired":"waiting";if(code==8821)risk=reply;return code;}
        private String field(JSONObject o,String a,String b){String v=o.optString(a,"");return v.isEmpty()?o.optString(b,""):v;}
        public String verificationQr()throws Exception{
            if(riskUrl!=null)return riskUrl;if(risk==null)throw new IOException("尚未收到验证要求");
            verificationStage="metadata";JSONObject r=risk;if(field(r,"verifyId","verifyConfigId").isEmpty()){JSONObject nested=r.optJSONObject("data");if(nested==null){try{nested=new JSONObject(r.optString("data","{}"));}catch(Exception ignored){}}if(nested!=null&&nested.length()>0)r=nested;}
            String id=field(r,"verifyId","verifyConfigId"),type=r.optString("verifyType",""),token=field(r,"verifyToken","token");
            JSONObject params=r.optJSONObject("params");if(params==null){try{params=new JSONObject(r.optString("params","{}"));}catch(Exception ignored){params=new JSONObject();}}
            hasVerifyId=!id.isEmpty();hasVerifyType=!type.isEmpty();hasVerifyToken=!token.isEmpty();if(id.isEmpty()||type.isEmpty()||token.isEmpty()){verificationStage="missing_metadata";throw new IOException("网易云验证信息不完整");}
            JSONObject data=new JSONObject().put("verifyConfigId",id).put("verifyType",type).put("token",token).put("params",params.toString()).put("size",150);
            verificationStage="request";JSONObject reply=webPost("/weapi/frontrisk/verify/getqrcode",data,jar);verificationCode=reply.optInt("code");verificationStage="response";JSONObject result=reply.optJSONObject("data");String qr=result==null?"":result.optString("qrCode","");if(qr.isEmpty())throw new IOException("网易云尚未提供验证二维码");
            verificationStage="qr_ready";riskUrl="https://st.music.163.com/encrypt-pages?qrCode="+URLEncoder.encode(qr,"UTF-8")+"&verifyToken="+URLEncoder.encode(token,"UTF-8")+"&verifyId="+URLEncoder.encode(id,"UTF-8")+"&verifyType="+URLEncoder.encode(type,"UTF-8")+"&params="+URLEncoder.encode(params.toString(),"UTF-8");return riskUrl;
        }
        public void commit()throws Exception{accept(jar);}
    }
    public static JSONObject diagnostics(){try{return new JSONObject().put("authorized",logged()).put("phase",phase).put("lastCode",lastCode).put("verificationStage",verificationStage).put("verificationCode",verificationCode).put("hasVerificationId",hasVerifyId).put("hasVerificationType",hasVerifyType).put("hasVerificationToken",hasVerifyToken);}catch(Exception e){return new JSONObject();}}
}
