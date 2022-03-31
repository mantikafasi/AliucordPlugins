package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.DangerButton;
import com.aliucord.widgets.LinearLayout;
import com.discord.utilities.color.ColorCompat;
import com.discord.views.CheckedSetting;
import com.google.android.material.card.MaterialCardView;

public class EpicPatternCard extends MaterialCardView {
    DangerButton deleteButton;
    EditText titleView;
    EditText patternView;
    CheckedSetting checkbox;
    Button button;
    Context ctx;

    public EpicPatternCard(Context ctx) {
        super(ctx);
        this.ctx = ctx;
        LinearLayout root = new LinearLayout(ctx);

        titleView = new EditText(ctx);
        titleView.setBackgroundResource(android.R.color.transparent);
        titleView.setTextSize(16.0f);
        titleView.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));
        titleView.setMovementMethod(LinkMovementMethod.getInstance());
        titleView.setTextColor(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.primary_dark_200));
        int px = DimenUtils.dpToPx(15);
        titleView.setPadding(px, px, 0, 0);

        root.addView(titleView);


        patternView = new EditText(ctx);
        patternView.setTextColor(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.primary_dark_200));
        patternView.setPadding(px,0,0,0);
        patternView.setBackgroundResource(android.R.color.transparent);
        patternView.setHintTextColor(ContextCompat.getColor(ctx, com.lytefast.flexinput.R.c.grey_2));
        patternView.setHint("sleepMS,vibrateMS,sleepMS,vibrateMS...'");
        patternView.setTextSize(13f);

        root.addView(patternView);

        checkbox = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.CHECK,"Repeat","");

        button = new Button(ctx);
        button.setText("Run");

        deleteButton = new DangerButton(ctx);
        deleteButton.setText("Delete Pattern");

        root.addView(checkbox);
        root.addView(button);
        root.addView(deleteButton);
        addView(root);
    }

    public void configure(Pattern pattern){
        titleView.setText(pattern.patternName);
        titleView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                pattern.patternName = s.toString();
                Vibrator.savePattern(pattern);
            }
        });

        patternView.addTextChangedListener(new TextWatcher() {
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
                        patternView.setTextColor(Color.parseColor("#FF0000"));
                        break;
                    }
                    patternView.setTextColor(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.primary_dark_200));

                }
                pattern.patternData = patt;
                Vibrator.savePattern(pattern);
            }
        });

        button.setOnClickListener(v -> {
            try {
                if (pattern.patternData == null) {
                    Utils.showToast("Insert a pattern");
                } else {
                    Vibrator.vibrate(pattern.patternData,checkbox.isChecked());
                }
            } catch (Exception e) { Utils.showToast("An Error Occured"); }
        });

        if (pattern.patternData != null) {
            String b = "";
            for (long a :pattern.patternData) {
                b += a + ",";
            }
            b = b.substring(0,b.length()-1);
            patternView.setText(b);
        }

        checkbox.setOnCheckedListener(aBoolean -> {
            pattern.repeat = aBoolean;
            Vibrator.savePattern(pattern);
        });
        checkbox.setChecked(pattern.repeat);


    }
}
