package com.aliucord.plugins;

import static java.util.Collections.emptyList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.utils.RxUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.models.domain.NonceGenerator;
import com.discord.models.message.Message;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.StoreStream;
import com.discord.utilities.rest.RestAPI;
import com.discord.utilities.time.ClockFactory;

import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class TestPlugin extends Plugin {
    @Override
    public void start(Context context) {
        /*
        RxUtils.subscribe(RxUtils.onBackpressureBuffer(StoreStream.getGatewaySocket().getMessageCreate()), RxUtils.createActionSubscriber(message -> {
            if (message == null) return;

            Message entry = new Message(message);
            String cont = entry.getContent().toLowerCase();
            //
            if (cont.contains("i love alicord") ) {
                RxUtils.subscribe(RestAPI.getApi().createOrFetchDM(entry.getAuthor().i()),channel -> {
                    RxUtils.subscribe(RestAPI.getApi().sendMessage(ChannelWrapper.getId(channel),createMessage("I love alicord too <@"+entry.getAuthor().i()+">")),message1 -> null);
                    return null;
                });
            }
        }));

         */
    }
    public RestAPIParams.Message createMessage(String message){
        return new RestAPIParams.Message(
                message, // Content
                String.valueOf(NonceGenerator.computeNonce(ClockFactory.get())), // Nonce
                null, // ApplicationId
                null, // Activity
                emptyList(), // stickerIds
                null, // messageReference
                new RestAPIParams.Message.AllowedMentions( // https://discord.com/developers/docs/resources/channel#allowed-mentions-object-allowed-mentions-structure
                        emptyList(), // parse
                        emptyList(), //users
                        emptyList(), // roles
                        false // repliedUser
                ),null
        );

    }
    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
