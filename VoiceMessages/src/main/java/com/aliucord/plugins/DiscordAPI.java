package com.aliucord.plugins;

import com.aliucord.Http;
import com.aliucord.utils.GsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import kotlin.io.FilesKt;

public class DiscordAPI {

    public static String uploadFile(File file, long channel, String extension) {
        try {
            var req = Http.Request.newDiscordRNRequest("/channels/" + channel + "/attachments","POST")
                    .setHeader("content-type", "application/json")
                    .setHeader("x-discord-locale","en-US");

            var body = new AttachmentBody(file.getName(), (int) file.length());

            var response = req.executeWithBody(GsonUtils.toJson(body));

            if (!isSuccessful(response.statusCode)) {
                throw new RuntimeException("Failed to upload file: " + response.statusCode + " " + response.text());
            }
            var jsonResponse = new JSONObject(response.text());

            var attachment = jsonResponse.getJSONArray("attachments").getJSONObject(0);

            String type = ".ogg".equals(extension) ? "audio/ogg" : "audio/x-aac";
            var uploadReq = new Http.Request(attachment.getString("upload_url")).setHeader("Content-Type", type)
                    .setHeader("Content-Length", file.length() + "")
                    .setHeader("user-agent", "Discord-Android/175207;RNA");

            uploadReq.conn.setRequestMethod("PUT");
            var upload = uploadReq.executeWithBody(FilesKt.readBytes(file));
            if (!isSuccessful(upload.statusCode)) {
                throw new RuntimeException("Failed to upload file: " + upload.statusCode + " " + upload.text());
            }
            return attachment.getString("upload_filename");
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static String sendVoiceMessage(String fileName, float duration, String waveform, long channelID, String extension) {
        try {
            var request = Http.Request.newDiscordRNRequest("/channels/" + channelID + "/messages","POST");
            request.setHeader("content-type", "application/json");

            VoiceMessageBody body = new VoiceMessageBody(channelID, new VoiceMessageBody.Attachment(
                    "voice-message" + extension,
                    fileName,
                    duration,
                    waveform
            ));
            var response = request.executeWithBody(GsonUtils.toJson(body));
            if (!isSuccessful(response.statusCode)) {
                throw new RuntimeException("Failed to send message: " + response.statusCode + " " + response.text());
            }
            return "Success";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
