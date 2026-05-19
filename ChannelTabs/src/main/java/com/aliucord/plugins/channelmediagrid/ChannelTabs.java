package com.aliucord.plugins.channelmediagrid;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.view.ViewGroup;
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
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheet;
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheetViewModel;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@AliucordPlugin
@SuppressWarnings("unused")
public class ChannelTabs extends Plugin {
    private static final String MEDIA_BUTTON_TAG = "ChannelTabsButton";
    private static final int GUILD_WIDE_BTN_ID = View.generateViewId();
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

        patcher.patch(
                WidgetGuildProfileSheet.class.getDeclaredMethod("configureUI", WidgetGuildProfileSheetViewModel.ViewState.Loaded.class),
                new Hook(callFrame -> {
                    try {
                        Object viewState = callFrame.args[0];
                        if (!(viewState instanceof WidgetGuildProfileSheetViewModel.ViewState.Loaded))
                            return;

                        var state = (WidgetGuildProfileSheetViewModel.ViewState.Loaded) viewState;
                        var thisObject = (WidgetGuildProfileSheet) callFrame.thisObject;

                        var linearLayout = (LinearLayout) (WidgetGuildProfileSheet.access$getGuildActionBinding$p(thisObject)).getRoot();
                        if (linearLayout == null) return;

                        var constraintLayout = (ViewGroup) linearLayout.getParent();
                        if (constraintLayout == null) return;

                        int tabItemsId = Utils.getResId("guild_profile_sheet_tab_items", "id");
                        var tabItems = (LinearLayout) constraintLayout.findViewById(tabItemsId);
                        if (tabItems == null) return;

                        if (tabItems.findViewById(GUILD_WIDE_BTN_ID) != null) {
                            return;
                        }

                        com.google.android.material.button.MaterialButton refBtn = null;
                        int notificationsId = Utils.getResId("guild_profile_sheet_notifications", "id");
                        if (notificationsId != 0) {
                            refBtn = (com.google.android.material.button.MaterialButton) tabItems.findViewById(notificationsId);
                        }
                        if (refBtn == null) {
                            int boostsId = Utils.getResId("guild_profile_sheet_boosts", "id");
                            if (boostsId != 0) {
                                refBtn = (com.google.android.material.button.MaterialButton) tabItems.findViewById(boostsId);
                            }
                        }
                        if (refBtn == null) {
                            int settingsId = Utils.getResId("guild_profile_sheet_settings", "id");
                            if (settingsId != 0) {
                                refBtn = (com.google.android.material.button.MaterialButton) tabItems.findViewById(settingsId);
                            }
                        }

                        Context tabItemsContext = tabItems.getContext();
                        com.google.android.material.button.MaterialButton mediaBtn = new com.google.android.material.button.MaterialButton(tabItemsContext);
                        mediaBtn.setId(GUILD_WIDE_BTN_ID);
                        mediaBtn.setText("Media");
                        mediaBtn.setAllCaps(false);

                        if (refBtn != null) {
                            if (refBtn.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                                LinearLayout.LayoutParams refParams = (LinearLayout.LayoutParams) refBtn.getLayoutParams();
                                LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(0, refParams.height, 1f);
                                newParams.gravity = refParams.gravity;
                                newParams.setMargins(refParams.leftMargin, refParams.topMargin, refParams.rightMargin, refParams.bottomMargin);
                                mediaBtn.setLayoutParams(newParams);
                            } else {
                                mediaBtn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                            }

                            mediaBtn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, refBtn.getTextSize());
                            mediaBtn.setTextColor(refBtn.getTextColors());
                            mediaBtn.setTypeface(refBtn.getTypeface());
                            mediaBtn.setGravity(refBtn.getGravity());
                            mediaBtn.setPadding(refBtn.getPaddingLeft(), refBtn.getPaddingTop(), refBtn.getPaddingRight(), refBtn.getPaddingBottom());
                            mediaBtn.setCompoundDrawablePadding(refBtn.getCompoundDrawablePadding());

                            try {
                                if (refBtn.getBackgroundTintList() != null) {
                                    mediaBtn.setBackgroundTintList(refBtn.getBackgroundTintList());
                                }
                                if (refBtn.getBackgroundTintMode() != null) {
                                    mediaBtn.setBackgroundTintMode(refBtn.getBackgroundTintMode());
                                }
                                if (refBtn.getRippleColor() != null) {
                                    mediaBtn.setRippleColor(refBtn.getRippleColor());
                                }
                            } catch (Throwable ignored) {}

                            try {
                                if (refBtn.getBackground() != null && refBtn.getBackground().getConstantState() != null) {
                                    mediaBtn.setBackground(refBtn.getBackground().getConstantState().newDrawable().mutate());
                                }
                            } catch (Throwable ignored) {}
                        } else {
                            mediaBtn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                        }

                        Drawable icon = ContextCompat.getDrawable(tabItemsContext, com.lytefast.flexinput.R.e.ic_image_library_24dp);
                        if (icon != null) {
                            icon = icon.mutate();
                            int tintColor = ColorCompat.getThemedColor(tabItemsContext, com.lytefast.flexinput.R.b.colorInteractiveNormal);
                            icon.setTint(tintColor);
                            mediaBtn.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
                        }

                        long guildId = state.getGuildId();
                        mediaBtn.setOnClickListener(v1 -> {
                            try {
                                thisObject.dismiss();
                            } catch (Throwable ignored) {}
                            openMediaSheet(v1, guildId, 0L);
                        });

                        int insertionIndex = 2; // Default to after boosts and notifications
                        int notificationsIndex = -1;
                        if (notificationsId != 0) {
                            for (int i = 0; i < tabItems.getChildCount(); i++) {
                                if (tabItems.getChildAt(i).getId() == notificationsId) {
                                    notificationsIndex = i;
                                    break;
                                }
                            }
                        }
                        if (notificationsIndex != -1) {
                            insertionIndex = notificationsIndex + 1;
                        }

                        tabItems.addView(mediaBtn, insertionIndex);
                    } catch (Throwable t) {
                        logger.error("Failed to add guild wide media button next to notifications", t);
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

            Drawable icon = androidx.core.content.ContextCompat.getDrawable(context, com.lytefast.flexinput.R.e.ic_image_library_24dp);
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
