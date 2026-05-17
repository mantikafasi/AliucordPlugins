package com.aliucord.plugins;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;
import com.discord.views.RadioManager;
import com.lytefast.flexinput.R;

import java.util.Arrays;
import java.util.List;

public class MoreGifProvidersSettings extends BottomSheet {
    private final SettingsAPI settings;

    public MoreGifProvidersSettings(SettingsAPI settings) {
        this.settings = settings;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        Context context = requireContext();
        setPadding(20);

        TextView title = new TextView(context, null, 0, R.i.UiKit_Settings_Item_Header);
        title.setText("Default search and trending gif provider");
        addView(title);

        List<CheckedSetting> radios = Arrays.asList(
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Tenor", "Uses webp"),
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Klipy", "Uses webp"),
                Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Giphy", "Uses webp")
        );
        RadioManager radioManager = new RadioManager(radios);

        int selectedIndex = Arrays.asList(MoreGifProviders.PROVIDERS).indexOf(
                settings.getString(MoreGifProviders.PROVIDER_KEY, MoreGifProviders.PROVIDERS[0])
        );
        if (selectedIndex == -1) selectedIndex = 0;

        CheckedSetting selected = radios.get(selectedIndex);
        selected.setChecked(true);
        radioManager.a(selected);

        for (int i = 0; i < radios.size(); i++) {
            int index = i;
            CheckedSetting radio = radios.get(i);
            radio.e(v -> {
                settings.setString(MoreGifProviders.PROVIDER_KEY, MoreGifProviders.PROVIDERS[index]);
                radioManager.a(radio);
            });
            addView(radio);
        }
    }
}
