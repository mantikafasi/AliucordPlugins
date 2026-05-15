package com.aliucord.plugins.dataclasses;

public class ReviewDBSettings {
    public String discordID;
    public boolean opt;

    public boolean isOptedOut() {
        return opt;
    }
}
