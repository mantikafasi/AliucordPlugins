package com.aliucord.plugins;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.PreHook;
import com.aliucord.utils.ReflectUtils;
import com.discord.models.message.Message;
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListAdapter;
import com.discord.widgets.chat.list.entries.MessageEntry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Ref;
import java.util.Collections;

import de.robv.android.xposed.XposedBridge;

@SuppressWarnings("unused")
@AliucordPlugin
public class SusCord extends Plugin {



    @Override
    public void start(Context context) {
        try {
            patcher.patch(TextView.class.getDeclaredMethod("setText", CharSequence.class),new PreHook(
                    (cf)-> {
                        try {
                            XposedBridge.invokeOriginalMethod(cf.method,cf.thisObject, new String[]{"Sus"});
                            cf.setResult(null);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
            ));

            patcher.patch("com.discord.models.message.Message","getContent",null,new Hook(cf -> {

                    cf.setResult("Sus");


            }));
            patcher.patch(ChannelMembersListAdapter.Item.Member.class.getDeclaredMethod("getName"),
                    new PreHook((cf)->{cf.setResult("Sus");}));/*
            for (Constructor<?> constructor : MessageEntry.class.getConstructors()) {
                patcher.patch(constructor,new Hook((cf)->{

                    try {
                        ReflectUtils.setField(cf.thisObject,"message","Sus");
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }));
            }
            */


        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
