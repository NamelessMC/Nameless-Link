package com.namelessmc.bot;

import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;

public class Utils {

	public static void messageGuildOwner(final String guild_id, final String message) {
		messageUser(Main.getJda().getGuildById(guild_id).retrieveOwner().complete().getUser(), message);
	}

	public static void messageUser(final User user, final String message) {
		user.openPrivateChannel().queue((channel) -> channel.sendMessage(message).queue());
	}

	public static void messageUser(final User user, final EmbedBuilder embed) {
		user.openPrivateChannel().queue((channel) -> channel.sendMessage(embed.build()).queue());
	}

	public static String listToString(final List<String> list, final String delimiter) {
		return String.join(delimiter, list);
	}
}
