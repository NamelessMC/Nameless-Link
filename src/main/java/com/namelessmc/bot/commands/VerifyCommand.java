package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.exception.ApiError;
import com.namelessmc.java_api.exception.ApiException;
import com.namelessmc.java_api.exception.NamelessException;
import com.namelessmc.java_api.integrations.DiscordIntegrationData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.namelessmc.bot.Language.Term.*;

public class VerifyCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyCommand.class);

	VerifyCommand() {
		super("verify");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return Commands.slash(this.name, language.get(VERIFY_DESCRIPTION))
				.addOption(OptionType.STRING, "token", language.get(VERIFY_OPTION_TOKEN), true);
	}

	private boolean verifyIntegration(InteractionHook hook, Language language, NamelessAPI api, DiscordIntegrationData integrationData, String token, boolean usingDummyDiscriminator) {
		try {
			api.verifyIntegration(integrationData, token);
			hook.sendMessage(language.get(VERIFY_SUCCESS)).queue();
			return true;
		} catch (final NamelessException e) {
			if (e instanceof ApiException apiException) {
				if (apiException.apiError() == ApiError.CORE_INVALID_CODE) {
					LOGGER.info("Invalid verification token");
					hook.sendMessage(language.get(VERIFY_TOKEN_INVALID)).queue();
				} else if (apiException.apiError() == ApiError.CORE_INTEGRATION_USERNAME_ERROR) {
					// Perhaps an older NamelessMC version that requires a discriminator
					if (usingDummyDiscriminator) {
						LOGGER.info("Invalid username error, already linked?");
						hook.sendMessage(language.get(VERIFY_ALREADY_LINKED)).queue();
					} else {
						LOGGER.info("Invalid username error, trying again with dummy discriminator");
						var newData = new DiscordIntegrationData(integrationData.idLong(), integrationData.username() + "#0000");
						return verifyIntegration(hook, language, api, newData, token, true);
					}
				}
			}
			hook.sendMessage(language.get(ERROR_WEBSITE_CONNECTION)).queue();
			Main.logConnectionError(LOGGER, e);
		}

		return false;
	}

	@Override
	public void execute(final SlashCommandInteractionEvent event,
						final InteractionHook hook,
						final Language language,
						final Guild guild,
						final @Nullable NamelessAPI api) {
		final String token = event.getOption("token").getAsString();

		final long guildId = guild.getIdLong();
		final long userId = event.getUser().getIdLong();
		final String username = event.getUser().getName();

		if (api == null) {
			hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
			return;
		}

		if (verifyIntegration(hook, language, api, new DiscordIntegrationData(userId, username), token, false)) {
			LOGGER.info("Verified user {} in guild {}", username, guildId);
			DiscordRoleListener.sendUserRolesAsync(guildId, userId);
		}
	}
}
