package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.aliucord.utils.DimenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PowerModeParticleView extends View {
    private static final int[] COLORS = {
            0xffff5555,
            0xffffcc33,
            0xff55ddff,
            0xff66ff99,
            0xffff77cc,
            0xffffffff
    };
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();

    public PowerModeParticleView(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public void addBurst(int x, int y, int count) {
        for (int i = 0; i < count; i++) {
            Particle particle = new Particle();
            particle.x = x;
            particle.y = y;
            particle.vx = randomRange(-10f, 10f);
            particle.vy = randomRange(-18f, -5f);
            particle.life = randomRange(0.65f, 1f);
            particle.size = DimenUtils.dpToPx(randomRange(10f, 18f));
            particle.color = COLORS[random.nextInt(COLORS.length)];
            particles.add(particle);
        }

        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            particle.life -= 0.045f;

            if (particle.life <= 0f) {
                particles.remove(i);
                continue;
            }

            particle.x += particle.vx;
            particle.y += particle.vy;
            particle.vy += 0.75f;

            paint.setAlpha((int) (255 * Math.min(1f, particle.life)));
            paint.setColor(particle.color);
            canvas.drawCircle(particle.x, particle.y, particle.size / 2f, paint);
        }

        paint.setAlpha(255);

        if (!particles.isEmpty())
            postInvalidateOnAnimation();
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float size;
        int color;
    }
}
