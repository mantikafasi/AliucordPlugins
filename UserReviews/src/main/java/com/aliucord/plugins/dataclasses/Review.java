package com.aliucord.plugins.dataclasses;

import androidx.annotation.NonNull;

import com.discord.stores.StoreStream;

public class Review {
    private String username;
    public String comment;
    private Long senderuserid;
    private int star;
    private Long senderdiscordid;
    public User user;
    public com.discord.models.user.User discordUser;

    public Review(String comment, Long senderUserID, Long senderDiscordID, int star, String username) {
        this.comment = comment;
        this.senderdiscordid = senderDiscordID;
        this.senderuserid = senderUserID;
        this.star = star;
        this.username = username;
        this.discordUser = StoreStream.getUsers().getUsers().get(senderDiscordID);
    }

    public User getUser() {
        return user;
    }

    public Long getSenderdiscordid() { return senderdiscordid; }

    public String getComment() {
        return comment;
    }

    public Long getSenderuserid() {
        return senderuserid;
    }

    public int getStar() {
        return star;
    }

    public String getUsername() {
        return username;
    }

    @NonNull
    @Override
    public String toString() {
        return "Review{" +
                "username='" + username + '\'' +
                ", comment='" + comment + '\'' +
                ", senderUserID=" + senderuserid +
                ", star=" + star +
                '}';
    }

}
