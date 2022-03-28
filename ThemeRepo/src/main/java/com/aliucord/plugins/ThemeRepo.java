package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.settings.Updater;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.settings.WidgetSettings;

import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class ThemeRepo extends Plugin {
    @Override
    public void start(Context context) throws NoSuchMethodException {

        patcher.patch(WidgetSettings.class.getDeclaredMethod("onViewBound", View.class), new Hook(cf -> {
            Context ctx = ((WidgetSettings) cf.thisObject).requireContext();
            CoordinatorLayout view = (CoordinatorLayout) cf.args[0];
            LinearLayoutCompat v = (LinearLayoutCompat) ((NestedScrollView) view.getChildAt(1)).getChildAt(0);
            Typeface font = ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_medium);
            // Stole this from Main.java
            int baseIndex = v.indexOfChild(v.findViewById(Utils.getResId("developer_options_divider", "id")));

            TextView openThemeRepo = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon);
            openThemeRepo.setText("Open Theme Repo");
            int iconColor = ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveNormal);
            openThemeRepo.setTypeface(font);
            Drawable icon = ContextCompat.getDrawable(ctx, com.lytefast.flexinput.R.e.ic_theme_24dp);
            if (icon != null) {
                Drawable copy = icon.mutate();
                copy.setTint(iconColor);
                openThemeRepo.setCompoundDrawablesRelativeWithIntrinsicBounds(copy, null, null, null);
            }
            openThemeRepo.setOnClickListener(e -> Utils.openPage(e.getContext(), Updater.class));
            v.addView(openThemeRepo, baseIndex);
            openThemeRepo.setOnClickListener(v1 -> Utils.openPageWithProxy(Utils.getAppActivity(), new ThemesPage()));
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
