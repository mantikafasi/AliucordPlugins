package com.mantikafasi.plugins;

import android.content.Context;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.discord.stores.StoreGatewayConnection;

@SuppressWarnings("unused")
@AliucordPlugin
public class byebyeSlashCommands extends Plugin {
    int tryCount=0;
    public static final Logger logger = new Logger("byebyeSlashCommands");

    Context context ;
    @Override
    public void start(Context context) {
        /*
        TODO : WRITE CODE OR SOMETHING
         */


        this.context= context;
    }



    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }

}
