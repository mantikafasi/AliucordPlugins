package com.mantikafasi.plugins;
import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.ColorInt;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.plugins.R;
import com.aliucord.utils.ReflectUtils;
import com.discord.stores.StoreUserTyping;
import com.discord.widgets.chat.input.MessageDraftsRepo;
import com.discord.widgets.chat.input.WidgetChatInputEditText;
import com.lytefast.flexinput.widget.FlexEditText;

import java.lang.reflect.InvocationTargetException;

import kotlin.jvm.internal.DefaultConstructorMarker;
import top.canyie.pine.callback.MethodReplacement;

@SuppressWarnings("unused")
@AliucordPlugin
public class BetterSilentTyping extends Plugin {

    @Override
    public void start(Context context) {

      /*
      TODO write something
       */
        int keyboard = Utils.getResId("keyboardxml","drawable");
        int keyboardDisabled = Utils.getResId("keyboarddisabled","drawable");



        try {
            patcher.patch(WidgetChatInputEditText.class.getConstructor(new Class[]{FlexEditText.class , MessageDraftsRepo.class}),new PinePatchFn(callFrame -> {

                try {
                    callFrame.invokeOriginalMethod();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                WidgetChatInputEditText thisobj = ((WidgetChatInputEditText)callFrame.thisObject);
                try {
                    FlexEditText et = (FlexEditText) ReflectUtils.getField(thisobj,"editText");
                    ViewGroup group = (ViewGroup) et.getParent();
                    Button  button = new Button(group.getContext());
                    //int id  = resources.getIdentifier("keyboard","drawable","com.mantikafasi.plugins");
                    //new Logger("dakofoakdf").info(keyboard + " " + R.drawable.keyboard);
                    //button.setBackgroundResource(keyboard);
                    button.setBackgroundColor(100);
                    button.setText("Disabled");
                    button.setTextColor(Color.parseColor("#FFFFFF"));

                    button.setOnClickListener(v -> {

                        String status = "";

                        settings.setBool("isEnabled",!settings.getBool("isEnabled",false));
                        if (settings.getBool("isEnabled",false)){
                            status = "Enabled";
                        } else {status ="Disabled";}

                        patchUserTyping();


                        ((Button)v).setText(status);
                    });

                    new Logger("afddasd").info(String.valueOf(keyboard));

                    group.addView(button);




                    new Logger("koasdasd").info( et.getParent().toString());
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace(); }
            }));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    Runnable patch = null;
    public void patchUserTyping(){
        if (settings.getBool("isEnabled", false)) {
            try {
                patch = patcher.patch(StoreUserTyping.class.getDeclaredMethod("setUserTyping", long.class), MethodReplacement.DO_NOTHING);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        else
            {
                if (patch!=null){
                    patch.run();
                }
            }
        }



    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
