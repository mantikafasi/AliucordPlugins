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
import com.discord.app.AppBottomSheet;

import java.util.ArrayList;

public class BottomSheet extends AppBottomSheet {
    SettingsAPI settings ;
    public BottomSheet(SettingsAPI settings){
        this.settings= settings;

    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        var context = layoutInflater.getContext();
        var layout = new LinearLayout(context);

        Button button = new Button(context);
        button.setText("Reset All Channel Names");

        button.setOnClickListener(v -> {
            new AlertDialog.Builder(context).setNegativeButton("No",(dialog, which) -> {}).setTitle("Are You Sure You Want To Delete All Channel Names")
                    .setPositiveButton("Yes",(dialog, which) -> {
                        settings.setObject("data",new ArrayList<ChannelData>());
                        Toast.makeText(context, "Deleted All Channel Names,Restart discord to see changes", Toast.LENGTH_LONG).show();

                    }).show();

        });
        layout.addView(button);
        return  layout;
    }

    @Override
    public int getContentViewResId() {
        return 0;
    }
}
