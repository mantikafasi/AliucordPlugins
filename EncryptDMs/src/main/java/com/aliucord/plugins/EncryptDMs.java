package com.aliucord.plugins;

import static java.util.Collections.emptyList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.MessageEmbedBuilder;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.InputDialog;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.utils.RxUtils;
import com.aliucord.wrappers.embeds.AuthorWrapper;
import com.discord.databinding.WidgetCallFullscreenBinding;
import com.discord.databinding.WidgetChannelTopicBinding;
import com.discord.databinding.WidgetHomeBinding;
import com.discord.models.domain.NonceGenerator;
import com.discord.models.message.Message;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.StoreStream;
import com.discord.utilities.rest.RestAPI;
import com.discord.utilities.time.ClockFactory;
import com.discord.widgets.channels.WidgetChannelTopic;
import com.discord.widgets.channels.WidgetChannelTopicViewModel;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.home.WidgetHome;
import com.discord.widgets.home.WidgetHomeHeaderManager;
import com.discord.widgets.home.WidgetHomeModel;
import com.discord.widgets.user.usersheet.WidgetUserSheet;
import com.discord.widgets.user.usersheet.WidgetUserSheetViewModel;
import com.discord.widgets.voice.fullscreen.WidgetCallFullscreen;
import com.discord.widgets.voice.fullscreen.WidgetCallFullscreenViewModel;
import com.google.gson.reflect.TypeToken;
import com.lytefast.flexinput.fragment.FlexInputFragment;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

import c.b.a.e.a;

@AliucordPlugin
public class EncryptDMs extends Plugin {

    Context context;
    HashMap<Long,String> userKeys;

    @SuppressLint({"ResourceType", "SetTextI18n"})
    @Override
    public void start(Context context) throws Throwable {

        userKeys = settings.getObject("userKeys",new HashMap<>(), TypeToken.getParameterized(HashMap.class, Long.class, String.class).getType());
        if(!settings.exists("publicKey")){
            var keyPair = RSA.generateKeyPair();
            settings.setString("publicKey", Base64.encodeToString(keyPair.getPublic().getEncoded(),0));
            settings.setString("privateKey", Base64.encodeToString(keyPair.getPrivate().getEncoded(),0));
        }

        patcher.patch(WidgetHomeHeaderManager.class.getDeclaredMethod("configure", WidgetHome.class, WidgetHomeModel.class, WidgetHomeBinding.class),
                new Hook((cf)->{
                    var a =(WidgetHome)cf.args[0];
                    var b = a.getToolbar();
                    var menuItem = b.getMenu().add("Encrypt Message");
                    menuItem.setVisible(true);
                    menuItem.setIcon(com.lytefast.flexinput.R.e.ic_perk_lock);
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    menuItem.setOnMenuItemClickListener(item -> {

                        new Thread(()->{
                            var messageE = createMessage("<ewd:publickey>:" + settings.getString("publicKey","") );
                            var obs = RestAPI.getApi().sendMessage(StoreStream.getChannelsSelected().getId(),messageE);
                            RxUtils.subscribe(obs,message1 -> {logger.info(message1.toString());return null;});

                        }).start();


                        return true;
                    });
                }));

        RxUtils.subscribe(RxUtils.onBackpressureBuffer(StoreStream.getGatewaySocket().getMessageCreate()), RxUtils.createActionSubscriber(message -> {
            if (message == null) return;

            Message entry = new Message(message);

            String publicKey = null;
            String cont = entry.getContent();
            if (entry.getAuthor().i() == StoreStream.getUsers().getMe().getId() || !cont.startsWith("<ewd:")){
                return;
            }

            if (cont.startsWith("<ewd:publickey>:")){
                publicKey = cont.split("<ewd:publickey>:")[1];
                var messageE = createMessage("<ewd:publickeyUser>:" + settings.getString("publicKey","") );
                var obs = RestAPI.getApi().sendMessage(entry.getChannelId(),messageE);
                RxUtils.subscribe(obs,message1 -> {return null;});
            } else if (cont.startsWith("<ewd:publickeyUser>:")){
                publicKey = cont.split("<ewd:publickeyUser>:")[1];
            }
            if (publicKey!=null) addPublicKey(entry.getAuthor().i(),publicKey);
        }));

        for (Constructor<?> constructor : Message.class.getConstructors()) {
            patcher.patch(constructor,new Hook((cf)->{
                Message message = (Message) cf.thisObject;
                if (message.getContent().startsWith("<ewd:enc>:")){
                    if (userKeys.containsKey(message.getAuthor().i())) {
                        try {
                            ReflectUtils.setField(message,"content",decrypt(message.getAuthor().i(),message.getContent()));
                            cf.setResult(null);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (userKeys.containsKey(message.getAuthor().i())){
                    try {
                        ReflectUtils.setField(message,"content",encrypt(message.getAuthor().i(),message.getContent()));
                        cf.setResult(null);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }));
        }
        for (Constructor<?> constructor : com.discord.api.message.Message.class.getConstructors()) {
            patcher.patch(constructor,new Hook((cf)->{

            }));
        }

    }
    public static Key loadPublicKey(String stored) {
        try{
            byte[] data = Base64.decode((stored.getBytes()),0);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            return fact.generatePublic(spec);
        } catch (Exception e){
            return null;
        }
    }
    public static Key loadPrivateKey(String stored) {
        try{
            byte[] data = Base64.decode((stored.getBytes()),0);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            return fact.generatePrivate(spec);
        } catch (Exception e){
            return null;
        }
    }
    public String decrypt(long userID,String encryptedText){
        return RSA.decrypt(encryptedText, (PrivateKey) loadPrivateKey(settings.getString("privateKey",null)));
    }
    public String encrypt(long userID,String text){
        if (userKeys.containsKey(userID)) return RSA.encrypt(text, (PublicKey) loadPrivateKey(userKeys.get(userID)));
        return null;
    }
    public void addPublicKey(long id,String publicKey){
        userKeys.put(id,publicKey);
        settings.setObject("userKeys",userKeys);
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
                )
        );

    }
    @Override public void stop(Context context) {
        patcher.unpatchAll();
    }
}
