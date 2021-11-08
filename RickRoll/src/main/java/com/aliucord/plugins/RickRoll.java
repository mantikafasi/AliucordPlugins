package com.aliucord.plugins;

import static com.aliucord.Main.logger;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.RxUtils;
import com.discord.models.message.Message;
import com.discord.models.user.CoreUser;
import com.discord.models.user.MeUser;
import com.discord.stores.StoreStream;
import com.discord.stores.StoreStream$initGatewaySocketListeners$31;
import com.discord.stores.StoreStream$initGatewaySocketListeners$32;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.entries.ChatListEntry;

import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class RickRoll extends Plugin {



    @Override
    public void start(Context context) {

        RxUtils.subscribe(RxUtils.onBackpressureBuffer(StoreStream.getGatewaySocket().getMessageCreate()), RxUtils.createActionSubscriber(message -> {
         	if (message == null) return;

        	Message entry = new Message(message);
        	String cont = entry.getContent().toLowerCase();
            if ((cont.contains("rick")&& cont.contains("roll"))||entry.getContent().contains("dQw4w9WgXcQ"))  Utils.appActivity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")));
        }));

    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
