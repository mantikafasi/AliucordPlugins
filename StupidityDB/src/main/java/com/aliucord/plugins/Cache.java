package com.aliucord.plugins;

import android.util.Pair;

import com.aliucord.SettingsUtilsJSON;
import com.aliucord.api.SettingsAPI;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class Cache {
    SettingsAPI settings = new SettingsAPI("StupidityDBCache");

    public void setUserCache(long id,String stupidity){
        long time = Calendar.getInstance().getTimeInMillis()/1000 + 30;
        settings.setObject(String.valueOf(id),new Pair<String,Long>(stupidity,time));//stupidity and time its saved
    }
    public boolean isCached(long id){
        return settings.exists(String.valueOf(id));
    }
    public String getCached(long id){
        var pair = settings.getObject(String.valueOf(id), new Pair<>("", (long) 0), TypeToken.getParameterized(Pair.class,String.class,Long.class).getType());
        long time = Calendar.getInstance().getTimeInMillis()/1000;
        if(pair.second<time)settings.remove(String.valueOf(id));
        return pair.first;
    }
}
