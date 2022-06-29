package com.aliucord.plugins.dataclasses;

public class Review {
    private String username;
    private String comment;
    private Long senderUserID;
    private int star;

    public Review(String comment, Long senderUserID, int star, String username) {
        this.comment = comment;
        this.senderUserID = senderUserID;
        this.star = star;
        this.username = username;
    }



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


}
