package com.aliucord.plugins;

import com.aliucord.Constants;
import com.aliucord.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import kotlin.io.FilesKt;

public class ThemeRepoUtils {
    public static final File THEME_DIR = new File(Constants.BASE_PATH, "themes");
    /*  Returns themes in <filename,themename> format */
    public static HashMap<String, String> themes = new HashMap<>();

    public static String getThemeName(File theme) {
        var themeJSON = getTheme(theme);
        if (themeJSON == null) return null;
        try {
            if (themeJSON.has("manifest")) {
                return themeJSON.getJSONObject("manifest").getString("name");
            } else if (themeJSON.has("name")) return themeJSON.getString("name");

        } catch (Exception e) {
            new Logger("ThemeRepo").error(new Exception("Bad Things Happened;1"));
        }
        return null;
    }

    public static String getFileNameWithName(String themeName) {
        for (var theme : getThemeNames().entrySet()) {
            if (theme.getValue().equals(themeName)) return theme.getKey();
        }
        return null;
    }

    public static JSONObject getTheme(File file) {
        try {
            return new JSONObject(FilesKt.readText(file, StandardCharsets.UTF_8));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HashMap<String, String> getThemeNames() {
        var themeFiles = THEME_DIR.listFiles();
        for (var theme : themeFiles) {
            if (themes.containsKey(theme.getName())) continue;
            try {
                var themeName = getThemeName(theme);
                if (themeName != null) themes.put(theme.getName(), themeName);
            } catch (Exception e) {
                new Logger("ThemeRepo").error(new Exception("Bad Things Happened"));
            }
        }
        return themes;
    }


}
