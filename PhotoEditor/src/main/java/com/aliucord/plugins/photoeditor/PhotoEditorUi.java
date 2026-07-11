package com.aliucord.plugins.photoeditor;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.android.material.slider.Slider;

final class PhotoEditorUi {
    private PhotoEditorUi() {}

    static Slider createDiscordSlider(Context context, float from, float to, float value) {
        Slider slider = new Slider(context);
        slider.setValueFrom(from);
        slider.setValueTo(to);
        slider.setStepSize(1f);
        slider.setValue(value);
        ColorStateList blurple = ColorStateList.valueOf(0xff5865f2);
        slider.setThumbTintList(blurple);
        slider.setTrackActiveTintList(blurple);
        slider.setTrackInactiveTintList(ColorStateList.valueOf(0xff4e5058));
        return slider;
    }

    static Bitmap createColorPlane(int size) {
        int[] pixels = new int[size * size];
        float[] hsv = new float[3];
        for (int y = 0; y < size; y++) {
            float position = y / (float) (size - 1);
            hsv[1] = position <= 0.5f ? position * 2f : 1f;
            hsv[2] = position <= 0.5f ? 1f : (1f - position) * 2f;
            for (int x = 0; x < size; x++) {
                hsv[0] = x / (float) (size - 1) * 360f;
                pixels[y * size + x] = Color.HSVToColor(hsv);
            }
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888);
    }
}
