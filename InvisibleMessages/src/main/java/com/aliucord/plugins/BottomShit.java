package com.aliucord.plugins;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.widgets.BottomSheet;
import com.discord.app.AppBottomSheet;
import com.discord.views.CheckedSetting;

import org.w3c.dom.Text;

public class BottomShit extends AppBottomSheet {

    SettingsAPI settings;
    public BottomShit(SettingsAPI settingsAPI){
        settings=settingsAPI;
    }

    @Override
    public int getContentViewResId() {
        return 0;
    }
    TextView tw;
    EditText et;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Context context = layoutInflater.getContext();
        LinearLayout lay = new LinearLayout(context);
        lay.setOrientation(LinearLayout.VERTICAL);

        tw = new TextView(context);
        tw.setText("Set Encryption Password");

        et  = new EditText(context);


        lay.addView(tw);
        lay.addView(et);
        return lay;
    }

    @Override
    public void onDestroy() {
        if (!et.getText().toString().trim().isEmpty())settings.setString("encryptionPassword",et.getText().toString());
        super.onDestroy();
    }
}
