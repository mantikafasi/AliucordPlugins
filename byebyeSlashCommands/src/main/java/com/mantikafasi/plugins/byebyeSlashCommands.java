package com.mantikafasi.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.adjust.sdk.Reflection;
import com.aliucord.Logger;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.utils.ReflectUtils;
import com.discord.stores.StoreGatewayConnection;
import com.discord.widgets.chat.input.WidgetChatInputDiscoveryCommandsModel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@AliucordPlugin
public class byebyeSlashCommands extends Plugin {
    int tryCount=0;
    public static final Logger logger = new Logger("byebyeSlashCommands");


    Context context ;
    @Override
    public void start(Context context) {
        this.context= context;

        for (Method m: WidgetChatInputDiscoveryCommandsModel.class.getDeclaredMethods()) {
            patcher.patch("com.discord.widgets.chat.input.WidgetChatInputDiscoveryCommandsModel",m.getName(), m.getParameterTypes() ,new PinePatchFn(callFrame -> {
                callFrame.setResult(null);}));
        }
    }





    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }

}
