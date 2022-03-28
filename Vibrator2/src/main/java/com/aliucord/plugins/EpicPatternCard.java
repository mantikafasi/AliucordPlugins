package com.aliucord.plugins;

import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;

import com.aliucord.widgets.LinearLayout;
import com.google.android.material.card.MaterialCardView;

public class EpicPatternCard extends MaterialCardView {
    public EpicPatternCard(Context ctx) {
        super(ctx);

        LinearLayout root = new LinearLayout(ctx);

        var tw = new TextView(ctx);
        tw.setText("Pattern Name");
        root.addView(tw);

        var et = new EditText(ctx);
        root.addView(et);

        var tw2 = new TextView(ctx);
        tw2.setText("Pattern should be like this pattern 'sleepMS,vibrateMS,sleepMS,vibrateMS...'");
        root.addView(tw2);

        addView(root);
    }
}
