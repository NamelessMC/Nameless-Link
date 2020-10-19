package com.namelessmc.bot.listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.namelessmc.NamelessAPI.NamelessException;
import com.namelessmc.NamelessAPI.ParameterBuilder;
import com.namelessmc.NamelessAPI.Request;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Queries;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DiscordRoleListener extends ListenerAdapter {

    @Getter
    private static final List<Member> recentlyEdited = new ArrayList<>();

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        if (getRecentlyEdited().contains(event.getMember()) || event.getUser().isBot()) return;

        String api_url = Queries.getGuildApiUrl(event.getGuild().getId());

        if (api_url == null) {
            Main.debug("API URL not setup in " + event.getGuild().getName());
            return;
        }

        for (Role role : event.getRoles()) {
            String[] params = new ParameterBuilder().add("discord_user_id", event.getMember().getId()).add("discord_role_id", role.getId()).build();
            try {
                Request request = new Request(new URL(api_url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.SET_GROUP_FROM_DISCORD_ID, params);
                request.connect();
                JsonObject response = request.getResponse();
                if (!response.has("code")) {
                    Main.log("Processed role addition update (Discord -> Website) for " + event.getMember().getEffectiveName() + " for role " + role);
                } else {
                    Main.debug("NamelessMC error while updating webrank: " + Main.getGson().toJson(response) + " for " + event.getMember().getEffectiveName());
                }
            } catch (NamelessException | MalformedURLException | JsonSyntaxException exception) {
                Main.debug("[ERROR] Error while updating webrank: " + exception.getMessage() + " for " + event.getMember().getEffectiveName());
            }
        }
        Main.debug("Added " + event.getRoles() + " to " + event.getMember().getEffectiveName());
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        if (getRecentlyEdited().contains(event.getMember()) || event.getUser().isBot()) return;

        String api_url = Queries.getGuildApiUrl(event.getGuild().getId());

        if (api_url == null) {
            Main.debug("API URL not setup in " + event.getGuild().getName());
            return;
        }

        for (Role role : event.getRoles()) {
            String[] params = new ParameterBuilder().add("discord_user_id", event.getMember().getId()).add("discord_role_id", role.getId()).build();
            try {
                Request request = new Request(new URL(api_url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.REMOVE_GROUP_FROM_DISCORD_ID, params);
                request.connect();
                JsonObject response = request.getResponse();
                if (!response.has("code")) {
                    Main.log("Processed role removal (Discord -> Website) for " + event.getMember().getEffectiveName() + " for role " + role);
                } else {
                    Main.debug("NamelessMC error while updating webrank: `" + Main.getGson().toJson(response) + "` for " + event.getMember().getEffectiveName());
                }
            } catch (NamelessException | MalformedURLException | JsonSyntaxException exception) {
                Main.debug("[ERROR] Error while updating webrank: `" + exception.getMessage() + "` for " + event.getMember().getEffectiveName());
            }
        }
       Main.debug("Removed " + event.getRoles() + " from " + event.getMember().getEffectiveName());
    }
}
