package com.aliucord.plugins;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.discord.app.AppBottomSheet;
import com.discord.views.CheckedSetting;

public class Settings extends AppBottomSheet {
    SettingsAPI settings;
    BetterSilentTyping plugin;
    public Settings(SettingsAPI set,BetterSilentTyping plugin){
        settings= set;
        this.plugin= plugin;
    }
    @Override
    public int getContentViewResId() {
        return 0;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Context context = layoutInflater.getContext();
        LinearLayout lay = new LinearLayout(context);
        lay.setOrientation(LinearLayout.VERTICAL);
        CheckedSetting a= Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH,"Show Toast Message When Silent Typing Toggled","");
        a.setChecked(settings.getBool("showToast",false));
        a.setOnCheckedListener(aBoolean -> {
            settings.setBool("showToast",aBoolean);
        });
        CheckedSetting b= Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH,"Hide Keyboard Icon","");
        b.setChecked(settings.getBool("hideKeyboard",false));
        b.setOnCheckedListener(aBoolean -> {
            settings.setBool("hideKeyboard",aBoolean);
            plugin.setSettings(aBoolean);
        });
        CheckedSetting c= Utils.createCheckedSetting(context,CheckedSetting.ViewType.SWITCH,"Hide Keyboard When Text Gets Entered" ,"");
        c.setChecked(settings.getBool("hideOnText",false));

        c.setOnCheckedListener(aBoolean -> { settings.setBool("hideOnText",aBoolean);if(aBoolean){ plugin.patchHideKeybordOnText();} else{ plugin.unpatchHideKeybordOnText(); }});

        lay.addView(a);
        lay.addView(b);
        lay.addView(c);
        return lay;
    }
}
