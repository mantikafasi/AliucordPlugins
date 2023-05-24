package com.aliucord.plugins;

import static com.aliucord.plugins.ReviewDBAPI.AUTH_URL;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.InputDialog;
import com.aliucord.views.Button;
import com.aliucord.widgets.BottomSheet;
import com.discord.stores.StoreInviteSettings;
import com.discord.views.CheckedSetting;
import com.discord.widgets.guilds.invite.WidgetGuildInvite;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
        title.setText("UserReviews");
        title.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        title.setGravity(Gravity.START);

        Button crashing = new Button(context);
        crashing.setText("Crashing?");
        crashing.setOnClickListener(v -> {
            var dialog = new InputDialog();
            dialog.setOnDialogShownListener(view1 -> {
                dialog.setTitle("WARNING");
                dialog.getBody().setText("If your aliucord is crashing during authorization, long click the 'Enter Token Manually' button in settings and it will redirect you to the API to get a token. After you get your token click the 'Enter Token Manually' button again and paste it in there.");
                dialog.getInputLayout().setVisibility(View.GONE);
            });

            dialog.show(getParentFragmentManager(), "fart");

        });

        Button authorizate = new Button(context);
        authorizate.setText("Authorize");
        authorizate.setOnClickListener(oc -> ReviewDBAPI.authorize());

        Button enterTokenManually = new Button(context);
        enterTokenManually.setText("Enter OAUTH Token Manually");
        enterTokenManually.setOnClickListener(oc -> {
            var dialog = new InputDialog().setTitle("Enter Token").setDescription("Long click to get token (Discord sometimes ratelimits API requests so if you're getting an error that's probably why)");
            dialog.setOnOkListener(v -> {
                var token = dialog.getInput();
                if (!token.equals("")) settings.setString("token", token);
                else
                    Toast.makeText(context, "Please Enter Token", Toast.LENGTH_SHORT).show();
            });
            dialog.show(getParentFragmentManager(), "uga");
        });
        enterTokenManually.setOnLongClickListener(v -> {
            Utils.launchUrl(AUTH_URL);
            return true;
        });

        var notifyNewReviews = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK, "Notify for new reviews", "");
        notifyNewReviews.setChecked(settings.getBool("notifyNewReviews", true));

        notifyNewReviews.setOnCheckedListener(aBoolean -> {
            settings.setBool("notifyNewReviews", aBoolean);
            ReviewDBAPI.authorize();
        });

        var disableAds = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK, "Disables Ads in Reviews", "");
        disableAds.setChecked(settings.getBool("disableAds", false));

        disableAds.setOnCheckedListener(aBoolean -> {
            settings.setBool("disableAds", aBoolean);
        });

        var disableWarnings = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK, "Disables Warnings in Reviews", "You will still get banned if you do stupit");
        disableWarnings.setChecked(settings.getBool("disableWarnings", false));

        disableWarnings.setOnCheckedListener(aBoolean -> {
            settings.setBool("disableWarnings", aBoolean);
        });

        var supportServer = new Button(context);
        supportServer.setText("Support Server");

        supportServer.setOnClickListener(
            v -> WidgetGuildInvite.Companion.launch(Utils.getAppActivity(), new StoreInviteSettings.InviteCode("eWPBSbvznt", "", null))
        );

        var website = new Button(context);
        website.setText("Website");
            website.setOnClickListener(
                v -> {
                    var uri = Uri.parse("https://reviewdb.mantikafasi.dev").buildUpon();
                    if (settings.getString("token", "").equals("")) {
                        Utils.launchUrl(uri.build());
                    } else {
                        Utils.launchUrl(uri.appendPath("api").appendPath("redirect").appendQueryParameter("token",settings.getString("token", "")).build());
                    }
                } // passing oauth2 token to website so user can change settings
            );


        addView(title);
        addView(crashing);
        addView(authorizate);
        addView(enterTokenManually);
        addView(supportServer);
        addView(website);
        addView(notifyNewReviews);
        addView(disableAds);
        addView(disableWarnings);


    }
}
