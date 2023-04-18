package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WaveFormView extends android.view.View {
    public List<Integer> waves = new ArrayList<>();

    public WaveFormView(Context context) {
        super(context);
    }

    public WaveFormView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
    }

    public void addWave(int wave) {
        waves.add(0, wave);

        if (waves.size() > 300) {
            waves.remove(waves.size() - 1);
        }
    }

    public void reset() {
        waves.clear();
    }

    public String getWaveForm() {
        var quiet = true;
        // if sound is too quiet we double it so its shown in waveform
        for (int wave : waves) {
            if (wave > 128) {
                quiet = false;
                break;
            }
        }

        byte[] bytes = new byte[waves.size()];
        for (int i = 0; i < waves.size(); i++) {
            bytes[i] = (byte) (quiet ? (waves.get(i) * 2) : (waves.get(i)));
        }

        return new String(Base64.encode(bytes, Base64.NO_WRAP), StandardCharsets.UTF_8);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        Paint paint = new Paint();
        paint.setColor(Color.MAGENTA);
        int width = getWidth();
        int height = getHeight();

        for (int i = 1; i < waves.size(); i++) {
            int lineLenght = waves.get(i) / 3 + 1;
            int top = height / 2;
            int left = width - 10 * i;
            int lineHeight = (lineLenght * 3);
            canvas.drawRect(left, top - lineHeight, left + 5, top + lineHeight, paint);
        }

        super.onDraw(canvas);
    }
}
