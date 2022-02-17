package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Logger;
import com.aliucord.PluginManager;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.plugins.betterstatus.PresenceUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.widgets.BottomSheet;
import com.discord.api.presence.ClientStatus;
import com.discord.api.presence.ClientStatuses;
import com.discord.databinding.WidgetChannelMembersListItemUserBinding;
import com.discord.databinding.WidgetChannelsListItemChannelPrivateBinding;
import com.discord.models.message.Message;
import com.discord.models.presence.Presence;
import com.discord.models.user.User;
import com.discord.stores.StoreStream;
import com.discord.stores.StoreVoiceParticipants;
import com.discord.utilities.collections.SnowflakePartitionMap;
import com.discord.views.CheckedSetting;
import com.discord.views.StatusView;
import com.discord.views.VoiceUserView;
import com.discord.views.user.UserAvatarPresenceView;
import com.discord.widgets.channels.list.WidgetChannelsListAdapter;
import com.discord.widgets.channels.list.items.ChannelListItem;
import com.discord.widgets.channels.list.items.ChannelListItemPrivate;
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListAdapter;
import com.discord.widgets.channels.memberlist.adapter.ChannelMembersListViewHolderMember;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.adapter.WidgetChatListItem;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.MessageEntry;
import com.discord.widgets.user.profile.UserProfileHeaderView;
import com.discord.widgets.user.profile.UserProfileHeaderViewModel;
import com.facebook.drawee.span.DraweeSpanStringBuilder;
import com.facebook.drawee.span.SimpleDraweeSpanTextView;
import com.facebook.drawee.view.SimpleDraweeView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kotlin.jvm.functions.Function0;

@SuppressWarnings("unused")
@AliucordPlugin
public class BetterStatus extends Plugin {

    public Logger logger = new Logger("BetterStatus");

    public BetterStatus() {
        needsResources = true;
        settingsTab = new SettingsTab(BetterStatusSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);
    }

    public void setImageResource(AppCompatImageView appCompatImageView, int imageResource) {
        appCompatImageView.setImageResource(imageResource);
    }

    @SuppressLint("ResourceType")
    public void setImageDrawable(AppCompatImageView appCompatImageView, Drawable imageResource) {
        appCompatImageView.setImageDrawable(imageResource);
        appCompatImageView.setAdjustViewBounds(true);
        appCompatImageView.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    public void setImageDrawableAround(AppCompatImageView appCompatImageView, int visibility) {
        appCompatImageView.setVisibility(visibility);
    }

    public void setImageDrawable2(AppCompatImageView appCompatImageView, Drawable imageResource, int width) {
        appCompatImageView.setImageDrawable(imageResource);
        appCompatImageView.setAdjustViewBounds(true);
        appCompatImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        appCompatImageView.getLayoutParams().width = width;
    }

    @SuppressLint("ResourceType")
    public void setImageDrawableWidth2(AppCompatImageView appCompatImageView, Drawable imageResource, int width) {
        AppCompatImageView status2 = appCompatImageView.findViewById(0x7f0a0209);
        appCompatImageView.setImageDrawable(imageResource);
        status2.getLayoutParams().width = width;
    }

    @SuppressLint("ResourceType")
    @Override
    public void start(Context context) throws Throwable {
        VectorDrawable isWeb = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_web", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isWebDND = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_web_dnd", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isWebIDLE = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_web_idle", "drawable", "com.aliucord.plugins"), null);
        //-----
        VectorDrawable isDesktop = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isDesktopDND = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_dnd", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isDesktopIDLE = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_idle", "drawable", "com.aliucord.plugins"), null);
        //-----
        VectorDrawable isMobile = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_mobile", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isMobileDND = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_mobile_dnd", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isMobileIDLE = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_mobile_idle", "drawable", "com.aliucord.plugins"), null);
        //-----
        VectorDrawable isWebMobile = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_web_mobile", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isWebMobileDND = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_web_mobile_dnd", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isWebMobileIDLE = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_web_mobile_idle", "drawable", "com.aliucord.plugins"), null);
        //-----
        VectorDrawable isDesktopMobile = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_mobile", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isDesktopMobileDND = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_mobile_dnd", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isDesktopMobileIDLE = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_mobile_idle", "drawable", "com.aliucord.plugins"), null);
        //-----
        VectorDrawable isDesktopWeb = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_web", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isDesktopWebDND = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_web_dnd", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isDesktopWebIDLE = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_web_idle", "drawable", "com.aliucord.plugins"), null);
        //-----
        VectorDrawable isDesktopWebMobile = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_web_mobile", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isDesktopWebMobileDND = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_web_mobile_dnd", "drawable", "com.aliucord.plugins"), null);
        VectorDrawable isDesktopWebMobileIDLE = (VectorDrawable) ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_desktop_web_mobile_idle", "drawable", "com.aliucord.plugins"), null);
        //-----

        final boolean[] MoreIcons = {false};

        if (!settings.getBool("filled_colors", false)) {
            patcher.patch(StatusView.class.getDeclaredMethod("setPresence", Presence.class), new Hook(callFrame -> {
                Presence presence = (Presence) callFrame.args[0];
                if (presence == null) return;
                ClientStatuses clientStatuses = presence.getClientStatuses();

                if (clientStatuses != null) {
                    List<Drawable> drawableList = new ArrayList<>();
                    var desktopStatus = clientStatuses.a();
                    var mobileStatus = clientStatuses.b();
                    var webStatus = clientStatuses.c();

                    switch (webStatus) {
                        case ONLINE:
                            drawableList.add(isWeb);
                            break;
                        case DND:
                            drawableList.add(isWebDND);
                            break;
                        case IDLE:
                            drawableList.add(isWebIDLE);
                    }

                    switch (desktopStatus) {
                        case ONLINE:
                            drawableList.add(isDesktop);
                            break;
                        case DND:
                            drawableList.add(isDesktopDND);
                            break;
                        case IDLE:
                            drawableList.add(isDesktopIDLE);
                            break;
                    }

                    switch (mobileStatus) {
                        case ONLINE:
                            drawableList.add(isMobile);
                            break;
                        case DND:
                            drawableList.add(isMobileDND);
                            break;
                        case IDLE:
                            drawableList.add(isMobileIDLE);
                    }


                    View parent = (View) ((AppCompatImageView) callFrame.thisObject).getParent();

                    if (drawableList.size() == 1) {
                        setImageDrawable((AppCompatImageView) callFrame.thisObject, drawableList.get(0));
                        MoreIcons[0] = false;
                    } else if (drawableList.size() > 1) {
                        MoreIcons[0] = true;
                    }
                }
            }));

            patcher.patch(ChannelMembersListViewHolderMember.class.getDeclaredMethod("bind", ChannelMembersListAdapter.Item.Member.class, Function0.class), new Hook(callFrame -> {

                Field bindingField1 = null;
                try {
                    bindingField1 = ChannelMembersListViewHolderMember.class.getDeclaredField("binding");
                    bindingField1.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }

                WidgetChannelMembersListItemUserBinding binding = null;
                try {
                    binding = (WidgetChannelMembersListItemUserBinding) bindingField1.get(callFrame.thisObject);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                ConstraintLayout layout = (ConstraintLayout) binding.getRoot();

                ChannelMembersListAdapter.Item.Member member = (ChannelMembersListAdapter.Item.Member) callFrame.args[0];

                Presence presence = null;
                if (member.getUserId() != StoreStream.getUsers().getMe().getId()) {
                    presence = member.getPresence();
                }

                ClientStatuses clientStatuses = null;
                if (presence != null) {
                    clientStatuses = presence.getClientStatuses();
                }

                if (clientStatuses != null) {
                    //List<Drawable> drawableList = new ArrayList<>();
                    var desktopStatus = clientStatuses.a();
                    var mobileStatus = clientStatuses.b();
                    var webStatus = clientStatuses.c();

                    SimpleDraweeSpanTextView username_status = layout.findViewById(Utils.getResId("username_text", "id"));

                    if (username_status != null && MoreIcons[0]) {
                        switch (webStatus) {
                            case ONLINE:
                                DraweeSpanStringBuilder icon = new DraweeSpanStringBuilder();
                                icon.append(" ", new ImageSpan(isWeb, 1), 0);
                                username_status.append(" ");
                                username_status.append(icon);
                                break;
                            case DND:
                                DraweeSpanStringBuilder icon2 = new DraweeSpanStringBuilder();
                                icon2.append(" ", new ImageSpan(isWebDND, 1), 0);
                                username_status.append(" ");
                                username_status.append(icon2);
                                break;
                            case IDLE:
                                DraweeSpanStringBuilder icon3 = new DraweeSpanStringBuilder();
                                icon3.append(" ", new ImageSpan(isWebIDLE, 1), 0);
                                username_status.append(" ");
                                username_status.append(icon3);
                        }

                        switch (desktopStatus) {
                            case ONLINE:
                                DraweeSpanStringBuilder icon = new DraweeSpanStringBuilder();
                                icon.append(" ", new ImageSpan(isDesktop, 1), 0);
                                username_status.append(" ");
                                username_status.append(icon);
                                break;
                            case DND:
                                DraweeSpanStringBuilder icon2 = new DraweeSpanStringBuilder();
                                icon2.append(" ", new ImageSpan(isDesktopDND, 1), 0);
                                username_status.append(" ");
                                username_status.append(icon2);
                                break;
                            case IDLE:
                                DraweeSpanStringBuilder icon3 = new DraweeSpanStringBuilder();
                                icon3.append(" ", new ImageSpan(isDesktopIDLE, 1), 0);
                                username_status.append(" ");
                                username_status.append(icon3);
                        }

                        switch (mobileStatus) {
                            case ONLINE:
                                DraweeSpanStringBuilder icon = new DraweeSpanStringBuilder();
                                icon.append(" ", new ImageSpan(isMobile, 1), 0);
                                username_status.append(" ");
                                username_status.append(icon);
                                break;
                            case DND:
                                DraweeSpanStringBuilder icon2 = new DraweeSpanStringBuilder();
                                icon2.append(" ", new ImageSpan(isMobileDND, 1), 0);
                                username_status.append(" ");
                                username_status.append(icon2);
                                break;
                            case IDLE:
                                DraweeSpanStringBuilder icon3 = new DraweeSpanStringBuilder();
                                icon3.append(" ", new ImageSpan(isMobileIDLE, 1), 0);
                                username_status.append(" ");
                                username_status.append(icon3);
                        }
                    }
                }
            }));

            //--------UserProfileHeaderView---------

            try {
                patcher.patch(UserProfileHeaderView.class.getDeclaredMethod("updateViewState", UserProfileHeaderViewModel.ViewState.Loaded.class), new Hook(callFrame1 ->
                {
                    try {
                        Presence presence1 = ((UserProfileHeaderViewModel.ViewState.Loaded) callFrame1.args[0]).getPresence();
                        View username_profile_header = (View) ((UserProfileHeaderView) callFrame1.thisObject).getParent();
                        TextView username_profile_header2 = username_profile_header.findViewById(Utils.getResId("username_text", "id"));

                        if (presence1 == null) {
                            username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                            return;
                        }

                        ClientStatuses clientStatuses1 = presence1.getClientStatuses();
                        if (clientStatuses1 != null) {
                            username_profile_header2.setCompoundDrawablePadding(2);
                            if (PresenceUtils.isWebMobile(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isWebMobile, null);
                            }
                            if (PresenceUtils.isWebMobileDND(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isWebMobileDND, null);
                            }
                            if (PresenceUtils.isWebMobileIDLE(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isWebMobileIDLE, null);
                            }
                            //-------------------
                            if (PresenceUtils.isDesktopAndMobile(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isDesktopMobile, null);
                            }
                            if (PresenceUtils.isDesktopAndMobileDND(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isDesktopMobileDND, null);
                            }
                            if (PresenceUtils.isDesktopAndMobileIDLE(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isDesktopMobileIDLE, null);
                            }
                            //-------
                            if (PresenceUtils.isDesktopAndWeb(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isDesktopWeb, null);
                            }
                            if (PresenceUtils.isDesktopAndWebDND(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isDesktopWebDND, null);
                            }
                            if (PresenceUtils.isDesktopAndWebIDLE(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isDesktopWebIDLE, null);
                            }
                            //-------
                            if (PresenceUtils.isDesktopWebMobile(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isDesktopWebMobile, null);
                            }
                            if (PresenceUtils.isDesktopWebMobileDND(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isDesktopWebMobileDND, null);
                            }
                            if (PresenceUtils.isDesktopWebMobileIDLE(clientStatuses1)) {
                                username_profile_header2.setCompoundDrawablesWithIntrinsicBounds(null, null, isDesktopWebMobileIDLE, null);
                            }
                        }
                    } catch (Throwable e) {
                        logger.error("An error occurred in UserProfileHeaderView", e);
                    }
                }));
            } catch (Throwable e) {
                logger.error("An error occurred in UserProfileHeaderView", e);
            }

            //----------RADIAL STATUS----------

            //if ((PluginManager.plugins.containsKey("SquareAvatars") && !PluginManager.isPluginEnabled("SquareAvatars")) || !(PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) || (PluginManager.plugins.containsKey("AlwaysAnimate") && !PluginManager.isPluginEnabled("AlwaysAnimate")) || !(PluginManager.plugins.containsKey("AlwaysAnimate") && PluginManager.isPluginEnabled("AlwaysAnimate"))) {
            if (settings.getBool("radial_status_up", true) && (PluginManager.plugins.containsKey("SquareAvatars") && !PluginManager.isPluginEnabled("SquareAvatars")) || settings.getBool("radial_status_up", true) && !(PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars"))) {
                //-----------Radial Status on UserProfileHeaderView--------------

                patcher.patch(UserAvatarPresenceView.class.getDeclaredMethod("a", UserAvatarPresenceView.a.class), new Hook(callFrame -> {
                    UserAvatarPresenceView.a data = (UserAvatarPresenceView.a) callFrame.args[0];

                    View avatar = (View) ((UserAvatarPresenceView) callFrame.thisObject).getParent();
                    ImageView avatar2 = avatar.findViewById(Utils.getResId("avatar_cutout", "id"));


                    if (data.a.getAvatar() == null && data.a.getId() == StoreStream.getUsers().getMe().getId())
                        return;
                    if (data.b == null || data.a.getId() == StoreStream.getUsers().getMe().getId() && data.a.getAvatar() == null && !data.a.getAvatar().equals(StoreStream.getUsers().getMe().getAvatar())) {
                        avatar2.setPadding(0, 0, 0, 0);
                        avatar2.setBackground(null);
                    }

                    ClientStatuses clientStatuses = null;
                    if (data.b != null) {
                        clientStatuses = data.b.getClientStatuses();
                    }

                    if (clientStatuses != null) {
                        setRadialStatus(clientStatuses, avatar2);
                    }
                }));
            }
            //}

            //if ((PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) || (PluginManager.plugins.containsKey("AlwaysAnimate") && PluginManager.isPluginEnabled("AlwaysAnimate"))) {
            if (settings.getBool("radial_status_up", true) && PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) {
                //-----------Radial Status on UserProfileHeaderView--------------

                patcher.patch(UserAvatarPresenceView.class.getDeclaredMethod("a", UserAvatarPresenceView.a.class), new Hook(callFrame -> {
                    UserAvatarPresenceView.a data = (UserAvatarPresenceView.a) callFrame.args[0];

                    ClientStatuses clientStatuses = null;
                    if (data.b != null) {
                        clientStatuses = data.b.getClientStatuses();
                    }

                    if (clientStatuses != null) {

                        View avatar = (View) ((UserAvatarPresenceView) callFrame.thisObject).getParent();
                        ImageView avatar2 = avatar.findViewById(Utils.getResId("avatar", "id"));

                        if (data.a.getAvatar() == null && data.a.getId() == StoreStream.getUsers().getMe().getId())
                            return;
                        if (data.b == null || data.a.getId() == StoreStream.getUsers().getMe().getId() && data.a.getAvatar() == null && !data.a.getAvatar().equals(StoreStream.getUsers().getMe().getAvatar())) {
                            avatar2.setPadding(0, 0, 0, 0);
                            avatar2.setBackground(null);
                        }

                        ImageView avatar2_cut = avatar.findViewById(Utils.getResId("avatar_cutout", "id"));

                        setSquareStatus(clientStatuses, avatar2, "profile");

                        avatar2_cut.setVisibility(View.INVISIBLE);

                    }
                }));
            }
            //}

            //if ((PluginManager.plugins.containsKey("SquareAvatars")) && !PluginManager.isPluginEnabled("SquareAvatars") || !(PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) || (PluginManager.plugins.containsKey("AlwaysAnimate") && !PluginManager.isPluginEnabled("AlwaysAnimate")) || !(PluginManager.plugins.containsKey("AlwaysAnimate") && PluginManager.isPluginEnabled("AlwaysAnimate"))) {
            if (settings.getBool("radial_status_cml", true) && (PluginManager.plugins.containsKey("SquareAvatars") && !PluginManager.isPluginEnabled("SquareAvatars")) || settings.getBool("radial_status_cml", true) && !(PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars"))) {
                //-----ChannelMemberList--------
                patcher.patch(ChannelMembersListViewHolderMember.class.getDeclaredMethod("bind", ChannelMembersListAdapter.Item.Member.class, Function0.class), new Hook(callFrame -> {

                    Field bindingField1 = null;
                    try {
                        bindingField1 = ChannelMembersListViewHolderMember.class.getDeclaredField("binding");
                        bindingField1.setAccessible(true);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }

                    WidgetChannelMembersListItemUserBinding binding = null;
                    try {
                        binding = (WidgetChannelMembersListItemUserBinding) bindingField1.get(callFrame.thisObject);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    ConstraintLayout layout = (ConstraintLayout) binding.getRoot();

                    ChannelMembersListAdapter.Item.Member member = (ChannelMembersListAdapter.Item.Member) callFrame.args[0];

                    Presence presence = null;
                    if (member.getUserId() != StoreStream.getUsers().getMe().getId()) {
                        presence = member.getPresence();
                    }

                    SimpleDraweeView avatar = layout.findViewById(Utils.getResId("channel_members_list_item_avatar", "id"));

                    if (presence == null) {
                        if (avatar != null) {
                            avatar.setPadding(0, 0, 0, 0);
                            avatar.setBackground(null);
                        }
                    }

                    ClientStatuses clientStatuses = null;
                    if (presence != null) {
                        clientStatuses = presence.getClientStatuses();
                    }

                    if (clientStatuses != null && avatar != null) {
                        setRadialStatus(clientStatuses, avatar);
                    }
                }));

            }
            //}

            //if ((PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) || (PluginManager.plugins.containsKey("AlwaysAnimate") && PluginManager.isPluginEnabled("AlwaysAnimate"))) {
            if (settings.getBool("radial_status_cml", true) && PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) {
                //-----ChannelMemberList--------
                patcher.patch(ChannelMembersListViewHolderMember.class.getDeclaredMethod("bind", ChannelMembersListAdapter.Item.Member.class, Function0.class), new Hook(callFrame -> {

                    Field bindingField1 = null;
                    try {
                        bindingField1 = ChannelMembersListViewHolderMember.class.getDeclaredField("binding");
                        bindingField1.setAccessible(true);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }

                    WidgetChannelMembersListItemUserBinding binding = null;
                    try {
                        binding = (WidgetChannelMembersListItemUserBinding) bindingField1.get(callFrame.thisObject);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    ConstraintLayout layout = (ConstraintLayout) binding.getRoot();

                    ChannelMembersListAdapter.Item.Member member = (ChannelMembersListAdapter.Item.Member) callFrame.args[0];

                    Presence presence = null;
                    if (member.getUserId() != StoreStream.getUsers().getMe().getId()) {
                        presence = member.getPresence();
                    }

                    SimpleDraweeView avatar = layout.findViewById(Utils.getResId("channel_members_list_item_avatar", "id"));

                    if (presence == null) {
                        if (avatar != null) {
                            avatar.setPadding(0, 0, 0, 0);
                            avatar.setBackground(null);
                        }
                    }

                    ClientStatuses clientStatuses = null;
                    if (presence != null) {
                        clientStatuses = presence.getClientStatuses();
                    }

                    if (clientStatuses != null && avatar != null) {
                        setSquareStatus(clientStatuses, avatar, "non-profile");
                    }
                }));
            }
            //}

            //-------------DM List--------

            //if ((PluginManager.plugins.containsKey("SquareAvatars") || !PluginManager.isPluginEnabled("SquareAvatars")) || !(PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) || (PluginManager.plugins.containsKey("AlwaysAnimate") && !PluginManager.isPluginEnabled("AlwaysAnimate")) || !(PluginManager.plugins.containsKey("AlwaysAnimate") && PluginManager.isPluginEnabled("AlwaysAnimate"))) {
            if (settings.getBool("radial_status_dm", true) && (PluginManager.plugins.containsKey("SquareAvatars") && !PluginManager.isPluginEnabled("SquareAvatars")) || settings.getBool("radial_status_dm", true) && !(PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars"))) {

                patcher.patch(WidgetChannelsListAdapter.ItemChannelPrivate.class.getDeclaredMethod("onConfigure", int.class, ChannelListItem.class), new Hook(callFrame -> {

                    Field bindingField = null;
                    try {
                        bindingField = WidgetChannelsListAdapter.ItemChannelPrivate.class.getDeclaredField("binding");
                        bindingField.setAccessible(true);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    WidgetChannelsListItemChannelPrivateBinding binding = null;

                    try {
                        binding = (WidgetChannelsListItemChannelPrivateBinding) bindingField.get(callFrame.thisObject);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    RelativeLayout layout = (RelativeLayout) binding.getRoot();

                    ChannelListItemPrivate data = (ChannelListItemPrivate) callFrame.args[1];
                    Presence presence = data.getPresence();
                    ImageView avatar2 = layout.findViewById(Utils.getResId("channels_list_item_private_avatar", "id"));
                    Drawable radial_status = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_radial_status", "drawable", "com.aliucord.plugins"), null);

                    if (presence == null) {
                        avatar2.setPadding(0, 0, 0, 0);
                        avatar2.setBackground(null);
                    }

                    ClientStatuses clientStatuses = null;
                    if (presence != null) {
                        clientStatuses = presence.getClientStatuses();
                    }

                    if (clientStatuses != null) setRadialStatus(clientStatuses, avatar2);
                }));
            }
            //}

            //if ((PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) || (PluginManager.plugins.containsKey("AlwaysAnimate") && PluginManager.isPluginEnabled("AlwaysAnimate"))) {
            if (settings.getBool("radial_status_dm", true) && PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) {
                patcher.patch(WidgetChannelsListAdapter.ItemChannelPrivate.class.getDeclaredMethod("onConfigure", int.class, ChannelListItem.class), new Hook(callFrame -> {

                    Field bindingField = null;
                    try {
                        bindingField = WidgetChannelsListAdapter.ItemChannelPrivate.class.getDeclaredField("binding");
                        bindingField.setAccessible(true);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }

                    WidgetChannelsListItemChannelPrivateBinding binding = null;
                    try {
                        binding = (WidgetChannelsListItemChannelPrivateBinding) bindingField.get(callFrame.thisObject);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    RelativeLayout layout = (RelativeLayout) binding.getRoot();
                    ChannelListItemPrivate data = (ChannelListItemPrivate) callFrame.args[1];
                    Presence presence = data.getPresence();

                    ImageView avatar2 = layout.findViewById(Utils.getResId("channels_list_item_private_avatar", "id"));

                    if (presence == null) {
                        avatar2.setPadding(0, 0, 0, 0);
                        avatar2.setBackground(null);
                    }

                    ClientStatuses clientStatuses = null;
                    if (presence != null) {
                        clientStatuses = presence.getClientStatuses();
                    }

                    if (clientStatuses != null) {
                        setSquareStatus(clientStatuses, avatar2, "non-profile");
                    }
                }));

            }

            //}

            //----------WidgetChatListAdapterItemMessage---------

            //if ((PluginManager.plugins.containsKey("SquareAvatars") || !PluginManager.isPluginEnabled("SquareAvatars")) || !(PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) || (PluginManager.plugins.containsKey("AlwaysAnimate") && !PluginManager.isPluginEnabled("AlwaysAnimate")) || !(PluginManager.plugins.containsKey("AlwaysAnimate") && PluginManager.isPluginEnabled("AlwaysAnimate"))) {
            if (settings.getBool("radial_status_chat", true) && (PluginManager.plugins.containsKey("SquareAvatars") && !PluginManager.isPluginEnabled("SquareAvatars")) || settings.getBool("radial_status_chat", true) && !(PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars"))) {

                patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class), new Hook(callFrame -> {
                    ChatListEntry chatListEntry = (ChatListEntry) callFrame.args[1];
                    MessageEntry messageEntry = (MessageEntry) chatListEntry;
                    Message message = messageEntry.getMessage();
                    long userId = message.getAuthor().getId();

                    ImageView chat_list_avatar = ((WidgetChatListItem) callFrame.thisObject).itemView.findViewById(Utils.getResId("chat_list_adapter_item_text_avatar", "id"));
                    TextView chat_list_username = ((WidgetChatListItem) callFrame.thisObject).itemView.findViewById(Utils.getResId("chat_list_adapter_item_text_name", "id"));

                    SnowflakePartitionMap.CopiablePartitionMap<Presence> presences = getPresences();

                    Presence presence = null;
                    if (userId != StoreStream.getUsers().getMe().getId()) {
                        presence = (Presence) presences.get(userId);
                    }
                    //Presence local_presence = StoreStream.getPresences().getLocalPresence$app_productionCanaryRelease();

                    ClientStatuses clientStatuses = null;
                    if (presence != null) {
                        clientStatuses = presence.getClientStatuses();
                    }

                    Drawable radial_status = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_radial_status", "drawable", "com.aliucord.plugins"), null);

                    if (presence == null) {
                        if (chat_list_avatar != null) {
                            chat_list_avatar.setPadding(0, 0, 0, 0);
                            chat_list_avatar.setBackground(null);
                        }
                    }


                    if (clientStatuses != null) setRadialStatus(clientStatuses, chat_list_avatar);
                }));
                //-------
                patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("processMessageText", com.discord.utilities.view.text.SimpleDraweeSpanTextView.class, MessageEntry.class), new Hook(callFrame -> {
                    //ChatListEntry chatListEntry = (ChatListEntry) callFrame.args[1];
                    MessageEntry messageEntry = (MessageEntry) callFrame.args[1];
                    Message message = messageEntry.getMessage();
                    long userId = message.getAuthor().getId();

                    ImageView chat_list_avatar = ((WidgetChatListItem) callFrame.thisObject).itemView.findViewById(Utils.getResId("chat_list_adapter_item_text_avatar", "id"));
                    TextView chat_list_username = ((WidgetChatListItem) callFrame.thisObject).itemView.findViewById(Utils.getResId("chat_list_adapter_item_text_name", "id"));

                    SnowflakePartitionMap.CopiablePartitionMap<Presence> presences = getPresences();

                    Presence presence = null;
                    if (userId != StoreStream.getUsers().getMe().getId()) {
                        presence = (Presence) presences.get(userId);
                    }
                    //Presence local_presence = StoreStream.getPresences().getLocalPresence$app_productionCanaryRelease();

                    ClientStatuses clientStatuses = null;
                    if (presence != null) {
                        clientStatuses = presence.getClientStatuses();
                    }

                    Drawable radial_status = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_radial_status", "drawable", "com.aliucord.plugins"), null);

                    if (presence == null) {
                        if (chat_list_avatar != null) {
                            chat_list_avatar.setPadding(0, 0, 0, 0);
                            chat_list_avatar.setBackground(null);
                        }
                    }


                    if (clientStatuses != null) setRadialStatus(clientStatuses, chat_list_avatar);
                }));
            }
            //}

            //if ((PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) || (PluginManager.plugins.containsKey("AlwaysAnimate") && PluginManager.isPluginEnabled("AlwaysAnimate"))) {
            if (settings.getBool("radial_status_chat", true) && PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) {
                patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class), new Hook(callFrame -> {
                    ChatListEntry chatListEntry = (ChatListEntry) callFrame.args[1];
                    MessageEntry messageEntry = (MessageEntry) chatListEntry;
                    Message message = messageEntry.getMessage();
                    long userId = message.getAuthor().getId();

                    var presences = getPresences();

                    Presence presence = null;
                    if (userId != StoreStream.getUsers().getMe().getId()) {
                        presence = (Presence) presences.get(userId);
                    }

                    ClientStatuses clientStatuses = null;
                    if (presence != null) {
                        clientStatuses = presence.getClientStatuses();
                    }

                    ImageView chat_list_avatar = ((WidgetChatListItem) callFrame.thisObject).itemView.findViewById(Utils.getResId("chat_list_adapter_item_text_avatar", "id"));
                    TextView chat_list_username = ((WidgetChatListItem) callFrame.thisObject).itemView.findViewById(Utils.getResId("chat_list_adapter_item_text_name", "id"));

                    Drawable radial_status = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_radial_status", "drawable", "com.aliucord.plugins"), null);

                    if (presence == null) {
                        if (chat_list_avatar != null) {
                            chat_list_avatar.setPadding(0, 0, 0, 0);
                            chat_list_avatar.setBackground(null);
                        }
                    }


                    if (clientStatuses != null)
                        setSquareStatus(clientStatuses, chat_list_avatar, "non-profile");
                }));
                //------
                patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("processMessageText", com.discord.utilities.view.text.SimpleDraweeSpanTextView.class, MessageEntry.class), new Hook(callFrame -> {
                    //ChatListEntry chatListEntry = (ChatListEntry) callFrame.args[1];
                    MessageEntry messageEntry = (MessageEntry) callFrame.args[1];
                    Message message = messageEntry.getMessage();
                    long userId = message.getAuthor().getId();

                    var presences = getPresences();

                    Presence presence = null;
                    if (userId != StoreStream.getUsers().getMe().getId()) {
                        presence = (Presence) presences.get(userId);
                    }

                    ClientStatuses clientStatuses = null;
                    if (presence != null) {
                        clientStatuses = presence.getClientStatuses();
                    }

                    ImageView chat_list_avatar = ((WidgetChatListItem) callFrame.thisObject).itemView.findViewById(Utils.getResId("chat_list_adapter_item_text_avatar", "id"));
                    TextView chat_list_username = ((WidgetChatListItem) callFrame.thisObject).itemView.findViewById(Utils.getResId("chat_list_adapter_item_text_name", "id"));

                    Drawable radial_status = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_radial_status", "drawable", "com.aliucord.plugins"), null);

                    if (presence == null) {
                        if (chat_list_avatar != null) {
                            chat_list_avatar.setPadding(0, 0, 0, 0);
                            chat_list_avatar.setBackground(null);
                        }
                    }


                    if (clientStatuses != null)
                        setSquareStatus(clientStatuses, chat_list_avatar, "non-profile");
                }));
            }
            //}

            //------------Author status in chat--------------

            if (settings.getBool("status_chat", true)) {

                patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class), new Hook(callFrame -> {
                    ChatListEntry chatListEntry = (ChatListEntry) callFrame.args[1];
                    MessageEntry messageEntry = (MessageEntry) chatListEntry;
                    Message message = messageEntry.getMessage();
                    long userId = message.getAuthor().getId();

                    TextView chat_list_username = ((WidgetChatListItem) callFrame.thisObject).itemView.findViewById(Utils.getResId("chat_list_adapter_item_text_name", "id"));

                    var presences = getPresences();

                    Presence presence = null;
                    if (userId != StoreStream.getUsers().getMe().getId()) {
                        presence = (Presence) presences.get(userId);
                    }

                    ClientStatuses clientStatuses = null;

                    if (presence != null) {
                        clientStatuses = presence.getClientStatuses();

                        var desktopStatus = clientStatuses.a();
                        var mobileStatus = clientStatuses.b();
                        var webStatus = clientStatuses.c();

                        if (chat_list_username != null) {

                            switch (webStatus) {
                                case ONLINE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_online"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case DND:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_dnd"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case IDLE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_idle"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                            }

                            switch (desktopStatus) {
                                case ONLINE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_online"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case DND:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_dnd"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case IDLE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_idle"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                            }

                            switch (mobileStatus) {
                                case ONLINE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_online"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case DND:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_dnd"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case IDLE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_idle"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                            }
                        }
                    }

                    if (presence == null) {
                        if (chat_list_username != null) {
                            chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                            chat_list_username.setCompoundDrawablePadding(0);
                        }
                    }
                }));
                //------
                patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("processMessageText", com.discord.utilities.view.text.SimpleDraweeSpanTextView.class, MessageEntry.class), new Hook(callFrame -> {
                    //ChatListEntry chatListEntry = (ChatListEntry) callFrame.args[1];
                    MessageEntry messageEntry = (MessageEntry) callFrame.args[1];
                    Message message = messageEntry.getMessage();
                    long userId = message.getAuthor().getId();

                    TextView chat_list_username = ((WidgetChatListItem) callFrame.thisObject).itemView.findViewById(Utils.getResId("chat_list_adapter_item_text_name", "id"));

                    var presences = getPresences();

                    Presence presence = null;
                    if (userId != StoreStream.getUsers().getMe().getId()) {
                        presence = (Presence) presences.get(userId);
                    }

                    ClientStatuses clientStatuses = null;

                    if (presence != null) {
                        clientStatuses = presence.getClientStatuses();

                        var desktopStatus = clientStatuses.a();
                        var mobileStatus = clientStatuses.b();
                        var webStatus = clientStatuses.c();

                        if (chat_list_username != null) {

                            switch (webStatus) {
                                case ONLINE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_online"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case DND:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_dnd"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case IDLE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_idle"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                            }

                            switch (desktopStatus) {
                                case ONLINE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_online"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case DND:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_dnd"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case IDLE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_idle"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                            }

                            switch (mobileStatus) {
                                case ONLINE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_online"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case DND:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_dnd"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                                case IDLE:
                                    chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable("ic_idle"), null);
                                    chat_list_username.setCompoundDrawablePadding(8);
                                    break;
                            }
                        }
                    }

                    if (presence == null) {
                        if (chat_list_username != null) {
                            chat_list_username.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                            chat_list_username.setCompoundDrawablePadding(0);
                        }
                    }
                }));
            }

            //-------------VoiceChannel user speaking status----------------

            if (settings.getBool("status_voice", true)) {
                final StoreVoiceParticipants.VoiceUser[] voiceUser = new StoreVoiceParticipants.VoiceUser[1];

                patcher.patch(VoiceUserView.class.getDeclaredMethod("a", StoreVoiceParticipants.VoiceUser.class, int.class), new Hook(callFrame -> {
                    voiceUser[0] = (StoreVoiceParticipants.VoiceUser) callFrame.args[0];
                }));

                patcher.patch(VoiceUserView.class.getDeclaredMethod("setVoiceState", VoiceUserView.a.class), new Hook(callFrame -> {
                    VoiceUserView.a aVar = (VoiceUserView.a) callFrame.args[0];
                    FrameLayout status_ring = (FrameLayout) callFrame.thisObject;
                    Drawable radial_status = null;

                    if (PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars")) {
                        radial_status = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_rectangle_status", "drawable", "com.aliucord.plugins"), null);
                    } else if ((PluginManager.plugins.containsKey("SquareAvatars") && !PluginManager.isPluginEnabled("SquareAvatars")) || !(PluginManager.plugins.containsKey("SquareAvatars") && PluginManager.isPluginEnabled("SquareAvatars"))) {
                        radial_status = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_radial_status", "drawable", "com.aliucord.plugins"), null);
                    }

                    User user = null;
                    StoreVoiceParticipants.VoiceUser voiceUser1 = voiceUser[0];
                    if (voiceUser1 != null) {
                        user = voiceUser1.getUser();
                    }

                    long userId = user.getId();

                    var presences = getPresences();

                    Presence presence = (Presence) presences.get(userId);

                    ClientStatus clientStatus = null;
                    if (presence != null) {
                        clientStatus = presence.getStatus();
                    }

                    if (aVar.ordinal() != 1) {
                        status_ring.setBackgroundResource(0x0106000d);
                    } else {
                        if (clientStatus == ClientStatus.ONLINE) {
                            Objects.requireNonNull(radial_status).setTint(0xFF3BA55C - 1);         //0xFFED4245 - 1 DND  ||  0xFFFAA61A - 1 IDLE
                            status_ring.setBackground(radial_status);
                        } else if (clientStatus == ClientStatus.DND) {
                            Objects.requireNonNull(radial_status).setTint(0xFFED4245 - 1);
                            status_ring.setBackground(radial_status);
                        } else if (clientStatus == ClientStatus.IDLE) {
                            Objects.requireNonNull(radial_status).setTint(0xFFFAA61A - 1);
                            status_ring.setBackground(radial_status);
                        }
                    }
                }));
            }

            //---------END--------
        } else {
            patcher.patch(StatusView.class.getDeclaredMethod("setPresence", Presence.class), new Hook(callFrame -> {
                Presence presence = (Presence) callFrame.args[0];
                if (presence == null) return;

                ClientStatuses clientStatuses = presence.getClientStatuses();

                if (clientStatuses != null) {
                    var desktopStatus = clientStatuses.a();
                    var mobileStatus = clientStatuses.b();
                    var webStatus = clientStatuses.c();
                    switch (webStatus) {
                        case ONLINE:
                            setImageDrawable((AppCompatImageView) callFrame.thisObject, getDrawable("ic_online"));
                            break;
                        case DND:
                            setImageDrawable((AppCompatImageView) callFrame.thisObject, getDrawable("ic_dnd"));
                            break;
                        case IDLE:
                            setImageDrawable((AppCompatImageView) callFrame.thisObject, getDrawable("ic_idle"));
                            break;
                    }
                    switch (mobileStatus) {
                        case ONLINE:
                            setImageDrawable((AppCompatImageView) callFrame.thisObject, getDrawable("ic_mobile"));
                            break;
                        case DND:
                            setImageDrawable((AppCompatImageView) callFrame.thisObject, getDrawable("ic_dnd"));
                            break;
                        case IDLE:
                            setImageDrawable((AppCompatImageView) callFrame.thisObject, getDrawable("ic_idle"));
                            break;
                    }
                    switch (desktopStatus) {
                        case ONLINE:
                            setImageDrawable((AppCompatImageView) callFrame.thisObject, getDrawable("ic_online"));
                            break;
                        case DND:
                            setImageDrawable((AppCompatImageView) callFrame.thisObject, getDrawable("ic_dnd"));
                            break;
                        case IDLE:
                            setImageDrawable((AppCompatImageView) callFrame.thisObject, getDrawable("ic_idle"));
                            break;
                    }

                }
            }));
        }
    }

    public Drawable getDrawable(String name) {
        return ResourcesCompat.getDrawable(resources, resources.getIdentifier(name, "drawable", "com.aliucord.plugins"), null).mutate();

    }

    public void setSquareStatus(ClientStatuses clientStatuses, View avatar, String mode) {
        Drawable square_status = null;
        if (mode.equals("profile")) {
            square_status = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_profile_rectangle_status", "drawable", "com.aliucord.plugins"), null).mutate();
        } else if (mode.equals("non-profile")) {
            square_status = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_rectangle_status_cml", "drawable", "com.aliucord.plugins"), null).mutate();
        }
        setStatus(clientStatuses, avatar, square_status);
    }

    public void setRadialStatus(ClientStatuses clientStatuses, View avatar) {
        Drawable radial_status = ResourcesCompat.getDrawable(resources, resources.getIdentifier("ic_radial_status", "drawable", "com.aliucord.plugins"), null).mutate();
        setStatus(clientStatuses, avatar, radial_status);
    }

    public void setStatus(ClientStatuses clientStatuses, View avatar, Drawable background /* (UserAvatarPresenceView) */) {

        var desktopStatus = clientStatuses.a();
        var mobileStatus = clientStatuses.b();
        var webStatus = clientStatuses.c();

        if (avatar != null) {
            avatar.setPadding(8, 8, 8, 8);
            avatar.setBackground(background);
        }

        switch (webStatus) {
            case ONLINE:
                Objects.requireNonNull(background).setTint(0xFF3BA55C - 1);
                break;
            case DND:
                Objects.requireNonNull(background).setTint(0xFFED4245 - 1);
                break;
            case IDLE:
                Objects.requireNonNull(background).setTint(0xFFFAA61A - 1);
                break;
        }

        switch (desktopStatus) {
            case ONLINE:
                Objects.requireNonNull(background).setTint(0xFF3BA55C - 1);
                break;
            case DND:
                Objects.requireNonNull(background).setTint(0xFFED4245 - 1);
                break;
            case IDLE:
                Objects.requireNonNull(background).setTint(0xFFFAA61A - 1);
                break;
        }

        switch (mobileStatus) {
            case ONLINE:
                Objects.requireNonNull(background).setTint(0xFF3BA55C - 1);
                break;
            case DND:
                Objects.requireNonNull(background).setTint(0xFFED4245 - 1);
                break;
            case IDLE:
                Objects.requireNonNull(background).setTint(0xFFFAA61A - 1);
                break;
        }
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }

    public SnowflakePartitionMap.CopiablePartitionMap<Presence> getPresences() {
        try {
            var stupitShit = ReflectUtils.getField(StoreStream.getPresences(), "presences");
            return (SnowflakePartitionMap.CopiablePartitionMap<Presence>) stupitShit;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return new SnowflakePartitionMap.CopiablePartitionMap<Presence>();
        }
    }

    public static class BetterStatusSettings extends BottomSheet {
        private final SettingsAPI settings;

        public BetterStatusSettings(SettingsAPI settings) {
            this.settings = settings;
        }

        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            CheckedSetting filled_status = Utils.createCheckedSetting(requireContext(), CheckedSetting.ViewType.SWITCH, "Filled Colors", "Uses filled colors for status.");

            filled_status.setChecked(settings.getBool("filled_colors", false));
            filled_status.setOnCheckedListener(checked -> {
                settings.setBool("filled_colors", checked);
                Utils.showToast("Please restart Aliucord to apply");
            });
            addView(filled_status);

            CheckedSetting status_chat = Utils.createCheckedSetting(requireContext(), CheckedSetting.ViewType.SWITCH, "Chat Status", "Show little status circles in chat next to the username.");
            status_chat.setChecked(settings.getBool("status_chat", true));
            status_chat.setOnCheckedListener(checked -> {
                settings.setBool("status_chat", checked);
                Utils.showToast("Please restart Aliucord to apply");
            });
            addView(status_chat);

            CheckedSetting status_voice = Utils.createCheckedSetting(requireContext(), CheckedSetting.ViewType.SWITCH, "Voice Status", "Shows the status ring around the speaking user based on their status instead of only green.");
            status_voice.setChecked(settings.getBool("status_voice", true));
            status_voice.setOnCheckedListener(checked -> {
                settings.setBool("status_voice", checked);
                Utils.showToast("Please restart Aliucord to apply");
            });
            addView(status_voice);

            //------------------------------

            CheckedSetting radial_status_cml = Utils.createCheckedSetting(requireContext(), CheckedSetting.ViewType.SWITCH, "Radial Status (ChannelsMemberList)", "Shows a status ring around the user avatar in the ChannelsMembersList.");
            radial_status_cml.setChecked(settings.getBool("radial_status_cml", true));
            radial_status_cml.setOnCheckedListener(checked -> {
                settings.setBool("radial_status_cml", checked);
                Utils.showToast("Please restart Aliucord to apply");
            });
            addView(radial_status_cml);

            CheckedSetting radial_status_dm = Utils.createCheckedSetting(requireContext(), CheckedSetting.ViewType.SWITCH, "Radial Status (DM's)", "Shows a status ring around the user avatar in the DM List.");
            radial_status_dm.setChecked(settings.getBool("radial_status_dm", true));
            radial_status_dm.setOnCheckedListener(checked -> {
                settings.setBool("radial_status_dm", checked);
                Utils.showToast("Please restart Aliucord to apply");
            });
            addView(radial_status_dm);

            CheckedSetting radial_status_up = Utils.createCheckedSetting(requireContext(), CheckedSetting.ViewType.SWITCH, "Radial Status (UserProfile)", "Shows a status ring around the user avatar in the UserProfile.");
            radial_status_up.setChecked(settings.getBool("radial_status_up", true));
            radial_status_up.setOnCheckedListener(checked -> {
                settings.setBool("radial_status_up", checked);
                Utils.showToast("Please restart Aliucord to apply");
            });
            addView(radial_status_up);

            CheckedSetting radial_status_chat = Utils.createCheckedSetting(requireContext(), CheckedSetting.ViewType.SWITCH, "Radial Status (Chat)", "Shows a status ring around the user avatar in the Chat.");
            radial_status_chat.setChecked(settings.getBool("radial_status_chat", true));
            radial_status_chat.setOnCheckedListener(checked -> {
                settings.setBool("radial_status_chat", checked);
                Utils.showToast("Please restart Aliucord to apply");
            });
            addView(radial_status_chat);

        }
    }
}
