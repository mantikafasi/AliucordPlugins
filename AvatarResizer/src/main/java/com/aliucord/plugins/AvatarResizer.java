package com.aliucord.plugins;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.discord.models.message.Message;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.user.profile.UserProfileHeaderView;
import com.discord.widgets.user.profile.UserProfileHeaderViewModel;

@AliucordPlugin
@SuppressWarnings("unused")
public class AvatarResizer extends Plugin {
    public static final String MESSAGE_SIZE_KEY = "messageAvatarSize";
    public static final String POPOUT_SIZE_KEY = "popoutAvatarSize";
    public static final int DEFAULT_MESSAGE_SIZE = 40;
    public static final int DEFAULT_POPOUT_SIZE = 80;
    public static final int MIN_MESSAGE_SIZE = 16;
    public static final int MAX_MESSAGE_SIZE = 96;
    public static final int MIN_POPOUT_SIZE = 40;
    public static final int MAX_POPOUT_SIZE = 180;

    @Override
    public void start(Context context) throws Throwable {
        settingsTab = new SettingsTab(AvatarResizerSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        patcher.patch(
                WidgetChatListAdapterItemMessage.class.getDeclaredMethod("configureItemTag", Message.class, boolean.class),
                new Hook(callFrame -> {
                    try {
                        ImageView itemAvatar = (ImageView) ReflectUtils.getField(callFrame.thisObject, "itemAvatar");
                        resizeView(itemAvatar, getMessageAvatarSizePx());
                    } catch (Throwable e) {
                        logger.error(e);
                    }
                })
        );

        patcher.patch(
                UserProfileHeaderView.class.getDeclaredMethod("updateViewState", UserProfileHeaderViewModel.ViewState.Loaded.class),
                new Hook(callFrame -> {
                    try {
                        var binding = UserProfileHeaderView.access$getBinding$p((UserProfileHeaderView) callFrame.thisObject);
                        int size = getPopoutAvatarSizePx();
                        resizeView(binding.d, size);
                        resizeView(binding.f, size);
                    } catch (Throwable e) {
                        logger.error(e);
                    }
                })
        );
    }

    @Override
    public void stop(Context context) throws Throwable {
        patcher.unpatchAll();
        commands.unregisterAll();
    }

    private int getMessageAvatarSizePx() {
        return DimenUtils.dpToPx(clamp(settings.getInt(MESSAGE_SIZE_KEY, DEFAULT_MESSAGE_SIZE), MIN_MESSAGE_SIZE, MAX_MESSAGE_SIZE));
    }

    private int getPopoutAvatarSizePx() {
        return DimenUtils.dpToPx(clamp(settings.getInt(POPOUT_SIZE_KEY, DEFAULT_POPOUT_SIZE), MIN_POPOUT_SIZE, MAX_POPOUT_SIZE));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void resizeView(View view, int sizePx) {
        if (view == null)
            return;

        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null)
            return;

        if (params.width == sizePx && params.height == sizePx)
            return;

        params.width = sizePx;
        params.height = sizePx;
        view.setLayoutParams(params);
    }
}
