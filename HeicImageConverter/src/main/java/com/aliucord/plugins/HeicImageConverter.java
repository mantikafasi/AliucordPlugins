package com.aliucord.plugins;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.os.Build;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.patcher.PreHook;
import com.discord.utilities.rest.AttachmentRequestBody;
import com.discord.utilities.rest.SendUtils;
import com.lytefast.flexinput.model.Attachment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.WeakHashMap;

import okhttp3.MediaType;
import okio.BufferedSink;

@SuppressWarnings("unused")
@AliucordPlugin
public class HeicImageConverter extends Plugin {
    private static final MediaType JPEG_MEDIA_TYPE = MediaType.b("image/jpeg");
    private Context appContext;
    private Field attachmentField;
    private Field fileUploadContentLengthField;
    private Field fileUploadMimeTypeField;
    private Field fileUploadNameField;
    private final Map<String, ConvertedImage> conversionsByUri = Collections.synchronizedMap(new HashMap<>());
    private final Map<AttachmentRequestBody, ConvertedImage> convertedBodies = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void start(Context context) {
        appContext = context.getApplicationContext();

        try {
            attachmentField = AttachmentRequestBody.class.getDeclaredField("attachment");
            attachmentField.setAccessible(true);
            fileUploadContentLengthField = SendUtils.FileUpload.class.getDeclaredField("contentLength");
            fileUploadContentLengthField.setAccessible(true);
            fileUploadMimeTypeField = SendUtils.FileUpload.class.getDeclaredField("mimeType");
            fileUploadMimeTypeField.setAccessible(true);

            // change filename to .jpg from .heic
            patcher.patch(
                    SendUtils.class.getDeclaredMethod("getPart", Attachment.class, ContentResolver.class, String.class),
                    new PreHook(callFrame -> {
                        Attachment<?> attachment = (Attachment<?>) callFrame.args[0];
                        ConvertedImage converted = getOrConvertAttachment(attachment);
                        if (converted == null) return;

                        callFrame.args[0] = new Attachment<>(attachment.getId(), attachment.getUri(), converted.displayName, attachment.getData(), attachment.getSpoiler());
                        callFrame.args[2] = converted.displayName;
                    })
            );

            patcher.patch(
                    AttachmentRequestBody.class.getDeclaredConstructor(ContentResolver.class, Attachment.class),
                    new Hook(callFrame -> {
                        try {
                            Attachment<?> attachment = getAttachment((AttachmentRequestBody) callFrame.thisObject);
                            ConvertedImage converted = conversionsByUri.get(uriKey(attachment));
                            if (converted != null) convertedBodies.put((AttachmentRequestBody) callFrame.thisObject, converted);
                        } catch (Throwable e) {
                            logger.error("Failed to bind converted HEIC upload body", e);
                        }
                    })
            );

            patcher.patch(
                    AttachmentRequestBody.class.getDeclaredMethod("contentLength"),
                    new Hook(callFrame -> {
                        ConvertedImage converted = convertedBodies.get((AttachmentRequestBody) callFrame.thisObject);
                        if (converted != null) callFrame.setResult((long) converted.bytes.length);
                    })
            );

            // write jpeg bytes instead of heic bytes
            patcher.patch(
                    AttachmentRequestBody.class.getDeclaredMethod("writeTo", BufferedSink.class),
                    new InsteadHook(callFrame -> {
                        try {
                            ConvertedImage converted = convertedBodies.get((AttachmentRequestBody) callFrame.thisObject);
                            if (converted == null) return de.robv.android.xposed.XposedBridge.invokeOriginalMethod(callFrame.method, callFrame.thisObject, callFrame.args);

                            ((BufferedSink) callFrame.args[0]).write(converted.bytes);
                            logger.info("Wrote converted JPG upload: " + converted.displayName);
                            Utils.showToast("Converted HEIC to JPG");
                            return null;
                        } catch (Throwable e) {
                            logger.error("Failed to write converted JPG upload", e);
                            throw new RuntimeException(e);
                        }
                    })
            );

            patcher.patch(
                    SendUtils.FileUpload.class.getDeclaredMethod("getContentLength"),
                    new Hook(callFrame -> {
                        ConvertedImage converted = convertedFromUpload((SendUtils.FileUpload) callFrame.thisObject);
                        if (converted != null) callFrame.setResult((long) converted.bytes.length);
                    })
            );

            logger.info("HEIC to JPG converter loaded");
        } catch (Throwable e) {
            logger.error("Failed to start HEIC to JPG converter", e);
            Utils.showToast("Failed to start HEIC to JPG converter");
        }
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        convertedBodies.clear();
        conversionsByUri.clear();
    }

    private ConvertedImage getOrConvertAttachment(Attachment<?> attachment) {
        ConvertedImage existing = conversionsByUri.get(uriKey(attachment));
        if (existing != null) return existing;

        ConvertedImage converted = convertAttachment(attachment);
        if (converted != null) conversionsByUri.put(uriKey(attachment), converted);
        return converted;
    }

    private ConvertedImage convertAttachment(Attachment<?> attachment) {
        if (attachment == null || attachment.getDisplayName() == null || !isHeic(attachment.getDisplayName())) return null;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Utils.showToast("HEIC to JPG needs Android 9 or newer");
            logger.info("Cannot convert " + attachment.getDisplayName() + ": ImageDecoder needs API 28+");
            return null;
        }

        try {
            ImageDecoder.Source source = ImageDecoder.createSource(appContext.getContentResolver(), attachment.getUri());
            Bitmap bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
            String outputName = getJpegName(attachment.getDisplayName());
            byte[] jpegBytes = convertToJpegBytes(bitmap);

            logger.info("Converted HEIC attachment for JPG upload: " + attachment.getDisplayName() + " -> " + outputName);
            return new ConvertedImage(outputName, jpegBytes);
        } catch (Throwable e) {
            logger.error("Failed to convert HEIC attachment: " + attachment.getDisplayName(), e);
            Utils.showToast("Failed to convert HEIC image");
            return null;
        }
    }

    private Attachment<?> getAttachment(AttachmentRequestBody body) throws IllegalAccessException {
        return (Attachment<?>) attachmentField.get(body);
    }

    private ConvertedImage convertedFromUpload(SendUtils.FileUpload upload) {
        if (upload == null || upload.getPart() == null || !(upload.getPart().b instanceof AttachmentRequestBody)) return null;
        return convertedBodies.get((AttachmentRequestBody) upload.getPart().b);
    }

    private byte[] convertToJpegBytes(Bitmap bitmap) throws IOException {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw new IOException("Bitmap compression returned false");
            }

            byte[] bytes = stream.toByteArray();
            if (bytes.length == 0) throw new IOException("Converted image is empty");
            return bytes;
        } finally {
            bitmap.recycle();
        }
    }

    private String getJpegName(String displayName) {
        String baseName = displayName.replaceAll("(?i)\\.(heic|heif)$", "");
        baseName = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (baseName.isEmpty()) baseName = "image";
        return baseName + ".jpg";
    }

    private static boolean isHeic(String displayName) {
        String lowerName = displayName.toLowerCase(Locale.ROOT);
        return lowerName.endsWith(".heic") || lowerName.endsWith(".heif");
    }

    private static String uriKey(Attachment<?> attachment) {
        return attachment != null && attachment.getUri() != null ? attachment.getUri().toString() : "";
    }

    private static class ConvertedImage {
        final String displayName;
        final byte[] bytes;

        ConvertedImage(String displayName, byte[] bytes) {
            this.displayName = displayName;
            this.bytes = bytes;
        }
    }
}
