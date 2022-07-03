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
import com.aliucord.utils.RxUtils;
import com.aliucord.widgets.LinearLayout;
import com.discord.models.user.CoreUser;
import com.discord.models.user.User;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;
import com.discord.utilities.rest.RestAPI;

import java.util.ArrayList;
import java.util.List;

public class UserReviewsView extends LinearLayout {
    Adapter adapter;
    List<Review> reviews = new ArrayList<>();
    CustomEditText et;
    ImageView submit;
    int padding;
    LinearLayout sendCommentLayout;
    RecyclerView recycler;
    TextView title;
    TextView nobodyReviewed;
    User user;
    Cache cache = new Cache();
    Runnable loadData = (() -> {

        reviews.clear();
        var data = UserReviewsAPI.getReviews(user.getId());

        if (data != null) {
            for (int i = 0; i < data.size(); i++) {

                var review = data.get(i);
                if (cache.isCached(review.getSenderdiscordid()))
                    review.user = cache.getCached(review.getSenderdiscordid());
                if (review.user == null || review.user.getImageURL() == null) {

                    var discordUser = StoreStream.getUsers().getUsers().get(review.getSenderdiscordid());
                    if (discordUser != null) {
                        review.discordUser = discordUser;
                        review.user = new com.aliucord.plugins.dataclasses.User(discordUser.getId(), discordUser.getAvatar(), discordUser.getUsername() + "#" + discordUser.getDiscriminator());
                    }

                    data.set(i, review);
                }
            }
            reviews.addAll(data);
            fetchUsersAndUpdateRecyclerView();
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

    public UserReviewsView(Context ctx, User user) {
        super(ctx);
        setOrientation(android.widget.LinearLayout.VERTICAL);
        this.user = user;

        title = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UserProfile_Section_Header);
        recycler = new RecyclerView(ctx);
        sendCommentLayout = new LinearLayout(ctx);
        et = new CustomEditText(ctx);
        submit = new ImageView(ctx);
        nobodyReviewed = new TextView(ctx);
        padding = DimenUtils.getDefaultPadding();
        var buttonFrameLayout = new FrameLayout(ctx);

        //etLayout.setGravity(Gravity.CENTER_VERTICAL);

        sendCommentLayout.addView(et);
        sendCommentLayout.addView(buttonFrameLayout);
        sendCommentLayout.setOrientation(HORIZONTAL);

        nobodyReviewed.setText("Looks like nobody reviewed this user, You can be first");
        nobodyReviewed.setVisibility(GONE);
        nobodyReviewed.setPadding(0, 0, 0, padding);
        nobodyReviewed.setTypeface(null, Typeface.BOLD_ITALIC);
        nobodyReviewed.setTextSize(20f);

        addView(title);
        addView(recycler);
        addView(nobodyReviewed);
        addView(sendCommentLayout);
        setPadding(padding, padding, padding, padding);

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

        title.setText("User Reviews");
        title.setPadding(0, padding, 0, padding);

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
        submit.setPadding(padding / 2, 0, padding / 2, 0);


    }

    public void fetchUsersAndUpdateRecyclerView() {
        Utils.threadPool.execute(()->{
            for (int i = 0; i < reviews.size(); i++) {
                //fetching users that are not cached and updating recyclerview
                var review = reviews.get(i);
                if (review.user == null || review.user.getImageURL() == null) {
                    int index = i;

                    RxUtils.subscribe(RestAPI.getApi().userGet(review.getSenderdiscordid()), user1 -> {

                        StoreStream.access$getDispatcher$p(StoreStream.getNotices().getStream()).schedule(() -> {
                            StoreStream.getUsers().handleUserUpdated(user1);
                            return null;
                        });
                        var user = new CoreUser(user1);

                        review.discordUser = user;
                        review.user = new com.aliucord.plugins.dataclasses.User(user.getId(), user.getAvatar(), user.getUsername() + "#" + user.getDiscriminator());

                        cache.setUserCache(user.getId(), review.user);

                        reviews.set(index, review);

                        Utils.mainThread.post(() -> {
                            adapter.notifyItemChanged(index);
                        });
                        return null;
                    });
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    public void onSubmit(View v) {
        var message = et.getText().toString().trim();

        if (UserReviews.staticSettings.getString("token", "").equals("")) {
            Utils.showToast("You need to authorazite to send comment");
            Utils.openPageWithProxy(Utils.getAppActivity(), new AuthorazationPage());
        } else {
            if (message.isEmpty()) {
                Utils.showToast("Enter some comment and try again");
                return;
            }
            else if(message.length()>1000) {
                Utils.showToast("Comment Too Long");
                return;
            }

            submit.setClickable(false);
            et.clearFocus();
            Utils.threadPool.execute(() -> {
                var response = UserReviewsAPI.addReview(message, user.getId(), UserReviews.staticSettings.getString("token", ""));
                Utils.showToast(response.getText());
                Utils.mainThread.post(() -> submit.setClickable(true));

                if (response.isSuccessful()) {
                    var currentUsername = StoreStream.getUsers().getMe().getUsername() + "#" + StoreStream.getUsers().getMe().getDiscriminator();
                    var currentUserID = StoreStream.getUsers().getMe().getId();
                    Utils.mainThread.post(() -> {
                        et.setText("");

                        if (response.isUpdated()) {
                            var ix = CollectionUtils.findIndex(reviews, review -> review.getSenderdiscordid() == currentUserID);
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
