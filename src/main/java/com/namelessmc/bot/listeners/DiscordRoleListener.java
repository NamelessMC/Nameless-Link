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
		System.out.println(String.format("Processing role change guildid=%s userid=%s add=%s", guildId, userId, add));

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
			// TODO handle properly
			System.out.println("Website down - probably not an error");
			e.printStackTrace();
			return;
		}

		if (user.isEmpty()) {
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
			System.err.println("Error while updating webrank: " + e.getMessage() + " for " + userId);
		}
	}
}
