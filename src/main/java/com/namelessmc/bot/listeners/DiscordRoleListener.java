package com.namelessmc.bot.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class DiscordRoleListener extends ListenerAdapter {

	public static final Object EVENT_LOCK = new Object();

	private static final long EVENT_DISABLE_DURATION = 2000;

	private static final HashMap<Long, Long> temporarilyDisabledEvents = new HashMap<>();

	private static final Logger LOGGER = LoggerFactory.getLogger("Group sync discord->website");

	public static void temporarilyDisableEvents(final long userId) {
		temporarilyDisabledEvents.put(userId, System.currentTimeMillis());
	}

	@Override
	public void onRoleCreate(final RoleCreateEvent event) {
		Main.getExecutorService().execute(() -> {
			sendRoleListToWebsite(event.getGuild());
		});
	}

	@Override
	public void onRoleDelete(final RoleDeleteEvent event) {
		Main.getExecutorService().execute(() -> {
			sendRoleListToWebsite(event.getGuild());
		});
	}

	@Override
	public void onRoleUpdateName(final RoleUpdateNameEvent event) {
		Main.getExecutorService().execute(() -> {
			sendRoleListToWebsite(event.getGuild());
		});
	}

	public static void sendRoleListToWebsite(final Guild guild) {
		LOGGER.info("Sending roles for " + guild.getIdLong() + " to website");
		try {
			final Optional<NamelessAPI> optApi = Main.getConnectionManager().getApi(guild.getIdLong());
			if (optApi.isPresent()) {
				final Map<Long, String> roles = guild.getRoles().stream().collect(Collectors.toMap(Role::getIdLong, Role::getName));
				optApi.get().submitDiscordRoleList(roles);
			}
		} catch (final BackendStorageException e) {
			LOGGER.error("Storage error", e);
		} catch (final ApiError e) {
			LOGGER.warn("API error " + e.getError() + " while sending role list for guild " + guild.getIdLong());
		} catch (final NamelessException e) {
			LOGGER.warn("Website communication error while sending role list for guild " + guild.getIdLong());
		}
	}

	@Override
	public void onGuildMemberRoleAdd(final GuildMemberRoleAddEvent event) {
		LOGGER.info("[Received guild member role add event for " + event.getUser().getId() + " in " + event.getGuild().getId());
		Main.getExecutorService().execute(() -> {
			synchronized (EVENT_LOCK) {
				sendRolesToWebsite(event.getMember());
			}
		});
	}

	@Override
	public void onGuildMemberRoleRemove(final GuildMemberRoleRemoveEvent event) {
		LOGGER.info("Received guild member role remove event for %s in %s", event.getUser().getId(), event.getGuild().getId());
		Main.getExecutorService().execute(() -> {
			synchronized (EVENT_LOCK) {
				sendRolesToWebsite(event.getMember());
			}
		});
	}

	public static void sendRolesToWebsite(final Member member) {
		final User discordUser = member.getUser();
		final List<Role> roles = member.getRoles();
		final long guildId = member.getGuild().getIdLong();
		final long userId = discordUser.getIdLong();

		LOGGER.info(String.format("Processing role change guildid=%s userid=%s", guildId, userId));

		if (discordUser.isBot()) {
			LOGGER.info("Skipping, user is a bot.");
			return;
		}

		if (temporarilyDisabledEvents.containsKey(userId)) {
			final long diff = System.currentTimeMillis() - temporarilyDisabledEvents.get(userId);

			// No need to send rank change to website if we
			// just received this role update from the website
			if (diff < EVENT_DISABLE_DURATION) {
				LOGGER.info("Ignoring rank update event for " + discordUser.getId());
				return;
			} else {
				temporarilyDisabledEvents.remove(userId);
			}
		}

		Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			LOGGER.error("Storage error", e);
			return;
		}

		if (api.isEmpty()) {
			LOGGER.info("Skipping, guild is not linked.");
			return;
		}

		Optional<NamelessUser> user;
		try {
			user = api.get().getUserByDiscordId(userId);
		} catch (final ApiError e) {
			LOGGER.warn("API error " + e.getError() + " while sending role update for user " + userId + " guild " + guildId + " (getUserByDiscordId)");
			return;
		} catch (final NamelessException e) {
			LOGGER.warn("Website communication error while sending role update for user " + userId + " guild " + guildId + " (getUserByDiscordId)", e);
			return;
		}

		if (user.isEmpty()) {
			LOGGER.warn("Skipping, user not found on website.");
			return;
		}

		try {
			final long[] roleIds = roles.stream().mapToLong(Role::getIdLong).toArray();
			user.get().setDiscordRoles(roleIds);
		} catch (final ApiError e) {
			LOGGER.warn("API error " + e.getError() + " while sending role update for user " + userId + " guild " + guildId + " (setDiscordRoles)");
		} catch (final NamelessException e) {
			LOGGER.warn("Website communication error while sending role update for user " + userId + " guild " + guildId + " (setDiscordRoles)", e);
		}
	}
}
