package com.aliucord.plugins;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aliucord.api.SettingsAPI;
import com.aliucord.utils.DimenUtils;
import com.discord.app.AppBottomSheet;

public class BottomShit extends AppBottomSheet {
    SettingsAPI settings;
    TextView tw;
    EditText et;

    public BottomShit(SettingsAPI settings) {
        this.settings = settings;
    }

    @Override
    public int getContentViewResId() {
        return 0;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Context context = layoutInflater.getContext();
        LinearLayout lay = new LinearLayout(context);
        lay.setOrientation(LinearLayout.VERTICAL);
        int px = DimenUtils.dpToPx(20);
        tw = new TextView(context);
        tw.setText("Your Stupit Device");


        et = new EditText(context);

        et.setText(settings.getString("device", "Aliucord for Android"));
        tw.setPadding(px, px, px, 0);
        et.setPadding(px, px, px, px);


        lay.addView(tw);
        lay.addView(et);
        return lay;
    }

    @Override
    public void onDestroy() {
        settings.setString("device", et.getText().toString().trim().isEmpty() ? "Aliucord for Android" : et.getText().toString().trim());
        super.onDestroy();
    }
}