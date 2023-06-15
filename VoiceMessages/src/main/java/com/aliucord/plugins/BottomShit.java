package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;
import com.discord.views.RadioManager;

import java.util.Arrays;
import java.util.List;

public class BottomShit extends BottomSheet {

    SettingsAPI settings;

    public BottomShit(SettingsAPI settings) {
        this.settings = settings;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        var context = requireContext();
        setPadding(20);

        var highSamplingRate = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK, "Increases sampling rate", "This might fix speed up sound problem");
        highSamplingRate.setChecked(settings.getBool("highSamplingRate", false));

        highSamplingRate.setOnCheckedListener(aBoolean -> {
            settings.setBool("highSamplingRate", aBoolean);
        });

        addView(highSamplingRate);

        List<CheckedSetting> radios = Arrays.asList(
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "High", null),
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Normal", null),
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Low", null)
        );

        RadioManager radioManager = new RadioManager(radios);

        var bitDepth = Arrays.asList(192, 128, 64);

        var selectedRadio = radios.get(bitDepth.indexOf(settings.getInt("audioQuality", 128)));

        selectedRadio.setChecked(true);
        radioManager.a(selectedRadio);

        int radioSize = radios.size();

        for (int i = 0; i < radioSize; i++) {
            int finalSize = i;
            CheckedSetting radio = radios.get(i);
            radio.e(e -> {
                settings.setInt("audioQuality", bitDepth.get(finalSize));
                radioManager.a(radio);
            });

            addView(radio);
        }

    }
}
