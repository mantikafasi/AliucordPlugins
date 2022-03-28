package com.aliucord.plugins;

import com.aliucord.Constants;
import com.aliucord.Http;
import com.aliucord.SettingsUtilsJSON;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.utils.GsonUtils;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ThemeRepoAPI {
    public static final String API_URL = "https://mantikralligi1.pythonanywhere.com";
    public static final String GITHIB_THEMEREPO_URL = "https://raw.githubusercontent.com/mantikafasi/AliucordThemeRepo/main/";
    public static HashMap<String, Object> localFilters = new HashMap<>();
    public static HashMap<String, String> filters;
    public static final File THEME_DIR = new File(Constants.BASE_PATH, "themes");

    public static List<Theme> getThemes(){
        try {
            var response = Http.simpleGet(GITHIB_THEMEREPO_URL + "themeList.json");
            var themes = (List<Theme>)GsonUtils.fromJson(response, TypeToken.getParameterized(ArrayList.class,Theme.class).getType());
            return themes;
        } catch (IOException e) { e.printStackTrace(); }
        return null;
    }

    public static boolean addTheme(Theme theme,String token,String themeJSON) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("token",token);
            payload.put("themeInfo",GsonUtils.toJson(theme));
            payload.put("theme",themeJSON);
        } catch (JSONException e) { e.printStackTrace(); }

        try {
            String response = Http.simplePost(API_URL + "addTheme",payload.toString());
            if (response.equals("successful")) return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean setThemeStatus(String name,boolean status) {
        try {
            new SettingsUtilsJSON("Themer").setBool(name + "-enabled",status);
            return true;
        } catch (Exception ignored){}
        return false;
    }

    public static boolean exists(String name){
        return new File(THEME_DIR,name).exists();
    }

    public static boolean installTheme(String name) {
        try {
            new Http.Request(GITHIB_THEMEREPO_URL + "/themes/" + name + ".json").execute().saveToFile(new File(THEME_DIR,name +".json"));
            Utils.showToast("Successfully installed Theme");
            setThemeStatus(name,false);
            Utils.promptRestart();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Utils.showToast("Failed to install theme (definiletly not my fault)");
            return false;
        }
    }

    public static boolean deleteTheme(String name) {
        var status = new File(THEME_DIR,name + ".json").delete();
        if (status) Utils.showToast("Successfully Uninstalled Theme"); else Utils.showToast("Failed to uninstall theme");
        setThemeStatus(name,false);
        return status;
    }
}
