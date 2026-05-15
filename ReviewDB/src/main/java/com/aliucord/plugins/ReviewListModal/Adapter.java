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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.CollectionUtils;
import com.aliucord.Utils;
import com.aliucord.plugins.ReviewBottomSheet;
import com.aliucord.plugins.RoleIconView;
import com.aliucord.plugins.ReviewDB;
import com.aliucord.plugins.dataclasses.Badge;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.plugins.dataclasses.ReviewVote;
import com.aliucord.utils.DimenUtils;
import com.discord.stores.StoreStream;
import com.discord.widgets.user.usersheet.WidgetUserSheet;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Adapter extends RecyclerView.Adapter<ViewHolder> {
    private static final int layoutId = Utils.getResId("widget_chat_list_adapter_item_text", "layout");

    public final List<Review> reviews;
    public final Map<Integer, Boolean> votes = new HashMap<>();

    public Adapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var layout = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(this, (ViewGroup) layout);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        var review = reviews.get(position);

        if (review.getSystemMessage()) {
            holder.showTag();
        }

        holder.icon.setOnClickListener(v -> {
            var user = StoreStream.getUsers().getUsers().get(review.getSenderDiscordID());
            if (user != null && Utils.widgetChatList.isAdded()) {
                WidgetUserSheet.Companion.show(review.getSenderDiscordID(),Utils.widgetChatList.getChildFragmentManager());

                // FOR SOME WEIRD REASON IT DOESNT WORK ON UNCACHED USERS
            } /* else {
                RxUtils.subscribe(RestAPI.getApi().userGet(review.getSenderdiscordid()), user1 -> {
                    Utils.showToast("Fetched User");
                    StoreStream.access$getDispatcher$p(StoreStream.getNotices().getStream()).schedule(() -> {
                        StoreStream.getUsers().handleUserUpdated(user1);
                        Utils.mainThread.post(() -> {
                            Utils.showToast("sh");
                            WidgetUserSheet.Companion.show(review.getSenderdiscordid(), Utils.widgetChatList.getChildFragmentManager());
                        });
                        return null;
                    });
                    return null;
                });
                }
                */

        });

        holder.layout.setOnLongClickListener(v -> {
            if(Utils.widgetChatList.isAdded())
                new ReviewBottomSheet(review,this).show(Utils.widgetChatList.getChildFragmentManager(),"satanicthing");
            return true;
        } );

        if (review.getProfilePhoto() != null) {
            var pfp = review.getProfilePhoto();
            try {
                if (pfp.endsWith(".png?size=128"))
                    pfp = pfp.substring(0,pfp.lastIndexOf(".")) + ".webp?size=128";

                holder.icon.setImageURI(pfp);
                var builder = holder.icon.getControllerBuilder();
                builder.m = true;

                holder.icon.setController(builder.a());

            } catch (Exception e) {
                ReviewDB.logger.error(e);}
        }

        holder.message.setText(review.getComment());
        var name = review.getUsername();
        if (review.hasVoting()) {
            if (review.getScore() != 0) name += "  " + (review.getScore() >= 0 ? "+" : "") + review.getScore();
            if (votes.containsKey(review.getId())) name += votes.get(review.getId()) ? " ▲" : " ▼";
        }
        holder.username.setText(name);
        var timestamp = review.getTimestampText();
        holder.timestamp.setText(timestamp);
        holder.timestamp.setVisibility(timestamp.equals("") ? android.view.View.GONE : android.view.View.VISIBLE);
        holder.username.setOnLongClickListener(view -> {

            ClipboardManager clipboard = (ClipboardManager) Utils.getAppActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", review.getSenderDiscordID().toString());
            clipboard.setPrimaryClip(clip);

            Utils.showToast("User ID Copied to clipboard");
            return true;
        });

        if (review.getBadges() != null) {
            holder.badgeLayout.removeAllViews();

            for (Badge badge : review.getBadges()) {
                var view = new RoleIconView(holder.layout.getContext(), badge);
                holder.badgeLayout.addView(view);
                var params = view.getLayoutParams();
                params.height = DimenUtils.dpToPx(18);
                params.width = DimenUtils.dpToPx(18);
                view.setLayoutParams(params);
            }

        }
    }
    public int getReviewID(Review review) {
        return CollectionUtils.findIndex(reviews,review1 -> review==review1);
    }

    public void setVotes(List<ReviewVote> reviewVotes) {
        votes.clear();
        if (reviewVotes == null) return;
        for (ReviewVote vote : reviewVotes) {
            votes.put(vote.reviewID, vote.isUpvote);
        }
        notifyDataSetChanged();
    }

    public void setVote(Review review, boolean isUpvote) {
        Boolean oldVote = votes.get(review.getId());
        if (oldVote != null && oldVote == isUpvote) return;
        if (oldVote == null) review.score += isUpvote ? 1 : -1;
        else review.score += isUpvote ? 2 : -2;
        votes.put(review.getId(), isUpvote);
        var index = getReviewID(review);
        if (index != -1) notifyItemChanged(index);
    }
}
