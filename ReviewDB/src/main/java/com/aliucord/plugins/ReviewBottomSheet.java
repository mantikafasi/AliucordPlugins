package com.aliucord.plugins;

import android.content.Context;
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
    Drawable upvoteIcon;
    Drawable downvoteIcon;
    Drawable blockIcon;
    Adapter adapter;

    public ReviewBottomSheet(Review review, Adapter adapter) {
        this.review = review;
        this.adapter = adapter;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        var ctx = view.getContext();

        reportIcon = getTintedIcon(ctx, com.lytefast.flexinput.R.e.ic_flag_24dp);
        deleteIcon = getTintedIcon(ctx, com.lytefast.flexinput.R.e.ic_delete_24dp);
        upvoteIcon = getTintedIcon(ctx, com.lytefast.flexinput.R.e.ic_arrow_up_24dp);
        downvoteIcon = getTintedIcon(ctx, com.lytefast.flexinput.R.e.ic_arrow_down_24dp);
        blockIcon = getTintedIcon(ctx, com.lytefast.flexinput.R.e.ic_ban_red_24dp);

        var style = com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon;

        var reportReview = new TextView(ctx, null, 0, style);
        var deleteReview = new TextView(ctx,null,0,style);
        var upvoteReview = new TextView(ctx, null, 0, style);
        var downvoteReview = new TextView(ctx, null, 0, style);
        var blockUser = new TextView(ctx, null, 0, style);

        reportReview.setText("Report Review");
        reportReview.setCompoundDrawablesRelativeWithIntrinsicBounds(reportIcon, null, null, null);
        reportReview.setOnClickListener(v -> {
            Utils.threadPool.execute(() -> {
                var res = ReviewDBAPI.reportReview(ReviewDB.staticSettings.getString("token", ""), review.getId());
                Utils.showToast(res.getMessage());
                dismiss();
            });

        });
        var currentUserID = StoreStream.getUsers().getMe().getId();
        upvoteReview.setText("Upvote Review");
        upvoteReview.setCompoundDrawablesRelativeWithIntrinsicBounds(upvoteIcon, null, null, null);
        downvoteReview.setText("Downvote Review");
        downvoteReview.setCompoundDrawablesRelativeWithIntrinsicBounds(downvoteIcon, null, null, null);
        blockUser.setText("Block Reviewer");
        blockUser.setCompoundDrawablesRelativeWithIntrinsicBounds(blockIcon, null, null, null);

        upvoteReview.setOnClickListener(v -> vote(true));
        downvoteReview.setOnClickListener(v -> vote(false));
        blockUser.setOnClickListener(v -> {
            if (!ensureAuthorized()) return;
            Utils.threadPool.execute(() -> {
                var res = ReviewDBAPI.blockUser(ReviewDB.staticSettings.getString("token", ""), review.getSenderDiscordID(), true);
                Utils.showToast(res.isSuccessful() ? "Blocked reviewer" : res.getMessage());
                dismiss();
            });
        });

        if (review.getSenderDiscordID() != currentUserID && !ReviewDB.AdminList.contains(currentUserID)) {
            deleteReview.setVisibility(View.GONE);
        } else {
            deleteReview.setCompoundDrawablesRelativeWithIntrinsicBounds(deleteIcon, null, null, null);
            deleteReview.setText("Delete Review");
            deleteReview.setOnClickListener(v -> {
                Utils.threadPool.execute(() -> {
                    var res = ReviewDBAPI.deleteReview(ReviewDB.staticSettings.getString("token",""),review.getId());
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

        if (!review.hasVoting()) {
            upvoteReview.setVisibility(View.GONE);
            downvoteReview.setVisibility(View.GONE);
        }
        if (review.getId() == 0 || review.getSystemMessage()) {
            reportReview.setVisibility(View.GONE);
        }
        if (review.getSenderDiscordID() == currentUserID || review.getSenderDiscordID() == 0) {
            blockUser.setVisibility(View.GONE);
        }

        addView(upvoteReview);
        addView(downvoteReview);
        addView(reportReview);
        addView(blockUser);
        addView(deleteReview);
    }

    private boolean ensureAuthorized() {
        if (ReviewDB.staticSettings.getString("token", "").equals("")) {
            Utils.showToast("You need to authorize first");
            ReviewDBAPI.authorize();
            return false;
        }
        return true;
    }

    private void vote(boolean isUpvote) {
        if (!ensureAuthorized()) return;
        Utils.threadPool.execute(() -> {
            var res = ReviewDBAPI.voteReview(ReviewDB.staticSettings.getString("token", ""), review.getId(), isUpvote);
            if (res.isSuccessful()) {
                Utils.mainThread.post(() -> adapter.setVote(review, isUpvote));
            }
            Utils.showToast(res.getMessage() == null || res.getMessage().equals("") ? "Vote recorded" : res.getMessage());
            dismiss();
        });
    }

    private Drawable getTintedIcon(Context ctx, int resId) {
        var icon = ContextCompat.getDrawable(ctx, resId);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorInteractiveNormal));
        }
        return icon;
    }
}
