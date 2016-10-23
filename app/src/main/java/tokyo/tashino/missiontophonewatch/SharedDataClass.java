package tokyo.tashino.missiontophonewatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Created by akihiro on 2015/06/02.
 * このクラスはUIスレッドとは別に動きます。
 * AsyncTaskクラスの仕様に従ってください。
 */
public class SharedDataClass extends AsyncTask<Void, Void, String> {
    //ローカルファイルからコンテンツをLOADするときTRUE
    private static final boolean IS_SOURCE_LOCAL = false;
    //コンテンツサーバの指定
    private static final String SERVER_URL = "http://www.testmissionnotify.com/jp/";
    //チャンネルリストファイルの指定
    private static final String CHANNEL_LIST_XMLFILE = "missionnotify_list.xml";
    //チャンネルリスト
    private ArrayList<Map<String,String>> mMissionChannelList = null;
    //全コンテンツ
    private Map<String,ArrayList<Map<String,String>>> mChannelAll = new LinkedHashMap<String,ArrayList<Map<String,String>> >();
    private Activity mActivity;
    //LOAD終了のFLAG、このクラスは非同期処理なのでLOAD終了をPOLLINGされる。
    private boolean mIsLoadDone;

    SharedDataClass(Activity activity){
        mActivity = activity;
        mIsLoadDone = false;
    }

    //非同期処理の開始
    @Override
    protected String doInBackground(Void... params) {

        if(IS_SOURCE_LOCAL){
            //ローカルファイルからLOADします。
            buildFromClearFile(mActivity);
        }else{
            //インターネットからLOADします。失敗したらローカルからLOADします。
            if(buildFromInternet(mActivity)==false){
                buildFromClearFile(mActivity);
            }
        }
        //ロード終了のフラグを立てます。
        mIsLoadDone = true;
        return null;
    }

    //LOADINGが終わったかどうか
    public boolean getIsLoadDone(){
        return mIsLoadDone;
    }
    //インターネットからコンテンツを作成
    private boolean buildFromInternet(Activity activity){
        //全チャンネルLISTを取得
        mMissionChannelList = loadFromInternet(activity, SERVER_URL, CHANNEL_LIST_XMLFILE);
        if(mMissionChannelList==null){
            return false;
        }
        //全FILEをLOAD
        for(Map<String,String>  chanMap: mMissionChannelList){
            ArrayList<Map<String,String>> tmpChannnelList;

            String channelName = chanMap.get("title");
            String channelListFileName = chanMap.get("description");
            tmpChannnelList = loadFromInternet(activity,SERVER_URL,channelListFileName);
            if(tmpChannnelList==null){
                return false;
            }
            mChannelAll.put(channelName, tmpChannnelList);
        }
        return true;
    }
    //ローカルファイルからコンテンツを作成
    private void buildFromClearFile(Activity activity){
        //全チャンネルLISTを取得
        mMissionChannelList = load(activity, "clear/", CHANNEL_LIST_XMLFILE);
        //全FILEをLOAD
        for(Map<String,String>  chanMap: mMissionChannelList){
            ArrayList<Map<String,String>> tmpChannnelList;

            String channelName = chanMap.get("title");
            String channelListFileName = chanMap.get("description");
            tmpChannnelList = load(activity, "clear/", channelListFileName);
            mChannelAll.put(channelName, tmpChannnelList);
        }
    }

    //チャンネル名のArrayをかえします。
    synchronized public ArrayList<String> getChannelNameArray(){
        ArrayList<String> channnelNameArray = new ArrayList<String>();

        for(String key:mChannelAll.keySet()){
            channnelNameArray.add(key);
        }
        return channnelNameArray;
    }

    //チャンネル名をキーにコンテンツを返します。
    synchronized public ArrayList<Map<String,String>> getChannnelByName(String channnelName){
        return mChannelAll.get(channnelName);
    }

    //ローカルファイルからLOADします。
    private ArrayList<Map<String,String>> load(final Activity ac,String dir,String filename) {
        ArrayList<Map<String,String>> tmpArray = new ArrayList<Map<String,String>>();
        try {
            InputStream in = null;
            in = ac.getAssets().open(dir+filename);
            // XMLパーサを生成して
            XmlPullParserFactory xpf = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xpf.newPullParser();
            // BufferedReaderをセット
            parser.setInput(in,"UTF-8");

            int type = parser.getEventType();
            // イベントタイプがEND_DOCUMENTになるまでループ
            HashMap<String, String> mp = null;
            String title = "";
            String description = "";
            String link = "";
            boolean isItem = false;
            // 文書が終了するまで繰り返す
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    //　itemの場合に処理する
                    if (parser.getName().equals("item")) {
                        mp = new HashMap<String, String>();
                        isItem = true;
                    } else if (parser.getName().equals("title")) {
                        if (isItem) {
                            title = parser.nextText();
                            mp.put("title", title);
                        }
                    } else if (parser.getName().equals("description")) {
                        if (isItem) {
                            description = parser.nextText();
                            mp.put("description", description);
                        }
                    } else if (parser.getName().equals("link")){
                        if (isItem) {
                            link = parser.nextText();
                            mp.put("link", link);
                        }
                    }
                } else if (type == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("item")) {
                        tmpArray.add(mp);
                        isItem = false;
                    }
                }
                type = parser.next();
            }
        } catch (Exception e) {
            ac.finish();
            return null;
        }

        return tmpArray;
    }
    //インターネットからXMLファイルをロードします。
    private ArrayList<Map<String,String>> loadFromInternet(final Activity ac,String url,String filename) {
        ArrayList<Map<String,String>> tmpArray = new ArrayList<Map<String,String>>();

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet();

        try {
            get.setURI(new URI(url+filename));

            HttpResponse res = client.execute(get);
            InputStream in = res.getEntity().getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            // XMLパーサを生成して
            XmlPullParserFactory xpf = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xpf.newPullParser();
            // BufferedReaderをセット
            parser.setInput(br);

            int type = parser.getEventType();
            // イベントタイプがEND_DOCUMENTになるまでループ
            HashMap<String, String> mp = null;
            String title = "";
            String description = "";
            String link = "";
            boolean isItem = false;
            // 文書が終了するまで繰り返す
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    //　itemの場合に処理する
                    if (parser.getName().equals("item")) {
                        mp = new HashMap<String, String>();
                        isItem = true;
                    } else if (parser.getName().equals("title")) {
                        if (isItem) {
                            title = parser.nextText();
                            mp.put("title", title);
                        }
                    } else if (parser.getName().equals("description")) {
                        if (isItem) {
                            description = parser.nextText();
                            mp.put("description", description);
                        }
                    } else if (parser.getName().equals("link")){
                        if (isItem) {
                            link = parser.nextText();
                            mp.put("link", link);
                        }
                    }
                } else if (type == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("item")) {
                        tmpArray.add(mp);
                        isItem = false;
                    }
                }
                type = parser.next();
            }
        } catch (Exception e) {
            return null;
        }

        return tmpArray;
    }
}

