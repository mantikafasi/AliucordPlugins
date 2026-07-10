package com.aliucord.plugins.photoeditor;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;

@SuppressWarnings("unused")
@SuppressLint("SetTextI18n")
public class PhotoEditorSettings extends BottomSheet {

    private final SettingsAPI settings;

    public PhotoEditorSettings(SettingsAPI settings) {
        this.settings = settings;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        android.content.Context ctx = view.getContext();

        try {
            CheckedSetting quickEditSetting = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.SWITCH, "Quick Edit from Chat Input", "Instantly open the editor when tapping an image in the chat box, bypassing the attachment bottom sheet.");
            quickEditSetting.setChecked(settings.getBool("quick_edit", false));
            quickEditSetting.setOnCheckedListener(isChecked -> {
                settings.setBool("quick_edit", isChecked);
            });
            addView(quickEditSetting);
        } catch (Throwable t) {
            // Fallback in case CheckedSetting API is different
            android.widget.Switch sw = new android.widget.Switch(ctx);
            sw.setText("Quick Edit from Chat Input (Bypass Bottom Sheet)");
            sw.setChecked(settings.getBool("quick_edit", false));
            sw.setOnCheckedChangeListener((btn, isChecked) -> settings.setBool("quick_edit", isChecked));
            sw.setPadding(com.aliucord.utils.DimenUtils.dpToPx(16), com.aliucord.utils.DimenUtils.dpToPx(16), com.aliucord.utils.DimenUtils.dpToPx(16), com.aliucord.utils.DimenUtils.dpToPx(16));
            addView(sw);
        }
    }
}
