package com.aliucord.plugins;

import android.content.Context;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aliucord.Utils;
import com.aliucord.plugins.dataclasses.Badge;
import com.aliucord.widgets.BottomSheet;
import com.facebook.drawee.view.SimpleDraweeView;

public class BadgeBottomShit extends BottomSheet {

    public SimpleDraweeView badgeIcon;
    public TextView nameTW;
    public TextView infoTW;
    Badge badge;

    Context context;
    public BadgeBottomShit(Context context, Badge badge) {
        this.context = context;
        this.badge = badge;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        var layout = (LayoutInflater.from(context).inflate(Utils.getResId("widget_emoji_sheet","layout"), this.getLinearLayout(), false));
        addView(layout);
        badgeIcon = layout.findViewById(Utils.getResId("emoji_iv","id"));
        nameTW = layout.findViewById(Utils.getResId("name_tv","id"));
        infoTW = layout.findViewById(Utils.getResId("emoji_info_tv","id"));

        var builder = badgeIcon.getControllerBuilder();
        builder.m = true;
        badgeIcon.setController(builder.a());
        infoTW.setAutoLinkMask(Linkify.ALL);

        badgeIcon.setOnClickListener(v -> Utils.launchUrl(badge.getRedirect_url()));

        badgeIcon.setImageURI(badge.getBadge_icon());
        nameTW.setText(badge.getBadge_name());
        infoTW.setText(badge.getBadge_description());

    }
}
