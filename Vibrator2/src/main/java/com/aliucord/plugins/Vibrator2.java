package com.aliucord.plugins;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;

import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class Vibrator2 extends Plugin {
    Thread thread;
    @Override
    public void start(Context context) {
        settingsTab = new SettingsTab(SettingsShit.class);
        Vibrator.settings = settings;
        Vibrator.patternlist = settings.getObject("patternList", new ArrayList<>(), TypeToken.getParameterized(ArrayList.class,Pattern.class).getType());
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
