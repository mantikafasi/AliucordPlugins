package com.aliucord.plugins;

import android.content.Context;
import android.net.Uri;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.arthenica.mobileffmpeg.Config;
import com.discord.api.message.LocalAttachment;
import com.lytefast.flexinput.model.Attachment;

import java.io.File;

@SuppressWarnings("unused")
@AliucordPlugin
public class FFmpeg extends Plugin {

    @Override
    public void start(Context context) {
        try {
            // Patch the LocalAttachment creation to intercept HEIC files
            patchLocalAttachment();
            logger.info("FFmpeg plugin loaded - HEIC to JPEG conversion enabled");
        } catch (Exception e) {
            logger.error("Failed to start FFmpeg plugin", e);
        }
    }

    private void patchLocalAttachment() throws NoSuchMethodException {
        // Patch the LocalAttachment constructor to intercept HEIC files
        patcher.patch(
            LocalAttachment.class.getDeclaredConstructor(long.class, String.class, String.class),
            new Hook(cf -> {
                try {
                    String uriString = (String) cf.args[1];
                    String displayName = (String) cf.args[2];
                    
                    if (uriString != null && displayName != null) {
                        // Check if this is a HEIC file
                        String lowerDisplayName = displayName.toLowerCase();
                        if (lowerDisplayName.endsWith(".heic") || lowerDisplayName.endsWith(".heif")) {
                            logger.info("Intercepted HEIC file in LocalAttachment: " + displayName);
                            
                            // Get the file from URI
                            Uri uri = Uri.parse(uriString);
                            File heicFile = new File(uri.getPath());
                            
                            if (heicFile.exists()) {
                                File converted = convertHeicToJpeg(heicFile);
                                if (converted != null && !converted.equals(heicFile)) {
                                    // Replace the URI and display name with the converted file
                                    cf.args[1] = Uri.fromFile(converted).toString();
                                    cf.args[2] = converted.getName();
                                    logger.info("Replaced LocalAttachment with converted JPEG: " + converted.getName());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing LocalAttachment", e);
                }
            })
        );
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
