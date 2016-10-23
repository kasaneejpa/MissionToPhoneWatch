package tokyo.tashino.missiontophonewatch;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class WebActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        Intent intent = getIntent();
        String url = intent.getStringExtra("URL");

        WebView webView = (WebView)findViewById(R.id.webFullView);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                view.loadUrl("file:///android_asset/error.html");
            }
        });
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        try {
            webView.loadUrl(url);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        WebView webView = (WebView)findViewById(R.id.webFullView);
        webView.loadUrl("file:///android_asset/stop.html");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_web, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            new AlertDialog.Builder(this).setTitle("このアプリケーションについて").
                    setPositiveButton("閉じる",null).
                    setMessage("MissionNotifyは究極のめんどくさがり向のアプリケーションです。一度チャンネルを選んだら検索もお任せ、ミッションが降ってくる。気になったら大画面で詳しく見てまた受け身に戻れます。またコンテンツは政治的、宗教的、暴力的、反社会的、性的、誹謗嘲笑、個人情報、その他不適切な内容を含まないようにしています。コンテンツは将来的に安価ですが審査制で受け付けようと思っています。").
                    create().show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
