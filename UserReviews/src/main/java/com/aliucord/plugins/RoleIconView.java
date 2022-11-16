package com.aliucord.plugins;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.plugins.dataclasses.Badge;
import com.aliucord.utils.DimenUtils;
import com.discord.utilities.string.StringUtilsKt;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.Objects;

public class RoleIconView extends SimpleDraweeView {
    public RoleIconView(@NonNull Context context, Badge badge) {
        super(context);

        setImageURI(badge.getBadge_icon());

        setOnClickListener(view -> {
            Utils.showToast(badge.getBadge_name());
        });

        setOnLongClickListener(view -> {
            if (badge.getRedirect_url() != null && !Objects.equals(badge.getRedirect_url(), ""))
            Utils.launchUrl(badge.getRedirect_url());
            return true;
        });
    }

}
