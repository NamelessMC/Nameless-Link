package com.namelessmc.bot.listeners;

import java.awt.Color;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Utils;
import com.namelessmc.bot.commands.Command;
import com.namelessmc.bot.commands.CommandContext;

import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class PrivateMessageListener extends ListenerAdapter {

    @SneakyThrows
    @Override
    public void onPrivateMessageReceived(final PrivateMessageReceivedEvent event) {
        final User user = event.getAuthor();

        if (user.isBot()) {
			return;
		}
        final String message = event.getMessage().getContentRaw();
        final String[] args = message.split(" ");

        final Command command = Command.getCommand(args[0], CommandContext.PRIVATE_MESSAGE);
        if (command != null) {
			command.execute(user, args, event.getChannel());
		} else {
			// TODO How do we get the user's language here? Which website do we use?
        	final Language language = Language.DEFAULT;

            Main.getEmbedBuilder().clear().setColor(Color.GREEN).setTitle(language.get("commands")).addField(language.get("help"), language.get("invalid_command"), false);
            Utils.messageUser(user, Main.getEmbedBuilder());
        }
    }
}
