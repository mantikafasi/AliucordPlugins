package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.views.DangerButton;
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

        TextView title = new TextView(context, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Header);
        title.setText("StupidityDB");
        title.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        title.setGravity(Gravity.START);

        DangerButton clearCacheButton = new DangerButton(context);
        clearCacheButton.setText("Clear Cache");
        clearCacheButton.setOnClickListener(oc -> {
            StupidityDBAPI.cache.clearCache();
            Toast.makeText(context, "Cleared Cache", Toast.LENGTH_SHORT).show();
        });

        Button authorizate = new Button(context);
        authorizate.setText("Get OAUTH2 Token");
        authorizate.setTextColor(com.lytefast.flexinput.R.b.primary_800);
        authorizate.setOnClickListener(oc -> {
            Utils.openPageWithProxy(Utils.getAppActivity(), new AuthorazationPage());
        });

        TextView radioText = new TextView(context, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Header);
        radioText.setText("Voting Type");
        radioText.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        radioText.setGravity(Gravity.START);

        CheckedSetting useBot = Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "Discord Bot", "Votes Will be sent using discord bot (You need to be on server)");
        CheckedSetting useAPI = Utils.createCheckedSetting(context, CheckedSetting.ViewType.RADIO, "HTTP Requests", "Votes Will be sent using HTTP API (You need to authorize to use this)");
        List<CheckedSetting> radioList = Arrays.asList(useBot, useAPI);
        RadioManager manager = new RadioManager(radioList);
        manager.a(settings.getBool("useOAUTH2", false) ? useAPI : useBot);

        useBot.e(v -> {
            manager.a(useBot);
            settings.setBool("useOAUTH2", false);
        });
        useAPI.e(v -> {
            if (settings.getString("token", null) == null) {
                Toast.makeText(context, "You need to authorize to use HTTP Requests", Toast.LENGTH_SHORT).show();
                Utils.openPageWithProxy(Utils.getAppActivity(), new AuthorazationPage());
            }
            settings.setBool("useOAUTH2", true);
            manager.a(useAPI);
        });

        addView(title);
        addView(clearCacheButton);
        addView(authorizate);
        addView(radioText);
        addView(useAPI);
        addView(useBot);
    }
}
