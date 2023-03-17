package com.aliucord.plugins.dataclasses;

public class Sender {
    int id;
    String username;
    String profilePhoto;
    Badge[] badges;
    Long discordID;

    public Long getDiscordID() {
        return discordID;
    }

    public Sender(int id, String username, String profilePhoto, Long discordID) {
        this.id = id;
        this.username = username;
        this.profilePhoto = profilePhoto;
        this.discordID = discordID;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public Badge[] getBadges() {
        return badges;
    }
}
