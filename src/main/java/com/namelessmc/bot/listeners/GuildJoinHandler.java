package com.namelessmc.bot.listeners;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.commands.Command;
import com.namelessmc.bot.commands.ConfigureCommand;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessVersion;
import com.namelessmc.java_api.Website;
import com.namelessmc.java_api.exception.NamelessException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.namelessmc.bot.Language.Term.*;

public class GuildJoinHandler extends ListenerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(GuildJoinHandler.class);
	private static final String LINK_COMMAND = "/configure link";

	@Override
	public void onGuildJoin(final GuildJoinEvent event) {
		Guild guild = event.getGuild();
		LOGGER.info("Joined guild '{}'", guild.getName());

		Main.getExecutorService().execute(() -> Command.sendCommands(guild));

		event.getJDA().retrieveUserById(guild.getOwnerIdLong()).flatMap(User::openPrivateChannel).queue(channel -> {
			NamelessAPI api;
			try {
				api = Main.getConnectionManager().getApiConnection(event.getGuild().getIdLong());
			} catch (final BackendStorageException e) {
				LOGGER.error("Storage error during guild join", e);
				return;
			}

			final Language language = Language.getGuildLanguage(guild);

			if (api == null) {
				channel.sendMessage(language.get(GUILD_JOIN_SUCCESS, "command", LINK_COMMAND))
						.queue(message -> LOGGER.info("Sent new join message to {} for guild {}",
								channel.getUser().getName(), event.getGuild().getName()));
			} else {
				try {
					final Website info = api.website();
					final NamelessVersion version = info.parsedVersion();

					if (NamelessVersion.isSupportedByJavaApi(version)) {
						// Good to go
						channel.sendMessage(language.get(GUILD_JOIN_WELCOME_BACK, "command", LINK_COMMAND)).queue();
					} else {
						// Incompatible version
						channel.sendMessage(language.get(ERROR_WEBSITE_VERSION, "version", info.rawVersion(), "compatibleVersions", ConfigureCommand.supportedVersionsList())).queue();
					}
				} catch (final NamelessException e) {
					// Error with their stored url. Make them update the url
					channel.sendMessage(language.get(GUILD_JOIN_NEEDS_RENEW, "command", LINK_COMMAND)).queue();
					LOGGER.info("Guild join, previously stored URL doesn't work");
				}
			}
		});
	}

}
