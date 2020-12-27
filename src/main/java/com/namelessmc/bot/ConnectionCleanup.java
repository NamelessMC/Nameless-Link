package com.namelessmc.bot;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;

public class ConnectionCleanup {
	
	public static void run() {
		final Logger log = Main.getLogger();
		
		log.info("Cleaning up connections...");
		try {
			final long oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
			final List<URL> urls = Main.getConnectionManager().listConnectionsUsedBefore(oneWeekAgo);
			
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
				
				log.info("Guild exists, sending message to guild owner.");
				
				final PrivateChannel channel = guild.getOwner().getUser().openPrivateChannel().complete();
				
				System.out.println(guild.getOwner().getUser().getIdLong() + " " + channel.getIdLong());
				
				final NamelessAPI api = Main.newApiConnection(url);
				final Language language = Language.getDiscordUserLanguage(api, guild.getOwner().getUser());
				final String command = "!unlink " + guildId;
				final String s = language.get(Term.UNUSED_CONNECTION, "discordServerName", guild.getName(), "command", command);
				final Message message = channel.sendMessage(s).complete();
				if (message == null) {
					log.warning("Couldn't send message");
				}
			}
			
			log.info("Done cleaning up connections.");
		} catch (final BackendStorageException e) {
			log.severe("A database error occured while cleaning up connections");
			e.printStackTrace();
		}
	}

}
