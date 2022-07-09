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

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.discord.utilities.color.ColorCompat;
import com.facebook.drawee.view.SimpleDraweeView;

public class ViewHolder extends RecyclerView.ViewHolder {
    private static final int iconId = Utils.getResId("chat_list_adapter_item_text_avatar", "id");
    private static final int serverNameId = Utils.getResId("chat_list_adapter_item_text_name", "id");
    private static final int timeoutIconId = Utils.getResId("chat_list_adapter_item_communication_disabled_icon", "id");
    private static final int tagIconID = Utils.getResId("chat_list_adapter_item_text_tag", "id");
    private static final int serverNickId = Utils.getResId("chat_list_adapter_item_text", "id");
    public final SimpleDraweeView icon;
    public final TextView username;
    public final TextView message;
    private final Adapter adapter;
    public final ViewGroup layout;

    public ViewHolder(Adapter adapter, @NonNull ViewGroup layout) {
        super(layout);
        this.adapter = adapter;
        layout.setPadding(0, 0, DimenUtils.getDefaultPadding(), DimenUtils.getDefaultPadding());
        icon = layout.findViewById(iconId);
        username = layout.findViewById(serverNameId);
        message = layout.findViewById(serverNickId);
        message.setSingleLine(false);
        message.setTextColor(ColorCompat.getColor(layout.getContext(),com.lytefast.flexinput.R.c.primary_300));


        this.layout = layout;

        layout.findViewById(timeoutIconId).setVisibility(View.GONE);
        layout.findViewById(tagIconID).setVisibility(View.GONE);


    }

}
