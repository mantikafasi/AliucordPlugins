package com.aliucord.plugins;

import android.content.Context;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.DimenUtils;
import com.discord.widgets.chat.input.MessageDraftsRepo;
import com.discord.widgets.chat.input.WidgetChatInputEditText;
import com.discord.widgets.chat.input.WidgetChatInputEditText$setOnTextChangedListener$1;
import com.lytefast.flexinput.widget.FlexEditText;

import java.lang.ref.WeakReference;
import java.util.Random;

@AliucordPlugin
@SuppressWarnings("unused")
public class PowerMode extends Plugin {
    public static final String ENABLED_KEY = "enabled";
    public static final String PARTICLES_KEY = "particles";
    public static final String SHAKE_KEY = "shake";
    public static final String INTENSITY_KEY = "intensity";
    public static final int DEFAULT_INTENSITY = 4;

    private final Random random = new Random();
    private WeakReference<View> activeInput = new WeakReference<>(null);
    private PowerModeParticleView overlay;
    private int lastLength = 0;
    private long lastBurstAt = 0;

    @Override
    public void start(Context context) throws Throwable {
        settingsTab = new SettingsTab(PowerModeSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        patcher.patch(
                WidgetChatInputEditText.class.getConstructor(FlexEditText.class, MessageDraftsRepo.class),
                new Hook(callFrame -> activeInput = new WeakReference<>((View) callFrame.args[0]))
        );

        patcher.patch(
                WidgetChatInputEditText$setOnTextChangedListener$1.class.getDeclaredMethod("afterTextChanged", Editable.class),
                new Hook(callFrame -> {
                    Editable editable = (Editable) callFrame.args[0];
                    int length = editable == null ? 0 : editable.length();

                    if (!settings.getBool(ENABLED_KEY, true)) {
                        lastLength = length;
                        return;
                    }

                    if (length > lastLength)
                        triggerPowerMode();

                    lastLength = length;
                })
        );
    }

    @Override
    public void stop(Context context) throws Throwable {
        patcher.unpatchAll();
        commands.unregisterAll();

        if (overlay != null && overlay.getParent() instanceof ViewGroup)
            ((ViewGroup) overlay.getParent()).removeView(overlay);

        overlay = null;
    }

    private void triggerPowerMode() {
        long now = System.currentTimeMillis();
        if (now - lastBurstAt < 35)
            return;

        lastBurstAt = now;
        int intensity = clamp(settings.getInt(INTENSITY_KEY, DEFAULT_INTENSITY), 1, 10);

        if (settings.getBool(PARTICLES_KEY, true))
            spawnParticles(intensity);

        if (settings.getBool(SHAKE_KEY, true))
            shakeScreen(intensity);
    }

    private void spawnParticles(int intensity) {
        ViewGroup root = getRootView();
        if (root == null)
            return;

        if (overlay == null) {
            overlay = new PowerModeParticleView(root.getContext());
            overlay.setClickable(false);
            overlay.setFocusable(false);
        }

        if (overlay.getParent() instanceof ViewGroup && overlay.getParent() != root)
            ((ViewGroup) overlay.getParent()).removeView(overlay);

        if (overlay.getParent() == null)
            root.addView(overlay, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

        int[] point = getBurstPoint();
        overlay.addBurst(point[0], point[1], 5 + intensity * 3);
    }

    private void shakeScreen(int intensity) {
        View root = getRootView();
        if (root == null)
            return;

        float amount = DimenUtils.dpToPx(1 + intensity);
        root.animate().cancel();
        root.setTranslationX(0f);
        root.setTranslationY(0f);
        root.animate()
                .translationX(random.nextBoolean() ? amount : -amount)
                .translationY(random.nextBoolean() ? amount * 0.35f : -amount * 0.35f)
                .setDuration(35)
                .withEndAction(() -> root.animate()
                        .translationX(0f)
                        .translationY(0f)
                        .setDuration(85)
                        .start())
                .start();
    }

    private int[] getBurstPoint() {
        View input = activeInput.get();
        View overlayView = overlay;

        if (input == null || overlayView == null || input.getWidth() == 0)
            return new int[]{DimenUtils.dpToPx(160), DimenUtils.dpToPx(520)};

        int[] inputLocation = new int[2];
        int[] overlayLocation = new int[2];
        input.getLocationInWindow(inputLocation);
        overlayView.getLocationInWindow(overlayLocation);

        int x = inputLocation[0] - overlayLocation[0] + random.nextInt(Math.max(input.getWidth(), 1));
        int y = inputLocation[1] - overlayLocation[1] + input.getHeight() / 2;
        return new int[]{x, y};
    }

    private ViewGroup getRootView() {
        if (activeInput.get() != null) {
            return (ViewGroup) activeInput.get().getRootView();
        }

        return null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
