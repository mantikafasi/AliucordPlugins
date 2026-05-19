package com.aliucord.plugins.audioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import com.aliucord.Logger;

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

    private static MediaPlayer mediaPlayer;
    private static String currentUrl;
    private static PlayerState currentState = PlayerState.IDLE;
    private static PlayerListener activeListener;

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && currentState == PlayerState.PLAYING) {
                try {
                    int pos = mediaPlayer.getCurrentPosition();
                    int dur = mediaPlayer.getDuration();
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

    public static synchronized PlayerState getCurrentState() {
        return currentState;
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
                return mediaPlayer.getDuration();
            } catch (Throwable ignored) {}
        }
        return 0;
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

        // Stop current audio
        stopInternal();

        currentUrl = url;
        activeListener = listener;
        currentState = PlayerState.LOADING;
        if (activeListener != null) {
            activeListener.onStateChanged(PlayerState.LOADING);
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(url);

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    synchronized (AudioPlayerManager.class) {
                        if (!url.equals(currentUrl)) {
                            mp.release();
                            return;
                        }
                        try {
                            mp.start();
                            currentState = PlayerState.PLAYING;
                            if (activeListener != null) {
                                activeListener.onStateChanged(PlayerState.PLAYING);
                            }
                            handler.removeCallbacks(progressUpdater);
                            handler.post(progressUpdater);
                        } catch (Throwable t) {
                            logger.error("Failed to start media player", t);
                            currentState = PlayerState.IDLE;
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
                        if (activeListener != null) {
                            activeListener.onStateChanged(PlayerState.IDLE);
                        }
                        stopInternal();
                    }
                    return true;
                }
            });

            mediaPlayer.prepareAsync();
        } catch (Throwable t) {
            logger.error("Failed to set up media player for URL: " + url, t);
            currentState = PlayerState.IDLE;
            currentUrl = null;
            if (activeListener != null) {
                activeListener.onStateChanged(PlayerState.IDLE);
            }
            stopInternal();
        }
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
                mediaPlayer.seekTo(positionMs);
            } catch (Throwable t) {
                logger.error("Failed to seek media player", t);
            }
        }
    }

    public static synchronized void stop() {
        stopInternal();
        currentUrl = null;
        currentState = PlayerState.IDLE;
    }

    private static void stopInternal() {
        handler.removeCallbacks(progressUpdater);
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Throwable ignored) {}
            try {
                mediaPlayer.release();
            } catch (Throwable ignored) {}
            mediaPlayer = null;
        }
    }
}
