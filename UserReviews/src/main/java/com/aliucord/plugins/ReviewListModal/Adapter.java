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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Utils;
import com.aliucord.plugins.ReviewBottomSheet;
import com.aliucord.plugins.UserReviews;
import com.aliucord.plugins.UserReviewsView;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.utils.RxUtils;
import com.discord.stores.StoreStream;
import com.discord.utilities.icon.IconUtils;
import com.discord.utilities.images.MGImages;
import com.discord.utilities.rest.RestAPI;
import com.discord.widgets.user.usersheet.WidgetUserSheet;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<ViewHolder> {
    private static final int layoutId = Utils.getResId("widget_chat_list_adapter_item_text", "layout");

    private final List<Review> reviews;

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
                new ReviewBottomSheet(review).show(Utils.widgetChatList.getChildFragmentManager(),"satanicthing");
            return true;
        } );



        if (review.user != null && review.user.getImageURL() != null) {
            MGImages.setImage(holder.icon,IconUtils.getForUser(review.user.getUserID(),review.user.getImageURL()));
        }

        holder.message.setText(review.getComment());
        holder.username.setText(review.getUsername());
    }
}
