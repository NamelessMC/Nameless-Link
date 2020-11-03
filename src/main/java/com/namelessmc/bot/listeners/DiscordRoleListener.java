package com.namelessmc.bot.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessUser;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordRoleListener extends ListenerAdapter {

    @Getter
    private static final List<Member> recentlyEdited = new ArrayList<>();

    @Override
    public void onGuildMemberRoleAdd(final GuildMemberRoleAddEvent event) {
        if (getRecentlyEdited().contains(event.getMember()) || event.getUser().isBot()) {
			return;
		}
        
        process(event.getGuild().getIdLong(), event.getUser().getIdLong(), event.getRoles(), true);
    }

    @Override
    public void onGuildMemberRoleRemove(final GuildMemberRoleRemoveEvent event) {
        if (getRecentlyEdited().contains(event.getMember()) || event.getUser().isBot()) {
			return;
		}

        process(event.getGuild().getIdLong(), event.getUser().getIdLong(), event.getRoles(), false);
    }
    
    private void process(final long guildId, final long userId, final List<Role> roles, final boolean add) {
        Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			e.printStackTrace();
			return;
		}

        if (api.isEmpty()) {
            Main.debug("API URL not setup in " + guildId);
            return;
        }
        
        Optional<NamelessUser> user;
		try {
			user = api.get().getUserByDiscordId(guildId);
		} catch (final NamelessException e) {
			// API URL is invalid or website is down
			// TODO handle properly, probably just silence
			e.printStackTrace();
			return;
		}
        
        if (user.isEmpty()) {
			Main.debug("User is not registered in " + guildId);
			return;
		}
        
        try {
        	final long[] roleIds = roles.stream().mapToLong(Role::getIdLong).toArray();
        	if (add) {
        		user.get().addDiscordRoles(roleIds);
        	} else {
        		user.get().removeDiscordRoles(roleIds);
        	}
	    } catch (final NamelessException e) {
	        Main.debug("[ERROR] Error while updating webrank: " + e.getMessage() + " for " + userId);
	    }
    }
}
