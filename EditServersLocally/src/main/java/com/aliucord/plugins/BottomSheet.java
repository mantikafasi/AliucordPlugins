package com.aliucord.plugins;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.ConfirmDialog;
import com.aliucord.plugins.DataClasses.ChannelData;
import com.aliucord.plugins.DataClasses.GuildData;
import com.aliucord.utils.DimenUtils;
import com.discord.app.AppBottomSheet;

import java.util.ArrayList;
import java.util.HashMap;

public class BottomSheet extends AppBottomSheet {
    SettingsAPI settings ;
    public BottomSheet(SettingsAPI settings){
        this.settings= settings;

    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        var context = layoutInflater.getContext();
        var layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        Button resetChannels = new Button(context);
        resetChannels.setText("Reset All Channel Names");
        resetChannels.setOnClickListener(v -> {
            new AlertDialog.Builder(context).setNegativeButton("No",(dialog, which) -> {}).setTitle("Are You Sure You Want To Delete All Channel Names")
                    .setPositiveButton("Yes",(dialog, which) -> {
                        settings.setObject("channelData",new HashMap<Long,ChannelData>());
                        Toast.makeText(context, "Deleted All Channel Names,Restart discord to see changes", Toast.LENGTH_LONG).show();

                    }).show();

        });
        Button resetGuilds = new Button(context);
        resetGuilds.setText("Reset All Server Settings");
        resetGuilds.setOnClickListener(v -> {
            new AlertDialog.Builder(context).setNegativeButton("No",(dialog, which) -> {}).setTitle("Are You Sure You Want To Delete All Server Settings")
                    .setPositiveButton("Yes",(dialog, which) -> {
                        settings.setObject("guildData",new HashMap<Long, GuildData>());
                        Toast.makeText(context, "Deleted All Server Settings,Restart discord to see changes", Toast.LENGTH_LONG).show();

                    }).show();
        });


        layout.addView(resetChannels);

        layout.addView(resetGuilds);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) resetGuilds.getLayoutParams();
        params.height =DimenUtils.dpToPx(50);
        params.width = LinearLayout.LayoutParams.MATCH_PARENT;
        resetChannels.setLayoutParams(params);
        resetGuilds.setLayoutParams(params);
        return  layout;
    }

    @Override
    public int getContentViewResId() {
        return 0;
    }
}
