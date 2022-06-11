package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.exception.NamelessException;
import com.namelessmc.java_api.exception.*;
import com.namelessmc.java_api.integrations.DiscordIntegrationData;
import com.namelessmc.java_api.integrations.IntegrationData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.namelessmc.bot.Language.Term.*;

public class RegisterCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("register command");

	RegisterCommand() {
		super("register");
	}

	@Override
	public CommandData getCommandData(Language language) {
		return new CommandDataImpl(this.name, language.get(REGISTER_DESCRIPTION))
				.addOption(OptionType.STRING, "username", language.get(REGISTER_OPTION_USERNAME), true)
				.addOption(OptionType.STRING, "email", language.get(REGISTER_OPTION_EMAIL), true);
	}

	@Override
	public void execute(final @NotNull SlashCommandInteractionEvent event,
						final @NotNull InteractionHook hook,
						final @NotNull Language language,
						final @NotNull Guild guild,
						final @Nullable NamelessAPI api) {
		final String username = event.getOption("username").getAsString();
		final String email = event.getOption("email").getAsString();
		final long discordId = event.getUser().getIdLong();
		final String discordTag = event.getUser().getAsTag();

		if (api == null) {
			hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
			LOGGER.info("Website connection not set up");
			return;
		}

		IntegrationData integrationData = new DiscordIntegrationData(discordId, discordTag);
		try {
			Optional<String> verificationUrl = api.registerUser(username, email, integrationData);
			if (verificationUrl.isPresent()) {
				LOGGER.info("Registration successful, sending registration URL");
				hook.sendMessage(language.get(REGISTER_URL, "url", verificationUrl.get())).queue();
			} else {
				LOGGER.info("Registration successful, registration URL has been sent in an email");
				hook.sendMessage(language.get(REGISTER_EMAIL)).queue();
			}
		} catch (NamelessException e) {
			if (e instanceof ApiException apiException) {
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
				}
			}

			hook.sendMessage(language.get(ERROR_WEBSITE_CONNECTION)).queue();
			Main.logConnectionError(LOGGER, e);
		}
	}

}
