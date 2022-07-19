package com.aliucord.plugins.dataclasses;

public class User {
    private Long userID;
    private String imageURL;
    private String username;

    public User(Long userID, String imageURL, String username) {
        this.userID = userID;
        this.imageURL = imageURL;
        this.username = username;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public void setImageURL(com.discord.models.user.User user) {
        if (user != null) this.imageURL = user.getAvatar();
    }

    public Long getUserID() {
        return userID;
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "User{" +
                "userID=" + userID +
                ", imageURL='" + imageURL + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
