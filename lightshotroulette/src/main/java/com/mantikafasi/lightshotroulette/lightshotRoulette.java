package com.mantikafasi.lightshotroulette;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.CommandContext;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.app.AppFragment;
import com.discord.models.commands.ApplicationCommandOption;
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
    public void start(Context context) throws Throwable {
/*

        List<ApplicationCommandOption> options =
                Arrays.asList(new ApplicationCommandOption(ApplicationCommandType.INTEGER,
                        "number","number of generated screenshots",null,true,true,null,null));
        commands.registerCommand(
                "lightlette", "Try your luck to see if you can find something interesting",
                 options,
    
    commandContext -> new CommandsAPI.CommandResult(generateLinks(commandContext.getIntOrDefault("number",1)), null, true));
   */
   }
    
    public String generateLinks(int count) {
        String val = "";
        String str = "abcdefghijklmnoprstuxwyz0123456789";
        Random random = new Random();
        int charCount = random.nextInt(3) + 5;
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
