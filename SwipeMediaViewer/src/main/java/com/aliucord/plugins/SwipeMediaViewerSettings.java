package com.aliucord.plugins;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;

public class SwipeMediaViewerSettings extends BottomSheet {
    private final SettingsAPI settings;

    public SwipeMediaViewerSettings(SettingsAPI settings) {
        this.settings = settings;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = requireContext();
        setPadding(20);

        CheckedSetting showArrows = Utils.createCheckedSetting(
                context,
                CheckedSetting.ViewType.SWITCH,
                "Show media arrows",
                "Show left and right buttons in the media viewer"
        );
        showArrows.setChecked(settings.getBool(SwipeMediaViewer.SHOW_ARROWS_KEY, false));
        showArrows.setOnCheckedListener(value -> settings.setBool(SwipeMediaViewer.SHOW_ARROWS_KEY, value));
        addView(showArrows);
    }
}
