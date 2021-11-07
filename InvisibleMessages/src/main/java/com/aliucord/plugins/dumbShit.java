package com.aliucord.plugins;

import android.webkit.JavascriptInterface;

public class dumbShit {
    public static Callback callback;

    @JavascriptInterface
    public void output(String res){
        if (callback!=null) callback.onOutput(res);
    }

    public interface Callback {
        void onOutput(String res);
    }
}

