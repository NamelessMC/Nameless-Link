package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.java_api.ApiError;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessUser;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.namelessmc.bot.Language.Term.*;

public class UpdateUsernameCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("Update username command");

	UpdateUsernameCommand() {
		super("updateusername");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return new CommandDataImpl(this.name, language.get(UPDATEUSERNAME_DESCRIPTION));
	}

	@Override
	public void execute(final @NotNull SlashCommandInteractionEvent event,
						final @NotNull InteractionHook hook,
						final @NotNull Language language,
						final @NotNull Guild guild,
						final @Nullable NamelessAPI api) {
		final long userId = event.getUser().getIdLong();
		final String userTag = event.getUser().getAsTag();

		if (api == null) {
			hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
			return;
		}

		Optional<NamelessUser> optNameless;
		try {
			optNameless = api.getUserByDiscordId(userId);
		} catch (final NamelessException e) {
			Main.logConnectionError(LOGGER, "Website connection error during get user by discord id", e);
			hook.sendMessage(language.get(ERROR_WEBSITE_CONNECTION)).queue();
			return;
		}

		if (optNameless.isEmpty()) {
			hook.sendMessage(language.get(ERROR_NOT_LINKED)).queue();
			return;
		}

		try {
			api.updateDiscordUsername(userId, userTag);
			LOGGER.info("Updated username for user {} to '{}'", userId, userTag);
		} catch (final ApiError e) {
			if (e.getError() == ApiError.UNABLE_TO_FIND_USER) {
				hook.sendMessage(language.get(ERROR_NOT_LINKED)).queue();
			} else {
				LOGGER.warn("Error code {} while updating username", e.getError());
				hook.sendMessage(language.get(ERROR_GENERIC)).queue();
			}
			return;
		} catch (final NamelessException e) {
			Main.logConnectionError(LOGGER, "Website connection error during update discord username", e);
			hook.sendMessage(language.get(ERROR_WEBSITE_CONNECTION)).queue();
			return;
		}

		hook.sendMessage(language.get(UPDATEUSERNAME_SUCCESS)).queue();
	}

}
