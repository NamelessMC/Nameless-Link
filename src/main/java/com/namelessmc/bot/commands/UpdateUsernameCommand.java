package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.exception.NamelessException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.namelessmc.bot.Language.Term.*;

public class UpdateUsernameCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateUsernameCommand.class);

	UpdateUsernameCommand() {
		super("updateusername");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return Commands.slash(this.name, language.get(UPDATEUSERNAME_DESCRIPTION));
	}

	@Override
	public void execute(final SlashCommandInteractionEvent event,
						final InteractionHook hook,
						final Language language,
						final Guild guild,
						final @Nullable NamelessAPI api) {
		final long userId = event.getUser().getIdLong();
		final String userTag = event.getUser().getAsTag();

		if (api == null) {
			hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
			return;
		}

		try {
			api.discord().updateDiscordUsername(userId, userTag);
			LOGGER.info("Updated username for user {} to '{}'", userId, userTag);
		} catch (final NamelessException e) {
			Main.logConnectionError(LOGGER, "Website connection error during update discord username", e);
			hook.sendMessage(language.get(ERROR_WEBSITE_CONNECTION)).queue();
			return;
		}

		hook.sendMessage(language.get(UPDATEUSERNAME_SUCCESS)).queue();
	}

}
