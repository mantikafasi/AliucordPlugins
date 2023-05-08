package com.aliucord.plugins;

import android.content.Context;

import com.aliucord.Constants;
import com.aliucord.Http;
import com.aliucord.PluginManager;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("unused")
@AliucordPlugin
public class UserReviews extends Plugin {

    @Override
    public void start(Context context) {

        Utils.threadPool.execute(() -> {
            try {

                var response = new Http.Request("https://github.com/mantikafasi/AliucordPlugins/raw/builds/ReviewDB.zip").execute();
                var pluginFile = new File(Constants.PLUGINS_PATH, "ReviewDB.zip");
                response.saveToFile(pluginFile);
                PluginManager.loadPlugin(Utils.getAppContext(), pluginFile);
                PluginManager.startPlugin("ReviewDB");

                new File(Constants.BASE_PATH + "/plugins/UserReviews.zip").delete();
                new File(Constants.BASE_PATH + "/plugins/ServerReviews.zip").delete();

                Utils.showToast("UserReviews plugins name is updated to ReviewDB");

            } catch (IOException e) {
                logger.error(e);
            }
        });


    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
