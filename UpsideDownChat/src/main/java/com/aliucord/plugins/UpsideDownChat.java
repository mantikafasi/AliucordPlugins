package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.utils.ReflectUtils;
import com.discord.widgets.chat.list.WidgetChatList;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter;
import com.discord.widgets.chat.list.entries.ChatListEntry;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@AliucordPlugin
public class UpsideDownChat extends Plugin {



    @Override
    public void start(Context context) throws NoSuchMethodException {
        patcher.patch(WidgetChatListAdapter.class.getDeclaredMethod("setData", WidgetChatListAdapter.Data.class),new InsteadHook(cf->{
            var arg0 = (WidgetChatListAdapter.Data)cf.args[0];
            var list = arg0.getList();
            Collections.reverse(list);
            try {
                ReflectUtils.setField(cf.thisObject,"data",arg0);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            var thisObj = (WidgetChatListAdapter)cf.thisObject;
            thisObj.setData(list);
            return null;
        }));
    
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
