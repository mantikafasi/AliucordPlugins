/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.aliucord.plugins.ReviewListModal;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.facebook.drawee.view.SimpleDraweeView;

public class ViewHolder extends RecyclerView.ViewHolder {
    private static final int iconId = Utils.getResId("user_profile_adapter_item_server_image", "id");
    private static final int iconTextId = Utils.getResId("user_profile_adapter_item_server_text", "id");
    private static final int serverNameId = Utils.getResId("user_profile_adapter_item_server_name", "id");
    private static final int identityBarrierId = Utils.getResId("guild_member_identity_barrier", "id");
    private static final int serverAvatarId = Utils.getResId("guild_member_avatar", "id");
    private static final int serverNickId = Utils.getResId("user_profile_adapter_item_user_display_name", "id");
    public final SimpleDraweeView icon;
    public final TextView name;
    public final TextView serverNick;
    private final Adapter adapter;
    public final ViewGroup layout;

    public ViewHolder(Adapter adapter, @NonNull ViewGroup layout) {
        super(layout);
        this.adapter = adapter;
        layout.setPadding(0, 0, DimenUtils.getDefaultPadding(), DimenUtils.getDefaultPadding());
        icon = layout.findViewById(iconId);
        name = layout.findViewById(serverNameId);
        serverNick = layout.findViewById(serverNickId);
        serverNick.setSingleLine(false);
        serverNick.setMaxWidth(DimenUtils.dpToPx(280));
        //serverNick.setEllipsize(TextUtils.TruncateAt.END);
        this.layout = layout;


        // Hide server profile stuff
        layout.findViewById(iconTextId).setVisibility(View.GONE);
        layout.findViewById(identityBarrierId).setVisibility(View.GONE);
        layout.findViewById(serverAvatarId).setVisibility(View.GONE);

    }

}
