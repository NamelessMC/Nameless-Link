package com.namelessmc.bot.commands;

import static com.namelessmc.bot.Language.Term.ERROR_DUPLICATE_DISCORD_INTEGRATION;
import static com.namelessmc.bot.Language.Term.ERROR_DUPLICATE_EMAIL_ADDRESS;
import static com.namelessmc.bot.Language.Term.ERROR_DUPLICATE_USERNAME;
import static com.namelessmc.bot.Language.Term.ERROR_INVALID_EMAIL_ADDRESS;
import static com.namelessmc.bot.Language.Term.ERROR_INVALID_USERNAME;
import static com.namelessmc.bot.Language.Term.ERROR_NOT_SET_UP;
import static com.namelessmc.bot.Language.Term.ERROR_SEND_VERIFICATION_EMAIL;
import static com.namelessmc.bot.Language.Term.ERROR_WEBSITE_CONNECTION;
import static com.namelessmc.bot.Language.Term.REGISTER_DESCRIPTION;
import static com.namelessmc.bot.Language.Term.REGISTER_EMAIL;
import static com.namelessmc.bot.Language.Term.REGISTER_OPTION_EMAIL;
import static com.namelessmc.bot.Language.Term.REGISTER_OPTION_USERNAME;
import static com.namelessmc.bot.Language.Term.REGISTER_URL;

import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.exception.ApiException;
import com.namelessmc.java_api.exception.NamelessException;
import com.namelessmc.java_api.integrations.DiscordIntegrationData;
import com.namelessmc.java_api.integrations.IntegrationData;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class RegisterCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegisterCommand.class);

	RegisterCommand() {
		super("register");
	}

	@Override
	public CommandData getCommandData(Language language) {
		return Commands.slash(this.name, language.get(REGISTER_DESCRIPTION))
				.addOption(OptionType.STRING, "username", language.get(REGISTER_OPTION_USERNAME), true)
				.addOption(OptionType.STRING, "email", language.get(REGISTER_OPTION_EMAIL), true);
	}

	@Override
	public void execute(final SlashCommandInteractionEvent event,
						final InteractionHook hook,
						final Language language,
						final Guild guild,
						final @Nullable NamelessAPI api) {
		final String username = event.getOption("username").getAsString();
		final String email = event.getOption("email").getAsString();
		final long discordId = event.getUser().getIdLong();
		final String discordUsername = event.getUser().getName();

		if (api == null) {
			hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
			LOGGER.info("Website connection not set up");
			return;
		}

		final IntegrationData integrationData = new DiscordIntegrationData(discordId, discordUsername);
		try {
			final Optional<String> verificationUrl = api.registerUser(username, email, integrationData);
			if (verificationUrl.isPresent()) {
				LOGGER.info("Registration successful, sending registration URL");
				hook.sendMessage(language.get(REGISTER_URL, "url", verificationUrl.get())).queue();
			} else {
				LOGGER.info("Registration successful, registration URL has been sent in an email");
				hook.sendMessage(language.get(REGISTER_EMAIL)).queue();
			}
		} catch (final NamelessException e) {
			if (e instanceof final ApiException apiException) {
				switch (apiException.apiError()) {
					case CORE_INVALID_USERNAME:
						hook.sendMessage(language.get(ERROR_INVALID_USERNAME)).queue();
						return;
					case CORE_USERNAME_ALREADY_EXISTS:
						hook.sendMessage(language.get(ERROR_DUPLICATE_USERNAME)).queue();
						return;
					case CORE_UNABLE_TO_SEND_REGISTRATION_EMAIL:
						hook.sendMessage(language.get(ERROR_SEND_VERIFICATION_EMAIL)).queue();
						return;
					case CORE_INVALID_EMAIL_ADDRESS:
						hook.sendMessage(language.get(ERROR_INVALID_EMAIL_ADDRESS)).queue();
						return;
					case CORE_EMAIL_ALREADY_EXISTS:
						hook.sendMessage(language.get(ERROR_DUPLICATE_EMAIL_ADDRESS)).queue();
						return;
					case CORE_INTEGRATION_IDENTIFIER_ERROR:
					case CORE_INTEGRATION_USERNAME_ERROR:
						hook.sendMessage(language.get(ERROR_DUPLICATE_DISCORD_INTEGRATION)).queue();
						return;
					default:
						// generic error message is sent below
				}
			}

			hook.sendMessage(language.get(ERROR_WEBSITE_CONNECTION)).queue();
			Main.logConnectionError(LOGGER, e);
		}
	}

}
