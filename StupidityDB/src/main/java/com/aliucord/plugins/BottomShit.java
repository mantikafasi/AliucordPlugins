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
import com.aliucord.fragments.InputDialog;
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
        authorizate.setText("Authorize");
        authorizate.setTextColor(com.lytefast.flexinput.R.b.primary_800);
        authorizate.setOnClickListener(oc -> {
            Utils.openPageWithProxy(Utils.getAppActivity(), new AuthorazationPage());
        });

        Button enterTokenManually = new Button(context);
        enterTokenManually.setText("Enter Token Manually");
        enterTokenManually.setTextColor(com.lytefast.flexinput.R.b.primary_800);
        enterTokenManually.setOnClickListener(oc -> {
            var dialog = new InputDialog().setTitle("Enter Token").setDescription("Long Click To Button to get token (discord sometimes ratelimiting api so if youre getting error thats probably why)");
            dialog.setOnOkListener(v -> {
                var token =dialog.getInput();
                if (!token.equals("")) settings.setString("token",token); else
                    Toast.makeText(context, "Please Enter Token", Toast.LENGTH_SHORT).show();
            });
            dialog.show(getParentFragmentManager(),"uga");
        });
        enterTokenManually.setOnLongClickListener(v -> {
            Utils.launchUrl("https://discord.com/api/oauth2/authorize?client_id=915703782174752809&redirect_uri=https%3A%2F%2Fmantikralligi1.pythonanywhere.com%2Fauth&response_type=code&scope=identify");
            return true;
        });


        addView(title);
        addView(clearCacheButton);
        addView(authorizate);
        addView(enterTokenManually);


    }
}
