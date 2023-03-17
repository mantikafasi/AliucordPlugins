package com.aliucord.plugins;

import android.util.Pair;

import com.aliucord.api.SettingsAPI;
import com.discord.api.user.User;
import com.google.gson.reflect.TypeToken;

import java.util.Calendar;

public class Cache {
    SettingsAPI settings = new SettingsAPI("UserReviewsCache");

    public void setUserCache(long id, User user) {
        long time = Calendar.getInstance().getTimeInMillis() / 1000 + 21600;
        settings.setObject(String.valueOf(id), new Pair<User, Long>(user, time));//stupidity and time its saved
    }

    public boolean isCached(long id) {
        return settings.exists(String.valueOf(id));
    }

    public User getCached(long id) {
        var pair = settings.getObject(String.valueOf(id), new Pair<>(null, (long) 0), TypeToken.getParameterized(Pair.class, User.class, Long.class).getType());
        long time = Calendar.getInstance().getTimeInMillis() / 1000;
        if (pair.second < time) settings.remove(String.valueOf(id));
        return (User) pair.first;
    }

    public void clearCache() {
        settings.resetSettings();
    }
}
