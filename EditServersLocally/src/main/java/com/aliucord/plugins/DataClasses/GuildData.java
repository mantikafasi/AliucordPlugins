package com.aliucord.plugins.DataClasses;

public class GuildData implements DataBase{

    public long guildID;

    public GuildData(long guildID, String serverName, String imageURL) {
        this.guildID = guildID;
        this.serverName = serverName;
        this.imageURL = imageURL;
    }

    public String serverName;
    public String imageURL;

    public long getGuildID() {
        return guildID;
    }

    public String getServerName() {
        return serverName;
    }

    public String getImageURL() {
        return imageURL;
    }



    public GuildData(long guildID){
        this.guildID = guildID;
    }

}
