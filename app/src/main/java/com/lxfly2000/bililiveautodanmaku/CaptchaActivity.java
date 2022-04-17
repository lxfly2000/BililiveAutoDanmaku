package com.lxfly2000.bililiveautodanmaku;

import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.lxfly2000.utilities.StreamUtility;

import java.io.IOException;

public class CaptchaActivity extends AppCompatActivity {
    WebView webView;

    class CaptchaFinish{
        @JavascriptInterface
        public void finish(String s){
            Toast.makeText(CaptchaActivity.this,s,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captcha);
        webView=findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new CaptchaFinish(),"captchaFinish");
        try{
            webView.loadData(StreamUtility.GetStringFromStream(getResources().openRawResource(R.raw.captcha)),"text/html","utf8");
        }catch (IOException e){
            Toast.makeText(this,e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
        }
        Intent intent=new Intent();
        intent.putExtra("Cookies","");
        setResult(1,intent);
    }
}