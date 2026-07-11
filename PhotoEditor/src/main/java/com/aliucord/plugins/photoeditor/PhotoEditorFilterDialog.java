package com.aliucord.plugins.photoeditor;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aliucord.api.SettingsAPI;
import com.aliucord.utils.DimenUtils;

import ja.burhanrashid52.photoeditor.PhotoEditorView;

final class PhotoEditorFilterDialog {
    private PhotoEditorFilterDialog() {}
    private static int dp(int value) { return DimenUtils.dpToPx(value); }
    private static LinearLayout createContainer(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.setBackgroundColor(0xff313338);
        return layout;
    }

    static void show(Context context, PhotoEditorView editorView, com.aliucord.api.SettingsAPI settings, float[] customFilterValues) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = createContainer(context);
        android.widget.ScrollView scrollContent = new android.widget.ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        Runnable updateFilter = () -> {
            for (int i=0; i<editorView.getChildCount(); i++) {
                android.view.View child = editorView.getChildAt(i);
                if (child.getClass().getName().contains("ImageFilterView")) {
                    child.setVisibility(android.view.View.GONE);
                }
            }
            float[] matrix = PhotoEditorUtils.buildCustomMatrix(customFilterValues);
            boolean applyToEverything = settings.getInt("filter_apply_mode", 0) == 1;
            if (applyToEverything) {
                Paint p = new Paint();
                p.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
                editorView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, p);
                editorView.getSource().clearColorFilter();
            } else {
                editorView.setLayerType(android.view.View.LAYER_TYPE_NONE, null);
                editorView.getSource().setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
            }
        };

        String[] labels = {"Brightness", "Contrast", "Saturation", "Hue", "Temperature", "Tint"};
        float[] mins = {-100f, 0f, 0f, -180f, -50f, -50f};
        float[] maxs = {100f, 2f, 2f, 180f, 50f, 50f};
        float[] defaults = {0f, 1f, 1f, 0f, 0f, 0f};

        for (int i = 0; i < 6; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 0, 0, dp(16));

            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView label = new TextView(context);
            label.setText(labels[i]);
            label.setTextColor(Color.WHITE);
            label.setTextSize(14f);

            TextView valueText = new TextView(context);
            valueText.setText(String.format(java.util.Locale.US, "%.2f", customFilterValues[i]));
            valueText.setTextColor(Color.GRAY);
            valueText.setTextSize(12f);

            LinearLayout.LayoutParams lblParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            header.addView(label, lblParams);
            header.addView(valueText);

            row.addView(header);

            com.google.android.material.slider.Slider slider = PhotoEditorUi.createDiscordSlider(context, 0, 200, 0);

            float current = customFilterValues[i];
            float range = maxs[i] - mins[i];
            int progress = (int) (((current - mins[i]) / range) * 200f);
            slider.setValue(progress);

            slider.addOnChangeListener((view, value, fromUser) -> {
                if (fromUser) {
                    float val = mins[index] + (value / 200f) * range;
                    customFilterValues[index] = val;
                    valueText.setText(String.format(java.util.Locale.US, "%.2f", val));
                    updateFilter.run();
                }
            });
            row.addView(slider);
            content.addView(row);
        }

        TextView resetBtn = new TextView(context);
        resetBtn.setText("Reset Custom Filter");
        resetBtn.setTextColor(0xffda373c);
        resetBtn.setTextSize(14f);
        resetBtn.setGravity(Gravity.CENTER);
        resetBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        resetBtn.setOnClickListener(v -> {
            System.arraycopy(defaults, 0, customFilterValues, 0, defaults.length);
            updateFilter.run();
            dialog.dismiss();
            show(context, editorView, settings, customFilterValues); // Reopen to refresh sliders
        });
        content.addView(resetBtn);

        scrollContent.addView(content);
        layout.addView(scrollContent, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(300)
        ));

        dialog.setContentView(layout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

}
