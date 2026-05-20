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

    private static final int ORIGINAL_BOTTOM_TO_BOTTOM_KEY = 1937548293;
    private static final int ORIGINAL_BOTTOM_TO_TOP_KEY = 1937548294;
    private static final int ORIGINAL_BOTTOM_MARGIN_KEY = 1937548295;
    private static final int HAS_ALTERED_CONSTRAINTS_KEY = 1937548296;

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
                        } else {
                            applyPlayerConstraints(binding, playerView);
                        }

                        playerView.setVisibility(View.VISIBLE);
                        String playbackUrl = audio.url != null ? audio.url : audio.proxy_url;
                        playerView.configure(playbackUrl, audio.filename);
                    } else {
                        if (playerView != null) {
                            root.removeView(playerView);
                        }
                        restoreAnchorConstraints(binding);
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

    private static void applyPlayerConstraints(WidgetChatListAdapterItemAttachmentBinding binding, AudioPlayerView playerView) {
        View anchor = binding.d;
        if (anchor == null) return;

        ConstraintLayout.LayoutParams anchorLp = (ConstraintLayout.LayoutParams) anchor.getLayoutParams();
        if (anchorLp == null) return;

        // Only save original constraints once
        if (anchor.getTag(HAS_ALTERED_CONSTRAINTS_KEY) == null) {
            anchor.setTag(ORIGINAL_BOTTOM_TO_BOTTOM_KEY, anchorLp.bottomToBottom);
            anchor.setTag(ORIGINAL_BOTTOM_TO_TOP_KEY, anchorLp.bottomToTop);
            anchor.setTag(ORIGINAL_BOTTOM_MARGIN_KEY, anchorLp.bottomMargin);
            anchor.setTag(HAS_ALTERED_CONSTRAINTS_KEY, true);
        }

        Object bToB = anchor.getTag(ORIGINAL_BOTTOM_TO_BOTTOM_KEY);
        Object bToT = anchor.getTag(ORIGINAL_BOTTOM_TO_TOP_KEY);
        Object bMarg = anchor.getTag(ORIGINAL_BOTTOM_MARGIN_KEY);

        int origBToB = bToB != null ? (int) bToB : ConstraintLayout.LayoutParams.UNSET;
        int origBToT = bToT != null ? (int) bToT : ConstraintLayout.LayoutParams.UNSET;
        int origBMarg = bMarg != null ? (int) bMarg : 0;

        // Clear the anchor's bottom constraints
        anchorLp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
        anchorLp.bottomToTop = ConstraintLayout.LayoutParams.UNSET;
        anchorLp.bottomMargin = 0;
        anchor.setLayoutParams(anchorLp);

        // Apply player view constraints
        ConstraintLayout.LayoutParams playerLp = (ConstraintLayout.LayoutParams) playerView.getLayoutParams();
        if (playerLp == null) {
            playerLp = new ConstraintLayout.LayoutParams(0, com.aliucord.utils.DimenUtils.dpToPx(48));
        } else {
            playerLp.width = 0;
            playerLp.height = com.aliucord.utils.DimenUtils.dpToPx(48);
        }

        playerLp.leftToLeft = anchor.getId();
        playerLp.rightToRight = anchor.getId();
        playerLp.topToBottom = anchor.getId();
        playerLp.topMargin = -com.aliucord.utils.DimenUtils.dpToPx(10); // Overlap
        
        // Chain the player bottom constraints using the original anchor constraints
        playerLp.bottomToBottom = origBToB;
        playerLp.bottomToTop = origBToT;
        playerLp.bottomMargin = origBMarg;

        playerView.setLayoutParams(playerLp);
    }

    private static void restoreAnchorConstraints(WidgetChatListAdapterItemAttachmentBinding binding) {
        View anchor = binding.d;
        if (anchor == null) return;

        if (anchor.getTag(HAS_ALTERED_CONSTRAINTS_KEY) != null) {
            ConstraintLayout.LayoutParams anchorLp = (ConstraintLayout.LayoutParams) anchor.getLayoutParams();
            if (anchorLp != null) {
                Object bToB = anchor.getTag(ORIGINAL_BOTTOM_TO_BOTTOM_KEY);
                Object bToT = anchor.getTag(ORIGINAL_BOTTOM_TO_TOP_KEY);
                Object bMarg = anchor.getTag(ORIGINAL_BOTTOM_MARGIN_KEY);

                anchorLp.bottomToBottom = bToB != null ? (int) bToB : ConstraintLayout.LayoutParams.UNSET;
                anchorLp.bottomToTop = bToT != null ? (int) bToT : ConstraintLayout.LayoutParams.UNSET;
                anchorLp.bottomMargin = bMarg != null ? (int) bMarg : 0;
                anchor.setLayoutParams(anchorLp);
            }
            anchor.setTag(HAS_ALTERED_CONSTRAINTS_KEY, null);
        }
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
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                com.aliucord.utils.DimenUtils.dpToPx(48)
        );

        root.addView(playerView, playerLp);

        applyPlayerConstraints(binding, playerView);

        return playerView;
    }
}
