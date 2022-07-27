package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.exception.NamelessException;
import com.namelessmc.java_api.exception.ApiError;
import com.namelessmc.java_api.exception.ApiException;
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

import static com.namelessmc.bot.Language.Term.*;

public class VerifyCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("Verify command");

	VerifyCommand() {
		super("verify");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return new CommandDataImpl(this.name, language.get(VERIFY_DESCRIPTION))
				.addOption(OptionType.STRING, "token", language.get(VERIFY_OPTION_TOKEN), true);
	}

	@Override
	public void execute(final @NotNull SlashCommandInteractionEvent event,
						final @NotNull InteractionHook hook,
						final @NotNull Language language,
						final @NotNull Guild guild,
						final @Nullable NamelessAPI api) {
		final String token = event.getOption("token").getAsString();

		final long guildId = guild.getIdLong();
		final long userId = event.getUser().getIdLong();
		final String userTag = event.getUser().getAsTag();

		if (api == null) {
			hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
			return;
		}

		final IntegrationData integrationData = new DiscordIntegrationData(userId, userTag);

		try {
			api.verifyIntegration(integrationData, token);
		} catch (final NamelessException e) {
			if (e instanceof ApiException apiException) {
				if (apiException.apiError() == ApiError.CORE_INVALID_CODE) {
					hook.sendMessage(language.get(VERIFY_TOKEN_INVALID)).queue();
					return;
				} else if (apiException.apiError() == ApiError.CORE_INTEGRATION_USERNAME_ERROR) {
					hook.sendMessage(language.get(VERIFY_ALREADY_LINKED)).queue();
					return;
				}
			}
			hook.sendMessage(language.get(ERROR_WEBSITE_CONNECTION)).queue();
			Main.logConnectionError(LOGGER, e);
			return;
		}

		hook.sendMessage(language.get(VERIFY_SUCCESS)).queue();
		LOGGER.info("Verified user {} in guild {}", userTag, guildId);

		DiscordRoleListener.sendUserRolesAsync(guildId, userId);
	}
}
