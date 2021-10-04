package com.mantikafasi.plugins;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.adjust.sdk.Reflection;
import com.aliucord.Logger;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.utils.ReflectUtils;

import com.discord.databinding.WidgetChatInputBinding;
import com.discord.models.message.Message;
import com.discord.stores.StoreGatewayConnection;
import com.discord.widgets.chat.input.ChatInputViewModel;
import com.discord.widgets.chat.input.WidgetChatInput;
import com.discord.widgets.chat.input.WidgetChatInput$configureContextBarReplying$3;
import com.discord.widgets.chat.input.WidgetChatInputDiscoveryCommandsModel;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.widgets.chat.list.actions.WidgetChatListActions$configureUI$14;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage$onConfigure$4;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@AliucordPlugin
public class byebyeSlashCommands extends Plugin {
    public static final Logger logger = new Logger("byebyeSlashCommands");



    WidgetChatListAdapterItemMessage currentView = null;
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
