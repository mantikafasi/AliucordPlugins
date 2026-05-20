package com.aliucord.plugins.audioplayer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aliucord.utils.DimenUtils;
import com.aliucord.Logger;
import com.discord.utilities.color.ColorCompat;
import com.lytefast.flexinput.R;

public class AudioPlayerView extends LinearLayout implements AudioPlayerManager.PlayerListener {
    private static final Logger logger = new Logger("AudioPlayerView");

    private ImageView playButton;
    private ProgressBar loadingSpinner;
    private DiscordProgressView progressView;
    private TextView timerText;
    private String audioUrl;
    private String filename;
    private boolean isUserSeeking = false;
    private int interactiveActiveColor;
    private int interactiveNormalColor;

    public AudioPlayerView(Context context) {
        super(context);
        init(context);
    }



    private void init(Context context) {
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        int padHorizontal = DimenUtils.dpToPx(12);
        setPadding(padHorizontal, 0, padHorizontal, 0);

        int cardColor = ColorCompat.getThemedColor(context, R.b.colorBackgroundSecondary);
        if (cardColor == 0) {
            cardColor = Color.parseColor("#2F3136");
        }

        interactiveActiveColor = ColorCompat.getThemedColor(context, R.b.colorInteractiveActive);
        if (interactiveActiveColor == 0) {
            interactiveActiveColor = Color.parseColor("#5865F2");
        }

        interactiveNormalColor = ColorCompat.getThemedColor(context, R.b.colorInteractiveNormal);
        if (interactiveNormalColor == 0) {
            interactiveNormalColor = interactiveActiveColor;
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        int r = DimenUtils.dpToPx(8);
        bg.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        bg.setColor(cardColor);
        setBackground(bg);

        setMinimumHeight(0);

        FrameLayout controlSlot = new FrameLayout(context);
        int btnSize = DimenUtils.dpToPx(28);
        LayoutParams slotParams = new LayoutParams(btnSize, btnSize);
        slotParams.setMargins(0, 0, DimenUtils.dpToPx(8), 0);
        addView(controlSlot, slotParams);

        playButton = new ImageView(context);
        playButton.setScaleType(ImageView.ScaleType.CENTER);
        setPlayIcon(false);
        controlSlot.addView(playButton, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        loadingSpinner = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
        loadingSpinner.setVisibility(View.GONE);
        try {
            loadingSpinner.getIndeterminateDrawable().setColorFilter(interactiveNormalColor, android.graphics.PorterDuff.Mode.SRC_IN);
        } catch (Throwable ignored) {}
        controlSlot.addView(loadingSpinner, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            ));

        int textColor = ColorCompat.getThemedColor(context, R.b.colorInteractiveNormal);
        if (textColor == 0) {
            textColor = Color.parseColor("#8E9297");
        }

        int progressTrackColor = ColorCompat.getThemedColor(context, R.b.colorBackgroundModifierAccent);
        if (progressTrackColor == 0) {
            progressTrackColor = Color.parseColor("#4F545C");
        }

        progressView = new DiscordProgressView(context);
        progressView.setColors(progressTrackColor, interactiveActiveColor);
        progressView.setMax(100);
        progressView.setProgress(0);
        LayoutParams progressParams = new LayoutParams(0, DimenUtils.dpToPx(20), 1.0f);
        progressParams.setMargins(0, 0, DimenUtils.dpToPx(6), 0);
        addView(progressView, progressParams);

        timerText = new TextView(context);
        timerText.setTextSize(10f);
        timerText.setTextColor(textColor);
        timerText.setIncludeFontPadding(false);
        timerText.setText("0:00 / 0:00");
        LayoutParams timerParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        timerParams.setMargins(DimenUtils.dpToPx(4), 0, 0, 0);
        addView(timerText, timerParams);

        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioUrl != null) {
                    AudioPlayerManager.play(getContext(), audioUrl, AudioPlayerView.this);
                }
            }
        });

        progressView.setOnProgressChangeListener(new DiscordProgressView.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(int progress, boolean fromUser) {
                if (fromUser)
                    timerText.setText(formatTime(progress) + " / " + formatTime(progressView.getMax()));
            }

            @Override
            public void onStartTrackingTouch() {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch() {
                isUserSeeking = false;
                AudioPlayerManager.seekTo(progressView.getProgress());
            }
        });
    }

    public void configure(final String url, String filename) {
        this.audioUrl = url;
        this.filename = filename;

        setPlayIcon(false);
        playButton.setVisibility(View.VISIBLE);
        loadingSpinner.setVisibility(View.GONE);
        progressView.setProgress(0);
        progressView.setMax(100);
        timerText.setText("0:00 / 0:00");

        AudioPlayerManager.registerListener(url, this);

        AudioPlayerManager.getDurationAsync(url, new AudioPlayerManager.DurationCallback() {
            @Override
            public void onDurationFetched(int durationMs) {
                if (url.equals(audioUrl)) {
                    if (durationMs > 0) {
                        progressView.setMax(durationMs);
                        if (!url.equals(AudioPlayerManager.getCurrentUrl())) {
                            timerText.setText("0:00 / " + formatTime(durationMs));
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (audioUrl != null) {
            AudioPlayerManager.registerListener(audioUrl, this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (audioUrl != null && audioUrl.equals(AudioPlayerManager.getCurrentUrl())) {
            AudioPlayerManager.stop();
        }
        AudioPlayerManager.unregisterListener(this);
    }

    @Override
    public void onStateChanged(final AudioPlayerManager.PlayerState state) {
        post(new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case IDLE:
                        setPlayIcon(false);
                        playButton.setVisibility(View.VISIBLE);
                        loadingSpinner.setVisibility(View.GONE);
                        progressView.setProgress(0);
                        timerText.setText("0:00 / " + formatTime(AudioPlayerManager.getDuration()));
                        break;
                    case LOADING:
                        playButton.setVisibility(View.GONE);
                        loadingSpinner.setVisibility(View.VISIBLE);
                        break;
                    case PLAYING:
                        setPlayIcon(true);
                        playButton.setVisibility(View.VISIBLE);
                        loadingSpinner.setVisibility(View.GONE);
                        break;
                    case PAUSED:
                        setPlayIcon(false);
                        playButton.setVisibility(View.VISIBLE);
                        loadingSpinner.setVisibility(View.GONE);
                        break;
                }
            }
        });
    }

    @Override
    public void onProgressUpdate(final int position, final int duration) {
                if (isUserSeeking) return;

        post(new Runnable() {
            @Override
            public void run() {
                progressView.setMax(duration);
                progressView.setProgress(position);
                timerText.setText(formatTime(position) + " / " + formatTime(duration));
            }
        });
    }

    private String formatTime(int ms) {
        if (ms <= 0) return "0:00";
        int totalSecs = ms / 1000;
        int minutes = totalSecs / 60;
        int seconds = totalSecs % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void setPlayIcon(boolean pause) {
        if (pause) {
            playButton.setImageDrawable(new PauseDrawable(interactiveActiveColor));
            return;
        }

        try {
            Drawable playDrawable = getContext().getResources().getDrawable(R.e.ic_play_arrow_24dp);
            playDrawable.setColorFilter(interactiveNormalColor, android.graphics.PorterDuff.Mode.SRC_IN);
            playButton.setImageDrawable(playDrawable);
        } catch (Throwable ignored) {
            playButton.setImageDrawable(new PlayFallbackDrawable(interactiveNormalColor));
        }
    }

    private static class PauseDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PauseDrawable(int color) {
            paint.setColor(color);
        }

        @Override
        public void draw(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            float barWidth = bounds.width() * 0.18f;
            float gap = bounds.width() * 0.14f;
            float height = bounds.height() * 0.58f;
            float top = bounds.centerY() - height / 2f;
            float bottom = bounds.centerY() + height / 2f;
            float left = bounds.centerX() - gap / 2f - barWidth;
            float right = bounds.centerX() + gap / 2f;
            float radius = DimenUtils.dpToPx(1);
            canvas.drawRoundRect(left, top, left + barWidth, bottom, radius, radius, paint);
            canvas.drawRoundRect(right, top, right + barWidth, bottom, radius, radius, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    private static class PlayFallbackDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PlayFallbackDrawable(int color) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(Canvas canvas) {
            android.graphics.Path path = new android.graphics.Path();
            RectF bounds = new RectF(getBounds());
            path.moveTo(bounds.left + bounds.width() * 0.34f, bounds.top + bounds.height() * 0.24f);
            path.lineTo(bounds.left + bounds.width() * 0.34f, bounds.bottom - bounds.height() * 0.24f);
            path.lineTo(bounds.right - bounds.width() * 0.22f, bounds.centerY());
            path.close();
            canvas.drawPath(path, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    private static class DiscordProgressView extends View {
        interface OnProgressChangeListener {
            void onProgressChanged(int progress, boolean fromUser);
            void onStartTrackingTouch();
            void onStopTrackingTouch();
        }

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int trackColor;
        private int progressColor;
        private int max = 100;
        private int progress = 0;
        private OnProgressChangeListener listener;

        DiscordProgressView(Context context) {
            super(context);
            setMinimumHeight(0);
        }

        void setColors(int trackColor, int progressColor) {
            this.trackColor = trackColor;
            this.progressColor = progressColor;
            invalidate();
        }

        void setOnProgressChangeListener(OnProgressChangeListener listener) {
            this.listener = listener;
        }

        int getMax() {
            return max;
        }

        void setMax(int max) {
            this.max = Math.max(1, max);
            if (progress > this.max) progress = this.max;
            invalidate();
        }

        int getProgress() {
            return progress;
        }

        void setProgress(int progress) {
            this.progress = Math.max(0, Math.min(progress, max));
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);

            float trackHeight = DimenUtils.dpToPx(3);
            float radius = trackHeight / 2f;
            float centerY = getHeight() / 2f;
            float top = centerY - radius;
            float bottom = centerY + radius;
            float width = getWidth();

            paint.setColor(trackColor);
            canvas.drawRoundRect(0, top, width, bottom, radius, radius, paint);

            float progressWidth = width * (progress / (float) max);
            if (progressWidth > 0f) {
                paint.setColor(progressColor);
                canvas.drawRoundRect(0, top, progressWidth, bottom, radius, radius, paint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isEnabled()) return false;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (listener != null) listener.onStartTrackingTouch();
                    updateFromTouch(event, true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    updateFromTouch(event, true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    updateFromTouch(event, true);
                    if (listener != null) listener.onStopTrackingTouch();
                    return true;
            }

            return super.onTouchEvent(event);
        }

        private void updateFromTouch(MotionEvent event, boolean fromUser) {
            float x = Math.max(0f, Math.min(event.getX(), getWidth()));
            setProgress(Math.round((x / Math.max(1, getWidth())) * max));
            if (listener != null) listener.onProgressChanged(progress, fromUser);
        }
    }
}
