package com.aliucord.plugins;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.InsteadHook;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.user.usersheet.WidgetUserSheet;
import com.discord.widgets.user.usersheet.WidgetUserSheetViewModel;

import org.w3c.dom.Text;

import java.util.Collections;
import com.lytefast.flexinput.R;
@SuppressWarnings("unused")
@AliucordPlugin
public class UserReviews extends Plugin {
    public static SettingsAPI staticSettings;
    int viewID = View.generateViewId();
    @Override
    public void start(Context context) {
        staticSettings = settings;
        settingsTab = new SettingsTab(BottomShit.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);
        try {
            patcher.patch(WidgetUserSheet.class.getDeclaredMethod("configureUI", WidgetUserSheetViewModel.ViewState.class),new Hook(cf ->{
                var viewstate = (WidgetUserSheetViewModel.ViewState.Loaded)cf.args[0];
                var thisobj = (WidgetUserSheet)cf.thisObject;
                var binding = WidgetUserSheet.access$getBinding$p(thisobj);
                var scrollView = (NestedScrollView)binding.getRoot();
                var ctx = scrollView.getContext();
                var userid = viewstate.getUser().getId();
                if (scrollView.findViewById(viewID) == null) {
                    var root = new LinearLayout(ctx);
                    root.setOrientation(LinearLayout.VERTICAL);
                    var title = new TextView(ctx,null, R.i.UiKit_Settings_Item_Header);
                    title.setText("User Reviews");

                    RecyclerView recycler = new RecyclerView(ctx);
                    recycler.setLayoutManager(new LinearLayoutManager(ctx, RecyclerView.VERTICAL, false));
                    var et = new EditText(ctx);
                    et.setHintTextColor(ColorCompat.getColor(ctx,Utils.getResId("primary_dark_200","color")));
                    et.setHint("Enter Your Comment ");
                    Utils.threadPool.execute(() -> {
                        ReviewRecyclerAdapter adapter = new ReviewRecyclerAdapter(UserReviewsAPI.getReviews(userid));
                        Utils.mainThread.post(() -> {
                            recycler.setAdapter(adapter);
                        });
                    });

                    root.addView(title);
                    root.addView(recycler);
                    root.addView(et);

                    var submit = new Button(ctx);
                    submit.setText("Submit");
                    submit.setOnClickListener(v -> {
                        Utils.threadPool.execute(() -> {
                            Utils.showToast(UserReviewsAPI.addReview(et.getText().toString(),userid,settings.getString("token","")));
                        });
                    });

                    root.addView(submit);
                    et.setOnKeyListener((v, keyCode, event) -> {
                        return true;
                    });

                    root.setId(viewID);
                    var LL = (LinearLayout)scrollView.findViewById(Utils.getResId("user_sheet_content","id"));
                    LL.addView(root);

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
