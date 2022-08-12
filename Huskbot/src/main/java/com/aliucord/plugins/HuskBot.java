package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.utils.RxUtils;
import com.discord.models.message.Message;
import com.discord.stores.StoreStream;
import com.discord.utilities.rest.RestAPI;

import java.util.Collections;
import java.util.Random;

@SuppressWarnings("unused")
@AliucordPlugin
public class HuskBot extends Plugin {
    @Override
    public void start(Context context) {
        RxUtils.subscribe(RxUtils.onBackpressureBuffer(StoreStream.getGatewaySocket().getMessageCreate()), RxUtils.createActionSubscriber(message -> {
            if (message == null) return;

            Message entry = new Message(message);

            if (entry.getGuildId() != 811255666990907402L) return;

            if (entry.getAuthor().getId() == 289556910426816513L) {
                Utils.mainThread.postDelayed(() -> {
                    RxUtils.subscribe(RestAPI.getApi().addReaction(entry.getChannelId(),entry.getId(),"husk:859796756111294474"),unused -> null);
                }, (long) (new Random().nextFloat()*4000));
            } else {
                if (new Random().nextInt(10) != 1) return;
                Utils.mainThread.postDelayed(() -> {
                    RxUtils.subscribe(
                            RestAPI.getApi().addReaction(entry.getChannelId(),entry.getId(),"husk:859796756111294474"),
                            unused -> null
                    );
                }, (long) (new Random().nextFloat()*4000));
            }
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
