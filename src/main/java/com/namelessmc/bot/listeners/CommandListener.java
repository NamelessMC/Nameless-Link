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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.namelessmc.bot.Language.Term.ERROR_GENERIC;

public class 		CommandListener extends ListenerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger("Command handler");

	@Override
	public void onSlashCommandInteraction(final @NonNull SlashCommandInteractionEvent event) {
		final User user = event.getUser();

		if (user.isBot()) {
			return;
		}

		final String path = event.getCommandPath();
		final Command command = Command.getCommand(path);

		Guild guild = event.getGuild();

		if (guild == null) {
			LOGGER.error("I don't know how to handle DM command '/{}'", path);
			return;
		}

		if (command == null) {
			LOGGER.error("Unknown command '/{}'", path);
		} else {
			LOGGER.info("User {} ran command /{} in guild {}", event.getUser().getAsTag(), path, guild.getIdLong());

			event.deferReply(true).queue(hook -> {
				Main.getExecutorService().execute(() -> {
					final Language language = Language.getGuildLanguage(guild);
					final Optional<NamelessAPI> optApi;
					try {
						optApi = Main.getConnectionManager().getApiConnection(guild.getIdLong());
					} catch (final BackendStorageException e) {
						event.reply(language.get(ERROR_GENERIC)).setEphemeral(true).queue();
						LOGGER.error("storage backend", e);
						return;
					}
					final @Nullable NamelessAPI api = optApi.orElse(null);
					command.execute(event, hook, language, guild, api);
				});
			});
		}
	}
}
