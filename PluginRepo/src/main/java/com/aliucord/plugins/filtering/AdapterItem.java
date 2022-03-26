package com.aliucord.plugins.filtering;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.discord.utilities.color.ColorCompat;
import com.discord.views.CheckedSetting;

public class AdapterItem extends RelativeLayout {

    public TextView textView;
    public FilterAdapter.FilterType type;
    public View setting;
    public int viewType; //-1,0 is spinner,1 is checkbox

    public AdapterItem(Context context) {
        this(context, 0);
    }

    public AdapterItem(Context context, int viewType) {
        super(context);
        this.viewType = viewType;
        textView = new TextView(context);
        int px = DimenUtils.dpToPx(15);

        textView.setTextColor(ColorCompat.getColor(context, com.lytefast.flexinput.R.c.primary_dark_200));
        textView.setTextSize(16.0f);
        textView.setPadding(px, px, px, px);
        textView.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        addView(textView);

        switch (viewType) {
            case -1:
            case 0:
                setting = new Spinner(context);
                break;
            case 1:
                setting = Utils.createCheckedSetting(context, CheckedSetting.ViewType.CHECK, "", "");
                ((CheckedSetting) setting).setChecked(true);

                break;
        }
        addView(setting);


        var params = ((RelativeLayout.LayoutParams) setting.getLayoutParams());
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        setting.setLayoutParams(params);
    }
}
