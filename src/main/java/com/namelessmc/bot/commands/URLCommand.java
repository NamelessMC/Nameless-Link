package com.namelessmc.bot.commands;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class URLCommand extends Command {

	public URLCommand() {
		super("!apiurl", Collections.emptyList(), CommandContext.PRIVATE_MESSAGE);
	}

	@Override
	public void execute(final User user, final String[] args, final MessageChannel channel) {
		final Language language = Language.DEFAULT;

		if (args.length != 2) {
			channel.sendMessage(language.get("apiurl_usage"));
			return;
		}

		final long guildId;
		try {
			guildId = Long.parseLong(args[0]);
		} catch (final NumberFormatException e) {
			channel.sendMessage(language.get("apiurl_guild_invalid"));
			return;
		}

		URL apiUrl;
		try {
			apiUrl = new URL(args[1]);
		} catch (final MalformedURLException e) {
			channel.sendMessage(language.get("apiurl_url_malformed"));
			return;
		}

		final Guild guild = Main.getJda().getGuildById(guildId);

		if (guild == null) {
			channel.sendMessage(language.get("apiurl_guild_invalid"));
			return;
		}

		if (guild.getOwnerIdLong() != user.getIdLong()) {
			channel.sendMessage(language.get("apiurl_not_owner"));
			return;
		}

		// Check if API URL works
		NamelessAPI api;
		try {
			api = new NamelessAPI(apiUrl);
			api.checkWebAPIConnection();
		} catch (final NamelessException e) {
			channel.sendMessage(language.get("apiurl_failed_connection"));																										// Message
			return;
		}

		try {
			final Optional<NamelessAPI> oldApi = Main.getConnectionManager().getApi(guildId);

			if (oldApi.isEmpty()) {
				// User is setting up new connection
				Main.getConnectionManager().newConnection(guildId, apiUrl);
				channel.sendMessage(language.get("apiurl_success_new"));
			} else {
				// User is modifying API url for existing connection
				Main.getConnectionManager().updateConnection(guildId, apiUrl);
				channel.sendMessage(language.get("apiurl_success_updated"));
			}

			api.setDiscordBotUrl(Main.getBotUrl());
		} catch (final BackendStorageException | NamelessException e) {
			channel.sendMessage(language.get("apiurl_error"));
		}
	}
}
