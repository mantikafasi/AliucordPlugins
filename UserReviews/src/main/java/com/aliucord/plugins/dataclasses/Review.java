package com.aliucord.plugins.dataclasses;

public class Review {
    public String comment;
    private int star;
    public int id;
    public int type;
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

}
