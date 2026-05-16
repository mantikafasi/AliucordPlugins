package com.aliucord.plugins;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;
import com.discord.views.RadioManager;

import java.util.Arrays;
import java.util.List;

public class PowerModeSettings extends BottomSheet {
    private final SettingsAPI settings;

    public PowerModeSettings(SettingsAPI settings) {
        this.settings = settings;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = requireContext();
        setPadding(20);

        addView(createSwitch(context, "Enable power mode", PowerMode.ENABLED_KEY, true));
        addView(createSwitch(context, "Show particles", PowerMode.PARTICLES_KEY, true));
        addView(createSwitch(context, "Shake screen", PowerMode.SHAKE_KEY, true));
        addIntensityRadios(context);
    }

    private View createSwitch(Context context, String title, String key, boolean defaultValue) {
        CheckedSetting setting = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, title, "");
        setting.setChecked(settings.getBool(key, defaultValue));
        setting.setOnCheckedListener(value -> settings.setBool(key, value));
        return setting;
    }

    private void addIntensityRadios(Context context) {
        List<CheckedSetting> radios = Arrays.asList(
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Low intensity", ""),
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Normal intensity", ""),
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "High intensity", ""),
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Extreme intensity", "")
        );
        List<Integer> values = Arrays.asList(2, 4, 7, 10);
        RadioManager radioManager = new RadioManager(radios);

        int intensity = settings.getInt(PowerMode.INTENSITY_KEY, PowerMode.DEFAULT_INTENSITY);
        int selectedIndex = values.indexOf(intensity);
        if (selectedIndex == -1)
            selectedIndex = values.indexOf(PowerMode.DEFAULT_INTENSITY);

        CheckedSetting selected = radios.get(selectedIndex);
        selected.setChecked(true);
        radioManager.a(selected);

        for (int i = 0; i < radios.size(); i++) {
            int index = i;
            CheckedSetting radio = radios.get(i);
            radio.e(view -> {
                settings.setInt(PowerMode.INTENSITY_KEY, values.get(index));
                radioManager.a(radio);
            });
            addView(radio);
        }
    }
}
