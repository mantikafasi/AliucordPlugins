package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import com.aliucord.Constants;
import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.PluginManager;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.plugins.filtering.Developer;
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

public class PluginRepoAPI {
    public static final String API_URL = "https://mantikralligi1.pythonanywhere.com";
    static Logger logger = new Logger("PluginRepoAPI");

    public static List<Plugin> getPlugins(){
        return getPlugins("");
    }

    public static List<Plugin> getPlugins(String query) {
        return getPlugins(query,0);
    }


    public static HashMap<String,String> filters;
    public static List<Plugin> getPlugins(String query,int index){
        var plugins = new ArrayList<Plugin>();
        try {
            JSONObject filter = new JSONObject(filters);
            filter.put("index",index);
            filter.put("query",query);

            var pluginarray = new JSONArray(Http.simplePost(API_URL +"/getPlugins",filter.toString()));
            logger.info(pluginarray.toString());
            for (int i = 0;i<pluginarray.length();i++) {
                var plugin = (JSONObject)pluginarray.get(i);
                plugins.add(getPluginFromJson(plugin));
            }
        } catch (Exception e) {
            new Logger("PluginRepo").error(e);

        }
        return plugins;
    }

    public static Plugin getPluginFromJson(JSONObject json) throws JSONException {

        var manifest = new Plugin.Manifest(json.getString("plugin_name"));
        manifest.version = json.getString("version");
        manifest.description = json.getString("description");
        manifest.authors = GsonUtils.fromJson(json.getString("author"),Plugin.Manifest.Author[].class);
        manifest.changelog = json.getString("changelog");
        manifest.updateUrl = json.getString("download_link"); // I know this is not updateurl
        var plugin = new Plugin(manifest) {
            @Override public void start(Context context) throws Throwable {}
            @Override public void stop(Context context) throws Throwable {}
        };
        return plugin;
    }

    public static boolean installPlugin(String pluginName, String url) {
        try {
            //copied from PluginFile.kt
            var response = new Http.Request(url).execute();
            var pluginFile = new File(Constants.PLUGINS_PATH,pluginName + ".zip");
            response.saveToFile(pluginFile);
            PluginManager.loadPlugin(Utils.getAppContext(), pluginFile);
            PluginManager.startPlugin(pluginName);
            return true;
        } catch (IOException e) {
            logger.error(e);
            return false;
        }
    }

    public static boolean deletePlugin(String plugin) {
        var success = new File(Constants.PLUGINS_PATH,plugin + ".zip").delete();
        PluginManager.stopPlugin(plugin);
        PluginManager.unloadPlugin(plugin);
        return success;
    }

    public static List<Developer> getDevelopers() {
        try{
            return GsonUtils.fromJson(Http.simpleGet(API_URL +"/getDevelopers"), TypeToken.getParameterized(ArrayList.class, Developer.class).type);
        }catch (Exception e) { return new ArrayList<>();}
    }
}
