package com.aliucord.plugins.filtering;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.utils.DimenUtils;
import com.discord.utilities.color.ColorCompat;

public class AdapterItem extends RelativeLayout {

    public TextView textView;
    public Spinner spinner;
    public FilterAdapter.FilterType type;

    public AdapterItem(Context context) {
        super(context);
        textView = new TextView(context);
        int px = DimenUtils.dpToPx(15);

        textView.setTextColor(ColorCompat.getColor(context, com.lytefast.flexinput.R.c.primary_dark_200));
        textView.setTextSize(16.0f);
        textView.setPadding(px, px, px, px);
        textView.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        spinner = new Spinner(context);
        //spinner.setPopupBackgroundResource(com.lytefast.flexinput.R.c.primary_dark_600);

        addView(textView);
        addView(spinner);

        var params = ((RelativeLayout.LayoutParams) spinner.getLayoutParams());
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        //params.width = LayoutParams.FILL_PARENT;
        spinner.setLayoutParams(params);
    }
}
