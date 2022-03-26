package com.aliucord.plugins;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.InsteadHook;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;

@SuppressWarnings("unused")
@AliucordPlugin
public class MoreConfirmPlus extends Plugin {

    public void start(Context context) throws NoSuchMethodException {

        var dialogshown= new AtomicBoolean(false);
        patcher.patch(View.class.getDeclaredMethod("performClick"),new InsteadHook((cf)->{
            if(!dialogshown.get()){
                dialogshown.set(true);
                var dialog = new AlertDialog.Builder(Utils.getAppActivity());
                dialog.setTitle("Are you sure you wanna click?");
                dialog.setPositiveButton("yes",(dialog1,which) -> {
                    dialogshown.set(false);
                    try {
                        XposedBridge.invokeOriginalMethod(cf.method,cf.thisObject,cf.args);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logger.error("shit",e);
                    }
                    dialog1.dismiss();
                });
                dialog.setNegativeButton("no",(dialog1, which) -> {Toast.makeText(context, "Click Unsuccessful", Toast.LENGTH_SHORT).show();
                dialogshown.set(false);});
                dialog.setOnCancelListener(v -> dialogshown.set(false));
                var view = (View)cf.thisObject;
                dialog.show();
            } else {
                try {
                    XposedBridge.invokeOriginalMethod(cf.method,cf.thisObject,cf.args);
                    dialogshown.set(false);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    logger.error("shit",e);
                }
            }

            return true;
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
