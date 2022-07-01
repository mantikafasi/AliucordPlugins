package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.discord.widgets.user.usersheet.WidgetUserSheet;
import com.discord.widgets.user.usersheet.WidgetUserSheetViewModel;

@SuppressWarnings("unused")
@AliucordPlugin
public class UserReviews extends Plugin {
    public static SettingsAPI staticSettings;
    public static Logger logger = new Logger("UserReviews");
    int viewID = View.generateViewId();
    public static FragmentManager fragmentManager;
    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) {

        staticSettings = settings;
        settingsTab = new SettingsTab(BottomShit.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);
        try {
            patcher.patch(WidgetUserSheet.class.getDeclaredMethod("configureAboutMe", WidgetUserSheetViewModel.ViewState.Loaded.class), new Hook(cf -> {
                var viewstate = (WidgetUserSheetViewModel.ViewState.Loaded) cf.args[0];
                fragmentManager = ((WidgetUserSheet) cf.thisObject).getParentFragmentManager();
                var scrollView = (NestedScrollView) (WidgetUserSheet.access$getBinding$p((WidgetUserSheet) cf.thisObject)).getRoot();
                var ctx = scrollView.getContext();
                if (scrollView.findViewById(viewID) == null) {

                    var root = new UserReviewsView(ctx, viewstate.getUser());
                    root.setId(viewID);
                    ((LinearLayout) scrollView.findViewById(Utils.getResId("user_sheet_content", "id"))).addView(root);
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
