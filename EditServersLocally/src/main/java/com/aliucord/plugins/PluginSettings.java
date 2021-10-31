package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.ConfirmDialog;
import com.aliucord.plugins.DataClasses.ChannelData;
import com.aliucord.plugins.DataClasses.GuildData;
import com.aliucord.views.DangerButton;
import com.aliucord.widgets.BottomSheet;
import com.lytefast.flexinput.R;

import java.util.HashMap;

public class PluginSettings extends BottomSheet {
    SettingsAPI settings;

    public PluginSettings(SettingsAPI settings) {
        this.settings = settings;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        var context = requireContext();
        setPadding(20);

        TextView title = new TextView(context, null, 0, R.h.UiKit_Settings_Item_Header);
        title.setText("Edit Servers Locally");
        title.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        title.setGravity(Gravity.START);

        DangerButton resetChannels = new DangerButton(context);
        resetChannels.setText("Reset all Channel Names");
        resetChannels.setOnClickListener(oc -> {
            ConfirmDialog confirmReset = new ConfirmDialog().setTitle("Reset All Channel Names?");
            confirmReset.setIsDangerous(true);
            confirmReset.setDescription("Are you sure you want to delete all channel names?");
            confirmReset.setOnOkListener(ok -> {
                settings.setObject("channelData", new HashMap<Long,ChannelData>());
                Utils.showToast("Deleted all channel names, restart discord to see changes");
                confirmReset.dismiss();
                dismiss();
            });
            confirmReset.setOnCancelListener(lll -> confirmReset.dismiss());
            confirmReset.show(getParentFragmentManager(), "AAAAAAAAAAAAAAAAAAAAAAA");
        });

        DangerButton resetGuilds = new DangerButton(context);
        resetGuilds.setText("Reset all Server Settings");
        resetGuilds.setOnClickListener(oc -> {
            ConfirmDialog confirmReset = new ConfirmDialog().setTitle("Reset All Server Settings?");
            confirmReset.setIsDangerous(true);
            confirmReset.setDescription("Are you sure you want to delete all server settings?");
            confirmReset.setOnOkListener(ok -> {
                settings.setObject("guildData",new HashMap<Long, GuildData>());
                Utils.showToast("Deleted all server settings, restart discord to see changes");
                confirmReset.dismiss();
                dismiss();
            });
            confirmReset.show(getParentFragmentManager(), "idkwhatthisisforlol");
            confirmReset.setOnCancelListener(lll -> confirmReset.dismiss());
        });

        addView(title);
        addView(resetChannels);
        addView(resetGuilds);
    }
}
