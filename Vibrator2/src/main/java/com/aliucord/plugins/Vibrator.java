package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class Vibrator {
    public static android.os.Vibrator vibrator = (android.os.Vibrator) Utils.appActivity.getSystemService(Context.VIBRATOR_SERVICE);
    public static SettingsAPI settings;
    @SuppressLint("MissingPermission")
    public static void vibrate(long[] pattern,boolean r){
        int repeat;
        if (r) repeat = 0; else repeat = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern,repeat));
        } else {
            vibrator.vibrate(pattern,repeat);
        }
    }

    public static void savePattern(Pattern pattern){
        var patterns = (List<Pattern>)settings.getObject("patternList",new ArrayList<Pattern>(), TypeToken.getParameterized(ArrayList.class,Pattern.class).getType());
        for (var a:patterns) {
            if(a.ID == pattern.ID) {
                patterns.remove(a);
                patterns.add(pattern);
                break;
            }
        }
        settings.setObject("patternList",patterns);
    }
    @SuppressLint("MissingPermission")
    public static void stop(){
        vibrator.cancel();
    }

}
