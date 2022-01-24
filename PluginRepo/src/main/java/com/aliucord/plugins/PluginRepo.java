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
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.settings.Plugins;
import com.aliucord.settings.Updater;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.settings.WidgetSettings;

@AliucordPlugin
public class PluginRepo extends Plugin {
    @Override
    public void start(Context context) throws NoSuchMethodException {
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
        patcher.patch(WidgetSettings.class.getDeclaredMethod("onViewBound", View.class),new Hook(cf -> {
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
            openPluginRepo.setOnClickListener(v1 -> Utils.openPageWithProxy(Utils.getAppActivity(),new PluginsPage()));
        }));
    }

    @Override
    public void stop(Context context) throws Throwable {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
