package com.aliucord.plugins;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.aliucord.Utils;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.widgets.BottomSheet;
import com.discord.utilities.color.ColorCompat;

public class ReviewBottomSheet extends BottomSheet {
    Review review;
    Drawable icon;

    public ReviewBottomSheet(Review review) {
        this.review = review;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        var ctx = view.getContext();
        icon = ContextCompat.getDrawable(ctx, com.lytefast.flexinput.R.e.ic_flag_24dp);
        if (icon != null) icon.setTint(
                ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveNormal)
        );

        TextView tw = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon);
        tw.setText("Report Review");

        tw.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
        tw.setOnClickListener(v -> {
            Utils.threadPool.execute(() -> {
                var res = UserReviewsAPI.reportReview(UserReviews.staticSettings.getString("token", ""), review.getId());
                Utils.showToast(res);
                dismiss();
            });

        });
        addView(tw);
    }
}
