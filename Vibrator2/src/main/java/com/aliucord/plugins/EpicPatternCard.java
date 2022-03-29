package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.aliucord.widgets.LinearLayout;
import com.discord.utilities.color.ColorCompat;
import com.discord.views.CheckedSetting;
import com.google.android.material.card.MaterialCardView;

public class EpicPatternCard extends MaterialCardView {
    public EpicPatternCard(Context ctx,Pattern pattern) {
        super(ctx);

        LinearLayout root = new LinearLayout(ctx);

        var tw = new EditText(ctx); //titleview(actually i named it textview at first then after changing it to edittext I was too lazy to rename it so its now short for titleview)
        tw.setBackgroundResource(android.R.color.transparent);
        tw.setTextSize(16.0f);
        tw.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));
        tw.setMovementMethod(LinkMovementMethod.getInstance());
        tw.setTextColor(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.primary_dark_200));
        int px = DimenUtils.dpToPx(15);
        tw.setPadding(px, px, 0, 0);
        tw.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                pattern.patternName = s.toString();
                Vibrator.savePattern(pattern);
            }
        });


        tw.setText(pattern.patternName);
        root.addView(tw);

        var et = new EditText(ctx);
        et.setTextColor(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.primary_dark_200));
        et.setPadding(px,0,0,0);
        if (pattern.patternData != null) {
            String b = "";
            for (long a :pattern.patternData) {
                b += String.valueOf(a) + ",";
            }
            b = b.substring(0,b.length()-1);
            et.setText(b);
        }

        et.setBackgroundResource(android.R.color.transparent);
        et.setHint("sleepMS,vibrateMS,sleepMS,vibrateMS...'");
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                var pat = s.toString().split(",");
                long[] patt = new long[pat.length];
                for (int i = 0 ; i<pat.length;i++) {
                    var a = pat[i];
                    try {
                        patt[i] = Long.parseLong(a);
                    } catch (Exception e) {
                        et.setTextColor(Color.parseColor("#FF0000"));
                        break;
                    }
                    et.setTextColor(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.primary_dark_200));

                }
                pattern.patternData = patt;
                Vibrator.savePattern(pattern);
            }
        });
        root.addView(et);

        var checkbox = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.CHECK,"Repeat","");
        checkbox.setChecked(pattern.repeat);
        checkbox.setOnCheckedListener(aBoolean -> {
            pattern.repeat = aBoolean;
            Vibrator.savePattern(pattern);
        });

        var button = new Button(ctx);
        button.setText("Run");
        button.setOnClickListener(v -> {
            Vibrator.vibrate(pattern.patternData,checkbox.isChecked());
        });


        root.addView(checkbox);
        root.addView(button);
        addView(root);
    }
}
