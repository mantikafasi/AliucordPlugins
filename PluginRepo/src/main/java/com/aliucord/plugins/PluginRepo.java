package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.NotificationsAPI;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.NotificationData;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.settings.Updater;
import com.discord.models.domain.ModelUserSettings;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.settings.WidgetSettings;
import com.discord.widgets.settings.WidgetSettingsAppearance;
import com.discord.widgets.settings.WidgetSettingsAppearance$updateTheme$1;

import java.util.Calendar;

@AliucordPlugin
public class PluginRepo extends Plugin {
    public static SettingsAPI settingsAPI;

    @Override
    public void start(Context context) throws NoSuchMethodException {
        settingsAPI = settings;
        settingsTab = new SettingsTab(BottomShit.class, SettingsTab.Type.BOTTOM_SHEET);
        if (settings.getBool("checkNewPlugins", true)) {
            Utils.threadPool.execute(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                var newPlugins = PluginRepoAPI.checkNewPlugins();
                if (newPlugins) {
                    Utils.mainThread.post(() -> {
                        NotificationsAPI.display(new NotificationData().setTitle("PluginRepo").setBody("New Plugins are available").setOnClick(view -> null));
                    });
                }
            });
        }


        /*
        Utils.threadPool.execute(() -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Utils.mainThread.post(() -> {
                Utils.openPageWithProxy(Utils.getAppActivity(),new PluginsPage());
            });
        });
         */

        var guh = Calendar.getInstance().get(Calendar.MONTH);
        var guh2 = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        if (guh == 3 && guh2 == 1 && settings.getBool("dj391kl",true)) {
            StoreStream.Companion.getUserSettingsSystem().setTheme(ModelUserSettings.THEME_LIGHT, true, new WidgetSettingsAppearance$updateTheme$1(new WidgetSettingsAppearance(), ModelUserSettings.THEME_LIGHT));
            settings.setBool("dj391kl",false);
            Utils.mainThread.postDelayed(() -> {
                Utils.showToast("Have a blind day! **PluginRepo**");
            },3000);
        }


        patcher.patch(WidgetSettings.class.getDeclaredMethod("onViewBound", View.class), new Hook(cf -> {
            Context ctx = ((WidgetSettings) cf.thisObject).requireContext();
            CoordinatorLayout view = (CoordinatorLayout) cf.args[0];
            LinearLayoutCompat v = (LinearLayoutCompat) ((NestedScrollView) view.getChildAt(1)).getChildAt(0);
            Typeface font = ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_medium);
            // Stole this from Main.java
            int baseIndex = v.indexOfChild(v.findViewById(Utils.getResId("developer_options_divider", "id")));

            TextView openPluginRepo = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon);
            openPluginRepo.setText("Open Plugin Repo");
            int iconColor = ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveNormal);
            openPluginRepo.setTypeface(font);
            Drawable icon = ContextCompat.getDrawable(ctx, com.lytefast.flexinput.R.e.ic_upload_24dp);
            if (icon != null) {
                Drawable copy = icon.mutate();
                copy.setTint(iconColor);
                openPluginRepo.setCompoundDrawablesRelativeWithIntrinsicBounds(copy, null, null, null);
            }
            openPluginRepo.setOnClickListener(e -> Utils.openPage(e.getContext(), Updater.class));
            v.addView(openPluginRepo, baseIndex);
            openPluginRepo.setOnClickListener(v1 -> Utils.openPageWithProxy(Utils.getAppActivity(), new PluginsPage()));
        }));
    }

    @Override
    public void stop(Context context) throws Throwable {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
