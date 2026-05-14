package com.aliucord.plugins.dataclasses;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Review {
    public String comment;
    private int star;
    public int id;
    public int type;
    public int score;
    public long timestamp;
    public Review[] replies;
    public Sender sender;

    public Badge[] getBadges() {
        return sender.badges;
    }

    public Review(String comment, Long senderUserID, Long senderDiscordID, int star, String username) {
        this.comment = comment;
        this.sender = new Sender(0, username, null, senderDiscordID);
        this.star = star;
    }

    public Long getSenderDiscordID() {
        return sender.getDiscordID();
    }

    public String getComment() {
        return comment;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return sender.username;
    }

    public String getProfilePhoto() {
        return sender.profilePhoto;
    }

    public boolean getSystemMessage() {
        return type == 3;
    }

    public boolean hasVoting() {
        return id != 0 && type == 0;
    }

    public int getScore() {
        return score;
    }

    public String getTimestampText() {
        if (timestamp <= 0) return "";
        long millis = timestamp > 100000000000L ? timestamp : timestamp * 1000L;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(millis));
    }

}
