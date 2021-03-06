package com.namelessmc.bot.listeners;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildJoinHandler extends ListenerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger("Guild join listener");

	@Override
	public void onGuildJoin(final GuildJoinEvent event) {
		LOGGER.info("Joined guild '{}'", event.getGuild().getName());

		final Language language = Language.getDefaultLanguage();

		Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApi(event.getGuild().getIdLong());
		} catch (final BackendStorageException e) {
			LOGGER.error("Storage error during guild join", e);
			return;
		}

		final String apiUrlCommand = Main.getDefaultCommandPrefix() + "apiurl";
		final long guildId = event.getGuild().getIdLong();

		event.getJDA().retrieveUserById(event.getGuild().getOwnerIdLong()).flatMap(User::openPrivateChannel).queue(channel -> {
			if (api.isEmpty()) {
				channel.sendMessage(language.get(Term.GUILD_JOIN_SUCCESS, "command", apiUrlCommand, "guildId", guildId))
						.queue(message -> LOGGER.info("Sent new join message to {} for guild {}",
								channel.getUser().getName(), event.getGuild().getName()));
			} else {
				try {
					api.get().checkWebAPIConnection();
					// Good to go
					final Language ownerLanguage = Language.getDiscordUserLanguage(api.get(), channel.getUser());
					channel.sendMessage(ownerLanguage.get(Term.GUILD_JOIN_WELCOME_BACK, "command", apiUrlCommand, "guildId", guildId)).queue();
				} catch (final NamelessException e) {
					// Error with their stored url. Make them update the url
					channel.sendMessage(language.get(Term.GUILD_JOIN_NEEDS_RENEW, "command", apiUrlCommand, "guildId", guildId)).queue();
					LOGGER.info("Guild join, previously stored URL doesn't work");
				}
			}
		});

	}

}
