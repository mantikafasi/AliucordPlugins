package com.aliucord.plugins.dataclasses;

import java.util.ArrayList;
import java.util.List;

public class User {
    private float ID;
    private String discordID;
    private String username;
    private String profilePhoto;
    ArrayList<String> clientMods = new ArrayList <>();
    private int warningCount;
    List<Badge> badges = new ArrayList <> ();
    BanInfo banInfo;
    private int lastReviewID;
    private float type;


    public float getID() {
        return ID;
    }

    public String getDiscordID() {
        return discordID;
    }

    public String getUsername() {
        return username;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public float getWarningCount() {
        return warningCount;
    }

    public BanInfo getBanInfo() {
        return banInfo;
    }

    public int getLastReviewID() {
        return lastReviewID;
    }

    public float getType() {
        return type;
    }

    public void setID(float ID) {
        this.ID = ID;
    }

    public void setDiscordID(String discordID) {
        this.discordID = discordID;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public void setBanInfo(BanInfo banInfoObject) {
        this.banInfo = banInfoObject;
    }

    public void setLastReviewID(int lastReviewID) {
        this.lastReviewID = lastReviewID;
    }

    public void setType(float type) {
        this.type = type;
    }


    public static class BanInfo {
        private float id;
        private String discordID;
        private float reviewID;
        private String reviewContent;
        private String banEndDate;
        private String reviewTimestamp;


        public float getId() {
            return id;
        }

        public String getDiscordID() {
            return discordID;
        }

        public float getReviewID() {
            return reviewID;
        }

        public String getReviewContent() {
            return reviewContent;
        }

        public String getBanEndDate() {
            return banEndDate;
        }

        public String getReviewTimestamp() {
            return reviewTimestamp;
        }

        public void setId(float id) {
            this.id = id;
        }

        public void setDiscordID(String discordID) {
            this.discordID = discordID;
        }

        public void setReviewID(float reviewID) {
            this.reviewID = reviewID;
        }

        public void setReviewContent(String reviewContent) {
            this.reviewContent = reviewContent;
        }

        public void setBanEndDate(String banEndDate) {
            this.banEndDate = banEndDate;
        }

        public void setReviewTimestamp(String reviewTimestamp) {
            this.reviewTimestamp = reviewTimestamp;
        }
    }

}
