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

import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.roles.RoleIconView;
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
    TextView tagIcon;
    ConstraintLayout headerLayout;
    LinearLayout badgeLayout;

    public ViewHolder(Adapter adapter, @NonNull ViewGroup layout) {
        super(layout);
        this.adapter = adapter;
        layout.setPadding(0, 0, DimenUtils.getDefaultPadding(), DimenUtils.getDefaultPadding());
        icon = layout.findViewById(iconId);
        username = layout.findViewById(serverNameId);
        message = layout.findViewById(serverNickId);
        message.setSingleLine(false);
        message.setTextColor(ColorCompat.getColor(layout.getContext(),com.lytefast.flexinput.R.c.primary_300));
        tagIcon = layout.findViewById(tagIconID);
        headerLayout = layout.findViewById(Utils.getResId("chat_list_adapter_item_text_header","id"));


        badgeLayout = new LinearLayout(layout.getContext());
        badgeLayout.setPadding(DimenUtils.dpToPx(4),0,0,0);
        headerLayout.addView(badgeLayout);

        var badgeLayoutLayoutParams = (ConstraintLayout.LayoutParams)badgeLayout.getLayoutParams();
        badgeLayoutLayoutParams.startToEnd = Utils.getResId("chat_list_adapter_item_text_name","id");
        badgeLayout.setLayoutParams(badgeLayoutLayoutParams);

        this.layout = layout;
        message.setAutoLinkMask(Linkify.ALL);

        layout.findViewById(timeoutIconId).setVisibility(View.GONE);
        tagIcon.setVisibility(View.GONE);
        tagIcon.setText("SYSTEM");


        layout.findViewById(Utils.getResId("chat_list_adapter_item_text_role_icon", "id")).setVisibility(View.GONE);

    }

    public void showTag() {
        if (tagIcon != null)
            tagIcon.setVisibility(View.VISIBLE);
    }

}
