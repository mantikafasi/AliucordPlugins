package com.aliucord.plugins.photoeditor;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.Locale;

import ja.burhanrashid52.photoeditor.PhotoFilter;

final class PhotoEditorUtils {
    private PhotoEditorUtils() {}

    static float[] getColorMatrixForFilter(PhotoFilter filter) {
        if (filter == null || filter == PhotoFilter.NONE) return null;
        switch (filter) {
            case GRAY_SCALE:
                return new float[]{
                    0.21f, 0.72f, 0.07f, 0, 0,
                    0.21f, 0.72f, 0.07f, 0, 0,
                    0.21f, 0.72f, 0.07f, 0, 0,
                    0,     0,     0,     1, 0
                };
            case SEPIA:
                return new float[]{
                    0.393f, 0.769f, 0.189f, 0, 0,
                    0.349f, 0.686f, 0.168f, 0, 0,
                    0.272f, 0.534f, 0.131f, 0, 0,
                    0,      0,      0,      1, 0
                };
            case NEGATIVE:
                return new float[]{
                    -1,  0,  0, 0, 255,
                     0, -1,  0, 0, 255,
                     0,  0, -1, 0, 255,
                     0,  0,  0, 1,   0
                };
            case BRIGHTNESS:
                return new float[]{
                    1, 0, 0, 0, 55,
                    0, 1, 0, 0, 55,
                    0, 0, 1, 0, 55,
                    0, 0, 0, 1,  0
                };
            case CONTRAST: {
                float s = 1.5f, t = (-.5f * s + .5f) * 255f;
                return new float[]{
                    s, 0, 0, 0, t,
                    0, s, 0, 0, t,
                    0, 0, s, 0, t,
                    0, 0, 0, 1, 0
                };
            }
            case SATURATE:
                return new float[]{
                     1.8f, -0.4f, -0.4f, 0, 0,
                    -0.4f,  1.8f, -0.4f, 0, 0,
                    -0.4f, -0.4f,  1.8f, 0, 0,
                     0,     0,     0,    1, 0
                };
            case TEMPERATURE:
                // Warm: boost red, reduce blue
                return new float[]{
                    1.15f, 0,    0,     0,  10,
                    0,     1.0f, 0,     0,   0,
                    0,     0,    0.75f, 0, -15,
                    0,     0,    0,     1,   0
                };
            case TINT:
                // Subtle cool tint
                return new float[]{
                    0.9f, 0,    0,    0, 0,
                    0,    1.0f, 0,    0, 5,
                    0,    0,    1.1f, 0, 10,
                    0,    0,    0,    1, 0
                };
            case DUE_TONE:
                // Duotone: map to teal-purple
                return new float[]{
                    0.3f, 0.5f, 0.2f, 0, 20,
                    0.1f, 0.4f, 0.5f, 0, 30,
                    0.4f, 0.2f, 0.4f, 0, 40,
                    0,    0,    0,    1,  0
                };
            case LOMISH:
                // Lo-fi: faded, slightly warm
                return new float[]{
                    0.9f, 0.1f, 0,    0, 20,
                    0,    0.85f,0.1f, 0, 15,
                    0.1f, 0,    0.8f, 0, 10,
                    0,    0,    0,    1,  0
                };
            case POSTERIZE: {
                // Crude posterize via high contrast + slight desaturate
                float ps = 2.5f, pt = (-.5f * ps + .5f) * 255f;
                return new float[]{
                    ps, 0,  0,  0, pt,
                    0,  ps, 0,  0, pt,
                    0,  0,  ps, 0, pt,
                    0,  0,  0,  1,  0
                };
            }
            case GRAIN:
                // Grain is random, can't do with ColorMatrix — return null
                return null;
            case FILL_LIGHT:
                return new float[]{
                    1, 0, 0, 0, 30,
                    0, 1, 0, 0, 30,
                    0, 0, 1, 0, 30,
                    0, 0, 0, 1,  0
                };
            case FISH_EYE:
                // Geometric distortion — not possible with ColorMatrix
                return null;
            case VIGNETTE:
                // Radial effect — not possible with ColorMatrix
                return null;
            case DOCUMENTARY:
                // Desaturated + high contrast
                return new float[]{
                    0.33f, 0.50f, 0.17f, 0,  0,
                    0.33f, 0.50f, 0.17f, 0,  0,
                    0.33f, 0.50f, 0.17f, 0,  0,
                    0,     0,     0,     1, -5
                };
            case SHARPEN:
                return null; // Convolution kernel — not possible with ColorMatrix
            case AUTO_FIX:
                return new float[]{
                    1.1f, 0,    0,    0, 5,
                    0,    1.1f, 0,    0, 5,
                    0,    0,    1.1f, 0, 5,
                    0,    0,    0,    1, 0
                };
            default:
                return null;
        }
    }

    static File nextOutputFile(Context context, String displayName) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PhotoEditor");
        if (!dir.exists() && !dir.mkdirs())
            dir = context.getCacheDir();
        return new File(dir, editedFileName(displayName));
    }

    static String editedFileName(String displayName) {
        String baseName = displayName == null ? "image" : displayName.replaceAll("(?i)\\.[a-z0-9]{1,5}$", "");
        baseName = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (baseName.isEmpty())
            baseName = "image";
        return baseName + "-edited-" + System.currentTimeMillis() + ".png";
    }

        static String humanize(String name) {
        String lower = name.toLowerCase(Locale.ROOT).replace('_', ' ');
        return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
    }

    static float[] buildCustomMatrix(float[] values) {
        float brightness = values[0];
        float contrast = values[1];
        float saturation = values[2];
        float hue = values[3];
        float temp = values[4];
        float tint = values[5];

        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();

        // Brightness & Contrast
        float t = (1f - contrast) * 128f + brightness;
        android.graphics.ColorMatrix cmContrast = new android.graphics.ColorMatrix(new float[]{
            contrast, 0, 0, 0, t,
            0, contrast, 0, 0, t,
            0, 0, contrast, 0, t,
            0, 0, 0, 1, 0
        });
        cm.postConcat(cmContrast);

        // Saturation
        android.graphics.ColorMatrix cmSat = new android.graphics.ColorMatrix();
        cmSat.setSaturation(saturation);
        cm.postConcat(cmSat);

        // Hue
        if (hue != 0f) {
            float cos = (float) Math.cos(hue * Math.PI / 180f);
            float sin = (float) Math.sin(hue * Math.PI / 180f);
            float lumR = 0.213f, lumG = 0.715f, lumB = 0.072f;
            android.graphics.ColorMatrix cmHue = new android.graphics.ColorMatrix(new float[]{
                lumR + cos * (1 - lumR) + sin * (-lumR), lumG + cos * (-lumG) + sin * (-lumG), lumB + cos * (-lumB) + sin * (1 - lumB), 0, 0,
                lumR + cos * (-lumR) + sin * (0.143f), lumG + cos * (1 - lumG) + sin * (0.140f), lumB + cos * (-lumB) + sin * (-0.283f), 0, 0,
                lumR + cos * (-lumR) + sin * (-(1 - lumR)), lumG + cos * (-lumG) + sin * (lumG), lumB + cos * (1 - lumB) + sin * (lumB), 0, 0,
                0, 0, 0, 1, 0
            });
            cm.postConcat(cmHue);
        }

        // Temperature (Warm/Cool) & Tint (Green/Magenta)
        if (temp != 0f || tint != 0f) {
            float rTemp = temp > 0 ? temp * 0.1f : 0;
            float bTemp = temp < 0 ? -temp * 0.1f : 0;
            float gTint = tint > 0 ? tint * 0.1f : 0;
            float rTint = tint < 0 ? -tint * 0.1f : 0;
            float bTint = tint < 0 ? -tint * 0.1f : 0;

            android.graphics.ColorMatrix cmTempTint = new android.graphics.ColorMatrix(new float[]{
                1f + rTemp + rTint, 0, 0, 0, 0,
                0, 1f + gTint, 0, 0, 0,
                0, 0, 1f + bTemp + bTint, 0, 0,
                0, 0, 0, 1, 0
            });
            cm.postConcat(cmTempTint);
        }

        return cm.getArray();
    }

}
