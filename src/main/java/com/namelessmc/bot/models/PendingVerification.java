package com.namelessmc.bot.models;

import lombok.Getter;

public class PendingVerification {

    @Getter
    private final String discord_id;
    @Getter
    private final String token;
    @Getter
    private final String guild_id;
    @Getter
    private final String role;
    @Getter
    private final String site;

    public PendingVerification(String discord_id, String token, String guild_id, String role, String site) {
        this.discord_id = discord_id;
        this.token = token;
        this.guild_id = guild_id;
        this.role = role;
        this.site = site;
    }

}
