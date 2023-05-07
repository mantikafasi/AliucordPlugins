package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.InputDialog;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.views.Button;
import com.aliucord.widgets.BottomSheet;
import com.discord.app.AppActivity;
import com.discord.restapi.RestAPIParams;
import com.discord.views.CheckedSetting;
import com.discord.widgets.auth.WidgetOauth2Authorize;
import com.discord.widgets.auth.WidgetOauth2Authorize$authorizeApplication$2;

public class BottomShit extends BottomSheet {
    SettingsAPI settings;
    public static String AUTH_URL = "https://discord.com/oauth2/authorize?client_id=915703782174752809&redirect_uri=https%3A%2F%2Fmanti.vendicated.dev%2Fapi%2Freviewdb%2Fauth&response_type=code&scope=identify";
    Logger logger = new Logger("ServerReviewsAPI");
    Long CLIENT_ID = 915703782174752809L;


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
        title.setText("ServerReviews");
        title.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        title.setGravity(Gravity.START);

        Button crashing = new Button(context);
        crashing.setText("Crashing?");
        crashing.setOnClickListener(v -> {
            var dialog = new InputDialog();
            dialog.setOnDialogShownListener(view1 -> {
                dialog.setTitle("WARNING");
                dialog.getBody().setText("If your aliucord is crashing while authorization long click to 'Enter Token Manually' button in settings, it will redirtect you to api for getting token. After you get your token click to 'Enter Token Manually' button again and paste it in there");
                dialog.getInputLayout().setVisibility(View.GONE);
            });

            dialog.show(getParentFragmentManager(), "fart");

        });

        Button authorizate = new Button(context);
        authorizate.setText("Authorize");
        authorizate.setOnClickListener(oc -> {

            var intent = new Intent("android.intent.action.VIEW");
            intent.putExtra("REQ_URI", Uri.parse(AUTH_URL));
            intent.addFlags(268468224);

            Utils.openPage(Utils.getAppContext(), WidgetOauth2Authorize.class, intent);

            try {
                ServerReviews.staticPatcher.patch(WidgetOauth2Authorize$authorizeApplication$2.class.getDeclaredMethod("invoke", RestAPIParams.OAuth2Authorize.ResponsePost.class),
                        new InsteadHook(cf -> {
                            var thisObject = (WidgetOauth2Authorize$authorizeApplication$2) cf.thisObject;
                            var clientID = thisObject.this$0.getOauth2ViewModel().oauthAuthorize.getClientId();
                            var res = (RestAPIParams.OAuth2Authorize.ResponsePost) cf.args[0];

                            if (clientID == CLIENT_ID) {
                                Utils.threadPool.execute(() -> {
                                    logger.info("Got token: " + res.getLocation());
                                    var response = ServerReviewsAPI.authorize(res.getLocation());
                                    logger.info(response.toString());
                                    if (response.isSuccessful()) {
                                        ServerReviews.staticSettings.setString("token", response.getToken());
                                        Utils.showToast("Successfully Authorized", false);
                                    }
                                    var i = new Intent(Utils.appActivity, AppActivity.class);
                                    Utils.appActivity.startActivity(i);
                                });
                            }
                            return "morb";
                        }));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });

        Button enterTokenManually = new Button(context);
        enterTokenManually.setText("Enter OAUTH Token Manually");
        enterTokenManually.setOnClickListener(oc -> {
            var dialog = new InputDialog().setTitle("Enter Token").setDescription("Long Click To Button to get token (discord sometimes ratelimiting api so if youre getting error thats probably why)");
            dialog.setOnOkListener(v -> {
                var token = dialog.getInput();
                if (!token.equals("")) settings.setString("token", token);
                else
                    Toast.makeText(context, "Please Enter Token", Toast.LENGTH_SHORT).show();
            });
            dialog.show(getParentFragmentManager(), "uga");
        });
        enterTokenManually.setOnLongClickListener(v -> {
            Utils.launchUrl(AuthorazationPage.AUTH_URL);
            return true;
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

        addView(title);
        addView(crashing);
        addView(authorizate);
        addView(enterTokenManually);
        addView(disableAds);
        addView(disableWarnings);

    }
}
