package com.namelessmc.bot.listeners;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.namelessmc.NamelessAPI.NamelessException;
import com.namelessmc.NamelessAPI.ParameterBuilder;
import com.namelessmc.NamelessAPI.Request;
import com.namelessmc.bot.Data;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Utils;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class DiscordRoleListener extends ListenerAdapter {

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        String api_url = Data.getGuildApiUrl(event.getGuild().getId());

        if (api_url == null) {
            Main.getLogger().info("API URL not setup in " + event.getGuild().getName());
            return;
        }

        // TODO: change discord_role_id to a list of id seperated by "|" and then parse on nmc end
        for (Role role : event.getRoles()) {
            String[] params = new ParameterBuilder().add("discord_user_id", event.getMember().getId()).add("discord_role_id", role.getId()).build();
            try {
                Request request = new Request(new URL(api_url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.SET_GROUP_FROM_DISCORD_ID, params);
                request.connect();
                JsonObject response = request.getResponse();
                if (!response.has("code")) {
                    // success
                    Main.getLogger().info("Processed Discord role addition update -> website for " + event.getMember().getEffectiveName() + " for role " + role);
                } else {
                    // error on nmc api
                    Gson gson = new GsonBuilder().create();
                    event.getGuild().getTextChannelById(734627858617466963L).sendMessage("error while updating webrank: `" + gson.toJson(response) + "` for " + event.getMember().getEffectiveName()).queue();
                }
            } catch (NamelessException | MalformedURLException exception) {
                // error on our end
                event.getGuild().getTextChannelById(734627858617466963L).sendMessage("error while updating webrank: `" + exception.getMessage() + "` for " + event.getMember().getEffectiveName()).queue();
            }
        }
        event.getGuild().getTextChannelById(734627858617466963L).sendMessage("added " + event.getRoles() + " to " + event.getMember().getEffectiveName()).queue();
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        String api_url = Data.getGuildApiUrl(event.getGuild().getId());

        if (api_url == null) {
            Main.getLogger().info("API URL not setup in " + event.getGuild().getName());
            return;
        }

        for (Role role : event.getRoles()) {
            String[] params = new ParameterBuilder().add("discord_user_id", event.getMember().getId()).add("discord_role_id", role.getId()).build();
            try {
                Request request = new Request(new URL(api_url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.REMOVE_GROUP_FROM_DISCORD_ID, params);
                request.connect();
                JsonObject response = request.getResponse();
                if (!response.has("code")) {
                    // success
                    Main.getLogger().info("Processed Discord role removal update -> website for " + event.getMember().getEffectiveName() + " for role " + role);
                } else {
                    // error on nmc api
                    Gson gson = new GsonBuilder().create();
                    event.getGuild().getTextChannelById(734627858617466963L).sendMessage("error while updating webrank: `" + gson.toJson(response) + "` for " + event.getMember().getEffectiveName()).queue();
                }
            } catch (NamelessException | MalformedURLException exception) {
                // error on our end
                event.getGuild().getTextChannelById(734627858617466963L).sendMessage("error while updating webrank: `" + exception.getMessage() + "` for " + event.getMember().getEffectiveName()).queue();
            }
        }

        event.getGuild().getTextChannelById(734627858617466963L).sendMessage("Removed " + event.getRoles() + " from " + event.getMember().getEffectiveName()).queue();
    }

}
