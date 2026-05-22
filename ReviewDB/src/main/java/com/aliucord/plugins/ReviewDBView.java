package com.aliucord.plugins;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
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
    ImageView previousPage;
    TextView pageNumber;
    ImageView nextPage;
    LinearLayout pageControls;
    Long id;
    String titlePrefix;
    int reviewCount = 0;
    int offset = 0;
    int lastPageSize = 0;

    Runnable loadData = (() -> {
        loadPage(0);
    });

    private void loadPage(int newOffset) {
        var data = ReviewDBAPI.getReviewResponse(id, newOffset);
        var rating = ReviewDBAPI.getRating(id);
        var newReviews = new ArrayList<Review>();
        var newReviewCount = 0;
        var newOffsetValue = offset;
        var newLastPageSize = 0;
        if (data != null) {
            if (data.getReviews() != null) newReviews.addAll(data.getReviews());
            newReviewCount = data.getReviewCount();
            newOffsetValue = newOffset;
            newLastPageSize = data.getReviews() == null ? 0 : data.getReviews().size();
        } else {
            newReviews.add(new Review("There was an error while getting reviews", 0L, 0L, -1, ""));
        }

        var finalReviewCount = newReviewCount;
        var finalOffset = newOffsetValue;
        var finalLastPageSize = newLastPageSize;
        var finalHasNextPage = data != null && data.hasNextPage;
        Utils.mainThread.post(() -> {
            reviews.clear();
            reviews.addAll(newReviews);
            reviewCount = finalReviewCount;
            offset = finalOffset;
            lastPageSize = finalLastPageSize;

            if (reviews.size() == 0) nobodyReviewed.setVisibility(VISIBLE);
            else nobodyReviewed.setVisibility(GONE);

            var page = offset / 50 + 1;
            title.setText(titlePrefix + " (" + reviewCount + ", rating " + rating + ")");
            pageNumber.setText("Page " + page);
            setPageButtonState(previousPage, offset > 0);
            setPageButtonState(nextPage, finalHasNextPage);
            adapter.notifyDataSetChanged();
        });

        var token = ReviewDB.staticSettings.getString("token", "");
        if (!token.equals("")) {
            var votes = ReviewDBAPI.getVotes(id, token);
            Utils.mainThread.post(() -> adapter.setVotes(votes));
        }
    }

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
        previousPage = new ImageView(ctx);
        pageNumber = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon);
        nextPage = new ImageView(ctx);
        padding = DimenUtils.getDefaultPadding();
        var reporting = new TextView(ctx);
        var buttonFrameLayout = new FrameLayout(ctx);
        pageControls = new LinearLayout(ctx);

        //etLayout.setGravity(Gravity.CENTER_VERTICAL);
        reporting.setText("Note: To report someone's review, long click the review and click 'Report Review'");
        reporting.setTextSize(9f);

        sendCommentLayout.addView(et);
        sendCommentLayout.addView(buttonFrameLayout);
        sendCommentLayout.setOrientation(HORIZONTAL);
        pageControls.setOrientation(HORIZONTAL);

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
            titlePrefix = "User Reviews";
            title.setText(titlePrefix);
        } else {
            submit.setPadding(padding / 3 * 2, 0, padding / 2, 0);
            nobodyReviewed.setPadding(0,padding/3,0,padding);
            reporting.setPadding(0,padding/3,0,0);
            title.setPadding(0,padding,0,0);
            recycler.setPadding(0,padding,0,0);
            titlePrefix = "Server Reviews";
            title.setText(titlePrefix);
        }

        addView(title);
        addView(reporting);
        addView(recycler);
        addView(nobodyReviewed);
        addView(sendCommentLayout);
        addView(pageControls);

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

        var previousPageIcon = getTintedIcon(ctx, com.lytefast.flexinput.R.e.material_ic_keyboard_arrow_previous_black_24dp);
        var nextPageIcon = getTintedIcon(ctx, com.lytefast.flexinput.R.e.material_ic_keyboard_arrow_next_black_24dp);

        previousPage.setContentDescription("Previous Page");
        previousPage.setImageDrawable(previousPageIcon);
        previousPage.setScaleType(ImageView.ScaleType.CENTER);
        pageNumber.setText("Page 1");
        nextPage.setContentDescription("Next Page");
        nextPage.setImageDrawable(nextPageIcon);
        nextPage.setScaleType(ImageView.ScaleType.CENTER);
        pageNumber.setGravity(android.view.Gravity.CENTER);
        pageControls.setGravity(android.view.Gravity.CENTER_VERTICAL);
        pageControls.setPadding(padding, 0, padding, padding / 2);
        setPageButtonState(previousPage, false);
        setPageButtonState(nextPage, false);
        previousPage.setOnClickListener(v -> Utils.threadPool.execute(() -> loadPage(Math.max(0, offset - 50))));
        nextPage.setOnClickListener(v -> Utils.threadPool.execute(() -> loadPage(offset + 50)));
        pageControls.addView(previousPage);
        pageControls.addView(pageNumber);
        pageControls.addView(nextPage);
        var previousParams = (LinearLayout.LayoutParams) previousPage.getLayoutParams();
        previousParams.width = 0;
        previousParams.height = DimenUtils.dpToPx(40);
        previousParams.weight = 1;
        previousPage.setLayoutParams(previousParams);
        var pageParams = (LinearLayout.LayoutParams) pageNumber.getLayoutParams();
        pageParams.width = 0;
        pageParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        pageParams.weight = 1;
        pageNumber.setLayoutParams(pageParams);
        var nextParams = (LinearLayout.LayoutParams) nextPage.getLayoutParams();
        nextParams.width = 0;
        nextParams.height = DimenUtils.dpToPx(40);
        nextParams.weight = 1;
        nextPage.setLayoutParams(nextParams);

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

    private void setPageButtonState(View button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.35f);
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
