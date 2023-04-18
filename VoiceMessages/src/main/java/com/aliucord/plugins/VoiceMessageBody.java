package com.aliucord.plugins;

import com.discord.models.domain.NonceGenerator;
import com.discord.utilities.time.ClockFactory;

import java.util.ArrayList;
import java.util.List;

public class VoiceMessageBody {

    String content = "";
    Long channel_id;
    int type = 0;
    int flags = 8192;
    String nonce = String.valueOf(NonceGenerator.computeNonce(ClockFactory.get()));
    List<Attachment> attachments = new ArrayList<>();

    public VoiceMessageBody(Long channel_id, Attachment attachment) {
        this.channel_id = channel_id;
        this.attachments.add(attachment);
    }

    public static class Attachment {
        String id = "0";
        String filename;
        String uploaded_filename;
        float duration_secs;
        String waveform;

        public Attachment(String filename, String uploaded_filename, float duration_secs, String waveform) {
            this.filename = filename;
            this.uploaded_filename = uploaded_filename;
            this.duration_secs = duration_secs;
            this.waveform = waveform;
        }
    }
}

