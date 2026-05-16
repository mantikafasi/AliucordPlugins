package com.aliucord.plugins;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.utils.DimenUtils;
import com.discord.app.AppBottomSheet;

public class AvatarResizerSettings extends AppBottomSheet {
    private final SettingsAPI settings;

    public AvatarResizerSettings(SettingsAPI settings) {
        this.settings = settings;
    }

    @Override
    public int getContentViewResId() {
        return 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = DimenUtils.getDefaultPadding();
        layout.setPadding(padding, padding, padding, padding);

        layout.addView(createSlider(
                context,
                "Message avatar size",
                AvatarResizer.MESSAGE_SIZE_KEY,
                AvatarResizer.DEFAULT_MESSAGE_SIZE,
                AvatarResizer.MIN_MESSAGE_SIZE,
                AvatarResizer.MAX_MESSAGE_SIZE
        ));

        layout.addView(createSlider(
                context,
                "Popout avatar size",
                AvatarResizer.POPOUT_SIZE_KEY,
                AvatarResizer.DEFAULT_POPOUT_SIZE,
                AvatarResizer.MIN_POPOUT_SIZE,
                AvatarResizer.MAX_POPOUT_SIZE
        ));

        return layout;
    }

    private View createSlider(Context context, String title, String key, int defaultValue, int min, int max) {
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, 0, 0, DimenUtils.dpToPx(16));

        TextView label = new TextView(context);
        label.setTextSize(16f);

        SeekBar seekBar = new SeekBar(context);
        seekBar.setMax(max - min);

        int value = clamp(settings.getInt(key, defaultValue), min, max);
        settings.setInt(key, value);
        label.setText(title + ": " + value + "dp");
        seekBar.setProgress(value - min);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newValue = progress + min;
                settings.setInt(key, newValue);
                label.setText(title + ": " + newValue + "dp");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Utils.showToast("Reopen the current chat or popout to apply everywhere");
            }
        });

        wrapper.addView(label);
        wrapper.addView(seekBar);
        return wrapper;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
