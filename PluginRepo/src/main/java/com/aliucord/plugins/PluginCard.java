package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.Button;
import com.aliucord.views.DangerButton;
import com.aliucord.views.Divider;
import com.aliucord.views.ToolbarButton;
import com.aliucord.widgets.LinearLayout;
import com.discord.utilities.color.ColorCompat;
import com.google.android.material.card.MaterialCardView;
import com.lytefast.flexinput.R;

public class PluginCard extends MaterialCardView {
    public final LinearLayout root;
    public final TextView titleView;
    public final TextView descriptionView;
    public final GridLayout buttonLayout;
    public final Button installButton;
    public final DangerButton uninstallButton;
    public final ToolbarButton repoButton;
    public final ToolbarButton changeLogButton;
    //com.aliucord.widgets.PluginCard

    @SuppressLint("SetTextI18n")
    public PluginCard(Context ctx) {
        super(ctx);
        setRadius(DimenUtils.getDefaultCardRadius());
        setCardBackgroundColor(ColorCompat.getThemedColor(ctx, com.lytefast.flexinput.R.b.colorBackgroundSecondary));
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

        descriptionView = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Addition);
        descriptionView.setPadding(p, p, p, p2);
        root.addView(descriptionView);

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
        repoButton.setImageDrawable(ContextCompat.getDrawable(ctx, com.lytefast.flexinput.R.e.ic_account_github_white_24dp));

        changeLogButton = new ToolbarButton(ctx);
        changeLogButton.setImageDrawable(ContextCompat.getDrawable(ctx, com.lytefast.flexinput.R.e.ic_history_white_24dp));

        buttonLayout.addView(installButton, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(4)));
        buttonLayout.addView(uninstallButton, new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(4)));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(0));
        params.setGravity(Gravity.CENTER_VERTICAL);
        buttonLayout.addView(repoButton, params);
        GridLayout.LayoutParams clparams = new GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(1));
        clparams.setGravity(Gravity.CENTER_VERTICAL);
        buttonLayout.addView(changeLogButton, clparams);

        root.addView(buttonLayout);

        addView(root);
    }
}
