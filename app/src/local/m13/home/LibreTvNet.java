package local.m13.home;

import android.content.Context;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.*;
import javax.net.ssl.*;

/** Certificate validation is retained; Mozilla roots supplement the old system store. */
public final class LibreTvNet {
    public static final String UA="Mozilla/5.0 (Linux; Android 5.1.1) AppleWebKit/537.36 Chrome/122.0.0.0 Safari/537.36";
    private static SSLSocketFactory tls;
    public static synchronized void init(Context c)throws Exception{
        if(tls!=null)return;
        LibreTvSearch.loadAliases(c.getAssets().open("numbered-title-aliases.bin"));
        TrustManagerFactory system=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());system.init((KeyStore)null);
        KeyStore roots=KeyStore.getInstance(KeyStore.getDefaultType());roots.load(null,null);
        InputStream in=c.getAssets().open("mozilla-cacert.pem");
        try{
            String pem=new String(read(in,1024*1024),"US-ASCII");
            java.util.regex.Matcher blocks=java.util.regex.Pattern.compile("-----BEGIN CERTIFICATE-----[\\s\\S]*?-----END CERTIFICATE-----").matcher(pem);
            CertificateFactory certificates=CertificateFactory.getInstance("X.509");int i=0;
            // This vendor's old parser silently returns an empty collection for a PEM bundle with leading comments.
            while(blocks.find())roots.setCertificateEntry("mozilla-"+(i++),certificates.generateCertificate(new ByteArrayInputStream(blocks.group().getBytes("US-ASCII"))));
            if(i<100)throw new CertificateException("证书包没有完整载入");
        }finally{in.close();}
        TrustManagerFactory extra=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());extra.init(roots);
        final X509TrustManager a=(X509TrustManager)system.getTrustManagers()[0],b=(X509TrustManager)extra.getTrustManagers()[0];
        X509TrustManager combined=new X509TrustManager(){
            public void checkClientTrusted(X509Certificate[] chain,String auth)throws CertificateException{a.checkClientTrusted(chain,auth);}
            public void checkServerTrusted(X509Certificate[] chain,String auth)throws CertificateException{try{a.checkServerTrusted(chain,auth);}catch(CertificateException e){try{b.checkServerTrusted(chain,auth);}catch(CertificateException extra){for(X509Certificate x:chain)android.util.Log.e("M13TLS","subject="+x.getSubjectX500Principal()+" issuer="+x.getIssuerX500Principal()+" algorithm="+x.getSigAlgName());android.util.Log.e("M13TLS","auth="+auth+" roots="+b.getAcceptedIssuers().length+" failure="+extra);throw extra;}}}
            public X509Certificate[] getAcceptedIssuers(){return b.getAcceptedIssuers();}
        };
        SSLContext context=SSLContext.getInstance("TLS");context.init(null,new TrustManager[]{combined},null);tls=context.getSocketFactory();
    }
    public static URL checked(String value)throws Exception{
        URL u=new URL(value);if(!("http".equals(u.getProtocol())||"https".equals(u.getProtocol()))||u.getUserInfo()!=null)throw new IOException("不支持的片源地址");
        for(InetAddress a:InetAddress.getAllByName(u.getHost()))if(a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress()||a.isMulticastAddress())throw new IOException("片源地址不能指向内网设备");return u;
    }
    public static HttpURLConnection open(String value,String range)throws Exception{
        return open(value,range,10000,20000);
    }
    private static HttpURLConnection open(String value,String range,int connectMs,int readMs)throws Exception{
        URL u=checked(value);
        for(int redirect=0;redirect<6;redirect++){
            HttpURLConnection h=(HttpURLConnection)u.openConnection();h.setConnectTimeout(connectMs);h.setReadTimeout(readMs);h.setInstanceFollowRedirects(false);
            h.setRequestProperty("User-Agent",UA);h.setRequestProperty("Accept","*/*");h.setRequestProperty("Accept-Encoding","identity");
            if(range!=null)h.setRequestProperty("Range",range);
            if(h instanceof HttpsURLConnection)((HttpsURLConnection)h).setSSLSocketFactory(tls);
            int status;
            try{status=h.getResponseCode();}catch(Exception e){h.disconnect();throw e;}
            if(status==301||status==302||status==303||status==307||status==308){String location=h.getHeaderField("Location");h.disconnect();if(location==null)throw new IOException("片源重定向无效");u=checked(new URL(u,location).toString());continue;}
            return h;
        }throw new IOException("片源重定向过多");
    }
    public static byte[] read(InputStream in,int max)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>max)throw new IOException("片源返回内容过大");out.write(buffer,0,n);}return out.toByteArray();
    }
    public static String get(String value)throws Exception{
        return get(value,false);
    }
    public static String get(String value,boolean search)throws Exception{
        HttpURLConnection h=search?open(value,null,4000,5000):open(value,null);try{if(h.getResponseCode()!=200)throw new IOException("片源返回 "+h.getResponseCode());InputStream in=h.getInputStream();try{return new String(read(in,4*1024*1024),"UTF-8");}finally{in.close();}}finally{h.disconnect();}
    }
}
