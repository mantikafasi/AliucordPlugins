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

        int currentMode = settings.getInt("brush_layer_mode", 2);
        android.widget.TextView brushModeText = new android.widget.TextView(ctx);
        brushModeText.setText("Brush Layer Mode: " + getModeString(currentMode));
        brushModeText.setTextSize(16f);
        brushModeText.setPadding(com.aliucord.utils.DimenUtils.dpToPx(16), com.aliucord.utils.DimenUtils.dpToPx(16), com.aliucord.utils.DimenUtils.dpToPx(16), com.aliucord.utils.DimenUtils.dpToPx(16));
        brushModeText.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(ctx);
            builder.setTitle("Brush Layer Mode");
            String[] options = {"Always Behind Items", "Always In Front of Items", "Dynamic (Brings to front when drawing)"};
            builder.setSingleChoiceItems(options, settings.getInt("brush_layer_mode", 2), (dialog, which) -> {
                settings.setInt("brush_layer_mode", which);
                brushModeText.setText("Brush Layer Mode: " + getModeString(which));
                dialog.dismiss();
            });
            builder.show();
        });
        addView(brushModeText);
    }

    private String getModeString(int mode) {
        switch (mode) {
            case 0: return "Always Behind Items";
            case 1: return "Always In Front of Items";
            case 2: return "Dynamic (Brings to front when drawing)";
            default: return "Unknown";
        }
    }
}
