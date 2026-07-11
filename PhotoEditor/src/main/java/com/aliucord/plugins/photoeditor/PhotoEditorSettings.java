package com.aliucord.plugins.photoeditor;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Checkable;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;
import com.discord.views.RadioManager;

import java.util.ArrayList;
import java.util.List;

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

        CheckedSetting quickEditSetting = Utils.createCheckedSetting(
                view.getContext(),
                CheckedSetting.ViewType.SWITCH,
                "Quick Edit from Chat Input",
                "Open the editor when tapping an image in the chat box, without opening the attachment sheet."
        );
        quickEditSetting.setChecked(settings.getBool("quick_edit", false));
        quickEditSetting.setOnCheckedListener(isChecked -> settings.setBool("quick_edit", isChecked));
        addView(quickEditSetting);

        addRadioGroup(
                "Brush Layer Mode",
                new String[]{"Always Behind Items", "Always In Front of Items", "Dynamic"},
                new String[]{
                        "Brush strokes stay behind text, stickers, and images.",
                        "Brush strokes are drawn above text, stickers, and images.",
                        "Bring brush strokes forward while drawing."
                },
                settings.getInt("brush_layer_mode", 2),
                mode -> settings.setInt("brush_layer_mode", mode)
        );

        addRadioGroup(
                "Filter Apply Mode",
                new String[]{"Canvas Only", "Entire Edited Image"},
                new String[]{
                        "Apply filters only to the background image.",
                        "Apply filters to the background, text, stickers, and other edits."
                },
                settings.getInt("filter_apply_mode", 0),
                mode -> settings.setInt("filter_apply_mode", mode)
        );
    }

    private void addRadioGroup(String title, String[] labels, String[] descriptions, int selectedIndex, SelectionListener listener) {
        int safeSelectedIndex = Math.max(0, Math.min(selectedIndex, labels.length - 1));
        CheckedSetting[] options = new CheckedSetting[labels.length];
        List<Checkable> radioButtons = new ArrayList<>();

        for (int index = 0; index < labels.length; index++) {
            CheckedSetting option = Utils.createCheckedSetting(
                    requireContext(),
                    CheckedSetting.ViewType.RADIO,
                    title + ": " + labels[index],
                    descriptions[index]
            );
            option.setChecked(index == safeSelectedIndex);
            options[index] = option;
            radioButtons.add(option);
            addView(option);
        }

        RadioManager radioManager = new RadioManager(radioButtons);
        for (int index = 0; index < options.length; index++) {
            final int optionIndex = index;
            final CheckedSetting option = options[index];
            option.setOnCheckedListener(isChecked -> {
                if (isChecked) {
                    radioManager.a(option);
                    listener.onSelected(optionIndex);
                }
            });
        }
    }

    private interface SelectionListener {
        void onSelected(int index);
    }
}
