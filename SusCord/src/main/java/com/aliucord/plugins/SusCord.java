package com.aliucord.plugins;

import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.PreHook;
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListAdapter;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XposedBridge;

@SuppressWarnings("unused")
@AliucordPlugin
public class SusCord extends Plugin {
    @Override
    public void start(Context context) {
        try {
            patcher.patch(ResourcesCompat.class.getDeclaredMethod("getFont", Context.class, int.class, TypedValue.class, int.class, ResourcesCompat.FontCallback.class),
                    new PreHook(methodHookParam -> {
                    }));

            patcher.patch(TextView.class.getDeclaredMethod("setText", CharSequence.class), new PreHook(
                    (cf) -> {
                        try {
                            XposedBridge.invokeOriginalMethod(cf.method, cf.thisObject, new String[]{"Sus"});
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
