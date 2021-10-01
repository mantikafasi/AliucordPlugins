package com.mantikafasi.plugins;

import android.content.Context;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.utils.ReflectUtils;
import com.discord.models.message.Message;

@SuppressWarnings("unused")
@AliucordPlugin
public class POGPatch extends Plugin {
    public static final Logger logger = new Logger("POGPatch");
    @Override
    public void start(Context context) {


        SettingsPage page = new SettingsPage();

        logger.info( "POG" );
        patcher.patch("com.discord.models.message.Message","getContent",null,new PinePatchFn(cf -> {
            Message _this =(Message) cf.thisObject;

            String cont = null;
            try {
                cont = (String) ReflectUtils.getField(_this,"content");
            } catch (Exception e) {}

            if (cont.endsWith(" POG")){
                cf.setResult(cont);
            } else {
                cf.setResult(cont + " POG");
            }
        }));


    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }

}
