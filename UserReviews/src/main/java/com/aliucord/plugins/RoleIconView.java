package com.aliucord.plugins;

import android.content.Context;
import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.plugins.dataclasses.Badge;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.Objects;

public class RoleIconView extends SimpleDraweeView {
    public RoleIconView(@NonNull Context context, Badge badge) {
        super(context);

        var builder = getControllerBuilder();
        builder.m = true;
        setController(builder.a());
        setImageURI(badge.getBadge_icon());

        setOnClickListener(view -> {
            var sheet = new BadgeBottomShit(getContext(),badge);
            if (UserReviews.userSheet != null)
                sheet.show(UserReviews.userSheet.getChildFragmentManager(), "sheet");
        });

        setOnLongClickListener(view -> {
            if (badge.getRedirect_url() != null && !Objects.equals(badge.getRedirect_url(), ""))
            Utils.launchUrl(badge.getRedirect_url());
            return true;
        });
    }

}
