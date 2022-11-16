package com.aliucord.plugins.dataclasses;

public class Badge {
    private long discordid;
    private String badge_name;
    private String badge_icon;

    public long getDiscordid() {
        return discordid;
    }

    public String getBadge_name() {
        return badge_name;
    }

    public String getBadge_icon() {
        return badge_icon;
    }

    public String getRedirect_url() {
        return redirect_url;
    }

    private String redirect_url;
}
