package com.aliucord.plugins.filtering;

public class Developer {
    public int ID;
    public String github_username;
    public String plugin_repo_name;
    public int repo_stars;
    public Developer(String github_username, int ID) {
        this.github_username = github_username;
        this.ID = ID;
    }

    @Override
    public String toString() {
        return github_username;
    }
}
