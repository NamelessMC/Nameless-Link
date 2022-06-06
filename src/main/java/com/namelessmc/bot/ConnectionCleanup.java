package com.namelessmc.bot;

import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ConnectionCleanup {

	private static final Logger LOGGER = LoggerFactory.getLogger("Connection cleanup");

	public static void run() {
		LOGGER.info("Cleaning up connections...");
		try {
			final long someTimeAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14);
			final List<NamelessAPI> connections = Main.getConnectionManager().listConnectionsUsedBefore(someTimeAgo);

			if (connections.isEmpty()) {
				LOGGER.info("No connections to clean up.");
				return;
			} else {
				LOGGER.info("Found {} unused connections", connections.size());
			}

			for (final NamelessAPI connection : connections) {
				final Optional<Long> optGuildId = Main.getConnectionManager().getGuildIdByApiConnection(connection);
				if (optGuildId.isEmpty()) {
					LOGGER.warn("Connection does not have guild id in database? {}", connection);
					continue;
				}

				final long guildId = optGuildId.get();

				LOGGER.info("Checking {} (guild id {})", connection.apiUrl().getHost(), guildId);

				final Guild guild = Main.getJdaForGuild(guildId).getGuildById(guildId);

				if (guild == null) {
					LOGGER.info("Guild does not exist. Removing connection from database.");
					Main.getConnectionManager().removeConnection(guildId);
					continue;
				}

				LOGGER.info("Guild exists, not removing connection.");
			}

			LOGGER.info("Done cleaning up connections.");
		} catch (final BackendStorageException e) {
			LOGGER.error("A database error occurred while cleaning up connections", e);
		}
	}

}
