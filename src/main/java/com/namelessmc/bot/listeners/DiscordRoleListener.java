package com.namelessmc.bot.listeners;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.exception.NamelessException;
import com.namelessmc.java_api.NamelessUser;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DiscordRoleListener extends ListenerAdapter {

	private static final long EVENT_DISABLE_DURATION = 2000;

	private static final HashMap<Long, Long> temporarilyDisabledEvents = new HashMap<>();

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

	public static void temporarilyDisableEvents(final long userId) {
		temporarilyDisabledEvents.put(userId, System.currentTimeMillis());
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
			final Optional<NamelessAPI> optApi = Main.getConnectionManager().getApiConnection(guild.getIdLong());
			if (optApi.isPresent()) {
				final Map<Long, String> roles = guild.getRoles().stream()
						.filter(r -> !r.getName().equals("@everyone"))
						.collect(Collectors.toMap(Role::getIdLong, Role::getName));
				optApi.get().discord().updateRoleList(roles);
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
		LOGGER.info("Received guild member role add event for {} in {}", userId, guildId);
		sendUserRolesAsync(guildId, userId);

	}

	@Override
	public void onGuildMemberRoleRemove(final GuildMemberRoleRemoveEvent event) {
		final long userId = event.getUser().getIdLong();
		final long guildId = event.getGuild().getIdLong();
		LOGGER.info("Received guild member role remove event for {} in {}", userId, guildId);
		sendUserRolesAsync(guildId, userId);
	}

	public static void sendUserRolesAsync(final long guildId, final long userId) {
		executeAsyncSynchronized(guildId, () -> sendUserRoles(guildId, userId));
	}

	private static void sendUserRoles(final long guildId, final long userId) {
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

		final List<Role> roles = member.getRoles();

		if (temporarilyDisabledEvents.containsKey(userId)) {
			final long diff = System.currentTimeMillis() - temporarilyDisabledEvents.get(userId);

			// No need to send rank change to website if we
			// just received this role update from the website
			if (diff < EVENT_DISABLE_DURATION) {
				LOGGER.info("Ignoring role update event for guild={} user={}", guildId, userId);
				return;
			} else {
				temporarilyDisabledEvents.remove(userId);
			}
		}

		Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApiConnection(guildId);
		} catch (final BackendStorageException e) {
			LOGGER.error("Storage error", e);
			return;
		}

		if (api.isEmpty()) {
			LOGGER.info("Ignoring role update event for guild={}, guild is not linked.", guildId);
			return;
		}

		final NamelessUser user;
		try {
			user = api.get().userByDiscordId(userId);
		} catch (final NamelessException e) {
			Main.logConnectionError(LOGGER, "Website communication error while sending role update for user " + userId + " guild " + guildId + " (getUserByDiscordId)", e);
			return;
		}

		if (user == null) {
			LOGGER.warn("Ignoring role update event for guild={} user={}, user has no website account", guildId, userId);
			return;
		}

		try {
			final long[] roleIds = roles.stream().mapToLong(Role::getIdLong).toArray();
			user.discord().updateDiscordRoles(roleIds);
			LOGGER.info("Sent roles for guild={} user={} to website sent roles to website: {}", guildId, userId, roles.stream().map(Role::getId).collect(Collectors.joining(", ")));
		} catch (final NamelessException e) {
			Main.logConnectionError(LOGGER, "Website communication error while sending role update: user=" + userId + " guild=" + guildId + " (setDiscordRoles)", e);
		}
	}

}
