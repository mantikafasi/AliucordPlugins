package com.aliucord.plugins.dataclasses;

public class Response {
    boolean isUpdated;
    boolean successful;
    String text;

    public Response( boolean isUpdated, boolean successful, String text) {
        this.isUpdated = isUpdated;
        this.successful = successful;
        this.text = text;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getText() {
        return text;
    }


}
