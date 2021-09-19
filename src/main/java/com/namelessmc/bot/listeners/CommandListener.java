package com.namelessmc.bot.listeners;

import com.namelessmc.bot.commands.Command;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

	@Override
	public void onSlashCommand(final SlashCommandEvent event) {
		final User user = event.getUser();

		if (user.isBot()) {
			return;
		}

		final Command command = Command.getCommand(event.getCommandPath());

		if (command == null) {
			event.reply("unknown command?").queue();
		} else {
			command.execute(event);
		}
	}
}
