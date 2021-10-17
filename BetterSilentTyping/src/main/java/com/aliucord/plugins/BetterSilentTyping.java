package com.aliucord.plugins;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.discord.stores.StoreUserTyping;
import com.discord.widgets.chat.input.MessageDraftsRepo;
import com.discord.widgets.chat.input.WidgetChatInputEditText;
import com.discord.widgets.chat.input.WidgetChatInputEditText$setOnTextChangedListener$1;
import com.lytefast.flexinput.widget.FlexEditText;

@SuppressWarnings("unused")
@AliucordPlugin
public class BetterSilentTyping extends Plugin {
    FrameLayout keyboardViewLayout;
    Logger logger = new Logger("SilentTyping");
    int keyboardDisabledid;
    int keyboardid ;
    public static Drawable keyboard ;
    public static SettingsAPI Settings;
    Drawable keyboardDisabled;
    ImageView keyboardView;
    public static Drawable disableImage;
    Boolean hideKeyboard = settings.getBool("hideKeyboard",false) ;
    public BetterSilentTyping(){
        needsResources=true;
    }
    @Override
    public void start(Context context) {
        Settings  = settings;
        settingsTab=new SettingsTab(Settings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings,this);

        commands.registerCommand("togglekeyboard","Hides/Shows BetterSilentTyping Keyboard Icon",commandContext -> {
            Boolean bool = settings.getBool("hideKeyboard",false);
            setHideKeyboard(!bool);
            return new CommandsAPI.CommandResult();
        });
        registerCommand();


        keyboard= ResourcesCompat.getDrawable(resources,resources.getIdentifier("keyboard","drawable","com.aliucord.plugins"),null);
        disableImage =ResourcesCompat.getDrawable(resources,resources.getIdentifier("disableimage","drawable","com.aliucord.plugins"),null);

        int keyboardColor = settings.getInt("0colorInt",-4538437);
        int disabledColor = settings.getInt("1colorInt",-65536);

        disableImage.setTint(disabledColor);
        keyboard.setTint(keyboardColor);

        try {

            patcher.patch(WidgetChatInputEditText.class.getConstructor(FlexEditText.class, MessageDraftsRepo.class),new Hook(callFrame -> {
                WidgetChatInputEditText thisobj = ((WidgetChatInputEditText)callFrame.thisObject);
                try {
                    FlexEditText et = (FlexEditText) ReflectUtils.getField (thisobj,"editText");
                    LinearLayout group = (LinearLayout) et.getParent(); //getting edit Texts parent

                    keyboardViewLayout = new FrameLayout(context);
                    keyboardView = new ImageView(context);
                    ImageView imageView1 = new ImageView(context);
                    keyboardView.setImageDrawable(keyboard);

                    imageView1.setImageDrawable(disableImage);
                    keyboardViewLayout.addView(keyboardView);
                    keyboardViewLayout.addView(imageView1);

                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) keyboardView.getLayoutParams();
                    params.width=DimenUtils.dpToPx(25);
                    params.height=DimenUtils.dpToPx(25);

                    keyboardView.setLayoutParams(params);
                    imageView1.setLayoutParams(params);

                    if(hideKeyboard){
                        keyboardViewLayout.setVisibility(View.GONE);
                    }

                    keyboardViewLayout.setOnClickListener(v -> { setEnabled(!settings.getBool("isEnabled",false)); });

                    View v= group.getChildAt(1);



                    group.removeView(v); //remove emoji button and add if after ImageButton so emojibutton will be at the end of layout

                    group.addView(keyboardViewLayout);
                    keyboardViewLayout.setLayoutParams(v.getLayoutParams());
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

            setEnabled(!isEnabled);
            return new CommandsAPI.CommandResult();
        });
    }
    Runnable patch = null;
    public void updateButton(){
        if (keyboardViewLayout !=null){
            if (settings.getBool("isEnabled",false)){
                keyboardViewLayout.getChildAt(1).setVisibility(View.VISIBLE);

            } else { keyboardViewLayout.getChildAt(1).setVisibility(View.GONE);}
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
                keyboardViewLayout.setVisibility(status);
            } catch (Exception e){logger.error(e);}
        });
    }
    public void setHideKeyboard(Boolean bool) {
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
    public void setEnabled(boolean val){
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
                    Toast.makeText(keyboardViewLayout.getContext(),  value + " Silent Typing " + "You are Now " + status, Toast.LENGTH_SHORT).show();

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
