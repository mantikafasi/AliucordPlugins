package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.utils.DimenUtils;
import com.aliucord.widgets.LinearLayout;
import com.discord.utilities.color.ColorCompat;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ReviewRecyclerAdapter extends RecyclerView.Adapter<ReviewRecyclerAdapter.ViewHolder>{
    List<Review> reviewList;
    public ReviewRecyclerAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(this,new ReviewCard(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.card.configureCard(reviewList.get(position));
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {
        ReviewCard card;
        @SuppressLint("SetTextI18n")
        public ViewHolder(ReviewRecyclerAdapter adapter, ReviewCard card) {
            super(card);
            this.card = card;
        }
    }

    static class ReviewCard extends MaterialCardView {
        public final LinearLayout root;
        public final TextView username;
        public final TextView comment;

        public ReviewCard(Context ctx) {
            super(ctx);
            setRadius(DimenUtils.getDefaultCardRadius());
            setCardBackgroundColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundSecondary));
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            root = new LinearLayout(ctx);
            username = new TextView(ctx);
            comment = new TextView(ctx);

            root.addView(username);
            root.addView(comment);
        }
        public void configureCard(Review review) {
            username.setText(review.getUsername());
            comment.setText(review.getComment());
        }
    }

}


