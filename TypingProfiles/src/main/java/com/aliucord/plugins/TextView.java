package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.discord.utilities.color.ColorCompat;

@RequiresApi(api = Build.VERSION_CODES.O)
public class TextView extends androidx.appcompat.widget.AppCompatTextView {
    static LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, DimenUtils.dpToPx(24));
    Typeface typeface = Utils.appActivity.getResources().getFont(Utils.getResId("whitney_medium", "font"));
    int colorId = Utils.getResId("colorTextNormal", "attr");
    public TextView(@NonNull Context context, String text) {
        super(context);
        setTextSize(12);
        setTextColor(ColorCompat.getThemedColor(Utils.appActivity, colorId));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            setTypeface(typeface);
        }

        setGravity(Gravity.CENTER);
        setLayoutParams(textLayoutParams);
        setText(text);

    }
}
