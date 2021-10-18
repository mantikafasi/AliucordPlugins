package com.aliucord.plugins.DataClasses;

import com.aliucord.Logger;
import com.aliucord.wrappers.GuildWrapper;
import com.discord.models.guild.Guild;
import com.discord.stores.StoreStream;
import com.discord.utilities.guilds.GuildUtilsKt;
import com.discord.utilities.icon.IconUtils;

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
        //GuildWrapper wrapper = new GuildWrapper(guild);
        orginalName=guild.getName();

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
