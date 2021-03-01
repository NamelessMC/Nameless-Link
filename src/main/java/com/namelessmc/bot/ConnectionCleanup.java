package com.namelessmc.bot;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.namelessmc.bot.connections.BackendStorageException;

import net.dv8tion.jda.api.entities.Guild;

public class ConnectionCleanup {

	public static void run() {
		final Logger log = Main.getLogger();

		log.info("Cleaning up connections...");
		try {
			final long someTimeAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14);
			final List<URL> urls = Main.getConnectionManager().listConnectionsUsedBefore(someTimeAgo);

			if (urls.isEmpty()) {
				log.info("No connections to clean up.");
				return;
			} else {
				log.info("Found " + urls.size() + " unused connections");
			}

			for (final URL url : urls) {
				final Optional<Long> optGuildId = Main.getConnectionManager().getGuildIdByURL(url);
				if (optGuildId.isEmpty()) {
					log.warning("URL does not have guild id in database? " + url.toString());
					continue;
				}

				final long guildId = optGuildId.get();

				log.info(String.format("Checking %s (guild id %s)", url.getHost(), guildId));

				final Guild guild = Main.getJda().getGuildById(guildId);

				if (guild == null) {
					log.info("Guild does not exist. Removing connection from database.");
					Main.getConnectionManager().removeConnection(guildId);
					continue;
				}

				log.info("Guild exists, not removing connection.");

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

			log.info("Done cleaning up connections.");
		} catch (final BackendStorageException e) {
			log.severe("A database error occured while cleaning up connections");
			e.printStackTrace();
		}
	}

}
