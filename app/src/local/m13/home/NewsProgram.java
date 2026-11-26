package local.m13.home;

import android.text.Html;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;
import org.json.*;

final class NewsProgram implements Comparable<NewsProgram> {
    enum Source { CNR, MORNING, CHINANEWS }
    private static final String CNR="https://china.cnr.cn/news/yphf/";
    private static final String MORNING="https://www.xiaoyuzhoufm.com/podcast/67891a429407dcd1bfbfcfcb";
    private static final String VIDEO="https://www.chinanews.com.cn/shipin/";
    private static final Pattern REPLAY=Pattern.compile("href=[\"']([^\"']*/yphf/(\\d{8})/t\\d{8}_\\d+\\.shtml)[\"']");
    private static final Pattern ARTICLE=Pattern.compile("href=[\"']((?:https://www\\.chinanews\\.com\\.cn)?/[^?#\"']*shipin/[^?#\"']*?(\\d{4})/(\\d{2})-(\\d{2})/news(\\d+)\\.shtml)[\"']");
    private static final Pattern CNR_AUDIO=Pattern.compile("data-url=[\"'](https://[^\\s<>\"']+?\\.m3u8[^\"']*)[\"']");
    private static final Pattern MP4=Pattern.compile("https://[a-zA-Z0-9.-]+/[^\\s<>\"']+?\\.mp4\\b");
    private static final Pattern TITLE=Pattern.compile("<title>(.*?)</title>",Pattern.DOTALL);
    final Source source;
    final String id,day,title,url;

    NewsProgram(Source source,String id,String day,String title,String url){
        this.source=source;this.id=id;this.day=day;this.title=title;this.url=url;
    }
    public int compareTo(NewsProgram other){
        int byDay=other.day.compareTo(day);
        if(byDay!=0)return byDay;
        int byTopic=Integer.compare(topic(),other.topic());
        return byTopic!=0?byTopic:source.compareTo(other.source);
    }
    private static final Pattern TECH=Pattern.compile("科技|科研|芯片|半导体|人工智能|大模型|算力|量子|机器人|航天|卫星|空间站|火箭|核聚变|超导|光刻|科学家|天文|新能源|通信技术|集成电路|生物技术|\\bAI\\b",Pattern.CASE_INSENSITIVE);
    private static final Pattern MONEY=Pattern.compile("金融|财经|经济|央行|银行|货币|利率|汇率|证券|股市|股指|债券|财政|基金|融资|投资|通胀|外贸|进出口|关税|资本市场|证监会|美联储|金价|油价|贸易|上市公司");
    private static final Pattern MILITARY=Pattern.compile("军事|军方|军队|军舰|军演|战机|航母|国防|导弹|海军|空军|陆军|战区|演训|空袭|武装冲突");
    private static final Pattern EVENTS=Pattern.compile("峰会|联合国|国务院|外交部|总统|总理|大选|停火|制裁|条约|地震|海啸|紧急状态|撤侨|领导人会晤");
    private static final Pattern SPORT=Pattern.compile("比赛|锦标赛|世界杯|奥运|联赛|决赛|冠军|国足|篮球|乒乓|足球|网球|冰球");
    int topic(){
        if(source!=Source.CHINANEWS)return 2;
        if(TECH.matcher(title).find()||MONEY.matcher(title).find()||url.contains("/cj/")||url.contains("/it/")||url.contains("/kj/"))return 0;
        if(MILITARY.matcher(title).find()||url.contains("/mil/"))return 1;
        if(SPORT.matcher(title).find()||url.contains("/ty/"))return 3;
        if(EVENTS.matcher(title).find()||url.contains("/gj/")||url.contains("/gn/"))return 1;
        return 3;
    }
    String topicLabel(){
        switch(topic()){
            case 0:return "科技财经";
            case 1:return "军事与国内外大事";
            case 2:return "综合早报";
            default:return "体育民生及其他";
        }
    }
    static String day(int daysAgo){
        Calendar date=NewsSchedule.now();
        date.add(Calendar.DATE,-daysAgo);
        return formatDay(date.getTime());
    }
    private static String formatDay(Date d){
        SimpleDateFormat format=new SimpleDateFormat("yyyyMMdd",Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return format.format(d);
    }
    String displayDate(){return day.substring(4,6)+"月"+day.substring(6)+"日";}

    static List<NewsProgram> read(Source source)throws Exception{
        List<NewsProgram> items=new ArrayList<>();
        if(source==Source.MORNING){
            String page=text(MORNING,"UTF-8");
            int marker=page.indexOf("id=\"__NEXT_DATA__\"");
            if(marker<0)throw new IOException("找不到节目单");
            int start=page.indexOf('>',marker),end=page.indexOf("</script>",start);
            if(start<0||end<start)throw new IOException("节目单不完整");
            JSONArray episodes=new JSONObject(page.substring(start+1,end)).getJSONObject("props")
                .getJSONObject("pageProps").getJSONObject("podcast").getJSONArray("episodes");
            SimpleDateFormat utc=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US);
            utc.setTimeZone(TimeZone.getTimeZone("UTC"));
            for(int i=0;i<episodes.length();i++){
                JSONObject ep=episodes.getJSONObject(i), enclosure=ep.optJSONObject("enclosure");
                String stamp=ep.optString("pubDate"),id=ep.optString("eid");
                if(enclosure==null||id.isEmpty()||stamp.length()<20||!stamp.endsWith("Z")||
                    ep.optBoolean("isPrivateMedia")||!"FREE".equals(ep.optString("payType")))continue;
                String url=enclosure.optString("url");
                if(url.startsWith("https://"))items.add(new NewsProgram(source,"morning:"+id,
                    formatDay(utc.parse(stamp.substring(0,19))),"资讯早7点 · "+ep.getString("title"),url));
            }
        }else if(source==Source.CNR){
            Matcher m=REPLAY.matcher(text(CNR,"GB18030"));
            while(m.find())items.add(new NewsProgram(source,"cnr:"+m.group(2),m.group(2),"中国之声 · 新闻和报纸摘要",new URL(new URL(CNR),m.group(1)).toString()));
        }else{
            String page=text(VIDEO,"UTF-8");
            Matcher m=ARTICLE.matcher(page);
            while(m.find()){
                int end=page.indexOf("</a>",m.end());
                if(end<0||end-m.end()>4000)continue;
                String anchor=page.substring(m.end(),end);
                int open=anchor.indexOf('>');
                if(open<0)continue;
                String label=Html.fromHtml(anchor.substring(open+1).replaceAll("(?is)<[^>]+>"," ")).toString().trim();
                String url=new URL(new URL(VIDEO),m.group(1)).toString();
                if(!"www.chinanews.com.cn".equals(new URL(url).getHost())||label.isEmpty())continue;
                items.add(new NewsProgram(source,"cns:"+m.group(5),m.group(2)+m.group(3)+m.group(4),label,url));
            }
        }
        return items;
    }
    NewsProgram resolve()throws Exception{
        if(source==Source.MORNING)return this;
        boolean cnr=source==Source.CNR;
        String page=text(url,cnr?"GB18030":"UTF-8");
        Matcher file=(cnr?CNR_AUDIO:MP4).matcher(page);
        if(!file.find())throw new IOException("节目缺少播放地址");
        Matcher name=TITLE.matcher(page);
        String label=!cnr&&name.find()?Html.fromHtml(name.group(1)).toString().trim():title;
        String media=cnr?file.group(1).replace("&amp;","&"):file.group();
        return new NewsProgram(source,id,day,label,media);
    }
    private static String text(String url,String encoding)throws Exception{
        HttpURLConnection connection=LibreTvNet.open(url,null);
        try{
            if(connection.getResponseCode()!=200)throw new IOException("新闻源返回 "+connection.getResponseCode());
            try(BufferedInputStream body=new BufferedInputStream(connection.getInputStream())){
                body.mark(2);
                boolean gzip=body.read()==0x1f&&body.read()==0x8b;
                body.reset();
                try(InputStream decoded=gzip?new GZIPInputStream(body):body){
                    return new String(LibreTvNet.read(decoded,4*1024*1024),encoding);
                }
            }
        }finally{connection.disconnect();}
    }
}
