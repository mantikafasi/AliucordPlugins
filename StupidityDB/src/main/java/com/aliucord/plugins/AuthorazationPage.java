package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aliucord.Logger;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.widgets.LinearLayout;
import com.discord.app.AppFragment;
import com.discord.stores.StoreStream;

public class AuthorazationPage extends AppFragment {
    LinearLayout layout;
    Context context;
    String authURL = "https://discord.com/api/oauth2/authorize?client_id=915703782174752809&redirect_uri=https%3A%2F%2Fmantikralligi1.pythonanywhere.com%2Fauth&response_type=code&scope=identify";
    Logger logger = new Logger("StupidityDBAPI");
    @SuppressLint("SetJavaScriptEnabled") //SHUTUP
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = inflater.getContext();
        layout = new LinearLayout(context);

        Toast.makeText(context, "You need to authorize first to send vote", Toast.LENGTH_SHORT).show();
        //var token = StoreStream.getAuthentication().getAuthToken$app_productionCanaryRelease();
        String token = null;
        try {
            token = (String) ReflectUtils.getField(StoreStream.getAuthentication(), "authToken");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace(); }
        String finalToken = token;

        WebView wv = new WebView(context);
        var wvclient = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.startsWith("https://mantikralligi1.pythonanywhere.com/receiveToken/")) {
                    String token = url.split("https://mantikralligi1.pythonanywhere.com/receiveToken/")[1];
                    StupidityDB.staticSettings.setString("token", token);
                    Toast.makeText(context, "Successfully Authorized", Toast.LENGTH_SHORT).show();
                    getActivity().onBackPressed();
                } else if (url.contains("https://mantikralligi1.pythonanywhere.com/error")) {
                    Toast.makeText(context, "An Error Occured While Authorizing", Toast.LENGTH_SHORT).show();
                    getActivity().onBackPressed();
                } else if (url.contains("https://discord.com/login")) {
                    try{
                        wv.evaluateJavascript("webpackChunkdiscord_app.push([[Math.random()],{},(r)=>{Object.values(r.c).find(m=>m.exports&&m.exports.default&&m.exports.default.login!==void 0).exports.default.loginToken('" + finalToken + "')}]);", value -> { });
                    } catch (Exception e){
                        logger.error(e);
                    }
                }
                super.onPageFinished(view, url);
            }
        };

        wv.setWebViewClient(wvclient);
        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setDomStorageEnabled(true);

        layout.addView(wv);
        wv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wv.loadUrl(authURL);
        return layout;
    }


}
