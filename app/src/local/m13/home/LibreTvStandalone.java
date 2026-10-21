package local.m13.home;
import android.net.Uri;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Native Apple CMS catalogue/search adaptation; all requests run on the box. */
public final class LibreTvStandalone {
    public interface SearchProgress {void update(JSONObject snapshot);}
    public static JSONArray sources()throws Exception{return new JSONArray()
        .put(new JSONObject().put("key","direct_0").put("name","量子资源").put("url","https://cj.lziapi.com/api.php/provide/vod"))
        .put(new JSONObject().put("key","direct_1").put("name","无尽资源").put("url","https://api.wujinapi.com/api.php/provide/vod"));}
    public static JSONObject source(String key)throws Exception{JSONArray a=sources();for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("key").equals(key))return a.getJSONObject(i);throw new Exception("找不到这个来源");}
    private static JSONObject fetch(JSONObject s,String query)throws Exception{JSONObject d=new JSONObject(LibreTvNet.get(s.getString("url")+"?ac=videolist&"+query));LibreTvTitleIndex index=LibreTvTitleIndex.current();if(index!=null)try{index.learn(d.optJSONArray("list"));}catch(Exception ignored){}return d;}
    static boolean allowed(String type){return !type.matches(".*(伦理|倫理|福利|情色|色情|里番|无码|有码|成人).*");}
    private static JSONObject item(JSONObject v,JSONObject s)throws Exception{return new JSONObject().put("sourceKey",s.getString("key")).put("sourceName",s.getString("name")).put("sourceUrl",s.getString("url")).put("vodId",v.optString("vod_id")).put("name",v.optString("vod_name")).put("pic",v.optString("vod_pic")).put("year",v.optString("vod_year")).put("doubanId",v.optString("vod_douban_id")).put("language",v.optString("vod_lang")).put("area",v.optString("vod_area")).put("typeId",v.optString("type_id")).put("typeName",v.optString("type_name")).put("remarks",v.optString("vod_remarks")).put("updated",v.optString("vod_time"));}
    private static final Map<String,JSONObject> categoryCache=new HashMap<String,JSONObject>();
    public static synchronized JSONObject categories(String key)throws Exception{
        if(categoryCache.containsKey(key))return categoryCache.get(key);
        JSONObject s=source(key),d=new JSONObject(LibreTvNet.get(s.getString("url")+"?ac=list"));JSONArray raw=d.optJSONArray("class"),out=new JSONArray();
        if(raw!=null)for(int i=0;i<raw.length();i++){JSONObject c=raw.getJSONObject(i);if(allowed(c.optString("type_name")))out.put(c);}
        JSONObject result=new JSONObject().put("class",out);categoryCache.put(key,result);return result;
    }
    private static String types(JSONObject s,String group)throws Exception{
        if(group.matches("\\d+"))return group;
        String pattern=group.equals("movies")?".*(电影|动作|喜剧|爱情|科幻|恐怖|剧情|战争|纪录|记录|悬疑|惊悚|犯罪|奇幻|动画片|邵氏).*":group.equals("tv")?".*(连续剧|国产剧|香港剧|台湾剧|韩国剧|欧美剧|美国剧|日本剧|海外剧|泰剧|泰国剧).*":group.equals("shows")?".*综艺.*":".*动漫.*";
        JSONArray classes=categories(s.getString("key")).getJSONArray("class");StringBuilder ids=new StringBuilder();for(int i=0;i<classes.length();i++){JSONObject c=classes.getJSONObject(i);String name=c.optString("type_name");if(name.matches(pattern)&&!name.contains("解说")&&!name.contains("资讯")){if(ids.length()>0)ids.append(',');ids.append(c.optString("type_id"));}}
        if(ids.length()==0)throw new Exception("这个来源暂时没有此分类");return ids.toString();
    }
    public static JSONObject catalog(JSONObject b)throws Exception{
        final JSONObject s=source(b.optString("sourceKey","direct_0"));final int page=Math.max(1,b.optInt("page",1));String[] ids=types(s,b.optString("type","movies")).split(",");
        ExecutorService pool=Executors.newFixedThreadPool(Math.min(4,ids.length));List<Future<JSONObject>> jobs=new ArrayList<Future<JSONObject>>();
        for(final String id:ids)jobs.add(pool.submit(new Callable<JSONObject>(){public JSONObject call()throws Exception{return fetch(s,"t="+id+"&pg="+page);}}));
        LinkedHashMap<String,JSONObject> merged=new LinkedHashMap<String,JSONObject>();int pages=1,total=0,ok=0,failed=0;long end=System.currentTimeMillis()+35000;
        try{for(Future<JSONObject> f:jobs)try{JSONObject d=f.get(Math.max(1,end-System.currentTimeMillis()),TimeUnit.MILLISECONDS);ok++;int n=Math.max(1,d.optInt("pagecount",1));pages=Math.max(pages,n);total+=d.optInt("total",0);if(page>n)continue;JSONArray raw=d.optJSONArray("list");if(raw!=null)for(int i=0;i<raw.length();i++){JSONObject v=raw.getJSONObject(i);if(allowed(v.optString("type_name"))){JSONObject v2=item(v,s);merged.put(v2.optString("vodId"),v2);}}}catch(Exception e){failed++;}
            if(ok==0)throw new java.io.IOException("分类暂时无法加载，请重试");List<JSONObject> sorted=new ArrayList<JSONObject>(merged.values());Collections.sort(sorted,new Comparator<JSONObject>(){public int compare(JSONObject a,JSONObject b){return b.optString("updated").compareTo(a.optString("updated"));}});JSONArray out=new JSONArray();for(JSONObject v:sorted)out.put(v);
            return new JSONObject().put("list",out).put("page",page).put("pagecount",Math.max(page,pages)).put("total",total).put("failedCategories",failed);
        }finally{for(Future<?> f:jobs)f.cancel(true);pool.shutdownNow();}
    }
    private static class Batch {JSONArray list=new JSONArray();JSONObject cursor=new JSONObject();int fetched;String error="",detail="";boolean ok;volatile Batch checkpoint;}
    private static void publish(Batch r,Map<String,JSONObject> found)throws Exception{
        summarize(r.cursor);Batch saved=new Batch();saved.cursor=new JSONObject(r.cursor.toString());saved.fetched=r.fetched;saved.error=r.error;saved.detail=r.detail;saved.ok=r.ok;
        for(JSONObject item:found.values())saved.list.put(item);r.checkpoint=saved;
    }
    private static JSONObject searchCursor(JSONObject old,String word,List<String> terms)throws Exception{
        if(old!=null&&word.equals(old.optString("word"))&&old.optInt("version")==2&&old.optJSONArray("queries")!=null)return new JSONObject(old.toString());
        JSONArray queries=new JSONArray();for(String term:terms)queries.put(new JSONObject().put("term",term).put("page",0).put("pages",Integer.MAX_VALUE));
        return new JSONObject().put("version",2).put("word",word).put("queries",queries).put("next",0).put("page",0).put("pages",Integer.MAX_VALUE).put("hasMore",true);
    }
    private static void summarize(JSONObject cursor)throws Exception{
        JSONArray queries=cursor.getJSONArray("queries");int page=0;long pages=0;boolean more=false;
        for(int i=0;i<queries.length();i++){JSONObject q=queries.getJSONObject(i);page+=q.optInt("page");pages+=q.optInt("pages",Integer.MAX_VALUE);more|=q.optInt("page")<q.optInt("pages",Integer.MAX_VALUE);}
        cursor.put("page",page).put("pages",Math.min(Integer.MAX_VALUE,pages)).put("hasMore",more);
    }
    private static Batch searchBatch(JSONObject source,String word,JSONObject old,int batch,List<String> terms,Batch r,int initialLimit)throws Exception{
        r.cursor=searchCursor(old,word,terms);JSONArray queries=r.cursor.getJSONArray("queries");
        LinkedHashMap<String,JSONObject> found=new LinkedHashMap<String,JSONObject>();Set<Integer> failed=new HashSet<Integer>();long end=System.currentTimeMillis()+28000;
        for(int attempt=0;attempt<(batch==1?Math.min(initialLimit,terms.size()):batch);attempt++){
            if(Thread.currentThread().isInterrupted()||System.currentTimeMillis()>end)break;
            int selected=-1,next=r.cursor.optInt("next");
            for(int offset=0;offset<queries.length();offset++){int index=(next+offset)%queries.length();JSONObject q=queries.getJSONObject(index);if(!failed.contains(index)&&q.optInt("page")<q.optInt("pages",Integer.MAX_VALUE)){selected=index;break;}}
            if(selected<0)break;
            JSONObject q=queries.getJSONObject(selected);r.cursor.put("next",(selected+1)%queries.length());
            try{
                int page=q.optInt("page")+1;JSONObject d=new JSONObject(LibreTvNet.get(source.getString("url")+"?ac=videolist&wd="+LibreTvApi.enc(q.getString("term"))+"&pg="+page,true));JSONArray raw=d.optJSONArray("list");
                if(raw==null)throw new java.io.IOException("片源没有返回搜索列表");
                for(int j=0;j<raw.length();j++){JSONObject v=raw.getJSONObject(j);if(!allowed(v.optString("type_name")))continue;
                    int score=LibreTvSearch.score(v.optString("vod_name"),word,v.optString("vod_year"));if(score<0)continue;
                    JSONObject result=item(v,source).put("searchScore",score).put("searchMatch",LibreTvSearch.kind(score));found.put(result.optString("vodId"),result);
                }
                q.put("page",page).put("pages",Math.max(page,d.optInt("pagecount",page)));r.fetched++;r.ok=true;
            }catch(Exception e){if(Thread.currentThread().isInterrupted())throw new InterruptedException();failed.add(selected);r.error=source.getString("name")+"部分关键词暂时连接失败，可继续加载重试";r.detail=e.toString();android.util.Log.w("M13Search",source.optString("key")+" search request failed",e);}
            publish(r,found);
            if(batch==1&&!found.isEmpty())break;
        }
        summarize(r.cursor);if(!r.cursor.optBoolean("hasMore"))r.ok=true;
        for(JSONObject v:found.values())r.list.put(v);return r;
    }
    private static JSONObject searchSnapshot(List<Batch> progress,List<String> keys,JSONObject previous,String word,List<String> terms,List<String> resolved)throws Exception{
        JSONArray list=new JSONArray(),errors=new JSONArray(),debugErrors=new JSONArray();JSONObject cursors=new JSONObject();int ok=0,fetched=0;boolean more=false;
        for(int i=0;i<progress.size();i++){
            Batch r=progress.get(i).checkpoint;
            if(r==null){r=new Batch();r.cursor=searchCursor(previous==null?null:previous.optJSONObject(keys.get(i)),word,terms);}
            cursors.put(keys.get(i),r.cursor);if(r.ok)ok++;fetched+=r.fetched;more|=r.cursor.optBoolean("hasMore",true);
            if(!r.error.isEmpty()){errors.put(r.error);debugErrors.put(new JSONObject().put("source",keys.get(i)).put("detail",r.detail));}
            for(int j=0;j<r.list.length();j++)list.put(r.list.get(j));
        }
        return new JSONObject().put("list",list).put("cursors",cursors).put("hasMore",more).put("pagesFetched",fetched).put("errors",errors).put("sourceErrors",debugErrors).put("sourcesOk",ok).put("resolvedTitles",new JSONArray(resolved)).put("phoneticQuery",LibreTvPinyin.query(word));
    }
    public static JSONObject search(final JSONObject body)throws Exception{return search(body,null);}
    public static JSONObject search(final JSONObject body,SearchProgress listener)throws Exception{
        final String word=LibreTvSearch.canonicalQuery(body.getString("wd").trim());if(LibreTvSearch.normalize(word).isEmpty())throw new Exception("请输入有效片名");if(word.length()>100)throw new Exception("片名请控制在 100 字以内");
        final JSONObject previous=body.optJSONObject("cursors");final int batch=Math.min(5,Math.max(1,body.optInt("batchPages",1)));
        final boolean voice=body.optBoolean("voiceSearch",false);final int initialLimit=voice?2:5;
        final List<String> resolved=LibreTvPinyin.query(word)&&LibreTvTitleIndex.current()!=null?LibreTvTitleIndex.current().resolve(word):Collections.<String>emptyList();
        LinkedHashSet<String> plan=new LinkedHashSet<String>();if(!resolved.isEmpty()){for(String title:resolved){String anchor=LibreTvPinyin.anchor(title,word);plan.add(anchor.isEmpty()?title:anchor);}plan.add(word);}plan.addAll(LibreTvSearch.queries(word));final List<String> terms=new ArrayList<String>(plan);
        final JSONArray all=sources();ExecutorService pool=Executors.newFixedThreadPool(2);List<Future<Batch>> jobs=new ArrayList<Future<Batch>>();List<String> keys=new ArrayList<String>();List<Batch> progress=new ArrayList<Batch>();
        long at=android.os.SystemClock.elapsedRealtime();
        for(int i=0;i<all.length();i++){final JSONObject source=all.getJSONObject(i);final String key=source.getString("key");final Batch state=new Batch();progress.add(state);keys.add(key);jobs.add(pool.submit(new Callable<Batch>(){public Batch call()throws Exception{return searchBatch(source,word,previous==null?null:previous.optJSONObject(key),batch,terms,state,initialLimit);}}));}
        int delivered=-1;long end=System.currentTimeMillis()+(batch==1?(voice?8000:16000):32000);
        try{
            while(true){
                if(Thread.currentThread().isInterrupted())throw new InterruptedException();boolean done=true;for(Future<?> f:jobs)done&=f.isDone();
                JSONObject snapshot=searchSnapshot(progress,keys,previous,word,terms,resolved);int fetched=snapshot.optInt("pagesFetched");
                if(listener!=null&&fetched!=delivered&&snapshot.getJSONArray("list").length()>0){delivered=fetched;snapshot.put("elapsedMs",android.os.SystemClock.elapsedRealtime()-at);listener.update(snapshot);}
                if(done||System.currentTimeMillis()>=end)break;Thread.sleep(100);
            }
            for(int i=0;i<jobs.size();i++){
                Batch state=progress.get(i);if(jobs.get(i).isDone())try{jobs.get(i).get();}catch(Exception e){if(e instanceof InterruptedException)throw e;state.error=source(keys.get(i)).getString("name")+"暂时连接失败，可继续加载重试";state.detail=e.toString();}
                else{state.error=source(keys.get(i)).getString("name")+"响应较慢，可继续加载重试";state.detail="Search deadline";}
                if(!state.error.isEmpty()){Batch saved=state.checkpoint;if(saved==null){saved=new Batch();saved.cursor=searchCursor(previous==null?null:previous.optJSONObject(keys.get(i)),word,terms);}saved.error=state.error;saved.detail=state.detail;state.checkpoint=saved;}
            }
            JSONObject result=searchSnapshot(progress,keys,previous,word,terms,resolved);result.put("elapsedMs",android.os.SystemClock.elapsedRealtime()-at);
            if(result.optInt("sourcesOk")==0&&result.getJSONArray("list").length()==0&&result.getJSONArray("errors").length()>0)throw new java.io.IOException(result.getJSONArray("errors").getString(0));
            return result;
        }finally{for(Future<?> f:jobs)f.cancel(true);pool.shutdownNow();}
    }
    public static JSONObject detail(String path)throws Exception{
        Uri q=Uri.parse("http://local"+path);JSONObject request=new JSONObject(q.getQueryParameter("source")),s=source(request.optString("key"));String id=q.getQueryParameter("id");if(id==null||!id.matches("[\\w-]+"))throw new Exception("片源参数无效");
        JSONObject v=fetch(s,"ids="+LibreTvApi.enc(id)).getJSONArray("list").getJSONObject(0);JSONArray eps=new JSONArray(),names=new JSONArray();
        for(String group:v.optString("vod_play_url").split("\\$\\$\\$")){JSONArray found=new JSONArray(),labels=new JSONArray();boolean hls=false;for(String ep:group.split("#")){int split=ep.indexOf('$');if(split<0)continue;String url=ep.substring(split+1);if(url.startsWith("https://")||url.startsWith("http://")){found.put(url);labels.put(ep.substring(0,split));hls|=url.contains(".m3u8");}}if(found.length()>0){eps=found;names=labels;if(hls)break;}}
        if(eps.length()==0)throw new Exception("没有找到可播放的地址");return new JSONObject().put("episodes",eps).put("episodeNames",names).put("title",v.optString("vod_name")).put("item",item(v,s)).put("videoInfo",new JSONObject().put("title",v.optString("vod_name")).put("sourceName",s.getString("name")).put("cover",v.optString("vod_pic")).put("desc",v.optString("vod_content")));
    }
}
