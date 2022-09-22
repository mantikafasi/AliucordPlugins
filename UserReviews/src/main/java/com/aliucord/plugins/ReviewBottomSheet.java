package com.aliucord.plugins;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.aliucord.Utils;
import com.aliucord.plugins.ReviewListModal.Adapter;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.widgets.BottomSheet;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;

public class ReviewBottomSheet extends BottomSheet {
    Review review;
    Drawable reportIcon;
    Drawable deleteIcon;
    Adapter adapter;

    public ReviewBottomSheet(Review review, Adapter adapter) {
        this.review = review;
        this.adapter = adapter;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        var ctx = view.getContext();

        reportIcon = ContextCompat.getDrawable(ctx, com.lytefast.flexinput.R.e.ic_flag_24dp);
        deleteIcon = ContextCompat.getDrawable(ctx,com.lytefast.flexinput.R.e.ic_delete_24dp);

        if (reportIcon != null) reportIcon.setTint(
                ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveNormal)
        );
        if (deleteIcon != null) deleteIcon.setTint(
                ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveNormal)
        );

        var style = com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon;

        var reportReview = new TextView(ctx, null, 0, style);
        var deleteReview = new TextView(ctx,null,0,style);

        reportReview.setText("Report Review");
        reportReview.setCompoundDrawablesRelativeWithIntrinsicBounds(reportIcon, null, null, null);
        reportReview.setOnClickListener(v -> {
            Utils.threadPool.execute(() -> {
                var res = UserReviewsAPI.reportReview(UserReviews.staticSettings.getString("token", ""), review.getId());
                Utils.showToast(res);
                dismiss();
            });

        });
        var currentUserID = StoreStream.getUsers().getMe().getId();

        if (review.getSenderdiscordid() != currentUserID && !UserReviews.AdminList.contains(currentUserID)) {
            deleteReview.setVisibility(View.GONE);
        } else {
            deleteReview.setCompoundDrawablesRelativeWithIntrinsicBounds(deleteIcon, null, null, null);
            deleteReview.setText("Delete Review");
            deleteReview.setOnClickListener(v -> {
                Utils.threadPool.execute(() -> {
                    var res = UserReviewsAPI.deleteReview(UserReviews.staticSettings.getString("token",""),review.getId());
                    if (res.isSuccessful()) {
                        int revID = adapter.getReviewID(review);
                        Utils.mainThread.post(() -> {
                            if (revID != -1) {
                                adapter.reviews.remove(revID);
                                adapter.notifyItemRemoved(revID);
                            }
                            dismiss();
                        });
                    }
                    Utils.showToast(res.getMessage());

                });
            });
        }

        addView(reportReview);
        addView(deleteReview);
    }
}
