package com.aliucord.plugins.dataclasses;

import java.util.List;

public class Response {
    boolean success;
    String message;
    List<Review> reviews;

    public List<Review> getReviews() {
        return reviews;
    }

    public String getToken() {
        return token;
    }

    String token;
    boolean updated;

    public Response( boolean isUpdated, boolean success, String message) {
        this.success = success;
        this.message = message;
        this.reviews = reviews;
    }

    public boolean isSuccessful() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUpdated() {
        return updated;
    }

    @Override
    public String toString() {
        return "Response{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", reviews=" + reviews +
                ", token='" + token + '\'' +
                ", updated=" + updated +
                '}';
    }
}
