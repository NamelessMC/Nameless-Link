package com.namelessmc.bot.commands;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.ApiError;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class VerifyCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("Verify command");

	VerifyCommand() {
		super("verify");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return new CommandData(this.name, language.get(Term.VERIFY_DESCRIPTION))
				.addOption(OptionType.STRING, "token", language.get(Term.VERIFY_OPTION_TOKEN), true);
	}

	@Override
	public void execute(final SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		final Language language = Language.getGuildLanguage(guild);

		final String token = event.getOption("token").getAsString();

//		if (token.length() < 40 || !token.contains(":")) {
//			event.reply(language.get(Term.VERIFY_TOKEN_INVALID)).setEphemeral(true).queue();
//			return;
//		}

//		try {
//			final long providedGuildId = Long.parseLong(token.substring(0, token.indexOf(':')));
//
//			if (guild.getIdLong() != providedGuildId) {
//				event.reply("token for different guild").queue(); // TODO translate
//				return;
//			}
//		} catch (final NumberFormatException e) {
//			event.reply(language.get(Term.VERIFY_TOKEN_INVALID)).setEphemeral(true).queue();
//			return;
//		}
//		final String verify = token.substring(token.indexOf(':') + 1);

		event.deferReply().setEphemeral(true).queue();

		final long guildId = guild.getIdLong();
		final long userId = event.getUser().getIdLong();
		final String userTag = event.getUser().getAsTag();

		Main.getExecutorService().execute(() -> {
			final InteractionHook hook = event.getHook();
			Optional<NamelessAPI> api;
			try {
				api = Main.getConnectionManager().getApi(guildId);
			} catch (final BackendStorageException e) {
				LOGGER.error("Storage error", e);
				hook.sendMessage(language.get(Term.ERROR_GENERIC)).queue();
				return;
			}

			if (api.isEmpty()) {
				hook.sendMessage(language.get(Term.VERIFY_NOT_USED)).queue();
				return;
			}

			try {
				api.get().verifyDiscord(token, userId, userTag);
				hook.sendMessage(language.get(Term.VERIFY_SUCCESS)).queue();
				LOGGER.info("Verified user {} in guild {}", userTag, guildId);
			} catch (final ApiError e) {
				if (e.getError() == ApiError.INVALID_VALIDATE_CODE || e.getError() == ApiError.UNABLE_TO_FIND_USER) {
					hook.sendMessage(language.get(Term.VERIFY_TOKEN_INVALID)).queue();
				} else {
					LOGGER.warn("Unexpected error code {}", e.getError());
					hook.sendMessage(language.get(Term.ERROR_WEBSITE_CONNECTION)).queue();
				}
				return;
			} catch (final NamelessException e) {
				hook.sendMessage(language.get(Term.ERROR_WEBSITE_CONNECTION)).queue();
				Main.logConnectionError(LOGGER, "Website connection error", e);
				return;
			}

			DiscordRoleListener.sendUserRolesAsync(guildId, userId);
		});
	}
}
