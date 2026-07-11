package com.aliucord.plugins.photoeditor;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

final class PhotoEditorColorPicker {
    interface Callback { void onColorPicked(int color); }
    private PhotoEditorColorPicker() {}
    private static int dp(int value) { return DimenUtils.dpToPx(value); }
    private static void addRippleBorderless(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.util.TypedValue value = new android.util.TypedValue();
            view.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true);
            view.setBackgroundResource(value.resourceId);
        }
    }

    static void show(Context context, PhotoEditorView editorView, PhotoEditor editor, int initialColor, int[] colors, Callback callback) {
        Dialog dialog = new Dialog(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));

        android.graphics.drawable.GradientDrawable rootBg = new android.graphics.drawable.GradientDrawable();
        rootBg.setColor(0xff2b2d31);
        rootBg.setCornerRadius(dp(16));
        root.setBackground(rootBg);

        // 1. Title Row (Title + Eyedropper)
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("Custom Color");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        android.widget.ImageView eyedropper = new android.widget.ImageView(context);
        int dropperId = Utils.getResId("ic_colorize_24dp", "drawable");
        if (dropperId == 0) dropperId = Utils.getResId("ic_edit_24dp", "drawable");
        eyedropper.setImageResource(dropperId);
        eyedropper.setColorFilter(Color.WHITE);
        eyedropper.setPadding(dp(8), dp(8), dp(8), dp(8));
        addRippleBorderless(eyedropper);
        titleRow.addView(eyedropper);
        root.addView(titleRow);

        // Eyedropper Logic
        eyedropper.setOnClickListener(v -> {
            dialog.dismiss();
            android.widget.Toast.makeText(context, "Tap anywhere on the image to pick a color!", android.widget.Toast.LENGTH_SHORT).show();

            editor.setBrushDrawingMode(false);
            editorView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, android.view.MotionEvent event) {
                    int action = event.getAction();
                    if (action == android.view.MotionEvent.ACTION_DOWN || action == android.view.MotionEvent.ACTION_MOVE || action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                        try {
                            android.widget.ImageView source = editorView.getSource();
                            android.graphics.drawable.BitmapDrawable drawable = (android.graphics.drawable.BitmapDrawable) source.getDrawable();
                            android.graphics.Bitmap bitmap = drawable.getBitmap();

                            android.graphics.Matrix inverse = new android.graphics.Matrix();
                            source.getImageMatrix().invert(inverse);
                            float[] pts = {event.getX(), event.getY()};
                            inverse.mapPoints(pts);

                            int x = (int) pts[0];
                            int y = (int) pts[1];

                            if (x >= 0 && y >= 0 && x < bitmap.getWidth() && y < bitmap.getHeight()) {
                                int pixel = bitmap.getPixel(x, y);
                                callback.onColorPicked(pixel);
                            }
                        } catch (Throwable t) {
                            // Silently ignore out of bounds during drag
                        }

                        if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                            editorView.setOnTouchListener(null);
                            editor.setBrushDrawingMode(true);
                        }
                        return true;
                    }
                    return false;
                }
            });
        });

        // 2. Preview Box and HEX
        LinearLayout previewRow = new LinearLayout(context);
        previewRow.setOrientation(LinearLayout.HORIZONTAL);
        previewRow.setGravity(Gravity.CENTER_VERTICAL);
        previewRow.setPadding(0, dp(16), 0, dp(16));

        View previewBox = new View(context);
        android.graphics.drawable.GradientDrawable boxBg = new android.graphics.drawable.GradientDrawable();
        boxBg.setCornerRadius(dp(8));
        boxBg.setColor(initialColor);
        previewBox.setBackground(boxBg);
        previewRow.addView(previewBox, new LinearLayout.LayoutParams(dp(48), dp(48)));

        android.widget.EditText hexInput = new android.widget.EditText(context);
        hexInput.setTextColor(Color.WHITE);
        hexInput.setText(String.format("#%08X", initialColor));
        hexInput.setSingleLine(true);
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hexParams.setMargins(dp(16), 0, 0, 0);
        previewRow.addView(hexInput, hexParams);
        root.addView(previewRow);


        // 3. HSV Sliders
        float[] hsv = new float[3];
        Color.colorToHSV(initialColor, hsv);

        TextView hueLabel = new TextView(context); hueLabel.setText("Hue"); hueLabel.setTextColor(Color.LTGRAY); root.addView(hueLabel);
        com.google.android.material.slider.Slider hueSlider = PhotoEditorUi.createDiscordSlider(context, 0, 360, hsv[0]); root.addView(hueSlider);

        TextView satLabel = new TextView(context); satLabel.setText("Saturation"); satLabel.setTextColor(Color.LTGRAY); satLabel.setPadding(0, dp(8), 0, 0); root.addView(satLabel);
        com.google.android.material.slider.Slider satSlider = PhotoEditorUi.createDiscordSlider(context, 0, 100, hsv[1] * 100); root.addView(satSlider);

        TextView valLabel = new TextView(context); valLabel.setText("Lightness"); valLabel.setTextColor(Color.LTGRAY); valLabel.setPadding(0, dp(8), 0, 0); root.addView(valLabel);
        com.google.android.material.slider.Slider valSlider = PhotoEditorUi.createDiscordSlider(context, 0, 100, hsv[2] * 100); root.addView(valSlider);

        final android.widget.ImageView colorPlane = new android.widget.ImageView(context);
        int colorPlaneSize = dp(180);
        colorPlane.setImageBitmap(PhotoEditorUi.createColorPlane(colorPlaneSize));
        colorPlane.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
        root.addView(colorPlane, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, colorPlaneSize));
        colorPlane.setVisibility(View.GONE);
        previewBox.setOnClickListener(v -> colorPlane.setVisibility(colorPlane.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        TextView alphaLabel = new TextView(context); alphaLabel.setText("Opacity"); alphaLabel.setTextColor(Color.LTGRAY); alphaLabel.setPadding(0, dp(8), 0, 0); root.addView(alphaLabel);
        com.google.android.material.slider.Slider alphaSlider = PhotoEditorUi.createDiscordSlider(context, 0, 255, Color.alpha(initialColor)); root.addView(alphaSlider);

        // 4. Presets Horizontal Scroll
        android.widget.HorizontalScrollView presetScroll = new android.widget.HorizontalScrollView(context);
        presetScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout presetContainer = new LinearLayout(context);
        presetContainer.setOrientation(LinearLayout.HORIZONTAL);
        presetContainer.setPadding(0, dp(16), 0, dp(16));
        for (int c : colors) {
            View swatch = new View(context);
            android.graphics.drawable.GradientDrawable swBg = new android.graphics.drawable.GradientDrawable();
            swBg.setCornerRadius(dp(16)); swBg.setColor(c);
            swatch.setBackground(swBg);
            LinearLayout.LayoutParams swParams = new LinearLayout.LayoutParams(dp(32), dp(32));
            swParams.setMargins(0, 0, dp(8), 0);
            swatch.setLayoutParams(swParams);
            swatch.setOnClickListener(v -> {
                Color.colorToHSV(c, hsv);
                hueSlider.setValue((int) hsv[0]);
                satSlider.setValue((int) (hsv[1] * 100));
                valSlider.setValue((int) (hsv[2] * 100));
                alphaSlider.setValue(Color.alpha(c));
            });
            presetContainer.addView(swatch);
        }
        presetScroll.addView(presetContainer);
        root.addView(presetScroll);

        // Updates
        final int[] currentColor = {initialColor};

        Runnable updateColor = () -> {
            try {
                hsv[0] = hueSlider.getValue();
                hsv[1] = satSlider.getValue() / 100f;
                hsv[2] = valSlider.getValue() / 100f;
                int alpha = Math.round(alphaSlider.getValue());
                currentColor[0] = Color.HSVToColor(alpha, hsv);
                boxBg.setColor(currentColor[0]);
                previewBox.invalidate();
                if (!hexInput.hasFocus()) {
                    hexInput.setText(String.format("#%08X", currentColor[0]));
                }
            } catch (Exception e) {}
        };

        colorPlane.setOnTouchListener((v, event) -> {
            float hue = Math.max(0f, Math.min(360f, event.getX() / Math.max(1f, v.getWidth()) * 360f));
            float position = Math.max(0f, Math.min(1f, event.getY() / Math.max(1f, v.getHeight())));
            float saturation = position <= 0.5f ? position * 2f : 1f;
            float value = position <= 0.5f ? 1f : (1f - position) * 2f;
            hueSlider.setValue(Math.round(hue));
            satSlider.setValue(Math.round(saturation * 100f));
            valSlider.setValue(Math.round(value * 100f));
            return true;
        });
        com.google.android.material.slider.Slider.OnChangeListener sliderListener = (slider, value, fromUser) -> updateColor.run();
        hueSlider.addOnChangeListener(sliderListener);
        satSlider.addOnChangeListener(sliderListener);
        valSlider.addOnChangeListener(sliderListener);
        alphaSlider.addOnChangeListener(sliderListener);
        hexInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (hexInput.hasFocus() && s.toString().startsWith("#")) {
                    try {
                        int c = Color.parseColor(s.toString());
                        currentColor[0] = c;
                        boxBg.setColor(c);
                        Color.colorToHSV(c, hsv);
                        hueSlider.setValue((int) hsv[0]);
                        satSlider.setValue((int) (hsv[1] * 100));
                        valSlider.setValue((int) (hsv[2] * 100));
                        alphaSlider.setValue(Color.alpha(c));
                    } catch (Exception ignored) {}
                }
            }
        });

        // Buttons
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.RIGHT);

        TextView cancel = new TextView(context); cancel.setText("Cancel"); cancel.setTextColor(Color.GRAY); cancel.setPadding(dp(16), dp(8), dp(16), dp(8));
        cancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(cancel);

        TextView apply = new TextView(context); apply.setText("Select"); apply.setTextColor(0xff5865f2); apply.setPadding(dp(16), dp(8), dp(16), dp(8)); apply.setTypeface(null, android.graphics.Typeface.BOLD);
        apply.setOnClickListener(v -> {
            callback.onColorPicked(currentColor[0]);
            dialog.dismiss();
        });
        btnRow.addView(apply);

        root.addView(btnRow);

        dialog.setContentView(root);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

}
