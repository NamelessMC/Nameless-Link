package com.namelessmc.bot.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessUser;
import com.namelessmc.java_api.exception.ApiError;
import com.namelessmc.java_api.exception.ApiException;
import com.namelessmc.java_api.exception.NamelessException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordRoleListener extends ListenerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger("Group sync discord->website");

	private static final Map<Long, Object> ROLE_SEND_LOCK = new HashMap<>();

	/**
	 * The website doesn't handle role endpoints being called multiple times at once, so we
	 * need to make sure to only have one request at a time per guildId.
	 *
	 * This method calls the provided runnable, blocking until previous runnables with the same
	 * guildId have finished processing.
	 */
	private static void executeAsyncSynchronized(final long guildId, final Runnable runnable) {
		synchronized(ROLE_SEND_LOCK) {
			if (!ROLE_SEND_LOCK.containsKey(guildId)) {
				ROLE_SEND_LOCK.put(guildId, new Object());
			}
		}

		Main.getExecutorService().execute(() -> {
			synchronized(ROLE_SEND_LOCK.get(guildId)) {
				runnable.run();
			}
		});
	}

	@Override
	public void onRoleCreate(final RoleCreateEvent event) {
		sendRolesAsync(event.getGuild().getIdLong());
	}

	@Override
	public void onRoleDelete(final RoleDeleteEvent event) {
		sendRolesAsync(event.getGuild().getIdLong());
	}

	@Override
	public void onRoleUpdateName(final RoleUpdateNameEvent event) {
		sendRolesAsync(event.getGuild().getIdLong());
	}

	public static void sendRolesAsync(final long guildId) {
		executeAsyncSynchronized(guildId, () -> sendRoles(guildId));
	}

	private static void sendRoles(final long guildId) {
		final Guild guild = Main.getJdaForGuild(guildId).getGuildById(guildId);
		if (guild == null) {
			LOGGER.warn("Guild {} no longer exists?", guildId);
			return;
		}

		LOGGER.info("Sending roles for guild {} to website", guild.getIdLong());
		try {
			final NamelessAPI api = Main.getConnectionManager().getApiConnection(guild.getIdLong());
			if (api != null) {
				final Map<Long, String> roles = guild.getRoles().stream()
						.filter(r -> !r.getName().equals("@everyone"))
						.collect(Collectors.toMap(Role::getIdLong, Role::getName));
				api.discord().updateRoleList(roles);
			} else {
				LOGGER.warn("No API connection");
			}
		} catch (final BackendStorageException e) {
			LOGGER.error("Storage error", e);
		} catch (final NamelessException e) {
			Main.logConnectionError(LOGGER, "Website communication error while sending role list for guild " + guild.getIdLong(), e);
		}
	}

	@Override
	public void onGuildMemberRoleAdd(final GuildMemberRoleAddEvent event) {
		final long userId = event.getUser().getIdLong();
		final long guildId = event.getGuild().getIdLong();
		final long[] added = event.getRoles().stream().mapToLong(Role::getIdLong).toArray();
		LOGGER.info("Received guild member role add event for {} in {}", userId, guildId);
		sendUserRolesAsync(guildId, userId, added, new long[0]);

	}

	@Override
	public void onGuildMemberRoleRemove(final GuildMemberRoleRemoveEvent event) {
		final long userId = event.getUser().getIdLong();
		final long guildId = event.getGuild().getIdLong();
		final long[] removed = event.getRoles().stream().mapToLong(Role::getIdLong).toArray();
		LOGGER.info("Received guild member role remove event for {} in {}", userId, guildId);
		sendUserRolesAsync(guildId, userId, new long[0], removed);
	}
	
	public static void sendUserRolesAsync(final long guildId, final long userId, final long[] addedRoleIds, final long[] removedRoleIds) {
		executeAsyncSynchronized(guildId, () -> sendUserRoles(guildId, userId, addedRoleIds, removedRoleIds));
	}
	
	private static String toString(long[] longArr) {
		return "[" + Arrays.stream(longArr).mapToObj(String::valueOf).collect(Collectors.joining(", ")) + "]";
	}

	@SuppressWarnings("deprecation")
	private static void sendUserRoles(final long guildId, final long userId, final long[] addedRoleIds, final long[] removedRoleIds) {
		final Guild guild = Main.getJdaForGuild(guildId).getGuildById(guildId);
		if (guild == null) {
			LOGGER.warn("Guild {} no longer exists?", guildId);
			return;
		}

		final Member member = guild.retrieveMemberById(userId).complete();
		if (member == null) {
			LOGGER.warn("User {} no longer exists (left guild)?", userId);
			return;
		}

		if (member.getUser().isBot()) {
			LOGGER.info("Skipping role change in guild {}, user {} is a bot.", guildId, userId);
			return;
		}

		final NamelessAPI api;
		try {
			api = Main.getConnectionManager().getApiConnection(guildId);
		} catch (final BackendStorageException e) {
			LOGGER.error("Storage error", e);
			return;
		}

		if (api == null) {
			LOGGER.info("Ignoring role update event for guild={}, guild is not linked.", guildId);
			return;
		}

		final NamelessUser user;
		try {
			user = api.userByDiscordId(userId);
		} catch (final NamelessException e) {
			Main.logConnectionError(LOGGER, "Website communication error while sending role update for user " + userId + " guild " + guildId + " (getUserByDiscordId)", e);
			return;
		}

		if (user == null) {
			LOGGER.warn("Ignoring role update event for guild={} user={}, user has no website account", guildId, userId);
			return;
		}
		
		try {
			try {
				user.discord().syncRoles(addedRoleIds, removedRoleIds);
				LOGGER.info("Sent roles for guild={} user={} add={} remove={}", guildId, userId, toString(addedRoleIds), toString(removedRoleIds));
			} catch (final ApiException e) {
				if (e.apiError() == ApiError.NAMELESS_INVALID_API_METHOD) {
					LOGGER.warn("New role sync endpoint not supported, trying again with old endpoint");
					final long[] roleIds = member.getRoles().stream().mapToLong(Role::getIdLong).toArray();
					user.discord().updateDiscordRoles(roleIds);
					LOGGER.info("Sent roles for guild={} user={} to website: {}", guildId, userId, toString(roleIds));
				} else {
					throw e;
				}
			}
		} catch (final NamelessException e) {
			Main.logConnectionError(LOGGER, "Website communication error while sending role update: user=" + userId + " guild=" + guildId + " (setDiscordRoles)", e);
		}
	}

}
