package com.namelessmc.bot.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.commands.Command;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

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

		if (command == null) {
			LOGGER.error("Unkown command '{}'", path);
		} else {
			LOGGER.info("User {} ran command {}", event.getUser().getAsTag(), path);
			command.execute(event);
		}
	}
}
