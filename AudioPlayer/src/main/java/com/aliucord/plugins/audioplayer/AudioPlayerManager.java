package com.aliucord.plugins.audioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import com.aliucord.Logger;

import java.util.HashMap;
import java.util.Map;

public class AudioPlayerManager {
    private static final Logger logger = new Logger("AudioPlayerManager");

    public enum PlayerState {
        IDLE,
        LOADING,
        PLAYING,
        PAUSED
    }

    public interface PlayerListener {
        void onStateChanged(PlayerState state);
        void onProgressUpdate(int position, int duration);
    }

    public interface DurationCallback {
        void onDurationFetched(int durationMs);
    }

    private static MediaPlayer mediaPlayer;
    private static String currentUrl;
    private static PlayerState currentState = PlayerState.IDLE;
    private static PlayerListener activeListener;
    private static int currentDurationHint;
    private static final Map<String, Integer> durationCache = new HashMap<>();
    private static final Map<String, String> localFileCache = new HashMap<>();

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && currentState == PlayerState.PLAYING) {
                try {
                    int pos = mediaPlayer.getCurrentPosition();
                    int dur = getDisplayDuration(mediaPlayer.getDuration());
                    if (activeListener != null) {
                        activeListener.onProgressUpdate(pos, dur);
                    }
                } catch (Throwable t) {
                    logger.error("Error in progress updater", t);
                }
                handler.postDelayed(this, 100);
            }
        }
    };

    public static synchronized String getCurrentUrl() {
        return currentUrl;
    }


    public static synchronized int getCurrentPosition() {
        if (mediaPlayer != null && (currentState == PlayerState.PLAYING || currentState == PlayerState.PAUSED)) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    public static synchronized int getDuration() {
        if (mediaPlayer != null && (currentState == PlayerState.PLAYING || currentState == PlayerState.PAUSED)) {
            try {
                return getDisplayDuration(mediaPlayer.getDuration());
            } catch (Throwable ignored) {}
        }
        return currentDurationHint;
    }

    public static synchronized void getDurationAsync(final String url, final DurationCallback callback) {
        if (url == null) {
            if (callback != null) callback.onDurationFetched(0);
            return;
        }

        final Integer cached = durationCache.get(url);
        if (cached != null) {
            if (callback != null) callback.onDurationFetched(cached);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                int duration = 0;
                try {
                    duration = AudioDurationReader.readDurationMs(url);
                } catch (Throwable t) {
                    logger.error("Failed to pre-fetch duration for " + url, t);
                }

                final int finalDuration = duration;
                if (finalDuration > 0) {
                    synchronized (AudioPlayerManager.class) {
                        durationCache.put(url, finalDuration);
                    }
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onDurationFetched(finalDuration);
                        }
                    }
                });
            }
        }).start();
    }

    public static synchronized void registerListener(String url, PlayerListener listener) {
        if (url != null && url.equals(currentUrl)) {
            activeListener = listener;
            listener.onStateChanged(currentState);
            if (currentState == PlayerState.PLAYING) {
                // Restart progress loop for the newly bound view
                handler.removeCallbacks(progressUpdater);
                handler.post(progressUpdater);
            }
        }
    }

    public static synchronized void unregisterListener(PlayerListener listener) {
        if (activeListener == listener) {
            activeListener = null;
        }
    }

    public static synchronized void play(Context context, final String url, PlayerListener listener) {
        if (url == null) return;

        // If the same URL is already playing/paused, handle as resume/pause
        if (url.equals(currentUrl)) {
            if (currentState == PlayerState.PAUSED) {
                resume();
            } else if (currentState == PlayerState.PLAYING) {
                pause();
            }
            return;
        }

        PlayerListener previousListener = activeListener;

        // Stop current audio
        stopInternal();
        if (previousListener != null) {
            previousListener.onStateChanged(PlayerState.IDLE);
            previousListener.onProgressUpdate(0, 0);
        }

        currentUrl = url;
        activeListener = listener;
        Integer cachedDuration = durationCache.get(url);
        currentDurationHint = cachedDuration != null ? cachedDuration : 0;
        currentState = PlayerState.LOADING;
        if (activeListener != null) {
            activeListener.onStateChanged(PlayerState.LOADING);
        }

        boolean shouldDownload = false;
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".ogg") || lowerUrl.contains(".opus")) {
            shouldDownload = true;
        }

        if (shouldDownload) {
            downloadAndPlay(context, url, listener);
        } else {
            startMediaPlayerStreaming(context, url);
        }
    }

    private static void downloadAndPlay(final Context context, final String url, final PlayerListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String localPath;
                    synchronized (AudioPlayerManager.class) {
                        localPath = localFileCache.get(url);
                    }

                    if (localPath == null) {
                        java.io.File cacheFile = java.io.File.createTempFile("audio_cache_", ".opus", context.getCacheDir());
                        cacheFile.deleteOnExit();

                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(15000);

                        java.io.InputStream in = conn.getInputStream();
                        java.io.FileOutputStream out = new java.io.FileOutputStream(cacheFile);
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        out.close();
                        in.close();
                        conn.disconnect();

                        localPath = cacheFile.getAbsolutePath();
                        synchronized (AudioPlayerManager.class) {
                            localFileCache.put(url, localPath);
                        }
                    }

                    final String finalPath = localPath;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (AudioPlayerManager.class) {
                                if (!url.equals(currentUrl)) {
                                    return;
                                }
                                startMediaPlayerWithLocalFile(context, url, finalPath);
                            }
                        }
                    });
                } catch (final Throwable t) {
                    logger.error("Failed to download audio for local playback: " + url, t);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (AudioPlayerManager.class) {
                                if (!url.equals(currentUrl)) return;
                                startMediaPlayerStreaming(context, url);
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private static void startMediaPlayerWithLocalFile(final Context context, final String url, final String localPath) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(localPath);
            setupMediaPlayerListeners(url);
            mediaPlayer.prepareAsync();
        } catch (Throwable t) {
            logger.error("Failed to start media player for local file: " + localPath, t);
            startMediaPlayerStreaming(context, url);
        }
    }

    private static void startMediaPlayerStreaming(final Context context, final String url) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(url);
            fetchDurationHintAsync(url);
            setupMediaPlayerListeners(url);
            mediaPlayer.prepareAsync();
        } catch (Throwable t) {
            logger.error("Failed to set up streaming media player for URL: " + url, t);
            currentState = PlayerState.IDLE;
            currentUrl = null;
            currentDurationHint = 0;
            StickyAudioPlayerController.hide();
            if (activeListener != null) {
                activeListener.onStateChanged(PlayerState.IDLE);
            }
            stopInternal();
        }
    }

    private static void setupMediaPlayerListeners(final String url) {
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                synchronized (AudioPlayerManager.class) {
                    if (!url.equals(currentUrl)) {
                        mp.release();
                        return;
                    }
                    try {
                        int duration = safeDuration(mp);
                        if (duration > 0) {
                            currentDurationHint = duration;
                            durationCache.put(url, duration);
                        }
                        mp.start();
                        currentState = PlayerState.PLAYING;
                        if (activeListener != null) {
                            activeListener.onStateChanged(PlayerState.PLAYING);
                            activeListener.onProgressUpdate(mp.getCurrentPosition(), getDisplayDuration(duration));
                        }
                        handler.removeCallbacks(progressUpdater);
                        handler.post(progressUpdater);
                    } catch (Throwable t) {
                        logger.error("Failed to start media player", t);
                        currentState = PlayerState.IDLE;
                        StickyAudioPlayerController.hide();
                        if (activeListener != null) {
                            activeListener.onStateChanged(PlayerState.IDLE);
                        }
                    }
                }
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                synchronized (AudioPlayerManager.class) {
                    currentState = PlayerState.IDLE;
                    currentUrl = null;
                    currentDurationHint = 0;
                    StickyAudioPlayerController.hide();
                    if (activeListener != null) {
                        activeListener.onStateChanged(PlayerState.IDLE);
                        activeListener.onProgressUpdate(0, 0);
                    }
                    stopInternal();
                }
            }
        });

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                logger.error("MediaPlayer error: what=" + what + ", extra=" + extra, null);
                synchronized (AudioPlayerManager.class) {
                    currentState = PlayerState.IDLE;
                    currentUrl = null;
                    currentDurationHint = 0;
                    StickyAudioPlayerController.hide();
                    if (activeListener != null) {
                        activeListener.onStateChanged(PlayerState.IDLE);
                    }
                    stopInternal();
                }
                return true;
            }
        });
    }

    public static synchronized void clearCache() {
        for (String path : localFileCache.values()) {
            try {
                new java.io.File(path).delete();
            } catch (Throwable ignored) {}
        }
        localFileCache.clear();
        durationCache.clear();
    }

    public static synchronized void pause() {
        if (mediaPlayer != null && currentState == PlayerState.PLAYING) {
            try {
                mediaPlayer.pause();
                currentState = PlayerState.PAUSED;
                if (activeListener != null) {
                    activeListener.onStateChanged(PlayerState.PAUSED);
                }
            } catch (Throwable t) {
                logger.error("Failed to pause media player", t);
            }
        }
    }

    public static synchronized void resume() {
        if (mediaPlayer != null && currentState == PlayerState.PAUSED) {
            try {
                mediaPlayer.start();
                currentState = PlayerState.PLAYING;
                if (activeListener != null) {
                    activeListener.onStateChanged(PlayerState.PLAYING);
                }
                handler.removeCallbacks(progressUpdater);
                handler.post(progressUpdater);
            } catch (Throwable t) {
                logger.error("Failed to resume media player", t);
            }
        }
    }

    public static synchronized void seekTo(int positionMs) {
        if (mediaPlayer != null && (currentState == PlayerState.PLAYING || currentState == PlayerState.PAUSED)) {
            try {
                if (safeDuration(mediaPlayer) > 0) {
                    mediaPlayer.seekTo(positionMs);
                } else {
                    logger.warn("Stream is not seekable (native duration is 0 or unknown). Ignoring seek.");
                }
            } catch (Throwable t) {
                logger.error("Failed to seek media player", t);
            }
        }
    }

    public static synchronized void stop() {
        stopInternal();
        currentUrl = null;
        currentState = PlayerState.IDLE;
        currentDurationHint = 0;
        StickyAudioPlayerController.hide();
    }

    private static int safeDuration(MediaPlayer player) {
        if (player == null) return 0;
        try {
            int duration = player.getDuration();
            return duration > 0 ? duration : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int getDisplayDuration(int mediaPlayerDuration) {
        return mediaPlayerDuration > 0 ? mediaPlayerDuration : currentDurationHint;
    }

    private static void fetchDurationHintAsync(final String url) {
        if (url == null || durationCache.containsKey(url)) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int duration = AudioDurationReader.readDurationMs(url);
                    if (duration <= 0) return;

                    final int finalDuration = duration;
                    synchronized (AudioPlayerManager.class) {
                        durationCache.put(url, finalDuration);
                        if (!url.equals(currentUrl)) return;

                        currentDurationHint = finalDuration;
                        if (activeListener != null) {
                            activeListener.onProgressUpdate(getCurrentPosition(), finalDuration);
                        }
                    }
                } catch (Throwable t) {
                    logger.error("Failed to read media duration hint", t);
                }
            }
        }).start();
    }

    private static void stopInternal() {
        handler.removeCallbacks(progressUpdater);
        if (mediaPlayer != null) {
            if (currentState == PlayerState.PLAYING || currentState == PlayerState.PAUSED) {
                try {
                    mediaPlayer.stop();
                } catch (Throwable ignored) {}
            }
            try {
                mediaPlayer.release();
            } catch (Throwable ignored) {}
            mediaPlayer = null;
        }
    }
}
