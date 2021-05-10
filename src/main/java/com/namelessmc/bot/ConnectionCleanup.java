package com.namelessmc.bot;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.connections.BackendStorageException;

import net.dv8tion.jda.api.entities.Guild;

public class ConnectionCleanup {

	private static final Logger LOGGER = LoggerFactory.getLogger("Connection cleanup");

	public static void run() {
		LOGGER.info("Cleaning up connections...");
		try {
			final long someTimeAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14);
			final List<URL> urls = Main.getConnectionManager().listConnectionsUsedBefore(someTimeAgo);

			if (urls.isEmpty()) {
				LOGGER.info("No connections to clean up.");
				return;
			} else {
				LOGGER.info("Found {} unused connections", urls.size());
			}

			for (final URL url : urls) {
				final Optional<Long> optGuildId = Main.getConnectionManager().getGuildIdByURL(url);
				if (optGuildId.isEmpty()) {
					LOGGER.warn("URL does not have guild id in database? '{}'", url.toString());
					continue;
				}

				final long guildId = optGuildId.get();

				LOGGER.info("Checking {} (guild id {})", url.getHost(), guildId);

				final Guild guild = Main.getJda().getGuildById(guildId);

				if (guild == null) {
					LOGGER.info("Guild does not exist. Removing connection from database.");
					Main.getConnectionManager().removeConnection(guildId);
					continue;
				}

				LOGGER.info("Guild exists, not removing connection.");

//				Main.getJda().retrieveUserById(guild.getOwnerIdLong()).flatMap(User::openPrivateChannel).queue(channel -> {
//					log.info("Guild owner has user id " + channel.getUser().getIdLong() + ", opened channel id " + channel.getIdLong());
//
//					Main.getExecutorService().execute(() -> {
//						log.info("Making request to website for getting language");
//						final NamelessAPI api = Main.newApiConnection(url);
//						final Language language = Language.getDiscordUserLanguage(api, channel.getUser());
//						final String command = "!unlink " + guildId;
//						final String s = language.get(Term.UNUSED_CONNECTION, "discordServerName", guild.getName(), "command", command);
//						log.info("Got language, sending message");
//						channel.sendMessage(s).queue(RestAction.getDefaultSuccess(), ignored -> log.warning("Couldn't send message"));
//					});
//				});
			}

			LOGGER.info("Done cleaning up connections.");
		} catch (final BackendStorageException e) {
			LOGGER.error("A database error occured while cleaning up connections", e);
		}
	}

}
