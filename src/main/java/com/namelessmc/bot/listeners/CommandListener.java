package com.namelessmc.bot.listeners;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.commands.Command;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

	@Override
	public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
		Main.getExecutorService().execute(() -> {
			final User user = event.getAuthor();

			if (user.isBot()) {
				return;
			}

			Command.execute(event.getMessage());
		});
	}

	@Override
	public void onPrivateMessageReceived(final PrivateMessageReceivedEvent event) {
		Main.getExecutorService().execute(() -> {
			final User user = event.getAuthor();

			if (user.isBot()) {
				return;
			}

			Command.execute(event.getMessage());
		});
	}
}
