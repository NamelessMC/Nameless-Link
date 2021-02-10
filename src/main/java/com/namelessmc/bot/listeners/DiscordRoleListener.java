package com.namelessmc.bot.listeners;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.ApiError;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessUser;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DiscordRoleListener extends ListenerAdapter {

	public static final Object EVENT_LOCK = new Object();

	private static final long EVENT_DISABLE_DURATION = 2000;

	private static final HashMap<Long, Long> temporarilyDisabledEvents = new HashMap<>();

	public static void temporarilyDisableEvents(final long userId) {
		temporarilyDisabledEvents.put(userId, System.currentTimeMillis());
	}

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
		Main.getLogger().info("Sending roles for " + guild.getIdLong() + " to website");
		try {
			final Optional<NamelessAPI> optApi = Main.getConnectionManager().getApi(guild.getIdLong());
			if (optApi.isPresent()) {
				final Map<Long, String> roles = guild.getRoles().stream().collect(Collectors.toMap(Role::getIdLong, Role::getName));
				optApi.get().submitDiscordRoleList(roles);
			}
		} catch (final BackendStorageException e) {
			e.printStackTrace();
		} catch (final ApiError e) {
			Main.getLogger().warning("API error " + e.getError() + " while sending role list for guild " + guild.getIdLong());
		} catch (final NamelessException e) {
			Main.getLogger().warning("Website communication error while sending role list for guild " + guild.getIdLong());
		}
	}

	@Override
	public void onGuildMemberRoleAdd(final GuildMemberRoleAddEvent event) {
		synchronized (EVENT_LOCK) {
			sendRolesToWebsite(event.getMember());
		}
	}

	@Override
	public void onGuildMemberRoleRemove(final GuildMemberRoleRemoveEvent event) {
		synchronized (EVENT_LOCK) {
			sendRolesToWebsite(event.getMember());
		}
	}

	public static void sendRolesToWebsite(final Member member) {
		final User discordUser = member.getUser();
		final List<Role> roles = member.getRoles();
		final long guildId = member.getGuild().getIdLong();

		if (discordUser.isBot()) {
			return;
		}

		final long userId = discordUser.getIdLong();
		if (temporarilyDisabledEvents.containsKey(userId)) {
			final long diff = System.currentTimeMillis() - temporarilyDisabledEvents.get(userId);

			// No need to send rank change to website if we
			// just received this role update from the website
			if (diff < EVENT_DISABLE_DURATION) {
				Main.getLogger().info("Ignoring rank update event for " + discordUser.getId());
				return;
			} else {
				temporarilyDisabledEvents.remove(userId);
			}
		}

		Main.getLogger().info(String.format("Processing role change guildid=%s userid=%s", guildId, userId));

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
		} catch (final ApiError e) {
			Main.getLogger().warning("API error " + e.getError() + " while sending role update for user " + userId + " guild " + guildId + " (getUserByDiscordId)");
			return;
		} catch (final NamelessException e) {
			Main.getLogger().warning("Website communication error while sending role update for user " + userId + " guild " + guildId + " (getUserByDiscordId)");
			return;
		}

		if (user.isEmpty()) {
			return;
		}

		try {
			final long[] roleIds = roles.stream().mapToLong(Role::getIdLong).toArray();
			user.get().setDiscordRoles(roleIds);
		} catch (final ApiError e) {
			Main.getLogger().warning("API error " + e.getError() + " while sending role update for user " + userId + " guild " + guildId + " (setDiscordRoles)");
		} catch (final NamelessException e) {
			Main.getLogger().warning("Website communication error while sending role update for user " + userId + " guild " + guildId + " (setDiscordRoles)");
		}
	}
}
