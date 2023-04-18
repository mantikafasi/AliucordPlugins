package com.aliucord.plugins;

import com.aliucord.Http;
import com.aliucord.utils.GsonUtils;
import com.discord.utilities.analytics.AnalyticSuperProperties;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import kotlin.io.FilesKt;

public class DiscordAPI {

    public static String uploadFile(File file, long channel) {
        try {
            var req = Http.Request.newDiscordRequest("/channels/" + channel + "/attachments").setHeader("content-type", "application/json");
            req.conn.setRequestMethod("POST");

            var body = new AttachmentBody(file.getName(), (int) file.length());

            var response = req.executeWithBody(GsonUtils.toJson(body));

            if (response.statusCode != 200) {
                throw new RuntimeException("Failed to upload file: " + response.statusCode + " " + response.text());
            }
            var jsonResponse = new JSONObject(response.text());

            var attachment = jsonResponse.getJSONArray("attachments").getJSONObject(0);

            var uploadReq = new Http.Request(attachment.getString("upload_url")).setHeader("Content-Type", "audio/ogg")
                    .setHeader("Content-Length", file.length() + "")
                    .setHeader("user-agent", "Discord-Android/175207;RNA");
            uploadReq.conn.setRequestMethod("PUT");
            var upload = uploadReq.executeWithBody(FilesKt.readBytes(file));
            if (upload.statusCode != 200) {
                throw new RuntimeException("Failed to upload file: " + upload.statusCode + " " + upload.text());
            }
            return attachment.getString("upload_filename");
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static String sendVoiceMessage(String fileName, float duration, String waveform, long channelID) {
        try {
            var request = Http.Request.newDiscordRequest("/channels/" + channelID + "/messages");
            var superprops = AnalyticSuperProperties.INSTANCE.getSuperProperties();
            superprops.put("client_version", "175.6 - rn");
            superprops.put("client_build_number", "175206");

            request.setHeader("x-super-properties", String.valueOf(android.util.Base64.encodeToString(GsonUtils.toJson(superprops).getBytes(StandardCharsets.UTF_8), 2)));
            request.conn.setRequestMethod("POST");
            request.setHeader("content-type", "application/json")
                    .setHeader("user-agent", "Discord-Android/175207;RNA");

            VoiceMessageBody body = new VoiceMessageBody(channelID, new VoiceMessageBody.Attachment(
                    "voice-message.ogg",
                    fileName,
                    duration,
                    waveform
            ));
            request.executeWithBody(GsonUtils.toJson(body));
            return "Success";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
