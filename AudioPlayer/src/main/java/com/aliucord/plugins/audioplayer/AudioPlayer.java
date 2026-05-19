package com.aliucord.plugins.audioplayer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.GsonUtils;
import com.discord.databinding.WidgetChatListAdapterItemAttachmentBinding;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemAttachment;
import com.discord.widgets.chat.list.entries.AttachmentEntry;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.api.message.attachment.MessageAttachment;

import androidx.constraintlayout.widget.ConstraintLayout;

@AliucordPlugin
@SuppressWarnings("unused")
public class AudioPlayer extends Plugin {
    private static final Logger logger = new Logger("AudioPlayer");
    private static final String PLAYER_TAG = "AudioPlayerView";

    // Robust anti-obfuscation data model matching Discord API Attachment JSON properties
    public static class AudioAttachment {
        public String url;
        public String proxy_url;
        public String filename;
        public long size;
        public String content_type;
    }

    @Override
    public void start(Context context) throws Throwable {
        patcher.patch(
            WidgetChatListAdapterItemAttachment.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class),
            new Hook(callFrame -> {
                try {
                    AttachmentEntry entry = (AttachmentEntry) callFrame.args[1];
                    MessageAttachment attachment = entry.getAttachment();
                    if (attachment == null) return;

                    // Serialize to JSON and parse into our POJO to bypass ProGuard/R8 renaming
                    String json = GsonUtils.toJson(attachment);
                    AudioAttachment audio = GsonUtils.fromJson(json, AudioAttachment.class);
                    if (audio == null) return;

                    WidgetChatListAdapterItemAttachment item =
                            (WidgetChatListAdapterItemAttachment) callFrame.thisObject;
                    WidgetChatListAdapterItemAttachmentBinding binding =
                            WidgetChatListAdapterItemAttachment.access$getBinding$p(item);
                    ConstraintLayout root = binding.a;
                    AudioPlayerView playerView = root.findViewWithTag(PLAYER_TAG);

                    if (isAudioAttachment(audio)) {
                        if (playerView == null) {
                            playerView = createPlayerView(binding);
                        }

                        playerView.setVisibility(View.VISIBLE);
                        String playbackUrl = audio.url != null ? audio.url : audio.proxy_url;
                        playerView.configure(playbackUrl, audio.filename);
                    } else {
                        if (playerView != null) {
                            playerView.setVisibility(View.GONE);
                        }
                    }
                } catch (Throwable t) {
                    logger.error("Error in onConfigure attachment hook", t);
                }
            })
        );
        logger.info("AudioPlayer plugin successfully started!");
    }

    @Override
    public void stop(Context context) throws Throwable {
        patcher.unpatchAll();
        commands.unregisterAll();
        AudioPlayerManager.stop();
        logger.info("AudioPlayer plugin stopped.");
    }

    private static boolean isAudioAttachment(AudioAttachment audio) {
        if (audio == null) return false;

        if (audio.content_type != null && audio.content_type.toLowerCase().startsWith("audio/")) {
            return true;
        }

        if (audio.filename == null) return false;

        String lower = audio.filename.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".wav") ||
                lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".flac") ||
                lower.endsWith(".opus");
    }

    private static AudioPlayerView createPlayerView(WidgetChatListAdapterItemAttachmentBinding binding) {
        ConstraintLayout root = binding.a;
        AudioPlayerView playerView = new AudioPlayerView(root.getContext());
        playerView.setTag(PLAYER_TAG);
        playerView.setId(View.generateViewId());

        View anchor = binding.d;
        if (anchor.getId() == View.NO_ID) {
            anchor.setId(View.generateViewId());
        }

        ConstraintLayout.LayoutParams playerLp = new ConstraintLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = com.aliucord.utils.DimenUtils.dpToPx(6);
        playerLp.leftToLeft = anchor.getId();
        playerLp.rightToRight = anchor.getId();
        playerLp.topToBottom = anchor.getId();
        playerLp.topMargin = margin;

        root.addView(playerView, playerLp);
        return playerView;
    }
}
