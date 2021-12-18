package com.aliucord.plugins.DataClasses;

import com.discord.models.guild.Guild;

public class GuildData {

    public long guildID;
    public String serverName;
    public String imageURL;
    public String orginalURL;
    public String orginalName;



    public GuildData(long guildID){
        this.guildID = guildID;
    }
    public GuildData(Guild guild){
        guildID = guild.getId();
    }

    @Override
    public String toString() {
        return "GuildData{" +
                "guildID=" + guildID +
                ", serverName='" + serverName + '\'' +
                ", imageURL='" + imageURL + '\'' +
                ", orginalURL='" + orginalURL + '\'' +
                ", orginalName='" + orginalName + '\'' +
                '}';
    }
}
