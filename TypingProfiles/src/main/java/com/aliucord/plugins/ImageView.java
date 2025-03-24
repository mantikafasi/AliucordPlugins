package com.aliucord.plugins;

import android.content.Context;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.aliucord.utils.DimenUtils;
import com.discord.models.user.User;
import com.discord.utilities.icon.IconUtils;
import com.discord.utilities.images.MGImages;
import com.facebook.drawee.view.SimpleDraweeView;

public class ImageView extends SimpleDraweeView {
    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(DimenUtils.dpToPx(16), DimenUtils.dpToPx(16));
    {
        layoutParams.topMargin = DimenUtils.dpToPx(4);
        layoutParams.bottomMargin = DimenUtils.dpToPx(4);
        layoutParams.rightMargin = DimenUtils.dpToPx(4);
    }

    public ImageView(@NonNull Context context, User user) {
        super(context);

        setLayoutParams(layoutParams);
        IconUtils.setIcon(this, IconUtils.getForUser(user));
        MGImages.setRoundingParams(/* imageView = */ this, /* borderRadius = */ DimenUtils.dpToPx(8), false, null, null, null);

    }
}
