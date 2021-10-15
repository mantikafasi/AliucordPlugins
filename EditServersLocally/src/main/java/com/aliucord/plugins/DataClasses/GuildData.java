package com.aliucord.plugins.DataClasses;

public class GuildData {

    public long guildID;

    public GuildData(long guildID, String serverName, String imageURL) {
        this.guildID = guildID;
        this.serverName = serverName;
        this.imageURL = imageURL;
    }

    public String serverName;
    public String imageURL;
    public String orginalURL;
    public String orginalName;



    public GuildData(long guildID){
        this.guildID = guildID;
    }

}
