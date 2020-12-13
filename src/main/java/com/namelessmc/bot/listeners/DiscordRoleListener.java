package com.namelessmc.bot.listeners;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessUser;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordRoleListener extends ListenerAdapter {
	
	@Override
	public void onRoleCreate(final RoleCreateEvent event) {
		sendRoleListToWebsite(event.getGuild());
	}
	
	@Override
	public void onRoleDelete(final RoleDeleteEvent event) {
		sendRoleListToWebsite(event.getGuild());
	}
	
	@Override
	public void onRoleUpdateName(final RoleUpdateNameEvent event) {
		sendRoleListToWebsite(event.getGuild());
	}
	
	public static void sendRoleListToWebsite(final Guild guild) {
		System.out.println("Sending roles for " + guild.getIdLong() + " to website");
		try {
			final Optional<NamelessAPI> optApi = Main.getConnectionManager().getApi(guild.getIdLong());
			if (optApi.isPresent()) {
				final Map<Long, String> roles = guild.getRoles().stream().collect(Collectors.toMap(Role::getIdLong, Role::getName));
				optApi.get().submitDiscordRoleList(roles);
			}
		} catch (final BackendStorageException e) {
			e.printStackTrace();
		} catch (final NamelessException e) {
			System.err.println("API error sending role update for guild " + guild.getIdLong());
		}
	}

	@Override
	public void onGuildMemberRoleAdd(final GuildMemberRoleAddEvent event) {
		process(event);
	}

	@Override
	public void onGuildMemberRoleRemove(final GuildMemberRoleRemoveEvent event) {
		process(event);
	}

	private void process(final GenericGuildMemberEvent event) {
		final User discordUser = event.getUser();
		final List<Role> roles = event.getGuild().getMember(discordUser).getRoles();
		final long guildId = event.getGuild().getIdLong();
		
		if (discordUser.isBot()) {
			return;
		}
		
		final long userId = discordUser.getIdLong();
		
		System.out.println(String.format("Processing role change guildid=%s userid=%s", guildId, userId));
		
		Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			e.printStackTrace();
			return;
		}

		if (api.isEmpty()) {
			return;
		}

		Optional<NamelessUser> user;
		try {
			user = api.get().getUserByDiscordId(userId);
		} catch (final NamelessException e) {
			System.err.println("API error sending role update for user " + userId + " guild " + guildId);
			return;
		}

		if (user.isEmpty()) {
			return;
		}

		try {
			final long[] roleIds = roles.stream().mapToLong(Role::getIdLong).toArray();
			user.get().setDiscordRoles(roleIds);
		} catch (final NamelessException e) {
			System.err.println("Error while updating webrank: " + e.getMessage() + " for " + userId);
		}
	}
}
