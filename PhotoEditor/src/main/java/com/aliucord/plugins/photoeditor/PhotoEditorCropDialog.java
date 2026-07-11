package com.aliucord.plugins.photoeditor;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import ja.burhanrashid52.photoeditor.PhotoEditorView;

final class PhotoEditorCropDialog {
    private PhotoEditorCropDialog() {}

    static void show(PhotoEditorPlugin owner, Context context, android.widget.ImageView targetView, PhotoEditorView editorView) {
        try {
            android.graphics.drawable.BitmapDrawable drawable = (android.graphics.drawable.BitmapDrawable) targetView.getDrawable();
            if (drawable == null) {
                Toast.makeText(context, "No image to crop", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap src = drawable.getBitmap();
            if (src == null) {
                Toast.makeText(context, "No bitmap to crop", Toast.LENGTH_SHORT).show();
                return;
            }

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);

            TextView info = new TextView(context);
            info.setText("Drag corners to resize. Drag center to move crop area:");
            info.setTextColor(Color.parseColor("#dbdee1"));
            info.setTextSize(12f);
            info.setPadding(0, 0, 0, owner.dp(12));
            layout.addView(info);

            float[] rotation = {0f};
            boolean[] flip = {false, false};

            // Create downscaled base for smooth preview dragging
            int maxDim = Math.max(src.getWidth(), src.getHeight());
            float downscale = maxDim > 800 ? 800f / maxDim : 1f;
            Bitmap previewBase = downscale == 1f ? src : Bitmap.createScaledBitmap(src, (int)(src.getWidth() * downscale), (int)(src.getHeight() * downscale), true);

            Bitmap[] currentPreview = {previewBase};

            // Container for image and crop overlay
            FrameLayout container = new FrameLayout(context) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int width = View.MeasureSpec.getSize(widthMeasureSpec);
                    int height = (int) (width * ((float) currentPreview[0].getHeight() / currentPreview[0].getWidth()));
                    int maxHeight = owner.dp(350);
                    if (height > maxHeight) {
                        height = maxHeight;
                        width = (int) (height * ((float) currentPreview[0].getWidth() / currentPreview[0].getHeight()));
                    }
                    int wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
                    int hSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
                    super.onMeasure(wSpec, hSpec);
                }
            };
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            containerParams.setMargins(0, 0, 0, owner.dp(16));
            containerParams.gravity = Gravity.CENTER_HORIZONTAL;
            container.setLayoutParams(containerParams);

            android.widget.ImageView previewImage = new android.widget.ImageView(context);
            previewImage.setImageBitmap(currentPreview[0]);
            previewImage.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
            container.addView(previewImage, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            CropOverlayView cropOverlay = new CropOverlayView(context);
            container.addView(cropOverlay, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            layout.addView(container);

            Runnable updatePreview = () -> {
                android.graphics.Matrix m = new android.graphics.Matrix();
                m.postRotate(rotation[0]);
                if (flip[0]) m.postScale(-1, 1);
                if (flip[1]) m.postScale(1, -1);
                if (currentPreview[0] != null && currentPreview[0] != previewBase) {
                    currentPreview[0].recycle();
                }
                currentPreview[0] = Bitmap.createBitmap(previewBase, 0, 0, previewBase.getWidth(), previewBase.getHeight(), m, true);
                previewImage.setImageBitmap(currentPreview[0]);
                container.requestLayout();
            };

            // Rotation Controls
            LinearLayout rotateControls = new LinearLayout(context);
            rotateControls.setOrientation(LinearLayout.VERTICAL);
            rotateControls.setPadding(0, owner.dp(8), 0, owner.dp(16));

            LinearLayout sliderRow = new LinearLayout(context);
            sliderRow.setOrientation(LinearLayout.HORIZONTAL);
            sliderRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView degreeLabel = new TextView(context);
            degreeLabel.setText("0°");
            degreeLabel.setTextColor(Color.WHITE);
            degreeLabel.setMinWidth(owner.dp(40));
            degreeLabel.setGravity(Gravity.CENTER);
            sliderRow.addView(degreeLabel);

            com.google.android.material.slider.Slider rotateSlider = PhotoEditorUi.createDiscordSlider(context, 0, 360, 180);
            rotateSlider.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rotateSlider.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser) {
                    rotation[0] = value - 180f;
                    degreeLabel.setText((int) rotation[0] + "°");
                    updatePreview.run();
                }
            });
            sliderRow.addView(rotateSlider);
            rotateControls.addView(sliderRow);

            LinearLayout mirrorRow = new LinearLayout(context);
            mirrorRow.setOrientation(LinearLayout.HORIZONTAL);
            mirrorRow.setGravity(Gravity.CENTER);
            mirrorRow.setPadding(0, owner.dp(8), 0, 0);

            View flipHBtn = owner.iconButton(context, "Flip H", "ic_swap_horiz_24dp", v -> {
                flip[0] = !flip[0];
                updatePreview.run();
            });
            mirrorRow.addView(flipHBtn);

            View flipVBtn = owner.iconButton(context, "Flip V", "ic_swap_vert_24dp", v -> {
                flip[1] = !flip[1];
                updatePreview.run();
            });
            mirrorRow.addView(flipVBtn);

            View rotate90Btn = owner.iconButton(context, "Rotate 90°", "ucrop_ic_rotate", v -> {
                float newRot = rotation[0] + 90f;
                if (newRot > 180f) newRot -= 360f;
                rotation[0] = newRot;
                rotateSlider.setValue((int)newRot + 180);
                degreeLabel.setText((int)newRot + "°");
                updatePreview.run();
            });
            mirrorRow.addView(rotate90Btn);

            rotateControls.addView(mirrorRow);
            layout.addView(rotateControls);

            Dialog dialog[] = new Dialog[1];

            LinearLayout buttons = new LinearLayout(context);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setGravity(Gravity.RIGHT);

            TextView cancelBtn = new TextView(context);
            cancelBtn.setText("Cancel");
            cancelBtn.setTextColor(Color.parseColor("#dbdee1"));
            cancelBtn.setPadding(owner.dp(16), owner.dp(10), owner.dp(16), owner.dp(10));
            cancelBtn.setOnClickListener(v -> dialog[0].dismiss());
            buttons.addView(cancelBtn);

            TextView applyBtn = new TextView(context);
            applyBtn.setText("Crop");
            applyBtn.setTextColor(Color.WHITE);
            applyBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            applyBtn.setPadding(owner.dp(16), owner.dp(10), owner.dp(16), owner.dp(10));

            android.graphics.drawable.GradientDrawable applyBg = new android.graphics.drawable.GradientDrawable();
            applyBg.setColor(0xff5865f2);
            applyBg.setCornerRadius(owner.dp(6));
            applyBtn.setBackground(applyBg);

            applyBtn.setOnClickListener(v -> {
                try {
                    // 1. Generate full-res rotated bitmap
                    android.graphics.Matrix m = new android.graphics.Matrix();
                    m.postRotate(rotation[0]);
                    if (flip[0]) m.postScale(-1, 1);
                    if (flip[1]) m.postScale(1, -1);
                    Bitmap finalRotatedSrc = (rotation[0] == 0f && !flip[0] && !flip[1]) ? src : Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);

                    RectF percent = cropOverlay.getCropRectPercent();
                    int w = finalRotatedSrc.getWidth();
                    int h = finalRotatedSrc.getHeight();

                    int left = (int) (w * percent.left);
                    int top = (int) (h * percent.top);
                    int right = (int) (w * percent.right);
                    int bottom = (int) (h * percent.bottom);

                    int cropWidth = right - left;
                    int cropHeight = bottom - top;

                    if (cropWidth > 10 && cropHeight > 10) {
                        Bitmap cropped = Bitmap.createBitmap(finalRotatedSrc, left, top, cropWidth, cropHeight);
                        if (finalRotatedSrc != src) {
                            finalRotatedSrc.recycle();
                        }
                        targetView.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
                        targetView.setImageBitmap(cropped);

                        if (targetView == editorView.getSource()) {
                            owner.fillEditorBaseLayers(editorView);
                            if (editorView.getParent() instanceof View)
                                owner.fitEditorToBitmap(editorView, (View) editorView.getParent(), cropped);
                        } else {
                            targetView.requestLayout();
                        }
                        Toast.makeText(context, "Cropped successfully", Toast.LENGTH_SHORT).show();
                    }
                } catch (Throwable e) {
                    owner.logError(e);
                    Toast.makeText(context, "Crop failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                dialog[0].dismiss();
            });

            LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            applyParams.setMargins(owner.dp(8), 0, 0, 0);
            applyBtn.setLayoutParams(applyParams);
            buttons.addView(applyBtn);

            layout.addView(buttons);

            dialog[0] = owner.customDialog(context, "Crop Image", layout);
            dialog[0].show();
        } catch (Throwable t) {
            owner.logError("Failed to show crop dialog", t);
        }
    }

}
