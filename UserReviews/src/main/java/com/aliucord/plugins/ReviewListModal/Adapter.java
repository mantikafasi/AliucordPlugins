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
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.CollectionUtils;
import com.aliucord.Utils;
import com.aliucord.plugins.ReviewBottomSheet;
import com.aliucord.plugins.RoleIconView;
import com.aliucord.plugins.UserReviews;
import com.aliucord.plugins.dataclasses.Badge;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.utils.DimenUtils;
import com.discord.stores.StoreStream;
import com.discord.utilities.images.MGImages;
import com.discord.widgets.user.usersheet.WidgetUserSheet;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<ViewHolder> {
    private static final int layoutId = Utils.getResId("widget_chat_list_adapter_item_text", "layout");

    public final List<Review> reviews;

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

        if (review.getisSystemMessage()) {
            holder.showTag();
        }

        holder.icon.setOnClickListener(v -> {
            var user = StoreStream.getUsers().getUsers().get(review.getSenderdiscordid());
            if (user != null && Utils.widgetChatList.isAdded()) {
                WidgetUserSheet.Companion.show(review.getSenderdiscordid(),Utils.widgetChatList.getChildFragmentManager());

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
                pfp = pfp.substring(0,pfp.lastIndexOf(".")) + ".webp?size=128";

                MGImages.setImage(holder.icon,pfp);

            } catch (Exception e) {UserReviews.logger.error(e);}
        }

        holder.message.setText(review.getComment());
        holder.username.setText(review.getUsername());
        holder.username.setOnLongClickListener(view -> {

            ClipboardManager clipboard = (ClipboardManager) Utils.getAppActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", review.getSenderdiscordid().toString());
            clipboard.setPrimaryClip(clip);

            Utils.showToast("User ID Copied to clipboard");
            return true;
        });

        if (review.getBadges() != null) {
            var badgeLayout = new LinearLayout(holder.layout.getContext());

            badgeLayout.setPadding(DimenUtils.dpToPx(4),0,0,0);
            holder.headerLayout.addView(badgeLayout);

            var badgeLayoutLayoutParams = (ConstraintLayout.LayoutParams)badgeLayout.getLayoutParams();
            badgeLayoutLayoutParams.startToEnd = Utils.getResId("chat_list_adapter_item_text_name","id");
            badgeLayout.setLayoutParams(badgeLayoutLayoutParams);

            for (Badge badge : review.getBadges()) {
                var view = new RoleIconView(holder.layout.getContext(), badge);
                badgeLayout.addView(view);
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
}
