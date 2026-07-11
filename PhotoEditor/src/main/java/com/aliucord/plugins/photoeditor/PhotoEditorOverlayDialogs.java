package com.aliucord.plugins.photoeditor;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ja.burhanrashid52.photoeditor.PhotoEditorView;

final class PhotoEditorOverlayDialogs {
    private PhotoEditorOverlayDialogs() {}

    static void showOptions(PhotoEditorPlugin owner, Context context, View viewToRemove, android.view.ViewGroup parent) {
        android.app.Dialog dialog = new android.app.Dialog(context);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(owner.dp(24), owner.dp(24), owner.dp(24), owner.dp(24));

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xff313338);
        bg.setCornerRadius(owner.dp(16));
        layout.setBackground(bg);

        android.widget.TextView title = new android.widget.TextView(context);
        title.setText("Overlay Options");
        title.setTextColor(android.graphics.Color.WHITE);
        title.setTextSize(20f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, owner.dp(12));

        android.widget.TextView message = new android.widget.TextView(context);
        message.setText("What would you like to do with this item?");
        message.setTextColor(android.graphics.Color.parseColor("#dbdee1"));
        message.setTextSize(16f);
        message.setPadding(0, 0, 0, owner.dp(24));

        android.widget.LinearLayout buttons = new android.widget.LinearLayout(context);
        buttons.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        buttons.setGravity(android.view.Gravity.END);
        buttons.setPadding(0, owner.dp(8), 0, 0);

        android.widget.TextView cancel = new android.widget.TextView(context);
        cancel.setText("Cancel");
        cancel.setTextColor(android.graphics.Color.WHITE);
        cancel.setTextSize(14f);
        cancel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        cancel.setPadding(owner.dp(20), owner.dp(10), owner.dp(20), owner.dp(10));
        cancel.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancel);

        if (viewToRemove instanceof android.widget.ImageView) {
            android.widget.ImageView iv = (android.widget.ImageView) viewToRemove;
            if (iv.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
                android.widget.TextView crop = new android.widget.TextView(context);
                crop.setText("Crop");
                crop.setTextColor(android.graphics.Color.WHITE);
                crop.setTextSize(14f);
                crop.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                crop.setPadding(owner.dp(24), owner.dp(10), owner.dp(24), owner.dp(10));

                android.graphics.drawable.GradientDrawable cropBg = new android.graphics.drawable.GradientDrawable();
                cropBg.setColor(0xff5865f2);
                cropBg.setCornerRadius(owner.dp(6));
                crop.setBackground(cropBg);

                android.widget.LinearLayout.LayoutParams cropParams = new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cropParams.setMargins(owner.dp(8), 0, owner.dp(8), 0);
                crop.setLayoutParams(cropParams);

                crop.setOnClickListener(v -> {
                    dialog.dismiss();
                    PhotoEditorCropDialog.show(owner, context, iv, (PhotoEditorView) parent);
                });
                buttons.addView(crop);
            }
        }

        android.widget.TextView delete = new android.widget.TextView(context);
        delete.setText("Delete");
        delete.setTextColor(android.graphics.Color.WHITE);
        delete.setTextSize(14f);
        delete.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        delete.setPadding(owner.dp(24), owner.dp(10), owner.dp(24), owner.dp(10));

        android.graphics.drawable.GradientDrawable deleteBg = new android.graphics.drawable.GradientDrawable();
        deleteBg.setColor(0xffda373c);
        deleteBg.setCornerRadius(owner.dp(6));
        delete.setBackground(deleteBg);

        android.widget.LinearLayout.LayoutParams delParams = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        delParams.setMargins(owner.dp(8), 0, 0, 0);
        delete.setLayoutParams(delParams);

        delete.setOnClickListener(v -> {
            parent.removeView(viewToRemove);
            dialog.dismiss();
        });
        buttons.addView(delete);

        layout.addView(title);
        layout.addView(message);
        layout.addView(buttons);

        dialog.setContentView(layout);
        android.view.Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            win.setLayout((int) (android.content.res.Resources.getSystem().getDisplayMetrics().widthPixels * 0.9f), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }


    static void showTextEditor(PhotoEditorPlugin owner, Context context, TextView target) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = owner.createBottomSheetContainer(context, "Edit Text");
        EditText input = owner.dialogInput(context, "Enter your text...");
        input.setText(target.getText());
        input.setSelectAllOnFocus(false);
        input.setSelection(input.getText().length());

        android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
        inputBg.setColor(0xff1e1f22);
        inputBg.setCornerRadius(owner.dp(8));
        inputBg.setStroke(owner.dp(1), Color.parseColor("#3f4147"));
        input.setBackground(inputBg);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#80848e"));
        input.setTextSize(16f);
        input.setMinLines(2);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(owner.dp(12), owner.dp(12), owner.dp(12), owner.dp(12));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, owner.dp(16));
        layout.addView(input, inputParams);

        final int[] chosenColor = {target.getCurrentTextColor()};
        TextView colorLabel = new TextView(context);
        colorLabel.setText("Color");
        colorLabel.setTextColor(Color.parseColor("#b5bac1"));
        colorLabel.setTextSize(12f);
        colorLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        colorLabel.setPadding(0, 0, 0, owner.dp(8));
        layout.addView(colorLabel);

        LinearLayout colorRow = new LinearLayout(context);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setGravity(Gravity.CENTER_VERTICAL);
        List<View> swatches = new ArrayList<>();
        for (int color : PhotoEditorPlugin.COLORS) {
            View swatch = owner.colorSwatch(context, color, view -> {
                chosenColor[0] = color;
                owner.selectColorOnly(swatches, color);
            });
            swatch.setTag(color);
            swatches.add(swatch);
            colorRow.addView(swatch);
        }
        owner.selectColorOnly(swatches, chosenColor[0]);
        LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        colorParams.setMargins(0, 0, 0, owner.dp(18));
        layout.addView(colorRow, colorParams);

        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.RIGHT);

        TextView deleteBtn = dialogButton(owner, context, "Delete", Color.parseColor("#f23f42"), false);
        deleteBtn.setOnClickListener(v -> {
            ViewGroup parent = (ViewGroup) target.getParent();
            if (parent != null)
                parent.removeView(target);
            dialog.dismiss();
        });
        buttons.addView(deleteBtn);

        TextView cancelBtn = dialogButton(owner, context, "Cancel", Color.parseColor("#dbdee1"), false);
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancelBtn);

        TextView saveBtn = dialogButton(owner, context, "Save", Color.WHITE, true);
        saveBtn.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                target.setText(text);
                target.setTextColor(chosenColor[0]);
                target.requestLayout();
            }
            dialog.dismiss();
        });
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        saveParams.setMargins(owner.dp(8), 0, 0, 0);
        saveBtn.setLayoutParams(saveParams);
        buttons.addView(saveBtn);

        layout.addView(buttons);
        owner.showKeyboardDialog(dialog, layout, input);
    }


    private static TextView dialogButton(PhotoEditorPlugin owner, Context context, String text, int color, boolean primary) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextColor(color);
        button.setTextSize(14f);
        button.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(owner.dp(14), owner.dp(10), owner.dp(14), owner.dp(10));

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        if (primary) {
            bg.setColor(0xff5865f2);
        } else {
            bg.setColor(Color.TRANSPARENT);
        }
        bg.setCornerRadius(owner.dp(8));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.content.res.ColorStateList rippleColor = android.content.res.ColorStateList.valueOf(0x40ffffff);
            android.graphics.drawable.RippleDrawable ripple = new android.graphics.drawable.RippleDrawable(rippleColor, bg, null);
            button.setBackground(ripple);
        } else {
            button.setBackground(bg);
        }

        owner.addPressAnimation(button);
        return button;
    }

}
