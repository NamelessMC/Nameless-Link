package com.namelessmc.bot.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
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

	private static final Set<RoleUpdate> ROLE_UPDATE_QUEUE_SET = new HashSet<>();
	private static final Queue<RoleUpdate> ROLE_UPDATE_QUEUE = new LinkedList<>();

	private static class RoleUpdate {

		final long userId;
		final long guildId;

		RoleUpdate(final long userId, final long guildId){
			this.userId = userId;
			this.guildId = guildId;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.userId, this.guildId);
		}

	}

	public static void queueRoleUpdate(final long userId, final long guildId) {
		Main.getExecutorService().execute(() -> {
			final RoleUpdate roleUpdate = new RoleUpdate(userId, guildId);
			synchronized(ROLE_UPDATE_QUEUE) {
				if (!ROLE_UPDATE_QUEUE_SET.contains(roleUpdate)) {
					ROLE_UPDATE_QUEUE_SET.add(roleUpdate);
					ROLE_UPDATE_QUEUE.add(roleUpdate);
				}
			}
		});
	}

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
		LOGGER.info("Sending roles for {} to website", guild.getIdLong());
		try {
			final Optional<NamelessAPI> optApi = Main.getConnectionManager().getApi(guild.getIdLong());
			if (optApi.isPresent()) {
				final Map<Long, String> roles = guild.getRoles().stream().collect(Collectors.toMap(Role::getIdLong, Role::getName));
				optApi.get().submitDiscordRoleList(roles);
			}
		} catch (final BackendStorageException e) {
			LOGGER.error("Storage error", e);
		} catch (final ApiError e) {
			LOGGER.warn("API error {} while sending role list for guild {}", e.getError(), guild.getIdLong());
		} catch (final NamelessException e) {
			LOGGER.warn("Website communication error while sending role list for guild {}", guild.getIdLong());
		}
	}

	@Override
	public void onGuildMemberRoleAdd(final GuildMemberRoleAddEvent event) {
		final long userId = event.getUser().getIdLong();
		final long guildId = event.getGuild().getIdLong();
		LOGGER.info("Received guild member role add event for {} in {}, adding to queue ({})", userId, guildId, ROLE_UPDATE_QUEUE.size());
		queueRoleUpdate(userId, guildId);
	}

	@Override
	public void onGuildMemberRoleRemove(final GuildMemberRoleRemoveEvent event) {
		final long userId = event.getUser().getIdLong();
		final long guildId = event.getGuild().getIdLong();
		LOGGER.info("Received guild member role remove event for {} in {}, adding to queue ({})", userId, guildId, ROLE_UPDATE_QUEUE.size());
		queueRoleUpdate(userId, guildId);
	}

	public static void processQueue() {
		synchronized(ROLE_UPDATE_QUEUE) {
			if (ROLE_UPDATE_QUEUE.isEmpty()) {
				return;
			}

			final RoleUpdate roleUpdate = ROLE_UPDATE_QUEUE.remove();
			ROLE_UPDATE_QUEUE_SET.remove(roleUpdate);

			final long guildId = roleUpdate.guildId;
			final long userId = roleUpdate.userId;

			LOGGER.info("Checking user {} guild {} with {} items left in queue", userId, guildId, ROLE_UPDATE_QUEUE.size());

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
					LOGGER.info("Ignoring rank update event for {}", userId);
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
				LOGGER.warn("API error " + e.getError() + " while sending role update for user {} guild {} (getUserByDiscordId)", userId, guildId);
				return;
			} catch (final NamelessException e) {
				Main.logConnectionError(LOGGER, "Website communication error while sending role update for user " + userId + " guild " + guildId + " (getUserByDiscordId)", e);
				return;
			}

			if (user.isEmpty()) {
				LOGGER.warn("Skipping, user not found on website.");
				return;
			}

			try {
				final long[] roleIds = roles.stream().mapToLong(Role::getIdLong).toArray();
				user.get().setDiscordRoles(roleIds);
				LOGGER.info("Sucessfully sent roles to website: guildid={} userid={} roles=[{}]", guildId, userId, roles.stream().map(Role::getId).collect(Collectors.joining(", ")));
			} catch (final ApiError e) {
				LOGGER.warn("API error " + e.getError() + " while sending role update for user {} guild (setDiscordRoles)", userId, guildId);
			} catch (final NamelessException e) {
				Main.logConnectionError(LOGGER, "Website communication error while sending role update for user " + userId + " guild " + guildId + " (setDiscordRoles)", e);
			}
		}
	}
}
