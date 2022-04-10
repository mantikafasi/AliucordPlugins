package com.aliucord.plugins;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.aliucord.Constants;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.PreHook;
import com.aliucord.utils.ReflectUtils;
import com.discord.api.message.Message;
import com.discord.models.domain.ModelUserSettings;
import com.discord.stores.StoreStream;
import com.discord.widgets.chat.MessageContent;
import com.discord.widgets.chat.MessageManager;
import com.discord.widgets.chat.input.ChatInputViewModel;
import com.discord.widgets.settings.WidgetSettingsAppearance;
import com.discord.widgets.settings.WidgetSettingsAppearance$updateTheme$1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import kotlin.jvm.functions.Function1;

@SuppressWarnings("unused")
@AliucordPlugin
public class GoodPerson extends Plugin {

    List<String> badVerbs  = Arrays.asList("fuck"," cum","kill","destroy");
    List<String> badNouns = Arrays.asList("shit","bullshit","ass","bitch","nigga","hell","whore","dick","piss","pussy","slut",
            "tit"," fag","cum","cock","retard","blowjob","bastard","men","man","kotlin","die","sex","nigger");

    List<String> badVerbReplacements  = Arrays.asList("love","eat","deconstruct","marry","fart","teach","display","plug",
            "explode","undress","finish","freeze","beat","free","brush","allocate","date","melt","breed","educate",
            "injure","change");

    List<String> badNounReplacements = Arrays.asList("pasta","kebab","cake","potato","woman","computer","java",
            "hamburger","monster truck","osu!","Ukrainian ball in search of gas game","Anime","Anime girl",
            "good","keyboard","NVIDIA RTX 3090 Graphics Card","storm","queen","single","umbrella","mosque","physics",
            "bath","virus","bathroom","mom","owner","airport");

    public boolean isBadNoun(String noun) {
        noun = noun.toLowerCase();
        //returns boolean and replacement
        for (var badNoun: badNouns) {
            if (noun.startsWith(badNoun)) return true;
        }
        return false;
    }

    public boolean isBadVerb(String verb) {
        verb = verb.toLowerCase();
        //returns boolean and word
        for (var badVerb: badVerbs) {
            if (verb.length() > 1 &&verb.contains(badVerb)) return true;
        }
        return false;
    }

    public String getRandomVerb(){
        return badVerbReplacements.get(new Random().nextInt(badVerbReplacements.size() - 1));
    }

    public String getRandomNoun(){
        return badNounReplacements.get(new Random().nextInt(badNounReplacements.size() - 1));
    }

    public String filterWord(String word) {
        if (word.contains("http")) return null;
        if (isBadVerb(word) && isBadNoun(word)) {
            if (new Random().nextBoolean()) {
                return getRandomNoun();
            } else {
                return  getRandomVerb();
            }
        } else if (isBadVerb(word)) {
            return getRandomVerb();
        } else if (isBadNoun(word)) {
            return getRandomNoun();
        }

        return null;
    }

    @Override
    public void start(Context context) throws NoSuchMethodException {
        patcher.patch(ChatInputViewModel.class.getDeclaredMethod("sendMessage", Context.class, MessageManager.class, MessageContent.class, List.class, boolean.class, Function1.class),
                new PreHook(cf -> {
                    var thisobj = (ChatInputViewModel) cf.thisObject;
                    var content = (MessageContent) cf.args[2];
                    try {
                        var mes = content.component1().trim() +" ";

                        for (var word : mes.split(" ")) {
                            String filteredWord = filterWord(word);
                            if (filteredWord != null) mes = mes.replaceFirst(word,filteredWord);
                        }

                        ReflectUtils.setField(content, "textContent", mes);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
