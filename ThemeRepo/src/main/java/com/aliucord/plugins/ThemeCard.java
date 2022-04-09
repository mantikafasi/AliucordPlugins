package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.text.method.LinkMovementMethod;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.helper.widget.Carousel;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.Button;
import com.aliucord.views.DangerButton;
import com.aliucord.views.Divider;
import com.aliucord.views.ToolbarButton;
import com.aliucord.widgets.LinearLayout;
import com.discord.utilities.color.ColorCompat;
import com.google.android.material.card.MaterialCardView;
import com.lytefast.flexinput.R;

public class ThemeCard extends MaterialCardView {
    public final LinearLayout root;
    public final TextView titleView;
    public final GridLayout buttonLayout;
    public final Button installButton;
    public final DangerButton uninstallButton;
    public final ToolbarButton repoButton;
    public final ToolbarButton changeLogButton;
    public final ViewPager screenshotsViewPager;
    //com.aliucord.widgets.ThemeCard

    @SuppressLint("SetTextI18n")
    public ThemeCard(Context ctx) {
        super(ctx);
        setRadius(DimenUtils.getDefaultCardRadius());
        setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundSecondary));
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        int p = DimenUtils.getDefaultPadding();
        int p2 = p / 2;

        root = new LinearLayout(ctx);

        titleView = new TextView(ctx);
        titleView.setTextSize(16.0f);
        titleView.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));
        titleView.setMovementMethod(LinkMovementMethod.getInstance());
        titleView.setTextColor(ColorCompat.getColor(ctx, R.c.primary_dark_200));
        int px = DimenUtils.dpToPx(15);
        titleView.setPadding(px, px, px, px);

        root.addView(titleView);
        root.addView(new Divider(ctx));

        //carousel = new Carousel(ctx);
        //root.addView(carousel);

        buttonLayout = new GridLayout(ctx);
        buttonLayout.setRowCount(1);
        buttonLayout.setColumnCount(5);
        buttonLayout.setUseDefaultMargins(true);
        buttonLayout.setPadding(p2, 0, p2, 0);

        installButton = new Button(ctx);
        installButton.setText("Install");

        uninstallButton = new DangerButton(ctx);
        uninstallButton.setText("Uninstall");
        uninstallButton.setVisibility(GONE);

        repoButton = new ToolbarButton(ctx);
        repoButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.e.ic_account_github_white_24dp));

        changeLogButton = new ToolbarButton(ctx);
        changeLogButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.e.ic_history_white_24dp));

        buttonLayout.addView(installButton, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(4)));
        buttonLayout.addView(uninstallButton, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(4)));


        GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(0));
        params.setGravity(Gravity.CENTER_VERTICAL);
        buttonLayout.addView(repoButton, params);
        GridLayout.LayoutParams clparams = new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(1));
        clparams.setGravity(Gravity.CENTER_VERTICAL);
        buttonLayout.addView(changeLogButton, clparams);

        screenshotsViewPager = new ViewPager(ctx);
        screenshotsViewPager.setPadding(p2,0,p2*3,0);


        root.addView(screenshotsViewPager);
        var layparams = (ViewGroup.LayoutParams)screenshotsViewPager.getLayoutParams();
        Display display = Utils.appActivity.getWindowManager().getDefaultDisplay();
        var size = new Point();
        display.getSize(size);
        var width = size.x - p2*2;
        layparams.height = (int) (1080/width * size.y * 0.8);
        layparams.width = width;

        root.addView(buttonLayout);

        addView(root);
    }
}
