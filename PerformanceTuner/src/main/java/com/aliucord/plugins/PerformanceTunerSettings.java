package com.aliucord.plugins;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;

public class PerformanceTunerSettings extends BottomSheet {
    private final SettingsAPI settings;

    public PerformanceTunerSettings(SettingsAPI settings) {
        this.settings = settings;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = requireContext();
        setPadding(20);

        addSwitch(context, PerformanceTuner.KEY_DIAGNOSTICS, "Diagnostics", "Log channel switch, chat list, and media bind timings");
        addSwitch(context, PerformanceTuner.KEY_HOLD_LAST_ROWS_DURING_LOAD, "Hold chat rows while loading", "Avoid the short empty chat-list flash during channel switches");
        addSwitch(context, PerformanceTuner.KEY_PRESERVE_LOADER_TOUCH_STATE, "Preserve loader touch state", "Avoid the old-channel loader-state mutation during channel switches");
        addSwitch(context, PerformanceTuner.KEY_RECYCLER_REUSE, "Recycler reuse tuning", "Keep more chat row views reusable and disable row change animations");
        addSwitch(context, PerformanceTuner.KEY_DISABLE_TYPING_DOTS, "Disable typing dots", "Keep typing text but stop the repeating dot animation");
        addSwitch(context, PerformanceTuner.KEY_PREVENT_INLINE_PLAYERS, "Static inline media", "Show previews in chat and open media on tap instead of creating inline players");
    }

    private void addSwitch(Context context, String key, String title, String subtitle) {
        CheckedSetting setting = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, title, subtitle);
        setting.setChecked(settings.getBool(key, true));
        setting.setOnCheckedListener(value -> settings.setBool(key, value));
        addView(setting);
    }
}
