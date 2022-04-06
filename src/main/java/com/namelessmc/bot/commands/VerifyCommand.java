package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.exception.InvalidValidateCodeException;
import com.namelessmc.java_api.integrations.DiscordIntegrationData;
import com.namelessmc.java_api.integrations.IntegrationData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public void execute(final @NotNull SlashCommandEvent event,
						final @NotNull InteractionHook hook,
						final @NotNull Language language,
						final @NotNull Guild guild,
						final @Nullable NamelessAPI api) {
		final String token = event.getOption("token").getAsString();

		final long guildId = guild.getIdLong();
		final long userId = event.getUser().getIdLong();
		final String userTag = event.getUser().getAsTag();

		if (api == null) {
			hook.sendMessage(language.get(Term.ERROR_NOT_SET_UP)).queue();
			return;
		}

		final IntegrationData integrationData = new DiscordIntegrationData(userId, userTag);

		try {
			api.verifyIntegration(integrationData, token);
		} catch (InvalidValidateCodeException e) {
			hook.sendMessage(language.get(Term.VERIFY_TOKEN_INVALID)).queue();
			return;
		} catch (final NamelessException e) {
			hook.sendMessage(language.get(Term.ERROR_WEBSITE_CONNECTION)).queue();
			Main.logConnectionError(LOGGER, "Website connection error", e);
			return;
		}

		hook.sendMessage(language.get(Term.VERIFY_SUCCESS)).queue();
		LOGGER.info("Verified user {} in guild {}", userTag, guildId);

		DiscordRoleListener.sendUserRolesAsync(guildId, userId);
	}
}
