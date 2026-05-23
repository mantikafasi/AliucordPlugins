package com.aliucord.plugins.audioplayer;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;

class StickyAudioPlayerController {
    private static final String STICKY_TAG = "AudioPlayerStickyView";
    private static final int STICKY_HEIGHT_DP = 56;
    private static final int STICKY_TOP_MARGIN_DP = 88;
    private static final int STICKY_HORIZONTAL_MARGIN_DP = 12;

    private static AudioPlayerView stickyView;
    private static String stickyUrl;
    private static String stickyFilename;

    static void show(String url, String filename) {
        if (url == null || !url.equals(AudioPlayerManager.getCurrentUrl())) return;

        Activity activity = Utils.getAppActivity();
        if (activity == null) return;

        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        if (stickyView == null) {
            stickyView = new AudioPlayerView(content.getContext());
            stickyView.setTag(STICKY_TAG);
            stickyView.setStickyMode(true);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    DimenUtils.dpToPx(STICKY_HEIGHT_DP),
                    Gravity.TOP
            );
            int horizontalMargin = DimenUtils.dpToPx(STICKY_HORIZONTAL_MARGIN_DP);
            params.setMargins(horizontalMargin, DimenUtils.dpToPx(STICKY_TOP_MARGIN_DP), horizontalMargin, 0);
            content.addView(stickyView, params);
            stickyView.setElevation(DimenUtils.dpToPx(8));
        } else if (stickyView.getParent() == null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    DimenUtils.dpToPx(STICKY_HEIGHT_DP),
                    Gravity.TOP
            );
            int horizontalMargin = DimenUtils.dpToPx(STICKY_HORIZONTAL_MARGIN_DP);
            params.setMargins(horizontalMargin, DimenUtils.dpToPx(STICKY_TOP_MARGIN_DP), horizontalMargin, 0);
            content.addView(stickyView, params);
            stickyView.setElevation(DimenUtils.dpToPx(8));
        }

        stickyUrl = url;
        stickyFilename = filename;
        stickyView.setVisibility(View.VISIBLE);
        stickyView.configure(url, filename);
        stickyView.bringToFront();
    }

    static void hide() {
        if (stickyView == null) return;

        stickyView.setVisibility(View.GONE);
        AudioPlayerManager.unregisterListener(stickyView);
    }

    static void hideIfShowing(String url) {
        if (url != null && url.equals(stickyUrl)) {
            hide();
        }
    }

    static boolean isStickyView(AudioPlayerView view) {
        return view != null && view == stickyView;
    }

    static void destroy() {
        if (stickyView != null) {
            AudioPlayerManager.unregisterListener(stickyView);
            ViewGroup parent = (ViewGroup) stickyView.getParent();
            if (parent != null) {
                parent.removeView(stickyView);
            }
        }
        stickyView = null;
        stickyUrl = null;
        stickyFilename = null;
    }
}
