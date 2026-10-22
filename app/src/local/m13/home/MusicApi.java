package local.m13.home;

import android.text.Html;
import java.net.*;
import java.io.*;
import org.json.*;

/** Search/catalog/lyrics run directly on the box. No LX Server or custom URL source.
 * Public provider responses remain authoritative; paid/trial streams are rejected.
 */
public final class MusicApi {
    public static final int PAGE=25;
    public static final class Page {public final JSONArray rows;public final int total;public Page(JSONArray r,int t){rows=r;total=t;}}
    public static final class Stream {public final String url;public final long declaredMs;public Stream(String u,long ms){url=u;declaredMs=ms;}}
    private static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private static String clean(String s){return Html.fromHtml(s).toString().trim();}
    private static JSONObject get(String url)throws Exception{return url.startsWith("https://music.163.com/")?NeteaseAccount.get(url):new JSONObject(LibreTvNet.get(url));}
    public static Page search(String source,String query,int page,boolean playlists)throws Exception{
        if(page<1||page>200||query.trim().isEmpty()||query.length()>100)throw new IOException("请输入 1 至 100 字的歌名或歌手");
        if("kw".equals(source)&&!playlists)return kuwo(query,page);
        JSONObject response=get("https://music.163.com/api/search/get/web?s="+enc(query)+"&type="+(playlists?1000:1)+"&limit="+PAGE+"&offset="+((page-1)*PAGE));
        if(response.optInt("code")!=200)throw new IOException("音乐搜索暂不可用，请稍后再试");
        JSONObject result=response.optJSONObject("result");if(result==null)return new Page(new JSONArray(),0);
        JSONArray raw=result.optJSONArray(playlists?"playlists":"songs"),rows=new JSONArray();
        if(raw!=null)for(int i=0;i<raw.length();i++){
            JSONObject s=raw.getJSONObject(i);
            if(playlists)rows.put(new JSONObject().put("source","wy").put("id",s.optString("id")).put("name",s.optString("name")).put("artist",s.optInt("trackCount")+" 首歌曲").put("cover",s.optString("coverImgUrl").replace("http://","https://")).put("playlist",true));
            else rows.put(netease(s));
        }
        return new Page(rows,result.optInt(playlists?"playlistCount":"songCount",rows.length()));
    }
    private static JSONObject netease(JSONObject s)throws Exception{
        JSONArray artists=s.optJSONArray("artists");if(artists==null)artists=s.optJSONArray("ar");StringBuilder names=new StringBuilder();
        if(artists!=null)for(int i=0;i<artists.length();i++){if(i>0)names.append(" / ");names.append(artists.getJSONObject(i).optString("name"));}
        JSONObject album=s.optJSONObject("album");if(album==null)album=s.optJSONObject("al");
        return new JSONObject().put("source","wy").put("id",s.optString("id")).put("name",s.optString("name")).put("artist",names.toString()).put("album",album==null?"":album.optString("name")).put("cover",album==null?"":album.optString("picUrl").replace("http://","https://")).put("duration",s.optLong("duration",s.optLong("dt"))).put("fee",s.optInt("fee"));
    }
    private static Page kuwo(String query,int page)throws Exception{
        JSONObject result=get("https://search.kuwo.cn/r.s?client=kt&all="+enc(query)+"&pn="+(page-1)+"&rn="+PAGE+"&ft=music&encoding=utf8&rformat=json");
        JSONArray raw=result.optJSONArray("abslist"),rows=new JSONArray();
        if(raw!=null)for(int i=0;i<raw.length();i++){
            JSONObject s=raw.getJSONObject(i),pay=s.optJSONObject("payInfo");
            // Unknown or restricted rights never become a presumed free stream.
            JSONObject fees=pay==null?null:pay.optJSONObject("feeType");
            boolean free=pay!=null&&pay.optInt("paytype",-1)==0&&"0000".equals(pay.optString("play"))&&fees!=null&&fees.optInt("song",1)==0&&fees.optInt("album",1)==0&&fees.optInt("vip",1)==0;
            rows.put(new JSONObject().put("source","kw").put("id",s.optString("MUSICRID").replace("MUSIC_","")).put("name",clean(s.optString("SONGNAME",s.optString("NAME")))).put("artist",clean(s.optString("ARTIST"))).put("album",clean(s.optString("ALBUM"))).put("duration",s.optLong("DURATION")*1000).put("cover","").put("restricted",!free));
        }
        return new Page(rows,result.optInt("TOTAL",rows.length()));
    }
    public static Page playlist(String id,int page)throws Exception{
        if(!id.matches("[0-9]{1,20}"))throw new IOException("无效歌单");
        JSONObject response=get("https://music.163.com/api/playlist/detail?id="+id);
        JSONObject list=response.optJSONObject("result");if(list==null)throw new IOException("歌单暂不可用");
        JSONArray raw=list.optJSONArray("tracks"),rows=new JSONArray();int size=raw==null?0:raw.length(),start=(page-1)*PAGE;
        for(int i=start;i<Math.min(size,start+PAGE);i++)rows.put(netease(raw.getJSONObject(i)));
        return new Page(rows,size);
    }
    public static Stream resolve(JSONObject song)throws Exception{
        String id=song.optString("id"),source=song.optString("source");if(!id.matches("[0-9]{1,20}"))throw new IOException("无效歌曲");
        String url;long declared=song.optLong("duration");
        if("wy".equals(source)){
            JSONObject reply=get("https://music.163.com/api/song/enhance/player/url?ids="+enc("["+id+"]")+"&br=128000");
            JSONArray data=reply.optJSONArray("data");if(data==null||data.length()==0)throw new IOException("暂时无法获取播放地址");JSONObject track=data.getJSONObject(0);
            if(!track.isNull("freeTrialInfo"))throw new IOException("此曲目前仅提供试听，请换一首");
            url=track.optString("url");if(url.isEmpty()||url.equals("null"))throw new IOException("此曲需要会员、登录或暂不可播，请换一首");
            long available=track.optLong("time");if(available>0&&declared>0&&available+6000<declared)throw new IOException("此曲只提供片段，未作为完整歌曲播放");
            if(available>0)declared=available;
        }else if("kw".equals(source)){
            if(song.optBoolean("restricted",true))throw new IOException("此曲播放权限受限，请选择其他歌曲");
            url=LibreTvNet.get("https://antiserver.kuwo.cn/anti.s?type=convert_url&rid=MUSIC_"+id+"&format=mp3&response=url").trim();
        }else throw new IOException("不支持的音乐来源");
        LibreTvNet.checked(url);
        HttpURLConnection probe=LibreTvNet.open(url,"bytes=0-1023");
        try{if(probe.getResponseCode()!=200&&probe.getResponseCode()!=206)throw new IOException("音乐源暂不可播");String type=probe.getContentType();if(type!=null&&(type.contains("text/")||type.contains("json")))throw new IOException("音乐源没有返回音频");InputStream in=probe.getInputStream();try{if(in.read()<0)throw new IOException("音频内容为空");}finally{in.close();}}finally{probe.disconnect();}
        return new Stream(url,declared);
    }
    public static String lyrics(JSONObject song)throws Exception{
        if(!"wy".equals(song.optString("source")))return "";
        JSONObject response=get("https://music.163.com/api/song/lyric?id="+song.getString("id")+"&lv=-1&kv=-1&tv=-1");JSONObject lrc=response.optJSONObject("lrc");return lrc==null?"":lrc.optString("lyric");
    }
}
