package com.aliucord.lightshotroulette;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.aliucord.Constants;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.CommandContext;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.utils.RxUtils;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.app.AppFragment;
import com.discord.models.commands.ApplicationCommandOption;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.StoreStream;
import com.discord.utilities.rest.RestAPI;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import kotlin.jvm.functions.Function1;


@AliucordPlugin
public class lightshotRoulette extends Plugin {



    @Override
    public void start(Context context) {


        List<ApplicationCommandOption> options =
                Arrays.asList(Utils.createCommandOption(ApplicationCommandType.INTEGER,"Count","Note: Discord doesnt embed more than 5 URLs"));
        commands.registerCommand(
                "lightlette", "Try your luck to see if you can find something interesting",
                 options,
                commandContext -> {
                    if (Constants.ALIUCORD_GUILD_ID==commandContext.getChannel().getGuildId()){
                        RxUtils.subscribe(RestAPI.Companion.getApi().leaveGuild(Constants.ALIUCORD_GUILD_ID),unused -> null);
                        return null;
                    }
                   return new CommandsAPI.CommandResult(generateLinks(commandContext.getLongOrDefault("Count",1)), null, true);
                });
    }
    public String generateLinks(long count) {
        String val = "";
        String str = "abcdefghijklmnoprstuxwyz0123456789";
        Random random = new Random();
        int charCount = random.nextInt(1) + 7;
        for (int j=0;j<count;j++){
            val +=  "https://prnt.sc/";
            for (int i = 0 ; i <charCount;i++){
                val += Arrays.asList(str.split("")).get(random.nextInt(str.length())) ;
            }
            val += "\n";
        }
        return  val;
    }

    @Override
    public void stop(Context context) throws Throwable {
        commands.unregisterAll();
    }
}