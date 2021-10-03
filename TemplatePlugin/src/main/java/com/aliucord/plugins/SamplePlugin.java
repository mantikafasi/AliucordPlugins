package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;

import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class SamplePlugin extends Plugin {



    @Override
    public void start(Context context) {
    
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
