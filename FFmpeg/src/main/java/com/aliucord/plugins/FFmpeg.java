package com.aliucord.plugins;

import android.content.Context;

import com.aliucord.Constants;
import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.arthenica.mobileffmpeg.Config;

import java.io.File;

import kotlin.io.FilesKt;

@SuppressWarnings("unused")
@AliucordPlugin
public class FFmpeg extends Plugin {

    @Override
    public void start(Context context) {
        try {
            // Patch the HTTP request execution to intercept file uploads
            patchFileUploads();
            logger.info("FFmpeg plugin loaded - HEIC to JPEG conversion enabled");
        } catch (Exception e) {
            logger.error("Failed to start FFmpeg plugin", e);
        }
    }

    private void patchFileUploads() throws NoSuchMethodException {
        // Patch the Http.Request.executeWithBody method to intercept file uploads
        patcher.patch(
            Http.Request.class.getDeclaredMethod("executeWithBody", byte[].class),
            new Hook(cf -> {
                try {
                    // Check if this is a file upload request
                    Http.Request request = (Http.Request) cf.thisObject;
                    String contentType = request.headers.get("Content-Type");
                    
                    // Check if this is an image upload (HEIC files are uploaded as images)
                    if (contentType != null && contentType.startsWith("image/")) {
                        // Check for HEIC content type or check the actual file being uploaded
                        // We need to trace back to find the File object
                        // This is complex, so let's use a different approach
                    }
                } catch (Exception e) {
                    logger.error("Error in upload interception", e);
                }
            })
        );

        // Better approach: Patch FilesKt.readBytes which is used to read files for upload
        try {
            patcher.patch(
                FilesKt.class.getDeclaredMethod("readBytes", File.class),
                new Hook(cf -> {
                    try {
                        File file = (File) cf.args[0];
                        if (file != null && file.exists()) {
                            String fileName = file.getName().toLowerCase();
                            if (fileName.endsWith(".heic") || fileName.endsWith(".heif")) {
                                logger.info("Intercepted HEIC file: " + file.getName());
                                File converted = convertHeicToJpeg(file);
                                if (converted != null && !converted.equals(file)) {
                                    // Replace the file argument with the converted file
                                    cf.args[0] = converted;
                                    logger.info("Replaced with converted JPEG: " + converted.getName());
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error processing file in readBytes", e);
                    }
                })
            );
        } catch (Exception e) {
            logger.warn("Could not patch FilesKt.readBytes, trying alternative approach", e);
        }
    }

    /**
     * Convert a HEIC file to JPEG
     * @param heicFile The input HEIC file
     * @return The converted JPEG file, or the original file if conversion fails
     */
    public File convertHeicToJpeg(File heicFile) {
        if (heicFile == null || !heicFile.exists()) {
            logger.error("Input file does not exist");
            return heicFile;
        }

        String fileName = heicFile.getName().toLowerCase();
        if (!fileName.endsWith(".heic") && !fileName.endsWith(".heif")) {
            // Not a HEIC file, return as-is
            return heicFile;
        }

        try {
            // Create output file in the same directory
            String outputPath = heicFile.getAbsolutePath();
            outputPath = outputPath.replaceAll("(?i)\\.(heic|heif)$", "_converted.jpg");
            File outputFile = new File(outputPath);

            // Check if already converted
            if (outputFile.exists() && outputFile.length() > 0) {
                logger.info("Using previously converted file: " + outputFile.getName());
                return outputFile;
            }

            // Build FFmpeg command to convert HEIC to JPEG
            String[] command = {
                "-i", heicFile.getAbsolutePath(),
                "-q:v", "2", // Quality setting (2 is high quality)
                "-y", // Overwrite output file if exists
                outputFile.getAbsolutePath()
            };

            logger.info("Converting HEIC to JPEG: " + heicFile.getName() + " -> " + outputFile.getName());
            
            // Execute FFmpeg command
            int result = com.arthenica.mobileffmpeg.FFmpeg.execute(command);

            if (result == 0) {
                logger.info("Conversion successful: " + outputFile.getName() + " (" + outputFile.length() + " bytes)");
                Utils.showToast("Converted HEIC to JPEG: " + heicFile.getName());
                return outputFile;
            } else {
                logger.error("FFmpeg conversion failed with code: " + result);
                String output = Config.getLastCommandOutput();
                logger.error("FFmpeg output: " + output);
                Utils.showToast("Failed to convert HEIC image");
                return heicFile; // Return original file on failure
            }
        } catch (Exception e) {
            logger.error("Error converting HEIC to JPEG", e);
            Utils.showToast("Error converting HEIC: " + e.getMessage());
            return heicFile; // Return original file on error
        }
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
