package com.namelessmc.bot.commands;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.ApiError;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessUser;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class UpdateUsernameCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("Update username command");

	UpdateUsernameCommand() {
		super("updateusername");
	}


	@Override
	public CommandData getCommandData(final Language language) {
		return new CommandData(this.name, language.get(Term.UPDATEUSERNAME_DESCRIPTION));
	}

	@Override
	public void execute(final SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		final Language language = Language.getGuildLanguage(guild);

		final Optional<NamelessAPI> optApi;
		try {
			optApi = Main.getConnectionManager().getApi(guild.getIdLong());
		} catch (final BackendStorageException e) {
			event.reply(language.get(Term.ERROR_GENERIC)).setEphemeral(true).queue();
			return;
		}

		if (optApi.isEmpty()) {
			event.reply(language.get(Term.ERROR_NOT_SET_UP)).setEphemeral(true).queue();
			return;
		}

		event.deferReply().setEphemeral(true).queue();

		final long userId = event.getUser().getIdLong();
		final String userTag = event.getUser().getAsTag();

		Main.getExecutorService().execute(() -> {
			final NamelessAPI api = optApi.get();

			Optional<NamelessUser> optNameless;
			try {
				optNameless = api.getUserByDiscordId(userId);
			} catch (final NamelessException e) {
				Main.logConnectionError(LOGGER, "Website connection error during get user by discord id", e);
				event.getHook().sendMessage(language.get(Term.ERROR_WEBSITE_CONNECTION)).queue();
				return;
			}

			if (optNameless.isEmpty()) {
				event.getHook().sendMessage(language.get(Term.ERROR_NOT_LINKED)).queue();
				return;
			}

			try {
				api.updateDiscordUsername(userId, userTag);
				LOGGER.info("Updated username for user {} to '{}'", userId, userTag);
			} catch (final ApiError e) {
				if (e.getError() == ApiError.UNABLE_TO_FIND_USER) {
					event.getHook().sendMessage(language.get(Term.ERROR_NOT_LINKED)).queue();
				} else {
					LOGGER.warn("Error code {} while updating username", e.getError());
					event.getHook().sendMessage(language.get(Term.ERROR_GENERIC)).queue();
				}
				return;
			} catch (final NamelessException e) {
				Main.logConnectionError(LOGGER, "Website connection error during update discord username", e);
				event.getHook().sendMessage(language.get(Term.ERROR_WEBSITE_CONNECTION)).queue();
				return;
			}

			event.getHook().sendMessage(language.get(Term.UPDATEUSERNAME_SUCCESS)).queue();
		});
	}

}
