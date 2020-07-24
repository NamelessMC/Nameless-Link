package com.namelessmc.bot.models;

public class PendingVerification {

    public String discord_id;
    public String username;
    public String guild_id;
    public String role;
    public String site;

    public PendingVerification(String discord_id, String username, String guild_id, String role, String site) {
        this.discord_id = discord_id;
        this.username = username;
        this.guild_id = guild_id;
        this.role = role;
        this.site = site;
    }

}
