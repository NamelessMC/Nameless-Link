package com.namelessmc.bot.commands;

import com.google.common.base.Ascii;
import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.bot.connections.ConnectionCache;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.exception.NamelessException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import static com.namelessmc.bot.Language.Term.*;

public class URLCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("URL command");

	URLCommand() {
		super("apiurl");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return new CommandDataImpl(this.name, language.get(APIURL_DESCRIPTION))
				.addOption(OptionType.STRING, "url", language.get(APIURL_OPTION_URL), true)
				.addOption(OptionType.STRING, "apikey", language.get(APIURL_OPTION_APIKEY), true);
	}

	@Override
	public void execute(final @NotNull SlashCommandInteractionEvent event,
						final @NotNull InteractionHook hook,
						final @NotNull Language language,
						final @NotNull Guild guild,
						final @Nullable NamelessAPI oldApi) {
		final String apiUrlString = Objects.requireNonNull(event.getOption("url"), "url is a required option, it should never be null").getAsString();
		final String apiKey = Objects.requireNonNull(event.getOption("apikey"), "api key is a required option, it should never be null").getAsString();
		final long guildId = guild.getIdLong();

		if (Main.getConnectionManager().isReadOnly()) {
			hook.sendMessage(language.get(ERROR_READ_ONLY_STORAGE)).queue();
			LOGGER.warn("Read only storage");
			return;
		}

		Main.canModifySettings(event.getUser(), guild, canModifySettings -> {
			if (!canModifySettings) {
				hook.sendMessage(language.get(ERROR_NO_PERMISSION)).queue();
				LOGGER.warn("User not allowed to modify API URL");
				return;
			}

			if (apiUrlString.equals("none")) {
				if (oldApi == null) {
					hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
					return;
				}

				try {
					Main.getConnectionManager().removeConnection(guildId);
					hook.sendMessage(language.get(APIURL_UNLINKED)).queue();
					LOGGER.info("Unlinked from guild {}", guildId);
				} catch (final BackendStorageException e) {
					hook.sendMessage(language.get(ERROR_GENERIC)).queue();
					LOGGER.error("storage backend", e);
				}
				return;
			}

			URL apiUrl;
			try {
				apiUrl = new URL(apiUrlString);
			} catch (final MalformedURLException e) {
				hook.sendMessage(language.get(APIURL_URL_MALFORMED)).queue();
				return;
			}

			try {
				final Optional<Long> optExistingGuildId = Main.getConnectionManager().getGuildIdByApiUrl(apiUrl);

				if (optExistingGuildId.isPresent() && optExistingGuildId.get() != guildId) {
					hook.sendMessage(language.get(APIURL_ALREADY_USED, "command", "/apiurl none none")).queue();
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
					api.discord().updateBotSettings(Main.getBotUrl(), guildId, botUser.getAsTag(), botUser.getIdLong());

					if (oldApi == null) {
						// User is setting up new connection
						Main.getConnectionManager().createConnection(guildId, apiUrl, apiKey);
						hook.sendMessage(language.get(APIURL_SUCCESS_NEW)).queue();
						LOGGER.info("Set API URL for guild {} to {}", guildId, apiUrl);
					} else {
						// User is modifying API URL for existing connection
						Main.getConnectionManager().updateConnection(guildId, apiUrl, apiKey);
						hook.sendMessage(language.get(APIURL_SUCCESS_UPDATED)).queue();
						LOGGER.info("Updated API URL for guild {} from {} to {}", guildId, oldApi, apiUrl);
					}

					DiscordRoleListener.sendRolesAsync(guildId);
				} catch (final NamelessException e) {
					hook.sendMessage("```\n" + Ascii.truncate(e.getMessage(), 1500, "[truncated]") + "\n```").queue();
					hook.sendMessage(language.get(APIURL_FAILED_CONNECTION)).queue();
					Main.logConnectionError(LOGGER, e);
				}
			} catch (final BackendStorageException e){
				hook.sendMessage(language.get(ERROR_GENERIC)).queue();
				LOGGER.error("storage backend", e);
			}
		});
	}
}
