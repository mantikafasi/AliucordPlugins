package com.mantikafasi.plugins;
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
import com.discord.stores.StoreStream;
import com.discord.stores.StoreUser;
import com.discord.stores.StoreUserTyping;
import com.discord.widgets.chat.input.MessageDraftsRepo;
import com.discord.widgets.chat.input.WidgetChatInputEditText;
import com.discord.widgets.chat.input.WidgetChatInputEditText$setOnTextChangedListener$1;
import com.lytefast.flexinput.widget.FlexEditText;

import java.lang.reflect.InvocationTargetException;

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
    public BetterSilentTyping(){
        needsResources=true;
    }
    @Override
    public void start(Context context) {
        keyboardDisabledid= resources.getIdentifier("keyboarddisabled","drawable","com.aliucord.plugins");
        keyboardid = resources.getIdentifier("keyboard","drawable","com.aliucord.plugins");
        keyboard= ResourcesCompat.getDrawable(resources,keyboardid,null);
        keyboardDisabled=ResourcesCompat.getDrawable(resources,keyboardDisabledid,null);
        try {
            patcher.patch(WidgetChatInputEditText.class.getConstructor(FlexEditText.class, MessageDraftsRepo.class),new PinePatchFn(callFrame -> {
                try {callFrame.invokeOriginalMethod();} catch (InvocationTargetException | IllegalAccessException e) {logger.error(e);}
                WidgetChatInputEditText thisobj = ((WidgetChatInputEditText)callFrame.thisObject);
                try {
                    FlexEditText et = (FlexEditText) ReflectUtils.getField (thisobj,"editText");
                    LinearLayout group = (LinearLayout) et.getParent(); //getting edit Texts parent
                    button = new ImageButton(group.getContext());
                    button.setOnClickListener(v -> {
                        setSetting(!settings.getBool("isEnabled", false));
                        updateButton(); });
                    button.setMaxHeight(80);
                    button.setAdjustViewBounds(true);
                    button.setMaxWidth(100);
                    button.setBackgroundColor(0);
                    View v= group.getChildAt(1);
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

    public void patchUserTyping(){
        if (settings.getBool("isEnabled", false)) {
            try {
                patch = patcher.patch(StoreUserTyping.class.getDeclaredMethod("setUserTyping", long.class), MethodReplacement.DO_NOTHING);
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
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
