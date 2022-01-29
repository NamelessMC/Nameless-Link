package com.namelessmc.bot.commands;

import com.google.common.base.Ascii;
import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.bot.connections.ConnectionCache;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public class URLCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("URL command");

	URLCommand() {
		super("apiurl");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return new CommandData(this.name, language.get(Term.APIURL_DESCRIPTION))
				.addOption(OptionType.STRING, "url", language.get(Term.APIURL_OPTION_URL), true)
				.addOption(OptionType.STRING, "apikey", language.get(Term.APIURL_OPTION_APIKEY), true);
	}

	@Override
	public void execute(final SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		final Language language = Language.getGuildLanguage(guild);

		if (Main.getConnectionManager().isReadOnly()) {
			event.reply(language.get(Term.ERROR_READ_ONLY_STORAGE)).setEphemeral(true).queue();
			LOGGER.warn("Read only storage");
			return;
		}

		final long guildId = guild.getIdLong();

		Main.canModifySettings(event.getUser(), guild, (canModifySettings) -> {
			if (!canModifySettings) {
				event.reply(language.get(Term.ERROR_NO_PERMISSION)).setEphemeral(true).queue();
				LOGGER.warn("User not allowed to modify API URL");
				return;
			}

			Main.getExecutorService().execute(() -> {
				final String apiUrlString = Objects.requireNonNull(event.getOption("url"), "url is a required option, it should never be null").getAsString();
				final String apiKey = Objects.requireNonNull(event.getOption("apikey"), "api key is a required option, it should never be null").getAsString();

				if (apiUrlString.equals("none")) {
					try {
						Main.getConnectionManager().removeConnection(guildId);
						event.reply(language.get(Term.APIURL_UNLINKED)).setEphemeral(true).queue();
						LOGGER.info("Unlinked from guild {}", guildId);
					} catch (final BackendStorageException e) {
						event.reply(language.get(Term.ERROR_GENERIC)).setEphemeral(true).queue();
						LOGGER.error("storage backend", e);
					}
					return;
				}

				URL apiUrl;
				try {
					apiUrl = new URL(apiUrlString);
				} catch (final MalformedURLException e) {
					event.reply(language.get(Term.APIURL_URL_MALFORMED)).setEphemeral(true).queue();
					return;
				}

				// This may take a while
				event.deferReply().setEphemeral(true).queue(hook -> {
					try {
						final Optional<Long> optExistingGuildId = Main.getConnectionManager().getGuildIdByApiUrl(apiUrl);

						if (optExistingGuildId.isPresent()) {
							hook.sendMessage(language.get(Term.APIURL_ALREADY_USED, "command", "/apiurl none")).queue();
							LOGGER.info("API URL already used");
							return;
						}

						LOGGER.info("Checking if API URL works...");

						NamelessAPI api = ConnectionCache.getApiConnection(apiUrl, apiKey);
						long ping = PingCommand.checkConnection(api, LOGGER, language, hook);

						if (ping == -1) {
							// it didn't work, the checkConnection method already send an error message
							return;
						}

						LOGGER.info("API URL seems to work. Sending bot settings...");

						try {
							final User botUser = Main.getJdaForGuild(guildId).getSelfUser();
							api.setDiscordBotSettings(Main.getBotUrl(), guildId, botUser.getAsTag(), botUser.getIdLong());

							final Optional<NamelessAPI> oldApi = Main.getConnectionManager().getApiConnection(guildId);

							if (oldApi.isEmpty()) {
								// User is setting up new connection
								Main.getConnectionManager().createConnection(guildId, apiUrl, apiKey);
								hook.sendMessage(language.get(Term.APIURL_SUCCESS_NEW)).queue();
								LOGGER.info("Set API URL for guild {} to {}", guildId, apiUrl);
							} else {
								// User is modifying API URL for existing connection
								Main.getConnectionManager().updateConnection(guildId, apiUrl, apiKey);
								hook.sendMessage(language.get(Term.APIURL_SUCCESS_UPDATED)).queue();
								LOGGER.info("Updated API URL for guild {} from {} to {}", guildId, oldApi.get(), apiUrl);
							}

							DiscordRoleListener.sendRolesAsync(guildId);
						} catch (final NamelessException e) {
							hook.sendMessage(new MessageBuilder().appendCodeBlock(Ascii.truncate(e.getMessage(), 1500, "[truncated]"), "txt").build()).queue();
							hook.sendMessage(language.get(Term.APIURL_FAILED_CONNECTION)).queue();
							Main.logConnectionError(LOGGER, "Website connection error while sending bot settings", e);
						}
					} catch (final BackendStorageException e){
						hook.sendMessage(language.get(Term.ERROR_GENERIC)).queue();
						LOGGER.error("storage backend", e);
					}
				});
			});
		});
	}
}
