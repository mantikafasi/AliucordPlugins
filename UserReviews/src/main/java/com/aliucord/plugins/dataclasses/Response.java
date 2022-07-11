package com.aliucord.plugins.dataclasses;

public class Response {
    boolean isUpdated;
    boolean successful;
    String message;

    public Response( boolean isUpdated, boolean successful, String message) {
        this.isUpdated = isUpdated;
        this.successful = successful;
        this.message = message;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }


}
