package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;

@SuppressWarnings("unused")
@AliucordPlugin
public class Vibrator extends Plugin {
    Thread thread;

    @SuppressLint("MissingPermission")
    @Override
    public void start(Context context) {
        thread = new Thread(() -> {
            android.os.Vibrator v = (android.os.Vibrator) Utils.appActivity.getSystemService(Context.VIBRATOR_SERVICE);
            while (true){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(10000, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(1000);
                }
            }

        });
        thread.start();
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
        thread.interrupt();
    }
}
