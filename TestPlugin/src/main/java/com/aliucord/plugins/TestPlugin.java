package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.Constants;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.ReflectUtils;
import com.discord.api.message.Message;
import com.discord.models.domain.ModelUserSettings;
import com.discord.stores.StoreStream;
import com.discord.widgets.settings.WidgetSettingsAppearance;
import com.discord.widgets.settings.WidgetSettingsAppearance$updateTheme$1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@AliucordPlugin
public class TestPlugin extends Plugin {

    Pattern regex = Pattern.compile("https:\\/\\/raw\\.githubusercontent\\.com[\\w.\\/-].*(json)\n");
    HashSet<String> githubURLs = new HashSet<>();
    @Override
    public void start(Context context){

        //SELFBOT PLUGINNNNNNN
        /*
        for (var cons: com.discord.models.message.Message.class.getConstructors()) {
            patcher.patch(cons,new Hook(cf -> {
                try {
                    var content =(String) ReflectUtils.getField(cf.thisObject,"content");
                    var matcher = regex.matcher(content);
                    if(matcher.find()) githubURLs.add(matcher.group());


                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    logger.error(
                            e
                    );
                }

            }));
        }
        commands.registerCommand("guh","guh",commandContext -> {

            try {
                new File(Constants.BASE_PATH +"/guh.json").createNewFile();

                var writer = new FileWriter(Constants.BASE_PATH +"/guh.json");
                writer.write(githubURLs.toString());
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new CommandsAPI.CommandResult("guh");
        });

         */


    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
