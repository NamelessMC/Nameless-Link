package com.namelessmc.bot.models;

public class PendingVerification {

    public String discord_id;
    public String username;
    public String site;

    public PendingVerification(String discord_id, String username, String site) {
        this.discord_id = discord_id;
        this.username = username;
        this.site = site;
    }

}
