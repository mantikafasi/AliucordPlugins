package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.discord.models.requiredaction.RequiredAction;
import com.discord.widgets.user.account.WidgetUserAccountVerify;

import java.util.Collections;

import de.robv.android.xposed.XC_MethodReplacement;

@SuppressWarnings("unused")
@AliucordPlugin
public class DisableVerifyScreen extends Plugin {



    @Override
    public void start(Context context) throws NoSuchMethodException {

        patcher.patch(WidgetUserAccountVerify.Companion.class
                .getDeclaredMethod("launch", Context.class, RequiredAction.class), XC_MethodReplacement.DO_NOTHING);
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
