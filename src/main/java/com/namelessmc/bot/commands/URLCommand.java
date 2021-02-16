package com.namelessmc.bot.commands;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class URLCommand extends Command {

	public URLCommand() {
		super("apiurl", Collections.emptyList(), CommandContext.PRIVATE_MESSAGE);
	}

	@Override
	public void execute(final User user, final String[] args, final Message message) {
		final Language language = Language.getDefaultLanguage();

		if (Main.getConnectionManager().isReadOnly()) {
			message.reply(language.get(Term.ERROR_READ_ONLY_STORAGE)).queue();
			return;
		}

		if (args.length != 2) {
			message.reply(language.get(Term.APIURL_USAGE, "command", getPrefix(message) + "apiurl")).queue();
			return;
		}

		if (!args[1].contains("/index.php?route=/api/v2/")) {
			message.reply(language.get(Term.APIURL_URL_INVALID)).queue();
			return;
		}

		final long guildId;
		try {
			guildId = Long.parseLong(args[0]);
		} catch (final NumberFormatException e) {
			message.reply(language.get(Term.ERROR_GUILD_ID_INVALID)).queue();
			return;
		}

		URL apiUrl;
		try {
			apiUrl = new URL(args[1]);
		} catch (final MalformedURLException e) {
			message.reply(language.get(Term.APIURL_URL_MALFORMED)).queue();
			return;
		}

		final Guild guild = Main.getJda().getGuildById(guildId);

		if (guild == null) {
			message.reply(language.get(Term.ERROR_GUILD_ID_INVALID)).queue();
			return;
		}
		
		Main.canModifySettings(user, guild, (canModifySettings) -> {
			if (!canModifySettings) {
				message.reply(language.get(Term.ERROR_NO_PERMISSION)).queue();
				return;
			}
			
			Main.getExecutorService().execute(() -> {
				// Check if API URL works
				NamelessAPI api;
				try {
					api = Main.newApiConnection(apiUrl);
					api.checkWebAPIConnection();
				} catch (final NamelessException e) {
					message.getChannel().sendMessage(new MessageBuilder().appendCodeBlock(StringUtils.truncate(e.getMessage(), 1500), "txt").build()).queue();
					message.reply(language.get(Term.APIURL_FAILED_CONNECTION)).queue();
					if (apiUrl.toString().startsWith("http://")) {
						message.getChannel().sendMessage(language.get(Term.APIURL_TRY_HTTPS)).queue();
					}
					return;
				}
		
				try {
					final Optional<Long> optExistingGuildId = Main.getConnectionManager().getGuildIdByURL(apiUrl);
		
					if (optExistingGuildId.isPresent()) {
						message.reply(language.get(Term.APIURL_ALREADY_USED, "command", "!unlink " + optExistingGuildId.get())).queue();
						return;
					}
		
					api.setDiscordBotUrl(Main.getBotUrl());
					api.setDiscordGuildId(guildId);
		
					final User botUser = Main.getJda().getSelfUser();
					api.setDiscordBotUser(botUser.getName() + "#" + botUser.getDiscriminator(), botUser.getIdLong());
		
					final Optional<NamelessAPI> oldApi = Main.getConnectionManager().getApi(guildId);
		
					if (oldApi.isEmpty()) {
						// User is setting up new connection
						Main.getConnectionManager().newConnection(guildId, apiUrl);
						message.reply(language.get(Term.APIURL_SUCCESS_NEW)).queue();
					} else {
						// User is modifying API url for existing connection
						Main.getConnectionManager().updateConnection(guildId, apiUrl);
						message.reply(language.get(Term.APIURL_SUCCESS_UPDATED)).queue();
					}
		
					DiscordRoleListener.sendRoleListToWebsite(guild);
		
					Main.getLogger().info("Set up API URL for guild " + guildId + " to " + apiUrl);
				} catch (final BackendStorageException e) {
					message.reply(language.get(Term.ERROR_GENERIC)).queue();
				} catch (final NamelessException e) {
					message.getChannel().sendMessage(new MessageBuilder().appendCodeBlock(StringUtils.truncate(e.getMessage(), 1500), "txt").build()).queue();
					message.reply(language.get(Term.APIURL_FAILED_CONNECTION)).queue();
				}
			});
		});
	}
}
