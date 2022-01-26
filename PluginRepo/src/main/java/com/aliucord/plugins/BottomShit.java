package com.aliucord.plugins;

import static com.aliucord.plugins.PluginRepo.settingsAPI;

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
import com.aliucord.fragments.InputDialog;
import com.aliucord.views.DangerButton;
import com.aliucord.widgets.BottomSheet;
import com.discord.views.CheckedSetting;
import com.discord.views.RadioManager;

public class BottomShit extends BottomSheet {

    public BottomShit() { }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        var context = requireContext();
        setPadding(20);

        TextView title = new TextView(context, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Header);
        title.setText("PluginRepo");
        title.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        title.setGravity(Gravity.START);

        var notifyNewPlugins = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK,"Notify new plugins","");
        notifyNewPlugins.setChecked(settingsAPI.getBool("checkNewPlugins",true));

        notifyNewPlugins.setOnCheckedListener(aBoolean -> {
            settingsAPI.setBool("checkNewPlugins",aBoolean);

        });
        addView(title);
        addView(notifyNewPlugins);

    }
}
