package com.aliucord.plugins;

import static com.aliucord.plugins.InvChatAPI.logger;

import android.content.Context;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.aliucord.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class webviewThings {
    public static void createWebView(InputStream input, ViewGroup layout){
        Context ctx = Utils.getAppContext();
        WebView webView = new WebView(ctx);

        var webViewClient = new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                logger.info(url);
                webView.evaluateJavascript("try{Android.output(StegCloak);}catch(err){Android.output(err);}", value -> {
                    Toast.makeText(ctx, value, Toast.LENGTH_SHORT).show();
                });
              //  webView.loadUrl("javascript:try{Android.output(StegCloak);}catch(err){Android.output(err);}");

            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                logger.info(error.toString());
            }
        };

        webView.setWebViewClient(webViewClient);

        webView.getSettings().setJavaScriptEnabled(true);
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Webpack App</title><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head><body><script>{script}</script></body></html>";
        //webView.loadData(new InputStream(), "text/html", "UTF-8");
//        Utils.appActivity.findViewById(com.lytefast.flexinput.R.e.chat_list_adapter_item_text_name)
        webView.setLayoutParams(layout.getChildAt(3).getLayoutParams());
        layout.addView(webView);
        webView.addJavascriptInterface(new dumbShit(),"Android");

        var javascrips = "";

        try   {
            int size;
            size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();
            javascrips = new String(buffer);
        } catch (IOException e) { logger.error(e);}
        logger.info(javascrips);
        webView.loadData(html.replace("{script}",javascrips), "text/html", "UTF-8");
        // webView.loadUrl("javascript:" + javascrips);


        webView.evaluateJavascript(javascrips, null);

    }
}
