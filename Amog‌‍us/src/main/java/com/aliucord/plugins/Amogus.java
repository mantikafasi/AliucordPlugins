package com.aliucord.plugins;

import android.content.Context;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.utils.RxUtils;
import com.discord.models.gifpicker.dto.ModelGif;
import com.discord.stores.StoreStream;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("unused")
@AliucordPlugin
public class Amogus extends Plugin {
    List<ModelGif> modelGifList;

    @Override
    public void start(Context context) {
        var gifpicker = StoreStream.Companion.getGifPicker();

        commands.registerCommand("amogus", "Sends random amogus gif", commandContext -> {
            if (modelGifList == null) {
                CountDownLatch alhamdulillah = new CountDownLatch(1);
                RxUtils.subscribe(gifpicker.observeGifsForSearchQuery("amogus"), modelGifs -> {
                    modelGifList = modelGifs;
                    alhamdulillah.countDown();
                    return null;
                });
                try {
                    alhamdulillah.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Random random = new Random();
            return new CommandsAPI.CommandResult(modelGifList.get(random.nextInt(modelGifList.size())).component1());
        });
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
