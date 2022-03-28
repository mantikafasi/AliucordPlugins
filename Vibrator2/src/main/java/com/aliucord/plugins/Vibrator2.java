package com.aliucord.plugins;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;

import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;

import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class Vibrator2 extends Plugin {
    Thread thread;
    @Override
    public void start(Context context) {
        settingsTab = new SettingsTab(SettingsShit.class).withArgs(settings);

        thread = new Thread(() -> {
            android.os.Vibrator v = (android.os.Vibrator) Utils.appActivity.getSystemService(Context.VIBRATOR_SERVICE);
            while (true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(10000, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(10000);
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });
        //thread.start();

    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
