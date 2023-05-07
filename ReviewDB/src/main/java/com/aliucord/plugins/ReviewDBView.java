package com.aliucord.plugins;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.CollectionUtils;
import com.aliucord.Utils;
import com.aliucord.plugins.ReviewListModal.Adapter;
import com.aliucord.plugins.ReviewListModal.CustomEditText;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.utils.DimenUtils;
import com.aliucord.widgets.LinearLayout;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;
import java.util.ArrayList;
import java.util.List;

public class ReviewDBView extends LinearLayout {
    Adapter adapter;
    List<Review> reviews = new ArrayList<>();
    CustomEditText et;
    ImageView submit;
    int padding;
    LinearLayout sendCommentLayout;
    RecyclerView recycler;
    TextView title;
    TextView nobodyReviewed;
    Long id;

    Runnable loadData = (() -> {

        reviews.clear();
        var data = ReviewDBAPI.getReviews(id);
        if (data != null) {
            reviews.addAll(data);
        } else {
            reviews.clear();
            reviews.add(new Review("There was an error while getting reviews", 0L, 0L, -1, ""));
        }

        Utils.mainThread.post(() -> {
            if (reviews.size() == 0) nobodyReviewed.setVisibility(VISIBLE);
            else nobodyReviewed.setVisibility(GONE);

            adapter.notifyDataSetChanged();
        });

    });

    public enum PaddingType {
        User,
        Server
    }

    public ReviewDBView(Context ctx, Long id) {
        this(ctx, id, PaddingType.User);
    }
    public ReviewDBView(Context ctx, Long id, PaddingType paddingType) {
        super(ctx);
        setOrientation(android.widget.LinearLayout.VERTICAL);
        this.id = id;

        title = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UserProfile_Section_Header);
        recycler = new RecyclerView(ctx);
        sendCommentLayout = new LinearLayout(ctx);
        et = new CustomEditText(ctx);
        submit = new ImageView(ctx);
        nobodyReviewed = new TextView(ctx);
        padding = DimenUtils.getDefaultPadding();
        var reporting = new TextView(ctx);
        var buttonFrameLayout = new FrameLayout(ctx);

        //etLayout.setGravity(Gravity.CENTER_VERTICAL);
        reporting.setText("Note: To report someone's review, long click the review and click 'Report Review'");
        reporting.setTextSize(9f);

        sendCommentLayout.addView(et);
        sendCommentLayout.addView(buttonFrameLayout);
        sendCommentLayout.setOrientation(HORIZONTAL);

        nobodyReviewed.setText("Looks like nobody has reviewed this user: you can be first");
        nobodyReviewed.setVisibility(GONE);
        nobodyReviewed.setTypeface(null, Typeface.BOLD_ITALIC);
        nobodyReviewed.setTextSize(20f);

        if (paddingType == PaddingType.User) {
            submit.setPadding(padding / 2, 0, padding / 2, 0);
            reporting.setPadding(padding,padding/3,padding,padding);
            sendCommentLayout.setPadding(padding/3*2,0,padding,0);
            nobodyReviewed.setPadding(padding, 0, padding, padding);
            title.setPadding(padding, padding, 0, 0);
            recycler.setPadding(padding/2,0,0,0);
            title.setText("User Reviews");
        } else {
            submit.setPadding(padding / 3 * 2, 0, padding / 2, 0);
            nobodyReviewed.setPadding(0,padding/3,0,padding);
            reporting.setPadding(0,padding/3,0,0);
            title.setPadding(0,padding,0,0);
            recycler.setPadding(0,padding,0,0);
            title.setText("Server Reviews");
        }

        addView(title);
        addView(reporting);
        addView(recycler);
        addView(nobodyReviewed);
        addView(sendCommentLayout);

        var etLayoutParams = (android.widget.LinearLayout.LayoutParams) et.getLayoutParams();
        etLayoutParams.width = 0;
        etLayoutParams.height = DimenUtils.dpToPx(40);
        etLayoutParams.weight = 1;
        etLayoutParams.rightMargin = padding / 3;
        et.setLayoutParams(etLayoutParams);

        var buttonLayoutParams = (LinearLayout.LayoutParams) buttonFrameLayout.getLayoutParams();
        buttonLayoutParams.width = DimenUtils.dpToPx(40);
        buttonLayoutParams.height = DimenUtils.dpToPx(40);
        buttonFrameLayout.setLayoutParams(buttonLayoutParams);


        recycler.setLayoutManager(new LinearLayoutManager(ctx, RecyclerView.VERTICAL, false));
        adapter = new com.aliucord.plugins.ReviewListModal.Adapter(reviews);
        recycler.setAdapter(adapter);

        Utils.threadPool.execute(loadData);

        et.setHint("Enter Your Comment ");
        et.setBackgroundResource(android.R.color.transparent);

        buttonFrameLayout.setBackgroundResource(Utils.getResId("drawable_circle_black", "drawable"));

        buttonFrameLayout.setBackgroundTintList(ColorStateList.valueOf(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.accent_material_light)));
        buttonFrameLayout.addView(submit);
        buttonFrameLayout.setOnClickListener(this::onSubmit);
        submit.setImageResource(Utils.getResId("ic_send_24dp", "drawable"));
    }

    public void onSubmit(View v) {
        var message = et.getText().toString().trim();

        if (ReviewDB.staticSettings.getString("token", "").equals("")) {
            Utils.showToast("You need to authorize to send a comment");
            ReviewDBAPI.authorize();

        } else {
            if (message.isEmpty()) {
                Utils.showToast("Enter a comment and try again");
                return;
            }
            else if(message.length()>500) {
                Utils.showToast("Comment Too Long");
                return;
            }

            et.clearFocus();
            Utils.threadPool.execute(() -> {
                var response = ReviewDBAPI.addReview(message, id, ReviewDB.staticSettings.getString("token", ""));
                Utils.showToast(response.getMessage());

                if (response.isSuccessful()) {
                    var currentUsername = StoreStream.getUsers().getMe().getUsername() + "#" + StoreStream.getUsers().getMe().getDiscriminator();
                    var currentUserID = StoreStream.getUsers().getMe().getId();
                    Utils.mainThread.post(() -> {
                        et.setText("");

                        if (response.isUpdated()) {
                            var ix = CollectionUtils.findIndex(reviews, review -> review.getSenderDiscordID() == currentUserID);
                            if (ix == -1) return;
                            var rev = reviews.get(ix);
                            rev.comment = message;
                            reviews.set(ix, rev);
                            adapter.notifyItemChanged(ix);
                        } else {
                            reviews.add(0, new Review(message, 0L, currentUserID, -1, currentUsername));
                            adapter.notifyItemInserted(0);
                            nobodyReviewed.setVisibility(GONE);
                        }
                    });
                } else {
                    Utils.showToast("An Error Occured");
                }
            });

        }

    }

}
