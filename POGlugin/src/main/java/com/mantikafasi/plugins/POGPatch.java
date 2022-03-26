package com.mantikafasi.plugins;

import android.content.Context;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.ReflectUtils;
import com.discord.models.message.Message;

import java.util.Random;

@SuppressWarnings("unused")
@AliucordPlugin
public class POGPatch extends Plugin {
    public static final Logger logger = new Logger("POGPatch");

    @Override
    public void start(Context context) {
        Random random = new Random();

        patcher.patch("com.discord.models.message.Message", "getContent", null, new Hook(cf -> {
            Message _this = (Message) cf.thisObject;
            String cont = null;
            if (random.nextInt(3) == 2) {
                try {
                    cont = (String) ReflectUtils.getField(_this, "content");
                } catch (Exception e) { }
                if (cont.endsWith(" POG")) {
                    cf.setResult(cont);
                } else {
                    cf.setResult(cont + " POG");
                }
            }
        }));

    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }

}
