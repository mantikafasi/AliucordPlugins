package com.aliucord.plugins.DataClasses;

import com.google.gson.Gson;

import java.io.Serializable;

public class ChannelData {

    public ChannelData(long guildID, long channelID, String channelName) {
        this.guildID = guildID;
        this.channelID = channelID;
        this.channelName = channelName;
    }
    public long getGuildID() {
        return guildID;
    }

    public long getChannelID() {
        return channelID;
    }

    public String getChannelName() {
        return channelName;
    }



    public long guildID;
    public long channelID;
    public String channelName;
}
