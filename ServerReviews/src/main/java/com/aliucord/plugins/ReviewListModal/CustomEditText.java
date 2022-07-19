package com.aliucord.plugins.ReviewListModal;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.widget.EditText;

import androidx.cardview.widget.CardView;

import com.aliucord.utils.DimenUtils;
import com.discord.utilities.color.ColorCompat;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

public class CustomEditText extends CardView {
    EditText et;

    public CustomEditText(Context context) {
        super(context);
        setRadius(DimenUtils.getDefaultCardRadius());
        et = new EditText(context);
        ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel()
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, DimenUtils.getDefaultCardRadius())
                .build();

        MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable(shapeAppearanceModel);
        shapeDrawable.setFillColor(ColorStateList.valueOf(ColorCompat.getColor(context, com.lytefast.flexinput.R.c.mtrl_textinput_default_box_stroke_color)));
        et.setBackgroundDrawable(shapeDrawable);
        et.setTextSize(16f);
        et.setPadding(DimenUtils.getDefaultPadding() / 2 , 0, 0, 0);
        addView(et);
    }

    public void setHint(CharSequence text) {
        et.setHint(text);
    }

    public Editable getText() {
        return et.getText();
    }

    public void setText(String s) {
        et.setText(s);
    }
}
