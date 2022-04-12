package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.text.method.LinkMovementMethod;
import android.view.Display;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.GridLayout;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.viewpager.widget.ViewPager;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.Button;
import com.aliucord.views.DangerButton;
import com.aliucord.views.ToolbarButton;
import com.aliucord.widgets.LinearLayout;
import com.discord.utilities.color.ColorCompat;
import com.discord.views.CheckedSetting;
import com.google.android.material.card.MaterialCardView;
import com.lytefast.flexinput.R;

public class ThemeCard extends MaterialCardView {
    public final LinearLayout root;
    public final CheckedSetting titleView;
    public final GridLayout buttonLayout;
    public final Button installButton;
    public final DangerButton uninstallButton;
    public final ToolbarButton transparencyIcon;
    public final ViewPager screenshotsViewPager;

    @SuppressLint("SetTextI18n")
    public ThemeCard(Context ctx) {
        super(ctx);
        setRadius(DimenUtils.getDefaultCardRadius());
        setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundSecondary));
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        int p = DimenUtils.getDefaultPadding();
        int p2 = p / 2;

        root = new LinearLayout(ctx);
        titleView = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.SWITCH, "", "");

        var titleTextView = titleView.l.a();
        titleTextView.setTextSize(16.0f);
        titleTextView.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));
        titleTextView.setMovementMethod(LinkMovementMethod.getInstance());
        titleView.setTextColor(ColorCompat.getColor(ctx, R.c.primary_dark_200));

        root.addView(titleView);

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

        transparencyIcon = new ToolbarButton(ctx);
        transparencyIcon.setImageDrawable(ContextCompat.getDrawable(ctx, R.e.ic_search));
        transparencyIcon.setOnClickListener(v -> Utils.showToast("Full Transparent Theme"));
        transparencyIcon.setVisibility(GONE);

        buttonLayout.addView(installButton, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(4)));
        buttonLayout.addView(uninstallButton, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(4)));


        GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(0));
        params.setGravity(Gravity.CENTER_VERTICAL);
        buttonLayout.addView(transparencyIcon, params);

        screenshotsViewPager = new ViewPager(ctx);
        screenshotsViewPager.setPadding(p2, 0, p2 * 3, 0);


        root.addView(screenshotsViewPager);
        var layparams = (ViewGroup.LayoutParams) screenshotsViewPager.getLayoutParams();
        Display display = Utils.appActivity.getWindowManager().getDefaultDisplay();
        var size = new Point();
        display.getSize(size);
        var width = size.x - p2 * 2;
        layparams.height = (int) (1080 / width * size.y * 0.8);
        layparams.width = width;

        root.addView(buttonLayout);

        addView(root);
    }
}
