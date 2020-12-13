package com.namelessmc.bot.commands;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class URLCommand extends Command {

	public URLCommand() {
		super("!apiurl", Collections.emptyList(), CommandContext.PRIVATE_MESSAGE);
	}

	@Override
	public void execute(final User user, final String[] args, final Message message) {
		final Language language = Language.DEFAULT;

		if (args.length != 3) {
			message.reply(language.get("apiurl_usage")).queue();
			return;
		}

		final long guildId;
		try {
			guildId = Long.parseLong(args[1]);
		} catch (final NumberFormatException e) {
			message.reply(language.get("apiurl_guild_invalid")).queue();
			return;
		}

		URL apiUrl;
		try {
			apiUrl = new URL(args[2]);
		} catch (final MalformedURLException e) {
			message.reply(language.get("apiurl_url_malformed")).queue();
			return;
		}

		final Guild guild = Main.getJda().getGuildById(guildId);

		if (guild == null) {
			message.reply(language.get("apiurl_guild_invalid")).queue();
			return;
		}

		if (guild.getOwnerIdLong() != user.getIdLong()) {
			message.reply(language.get("apiurl_not_owner")).queue();
			return;
		}

		// Check if API URL works
		NamelessAPI api;
		try {
			api = Main.newApiConnection(apiUrl);
			api.checkWebAPIConnection();
		} catch (final NamelessException e) {
			message.reply(language.get("apiurl_failed_connection")).queue();
			return;
		}

		try {
			api.setDiscordBotUrl(Main.getBotUrl());
			api.setDiscordGuildId(guildId);

			final Optional<NamelessAPI> oldApi = Main.getConnectionManager().getApi(guildId);

			if (oldApi.isEmpty()) {
				// User is setting up new connection
				Main.getConnectionManager().newConnection(guildId, apiUrl);
				message.reply(language.get("apiurl_success_new")).queue();
			} else {
				// User is modifying API url for existing connection
				Main.getConnectionManager().updateConnection(guildId, apiUrl);
				message.reply(language.get("apiurl_success_updated")).queue();
			}
			
			DiscordRoleListener.sendRoleListToWebsite(guild);
		} catch (final BackendStorageException e) {
			message.reply(language.get("error_generic")).queue();
		} catch (final NamelessException e) {
			message.reply(language.get("apiurl_failed_connection")).queue();
		}
	}
}
