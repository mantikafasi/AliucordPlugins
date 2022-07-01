package com.aliucord.plugins.dataclasses;

import androidx.annotation.NonNull;

import com.discord.stores.StoreStream;

public class Review {
    private String username;
    public String comment;
    private Long senderUserID;
    private int star;
    private Long senderDiscordID;
    public User user;
    public com.discord.models.user.User discordUser;

    public Review(String comment, Long senderUserID, Long senderDiscordID, int star, String username) {
        this.comment = comment;
        this.senderDiscordID = senderDiscordID;
        this.senderUserID = senderUserID;
        this.star = star;
        this.username = username;
        this.discordUser = StoreStream.getUsers().getUsers().get(senderDiscordID);
    }

    public User getUser() {
        return user;
    }

    public Long getSenderDiscordID() { return senderDiscordID; }

    public String getComment() {
        return comment;
    }

    public Long getSenderUserID() {
        return senderUserID;
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
                ", senderUserID=" + senderUserID +
                ", star=" + star +
                '}';
    }

}
