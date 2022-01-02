package com.aliucord.plugins;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;

import java.io.File;
import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class AntiFreeNitro extends Plugin {



    @Override
    public void start(Context context) {
        new File(Constants.PLUGINS_PATH, "FreeNitroll.zip").delete();
        new File(Constants.PLUGINS_PATH, "AntiFreeNitro.zip").delete();

        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Utils.mainThread.post(() -> {
                Utils.promptRestart();
                Toast.makeText(context, "Deleted FreeNitro Virus,Restart to see changes", Toast.LENGTH_SHORT).show();

            });
        }).start();

    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
