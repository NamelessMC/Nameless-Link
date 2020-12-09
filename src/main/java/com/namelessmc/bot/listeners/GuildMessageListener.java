package com.namelessmc.bot.listeners;

import com.namelessmc.bot.commands.Command;
import com.namelessmc.bot.commands.CommandContext;

import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildMessageListener extends ListenerAdapter {

	@SneakyThrows
	@Override
	public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
		final User user = event.getAuthor();

		if (user.isBot()) {
			return;
		}

		final String message = event.getMessage().getContentRaw();
		final String[] args = message.split(" ");

		final Command command = Command.getCommand(args[0], CommandContext.GUILD_MESSAGE);
		if (command != null) {
			command.execute(user, args, event.getMessage());
		}
	}
}
