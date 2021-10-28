package com.namelessmc.bot.listeners;

import com.namelessmc.bot.commands.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandListener extends ListenerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger("Command handler");

	@Override
	public void onSlashCommand(final SlashCommandEvent event) {
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
			command.execute(event);
		}
	}
}
