package com.aliucord.plugins.channelmediagrid;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.discord.utilities.color.ColorCompat;
import com.discord.views.channelsidebar.GuildChannelSideBarActionsView;
import com.discord.views.channelsidebar.PrivateChannelSideBarActionsView;
import com.discord.widgets.channels.WidgetChannelSidebarActions;
import com.discord.widgets.channels.WidgetChannelSidebarActionsViewModel;
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheet;
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheetViewModel;
import com.google.android.material.button.MaterialButton;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@AliucordPlugin
@SuppressWarnings("unused")
public class ChannelTabs extends Plugin {
    private static final String MEDIA_BUTTON_TAG = "ChannelTabsButton";
    private static final String GRID_MODE_KEY = "mediaGridMode";
    private static final int GUILD_WIDE_BTN_ID = View.generateViewId();
    private static ChannelTabs instance;
    private static long selectedGuildId;
    private static long selectedChannelId;

    @Override
    public void start(Context context) throws Throwable {
        instance = this;
        patcher.patch(
                WidgetChannelSidebarActions.class.getDeclaredMethod("configureUI", WidgetChannelSidebarActionsViewModel.ViewState.class),
                new Hook(callFrame -> {
                    try {
                        WidgetChannelSidebarActionsViewModel.ViewState viewState =
                                (WidgetChannelSidebarActionsViewModel.ViewState) callFrame.args[0];
                        if (viewState instanceof WidgetChannelSidebarActionsViewModel.ViewState.Guild) {
                            WidgetChannelSidebarActionsViewModel.ViewState.Guild guild =
                                    (WidgetChannelSidebarActionsViewModel.ViewState.Guild) viewState;
                            selectedGuildId = guild.getGuildId();
                            selectedChannelId = guild.getChannelId();
                        } else if (viewState instanceof WidgetChannelSidebarActionsViewModel.ViewState.Private) {
                            WidgetChannelSidebarActionsViewModel.ViewState.Private privateChannel =
                                    (WidgetChannelSidebarActionsViewModel.ViewState.Private) viewState;
                            selectedGuildId = 0;
                            selectedChannelId = privateChannel.getChannelId();
                        } else {
                            selectedGuildId = 0;
                            selectedChannelId = 0;
                        }
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
                PrivateChannelSideBarActionsView.class.getDeclaredMethod(
                        "a",
                        View.OnClickListener.class,
                        View.OnClickListener.class,
                        View.OnClickListener.class,
                        View.OnClickListener.class,
                        boolean.class
                ),
                new Hook(callFrame -> {
                    try {
                        if (selectedChannelId == 0)
                            return;

                        addMediaButton((PrivateChannelSideBarActionsView) callFrame.thisObject, 0L, selectedChannelId);
                    } catch (Throwable throwable) {
                        logger.error("Failed to add DM media button", throwable);
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

                        int notificationsId = Utils.getResId("guild_profile_sheet_notifications", "id");
                        Context tabItemsContext = tabItems.getContext();
                        MaterialButton mediaBtn = createNativeGuildProfileTabButton(tabItemsContext);
                        mediaBtn.setId(GUILD_WIDE_BTN_ID);
                        mediaBtn.setText("Media");
                        mediaBtn.setContentDescription("Media");
                        mediaBtn.setAllCaps(false);
                        mediaBtn.setSingleLine(true);

                        LinearLayout.LayoutParams mediaParams = mediaBtn.getLayoutParams() instanceof LinearLayout.LayoutParams
                                ? (LinearLayout.LayoutParams) mediaBtn.getLayoutParams()
                                : new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        mediaParams.width = 0;
                        mediaParams.weight = 1f;
                        mediaBtn.setLayoutParams(mediaParams);

                        Drawable icon = ContextCompat.getDrawable(tabItemsContext, com.lytefast.flexinput.R.e.ic_image_library_24dp);
                        if (icon != null) {
                            icon = icon.mutate();
                            int tintColor = ColorCompat.getThemedColor(tabItemsContext, com.lytefast.flexinput.R.b.colorInteractiveNormal);
                            icon.setTint(tintColor);
                            mediaBtn.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
                        } else {
                            mediaBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                        }
                        clearButtonBorder(mediaBtn);

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
        instance = null;
        selectedGuildId = 0;
        selectedChannelId = 0;
    }

    static boolean isMediaGridMode() {
        try {
            return instance != null && instance.settings.getBool(GRID_MODE_KEY, false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void setMediaGridMode(boolean enabled) {
        try {
            if (instance != null) {
                instance.settings.setBool(GRID_MODE_KEY, enabled);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addMediaButton(LinearLayout actionsView, long guildId, long channelId) {
        Context context = actionsView.getContext();
        View existing = actionsView.findViewWithTag(MEDIA_BUTTON_TAG);
        MaterialButton button = existing instanceof MaterialButton ? (MaterialButton) existing : null;
        if (existing != null && button == null) {
            actionsView.removeView(existing);
        }

        if (button == null) {
            button = createNativeSidebarActionButton(context);
            button.setTag(MEDIA_BUTTON_TAG);
            button.setText("Media");
            button.setContentDescription("Media");
            button.setAllCaps(false);
            button.setSingleLine(true);
            clearButtonBorder(button);

            var themedColor = ColorCompat.getThemedColor(context, com.lytefast.flexinput.R.b.colorInteractiveNormal);
            button.setTextColor(themedColor);

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

    private static MaterialButton createNativeSidebarActionButton(Context context) {
        int layoutId = Utils.getResId("guild_channel_side_bar_actions_view", "layout");
        if (layoutId != 0) {
            try {
                LinearLayout template = new LinearLayout(context);
                LayoutInflater.from(context).inflate(layoutId, template, true);
                for (int i = 0; i < template.getChildCount(); i++) {
                    View child = template.getChildAt(i);
                    if (child instanceof MaterialButton) {
                        template.removeView(child);
                        child.setId(View.NO_ID);
                        return (MaterialButton) child;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return new MaterialButton(context);
    }

    private static MaterialButton createNativeGuildProfileTabButton(Context context) {
        int layoutId = Utils.getResId("widget_guild_profile_sheet", "layout");
        int notificationsId = Utils.getResId("guild_profile_sheet_notifications", "id");
        if (layoutId != 0) {
            try {
                View root = LayoutInflater.from(context).inflate(layoutId, new android.widget.FrameLayout(context), false);
                MaterialButton button = notificationsId != 0
                        ? findMaterialButtonById(root, notificationsId)
                        : null;
                if (button == null) {
                    button = findFirstMaterialButton(root);
                }
                if (button != null) {
                    android.view.ViewParent parent = button.getParent();
                    if (parent instanceof ViewGroup) {
                        ((ViewGroup) parent).removeView(button);
                    }
                    button.setId(View.NO_ID);
                    return button;
                }
            } catch (Throwable ignored) {
            }
        }

        return new MaterialButton(context);
    }

    private static MaterialButton findMaterialButtonById(View view, int id) {
        if (view instanceof MaterialButton && view.getId() == id) {
            return (MaterialButton) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                MaterialButton result = findMaterialButtonById(group.getChildAt(i), id);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static MaterialButton findFirstMaterialButton(View view) {
        if (view instanceof MaterialButton) {
            return (MaterialButton) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                MaterialButton result = findFirstMaterialButton(group.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void clearButtonBorder(MaterialButton button) {
        try {
            button.setStrokeWidth(0);
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        } catch (Throwable ignored) {
        }
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
