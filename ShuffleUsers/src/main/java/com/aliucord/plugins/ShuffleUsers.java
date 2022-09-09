package com.aliucord.plugins;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.utils.RxUtils;
import com.discord.api.premium.PremiumTier;
import com.discord.api.user.User;
import com.discord.models.message.Message;
import com.discord.models.user.CoreUser;
import com.discord.stores.StoreStream;
import com.discord.stores.StoreUser;
import com.discord.utilities.user.UserUtils;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.MessageEntry;
import com.discord.widgets.chat.list.sheet.WidgetApplicationCommandBottomSheetViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

@SuppressWarnings("unused")
@AliucordPlugin
public class ShuffleUsers extends Plugin {

    @Override
    public void start(Context context) throws NoSuchMethodException {

        patcher.patch(Message.class.getDeclaredMethod("getAuthor"),new InsteadHook(cf -> {
            var users  = new ArrayList<>( StoreStream.getUsers().getUsers().values() );
            if (users.size() == 0) return Utils.buildClyde("Joseph",null);
            return UserUtils.INSTANCE.synthesizeApiUser(users.get(new Random().nextInt(users.size() - 1)));
        }));

        patcher.patch(MessageEntry.class.getDeclaredMethod("getNickOrUsernames"),new InsteadHook(cf -> {
            var thisObj = (MessageEntry) cf.thisObject;

            var usernameList = new HashMap<Long,String>();
            usernameList.put(thisObj.getAuthor().getUserId(),"trolley");
            return usernameList;

        }));
    patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class), cf -> {
        var thisObj = (WidgetChatListAdapterItemMessage)cf.thisObject;
        var chatListEntry = (MessageEntry)cf.args[1];

        try {
            var itemname = (TextView)ReflectUtils.getField(thisObj,"itemName");
            var message = chatListEntry.getMessage();
            itemname.setText(message.getAuthor().getUsername());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    });
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
