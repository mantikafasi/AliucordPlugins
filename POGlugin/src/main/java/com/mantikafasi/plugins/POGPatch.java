package com.mantikafasi.plugins;

import android.content.Context;
import android.widget.TextView;

import com.aliucord.CollectionUtils;
import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.NotificationsAPI;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.MessageEmbedBuilder;
import com.aliucord.entities.NotificationData;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.patcher.PinePatchFn;
import com.mantikafasi.plugins.ClassUtils;
import com.aliucord.wrappers.embeds.MessageEmbedWrapper;
import com.discord.models.message.Message;
import com.discord.rtcconnection.audio.DiscordAudioManager;
import com.discord.stores.ArchivedThreadsStore;
import com.discord.utilities.textprocessing.node.UserMentionNode;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.MessageEntry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
@AliucordPlugin
public class POGPatch extends Plugin {
    public static final Logger logger = new Logger("POGPatch");
    @Override
    public void start(Context context) {


        SettingsPage page = new SettingsPage();

        logger.info( "POG" );
        patcher.patch("com.discord.models.message.Message","getContent",null,new PinePatchFn(cf -> {
            Message _this =(Message) cf.thisObject;
            ClassUtils utils = new ClassUtils(_this);
            
            String cont = (String) utils.getPrivateField("content");

            if (cont.endsWith(" POG")){
                cf.setResult(cont);
            } else {
                cf.setResult(cont + " POG");
            }
        }));


    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }

}
