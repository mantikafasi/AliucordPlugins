package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheet;
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheetViewModel;

@SuppressWarnings("unused")
@AliucordPlugin
public class ServerReviews extends Plugin {
    public static SettingsAPI staticSettings;
    public static Logger logger = new Logger("ServerReviews");
    int viewID = View.generateViewId();

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) {
        staticSettings = new SettingsAPI("UserReviews");

        settingsTab = new SettingsTab(BottomShit.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(staticSettings);

        try {
            patcher.patch(WidgetGuildProfileSheet.class.getDeclaredMethod("configureUI", WidgetGuildProfileSheetViewModel.ViewState.Loaded.class), new Hook(cf -> {
                var viewstate = (WidgetGuildProfileSheetViewModel.ViewState.Loaded) cf.args[0];

                var linearLayout = (LinearLayout) (WidgetGuildProfileSheet.access$getGuildActionBinding$p((WidgetGuildProfileSheet) cf.thisObject)).getRoot();
                var ctx = linearLayout.getContext();
                if (linearLayout.findViewById(viewID) == null) {

                    var root = new ServerReviewsView(ctx, viewstate.getGuildId());
                    root.setId(viewID);
                    linearLayout.addView(root);
                }

            }));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
