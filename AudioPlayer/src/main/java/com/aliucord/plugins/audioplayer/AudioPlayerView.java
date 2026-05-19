package com.aliucord.plugins.audioplayer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.aliucord.utils.DimenUtils;
import com.aliucord.Logger;
import com.discord.utilities.color.ColorCompat;

public class AudioPlayerView extends LinearLayout implements AudioPlayerManager.PlayerListener {
    private static final Logger logger = new Logger("AudioPlayerView");

    private TextView playButton;
    private SeekBar seekBar;
    private TextView timerText;
    private String audioUrl;
    private String filename;
    private boolean isUserSeeking = false;
    private int interactiveActiveColor;

    public AudioPlayerView(Context context) {
        super(context);
        init(context);
    }

    public AudioPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        int padHorizontal = DimenUtils.dpToPx(10);
        int padVertical = DimenUtils.dpToPx(5);
        setPadding(padHorizontal, padVertical, padHorizontal, padVertical);

        int cardColor = ColorCompat.getThemedColor(context, com.lytefast.flexinput.R.b.colorBackgroundSecondary);
        if (cardColor == 0) {
            cardColor = Color.parseColor("#2F3136");
        }

        interactiveActiveColor = ColorCompat.getThemedColor(context, com.lytefast.flexinput.R.b.colorInteractiveActive);
        if (interactiveActiveColor == 0) {
            interactiveActiveColor = Color.parseColor("#5865F2");
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(DimenUtils.dpToPx(8));
        bg.setColor(cardColor);
        bg.setStroke(DimenUtils.dpToPx(1), blendColors(cardColor, interactiveActiveColor, 0.22f));
        setBackground(bg);

        playButton = new TextView(context);
        playButton.setTextSize(14f);
        playButton.setTextColor(Color.WHITE);
        playButton.setGravity(Gravity.CENTER);
        playButton.setTypeface(Typeface.DEFAULT_BOLD);

        int btnSize = DimenUtils.dpToPx(30);
        LayoutParams btnParams = new LayoutParams(btnSize, btnSize);
        btnParams.setMargins(0, 0, DimenUtils.dpToPx(8), 0);
        playButton.setLayoutParams(btnParams);

        GradientDrawable playBtnBg = new GradientDrawable();
        playBtnBg.setShape(GradientDrawable.OVAL);
        playBtnBg.setColor(interactiveActiveColor);
        playButton.setBackground(playBtnBg);

        playButton.setText("▶");
        addView(playButton);

        int textColor = ColorCompat.getThemedColor(context, com.lytefast.flexinput.R.b.colorTextMuted);
        if (textColor == 0) {
            textColor = Color.parseColor("#8E9297");
        }

        seekBar = new SeekBar(context);
        LayoutParams seekParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);
        seekParams.setMargins(-DimenUtils.dpToPx(4), 0, -DimenUtils.dpToPx(2), 0);
        seekBar.setLayoutParams(seekParams);
        seekBar.setMax(100);
        seekBar.setProgress(0);

        try {
            seekBar.getProgressDrawable().setColorFilter(interactiveActiveColor, android.graphics.PorterDuff.Mode.SRC_IN);
            seekBar.getThumb().setColorFilter(interactiveActiveColor, android.graphics.PorterDuff.Mode.SRC_IN);
        } catch (Throwable ignored) {}

        addView(seekBar);

        timerText = new TextView(context);
        timerText.setTextSize(10f);
        timerText.setTypeface(Typeface.MONOSPACE);
        timerText.setTextColor(textColor);
        timerText.setText("0:00 / 0:00");
        LayoutParams timerParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        timerParams.setMargins(DimenUtils.dpToPx(6), 0, 0, 0);
        addView(timerText, timerParams);

        // Play/Pause button click
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioUrl != null) {
                    AudioPlayerManager.play(getContext(), audioUrl, AudioPlayerView.this);
                }
            }
        });

        // Seekbar change listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    timerText.setText(formatTime(progress) + " / " + formatTime(seekBar.getMax()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                AudioPlayerManager.seekTo(seekBar.getProgress());
            }
        });
    }

    public void configure(String url, String filename) {
        this.audioUrl = url;
        this.filename = filename;

        playButton.setText("▶");
        seekBar.setProgress(0);
        seekBar.setMax(100);
        timerText.setText("0:00 / 0:00");

        AudioPlayerManager.registerListener(url, this);
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
        AudioPlayerManager.unregisterListener(this);
    }

    @Override
    public void onStateChanged(final AudioPlayerManager.PlayerState state) {
        post(new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case IDLE:
                        playButton.setText("▶");
                        seekBar.setProgress(0);
                        timerText.setText("0:00 / " + formatTime(AudioPlayerManager.getDuration()));
                        break;
                    case LOADING:
                        playButton.setText("⌛");
                        break;
                    case PLAYING:
                        playButton.setText("❚❚");
                        break;
                    case PAUSED:
                        playButton.setText("▶");
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
                seekBar.setMax(duration);
                seekBar.setProgress(position);
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

    private int blendColors(int from, int to, float ratio) {
        float inverse = 1f - ratio;
        return Color.rgb(
            Math.round(Color.red(from) * inverse + Color.red(to) * ratio),
            Math.round(Color.green(from) * inverse + Color.green(to) * ratio),
            Math.round(Color.blue(from) * inverse + Color.blue(to) * ratio)
        );
    }
}
