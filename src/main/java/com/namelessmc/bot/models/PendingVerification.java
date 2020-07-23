package com.namelessmc.bot.models;

import lombok.Getter;

public class PendingVerification {

    private String discord_id;
    @Getter
    private String username;
    @Getter
    private String site;

    public PendingVerification(String discord_id, String username, String site) {
        this.discord_id = discord_id;
        this.username = username;
        this.site = site;
    }

}
