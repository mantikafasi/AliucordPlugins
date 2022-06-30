package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.CollectionUtils;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.utils.DimenUtils;
import com.discord.stores.StoreStream;
import com.discord.widgets.user.usersheet.WidgetUserSheet;
import com.discord.widgets.user.usersheet.WidgetUserSheetViewModel;
import com.lytefast.flexinput.R;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@AliucordPlugin
public class UserReviews extends Plugin {
    public static SettingsAPI staticSettings;
    int viewID = View.generateViewId();
    List<Review> reviews = new ArrayList<>();
    Runnable loadData;

    @SuppressLint("SetTextI18n")
    @Override
    public void start(Context context) {
        staticSettings = settings;
        settingsTab = new SettingsTab(BottomShit.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);
        try {
            patcher.patch(WidgetUserSheet.class.getDeclaredMethod("configureAboutMe", WidgetUserSheetViewModel.ViewState.Loaded.class), new Hook(cf -> {
                var viewstate = (WidgetUserSheetViewModel.ViewState.Loaded) cf.args[0];
                var thisobj = (WidgetUserSheet) cf.thisObject;
                var binding = WidgetUserSheet.access$getBinding$p(thisobj);
                var scrollView = (NestedScrollView) binding.getRoot();
                var ctx = scrollView.getContext();
                var userid = viewstate.getUser().getId();
                if (scrollView.findViewById(viewID) == null) {

                    var root = new LinearLayout(ctx);
                    var title = new TextView(ctx, null, R.i.UserProfile_Section_Header);
                    var recycler = new RecyclerView(ctx);
                    var sendCommentLayout = new LinearLayout(ctx);
                    var et = new EditText(ctx);
                    var submit = new Button(ctx);
                    var padding = DimenUtils.getDefaultPadding();

                    sendCommentLayout.addView(et);
                    sendCommentLayout.addView(submit);

                    root.setOrientation(LinearLayout.VERTICAL);

                    title.setText("User Reviews");
                    title.setPadding(0, padding, 0, padding);

                    recycler.setLayoutManager(new LinearLayoutManager(ctx, RecyclerView.VERTICAL, false));
                    var adapter = new com.aliucord.plugins.clonemodal.Adapter(reviews);
                    recycler.setAdapter(adapter);


                    loadData = (() -> {
                        reviews.clear();
                        var data = UserReviewsAPI.getReviews(userid);
                        if (data != null) {
                            for (int i = 0;i<reviews.size();i++) {
                                var review = data.get(i);
                                if (review.user == null) {
                                    review.user = StoreStream.getUsers().getUsers().get(review.getSenderDiscordID());
                                    data.set(i,review);
                                }
                            }

                            reviews.addAll(data);
                        } else {
                            reviews.clear();
                            reviews.add(new Review("There was an error while getting reviews", 0L, 0L, -1, ""));
                        }
                        Utils.mainThread.post(() -> {
                            adapter.notifyDataSetChanged();
                            et.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                        });
                    });
                    new Thread(loadData).start();

                    et.setTextColor(ContextCompat.getColor(ctx, R.c.primary_000));
                    et.setHintTextColor(ContextCompat.getColor(ctx, R.c.grey_2));
                    et.setTextSize(16f);
                    et.setHint("Enter Your Comment ");

                    submit.setText("Submit");
                    submit.setTextSize(13f);
                    submit.setOnClickListener(v -> {
                        var message = et.getText().toString();

                        if (settings.getString("token", "").equals("")) {
                            Utils.showToast("You need to authorazite to send comment");
                            Utils.openPageWithProxy(Utils.getAppActivity(), new AuthorazationPage());
                        } else {
                            et.clearFocus();
                            et.setInputType(EditorInfo.TYPE_NULL);
                            Utils.threadPool.execute(() -> {
                                var response = UserReviewsAPI.addReview(message, userid, settings.getString("token", ""));
                                Utils.showToast(response.getText());
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
                                        }
                                        et.setInputType(EditorInfo.TYPE_CLASS_TEXT);

                                    });
                                } else {
                                    Utils.showToast("An Error Occured");
                                }
                            });

                        }

                    });


                    root.addView(title);
                    root.addView(recycler);
                    root.addView(sendCommentLayout);
                    root.setId(viewID);
                    root.setPadding(padding, padding, padding, padding);

                    var layParams = (LinearLayout.LayoutParams) et.getLayoutParams();
                    layParams.weight = 1;
                    et.setLayoutParams(layParams);

                    ((LinearLayout) scrollView.findViewById(Utils.getResId("user_sheet_content", "id"))).addView(root);
                }

            } ));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
