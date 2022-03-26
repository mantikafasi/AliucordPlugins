package com.aliucord.plugins;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.utils.RxUtils;
import com.discord.models.message.Message;
import com.discord.stores.StoreStream;

@SuppressWarnings("unused")
@AliucordPlugin
public class RickRoll extends Plugin {
    @Override
    public void start(Context context) {
        RxUtils.subscribe(RxUtils.onBackpressureBuffer(StoreStream.getGatewaySocket().getMessageCreate()), RxUtils.createActionSubscriber(message -> {
            if (message == null) return;

            Message entry = new Message(message);
            String cont = entry.getContent().toLowerCase();
            if ((cont.contains("rick") && cont.contains("roll")) || entry.getContent().contains("dQw4w9WgXcQ"))
                Utils.appActivity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")));
        }));

    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
