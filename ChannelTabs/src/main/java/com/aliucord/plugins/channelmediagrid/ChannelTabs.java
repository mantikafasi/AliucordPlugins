package com.aliucord.plugins.channelmediagrid;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.widget.LinearLayout;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.discord.utilities.color.ColorCompat;
import com.discord.utilities.drawable.DrawableCompat;
import com.discord.views.channelsidebar.GuildChannelSideBarActionsView;
import com.discord.widgets.channels.WidgetChannelSidebarActions;
import com.discord.widgets.channels.WidgetChannelSidebarActionsViewModel;
import android.widget.TextView;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@AliucordPlugin
@SuppressWarnings("unused")
public class ChannelTabs extends Plugin {
    private static final String MEDIA_BUTTON_TAG = "ChannelTabsButton";
    private static long selectedGuildId;
    private static long selectedChannelId;

    @Override
    public void start(Context context) throws Throwable {
        patcher.patch(
                WidgetChannelSidebarActions.class.getDeclaredMethod("configureUI", WidgetChannelSidebarActionsViewModel.ViewState.class),
                new Hook(callFrame -> {
                    try {
                        WidgetChannelSidebarActionsViewModel.ViewState viewState =
                                (WidgetChannelSidebarActionsViewModel.ViewState) callFrame.args[0];
                        if (!(viewState instanceof WidgetChannelSidebarActionsViewModel.ViewState.Guild))
                            return;

                        WidgetChannelSidebarActionsViewModel.ViewState.Guild guild =
                                (WidgetChannelSidebarActionsViewModel.ViewState.Guild) viewState;
                        selectedGuildId = guild.getGuildId();
                        selectedChannelId = guild.getChannelId();
                    } catch (Throwable throwable) {
                        logger.error("Failed to remember selected channel", throwable);
                    }
                })
        );

        patcher.patch(
                GuildChannelSideBarActionsView.class.getDeclaredMethod(
                        "a",
                        Function1.class,
                        Function1.class,
                        Function1.class,
                        Function1.class,
                        Function1.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class
                ),
                new Hook(callFrame -> {
                    try {
                        if (selectedGuildId == 0 || selectedChannelId == 0)
                            return;

                        addMediaButton((GuildChannelSideBarActionsView) callFrame.thisObject, selectedGuildId, selectedChannelId);
                    } catch (Throwable throwable) {
                        logger.error("Failed to add channel media button", throwable);
                    }
                })
        );
    }

    @Override
    public void stop(Context context) throws Throwable {
        patcher.unpatchAll();
        commands.unregisterAll();
        selectedGuildId = 0;
        selectedChannelId = 0;
    }

    private static void addMediaButton(LinearLayout actionsView, long guildId, long channelId) {
        Context context = actionsView.getContext();
        TextView button = actionsView.findViewWithTag(MEDIA_BUTTON_TAG);
        if (button == null) {
            button = new TextView(context);
            button.setTag(MEDIA_BUTTON_TAG);
            button.setText("Media");
            button.setContentDescription("Media");
            button.setAllCaps(false);
            button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(Utils.getResId("uikit_textsize_small", "dimen")));


            var themedColor = ColorCompat.getThemedColor(context, com.lytefast.flexinput.R.b.colorInteractiveNormal);
            button.setTextColor(themedColor);

            button.setGravity(android.view.Gravity.CENTER);
            button.setPadding(0, button.getPaddingTop(), 0, button.getPaddingBottom());
            button.setCompoundDrawablePadding(0);

            android.util.TypedValue ripple = new android.util.TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
            button.setBackgroundResource(ripple.resourceId);

            android.util.TypedValue typedValue = new android.util.TypedValue();
            int fontAttr = Utils.getResId("font_primary_medium", "attr");
            if (fontAttr == 0) fontAttr = Utils.getResId("font_primary_normal", "attr");
            if (context.getTheme().resolveAttribute(fontAttr, typedValue, true)) {
                if (typedValue.resourceId != 0) {
                    try {
                        button.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(context, typedValue.resourceId));
                    } catch (Exception ignored) {}
                }
            }

            int iconRes = Utils.getResId("ic_image_24dp", "drawable");
            if (iconRes == 0) iconRes = android.R.drawable.ic_menu_gallery;
            Drawable icon = androidx.core.content.ContextCompat.getDrawable(context, iconRes);
            if (icon != null) {
                icon.mutate().setTint(themedColor);
                button.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            actionsView.addView(button, 1, params);
            normalizeActionWeights(actionsView);
        }

        button.setOnClickListener(view -> openMediaSheet(view, guildId, channelId));
    }

    private static void openMediaSheet(View view, long guildId, long channelId) {
        FragmentActivity activity = findFragmentActivity(view.getContext());
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        if (fragmentManager.isDestroyed() || fragmentManager.isStateSaved()) return;

        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof ChannelTabsSheet) {
                fragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss();
            }
        }

        ChannelTabsSheet fragment = ChannelTabsSheet.newInstance(guildId, channelId);
        fragmentManager.beginTransaction()
                .add(android.R.id.content, fragment, "ChannelTabs")
                .addToBackStack("ChannelTabs")
                .commitAllowingStateLoss();
    }

    private static FragmentActivity findFragmentActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof FragmentActivity) return (FragmentActivity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return context instanceof FragmentActivity ? (FragmentActivity) context : null;
    }

    private static void normalizeActionWeights(LinearLayout actionsView) {
        for (int i = 0; i < actionsView.getChildCount(); i++) {
            View child = actionsView.getChildAt(i);
            LinearLayout.LayoutParams params = child.getLayoutParams() instanceof LinearLayout.LayoutParams
                    ? (LinearLayout.LayoutParams) child.getLayoutParams()
                    : new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.width = 0;
            params.weight = 1;
            child.setLayoutParams(params);
        }
    }
}
