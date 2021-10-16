package com.aliucord.plugins;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.plugins.R;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.discord.api.commands.ApplicationCommandOption;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.stores.StoreStream;
import com.discord.stores.StoreUser;
import com.discord.stores.StoreUserTyping;
import com.discord.widgets.chat.input.MessageDraftsRepo;
import com.discord.widgets.chat.input.WidgetChatInputEditText;
import com.discord.widgets.chat.input.WidgetChatInputEditText$setOnTextChangedListener$1;
import com.lytefast.flexinput.widget.FlexEditText;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import kotlin.jvm.internal.DefaultConstructorMarker;
import top.canyie.pine.callback.MethodReplacement;

@SuppressWarnings("unused")
@AliucordPlugin
public class BetterSilentTyping extends Plugin {
    ImageButton button;
    Logger logger = new Logger("SilentTyping");
    int keyboardDisabledid;
    int keyboardid ;
    Drawable keyboard ;
    Drawable keyboardDisabled;
    Boolean hideKeyboard = settings.getBool("hideKeyboard",false) ;
    public BetterSilentTyping(){
        needsResources=true;
    }
    @Override
    public void start(Context context) {

        settingsTab=new SettingsTab(Settings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings,this);

        commands.registerCommand("togglekeyboard","Hides/Shows BetterSilentTyping Keyboard Icon",commandContext -> {
            Boolean bool = settings.getBool("hideKeyboard",false);
            setSettings(!bool);
            return new CommandsAPI.CommandResult();
        });
        registerCommand();


        keyboardDisabledid= resources.getIdentifier("keyboarddisabled","drawable","com.aliucord.plugins");
        keyboardid = resources.getIdentifier("keyboard","drawable","com.aliucord.plugins");
        keyboard= ResourcesCompat.getDrawable(resources,keyboardid,null);
        keyboardDisabled=ResourcesCompat.getDrawable(resources,keyboardDisabledid,null);
        try {

            patcher.patch(WidgetChatInputEditText.class.getConstructor(FlexEditText.class, MessageDraftsRepo.class),new Hook(callFrame -> {
                WidgetChatInputEditText thisobj = ((WidgetChatInputEditText)callFrame.thisObject);
                try {
                    FlexEditText et = (FlexEditText) ReflectUtils.getField (thisobj,"editText");
                    LinearLayout group = (LinearLayout) et.getParent(); //getting edit Texts parent
                    button = new ImageButton(group.getContext());
                    if(hideKeyboard){
                        button.setVisibility(View.GONE);
                    }

                    button.setOnClickListener(v -> { setSetting(!settings.getBool("isEnabled",false)); });

                    button.setAdjustViewBounds(true);
                    button.setMaxWidth(DimenUtils.dpToPx(40));

                    button.setBackgroundColor(0);
                    View v= group.getChildAt(1);
                    button.setMaxHeight(DimenUtils.dpToPx(45));
                    button.setLayoutParams(v.getLayoutParams());
                    group.removeView(v); //remove emoji button and add if after ImageButton so emojibutton will be at the end of layout
                    group.addView(button);
                    group.addView(v);
                    updateButton();
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    logger.error(e);
                }
            }));
        } catch (NoSuchMethodException e) {
            logger.error(e);
        }

        if(settings.getBool("hideOnText",false)){

            patchHideKeybordOnText();
        }

    }
    Runnable hideKeyboardOnTextPatch;
    public void patchHideKeybordOnText() {
        try {
            hideKeyboardOnTextPatch=patcher.patch(WidgetChatInputEditText$setOnTextChangedListener$1.class.getDeclaredMethod("afterTextChanged", Editable.class),new Hook(
                    (cf)->{
                        Editable et = (Editable) cf.args[0];
                        hideButton(!et.toString().isEmpty());
                    }
            ));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
    public void unpatchHideKeybordOnText(){
        if (hideKeyboardOnTextPatch!=null)hideKeyboardOnTextPatch.run();
    }

    public void registerCommand(){
        var isEnabled = settings.getBool("isEnabled",false);
        var val  = isEnabled ? "Enabled" : "Disabled";
        commands.registerCommand("silentTyping","Toggles Silent Typing, Its Currently "+ val,commandContext -> {

            setSetting(!isEnabled);
            return new CommandsAPI.CommandResult();
        });
    }


    Runnable patch = null;
    public void updateButton(){
        if (button!=null){
            if (settings.getBool("isEnabled",false)){
                button.setImageDrawable(keyboardDisabled);

            } else {button.setImageDrawable(keyboard);}
        }
        patchUserTyping();
    }

    public void hideButton(Boolean bool){
        int status ;
        if(bool){
            status = View.GONE;
        } else {status=View.VISIBLE;}
        Utils.mainThread.post(()->{
            try {
                button.setVisibility(status);
            } catch (Exception e){logger.error(e);}
        });



    }

    public  void setSettings(Boolean bool) {
        hideButton(bool);
        hideKeyboard= bool;
        settings.setBool("hideKeyboard", bool);
    }

    public void patchUserTyping(){
        if (settings.getBool("isEnabled", false)) {
            try {
                patch = patcher.patch(StoreUserTyping.class.getDeclaredMethod("setUserTyping", long.class), InsteadHook.DO_NOTHING);
            } catch (NoSuchMethodException e) {
                logger.error(e);
            }
        }
        else
            {
                if (patch!=null){
                    patch.run();
                }
            }
        }

    public void setSetting(boolean val){
        settings.setBool("isEnabled",val);
        patchUserTyping();
        updateButton();
        commands.unregisterCommand("silentTyping");
        registerCommand();

        if (settings.getBool("showToast",false)){
            var value = ((val) ? "Enabled" :"Disabled");
            var status = ((val) ? "Invisible" :"Visible");
            Utils.mainThread.post(() -> {
                try {
                    Toast.makeText(button.getContext(),  value + " Silent Typing " + "You are Now " + status, Toast.LENGTH_SHORT).show();

                }catch (Exception e){
                    logger.error(e);
                }

            });
               }
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
