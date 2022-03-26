package com.aliucord.plugins.DataClasses;

public class ChannelData {

    public long guildID;
    public long channelID;
    public String channelName;
    public String orginalName;


    public ChannelData(long guildID, long channelID, String channelName) {
        this.guildID = guildID;
        this.channelID = channelID;
        this.channelName = channelName;
    }

    public ChannelData(long guildID, long channelID, String channelName, String orginalName) {
        this.guildID = guildID;
        this.channelID = channelID;
        this.channelName = channelName;
        this.orginalName = orginalName;
    }

    public ChannelData(long id) {
        this.channelID = id;
    }

    @Override
    public String toString() {
        return "ChannelData{" +
                "guildID=" + guildID +
                ", channelID=" + channelID +
                ", channelName='" + channelName + '\'' +
                ", orginalName='" + orginalName + '\'' +
                '}';
    }
}
