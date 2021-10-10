package com.aliucord.plugins;

import com.google.gson.Gson;

import java.io.Serializable;

public class ChannelData {

    public ChannelData(long guildID, long channelID, String channelName) {
        this.guildID = guildID;
        this.channelID = channelID;
        this.channelName = channelName;
    }
    public ChannelData (String text){
        String[] objs = text.split(", ");

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



    long guildID;
    long channelID;
    String channelName;
}
