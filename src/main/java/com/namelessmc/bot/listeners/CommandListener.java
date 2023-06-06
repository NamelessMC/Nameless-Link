package com.namelessmc.bot.listeners;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.commands.Command;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.namelessmc.bot.Language.Term.ERROR_GENERIC;

public class 		CommandListener extends ListenerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommandListener.class);

	@Override
	public void onSlashCommandInteraction(final SlashCommandInteractionEvent event) {
		final User user = event.getUser();

		if (user.isBot()) {
			return;
		}

		final String name = event.getName();
		final Command command = Command.getCommand(name);

		Guild guild = event.getGuild();

		if (guild == null) {
			LOGGER.error("I don't know how to handle DM command '/{}'", name);
			return;
		}

		if (command == null) {
			LOGGER.error("Unknown command '/{}'", name);
		} else {
			LOGGER.info("User {} ran command /{} in guild {}", event.getUser().getName(), name, guild.getIdLong());

			event.deferReply(true).queue(hook -> {
				Main.getExecutorService().execute(() -> {
					final Language language = Language.getGuildLanguage(guild);
					final NamelessAPI api;
					try {
						api = Main.getConnectionManager().getApiConnection(guild.getIdLong());
					} catch (final BackendStorageException e) {
						event.reply(language.get(ERROR_GENERIC)).setEphemeral(true).queue();
						LOGGER.error("storage backend", e);
						return;
					}
					command.execute(event, hook, language, guild, api);
				});
			});
		}
	}
}
