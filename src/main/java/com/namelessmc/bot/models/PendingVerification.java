package com.namelessmc.bot.models;

import lombok.Getter;

public class PendingVerification {

    @Getter
    private final String discord_id;
    @Getter
    private final String username;
    @Getter
    private final String guild_id;
    @Getter
    private final String role;
    @Getter
    private final String site;

    public PendingVerification(String discord_id, String username, String guild_id, String role, String site) {
        this.discord_id = discord_id;
        this.username = username;
        this.guild_id = guild_id;
        this.role = role;
        this.site = site;
    }

}
