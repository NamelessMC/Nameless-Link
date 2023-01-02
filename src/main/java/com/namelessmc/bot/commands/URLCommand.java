package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.java_api.NamelessAPI;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.namelessmc.bot.Language.Term.APIURL_DESCRIPTION;

public class URLCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger(URLCommand.class);

	URLCommand() {
		super("apiurl");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return Commands.slash(this.name, language.get(APIURL_DESCRIPTION));
	}

	@Override
	public void execute(final SlashCommandInteractionEvent event,
						final InteractionHook hook,
						final Language language,
						final Guild guild,
						final @Nullable NamelessAPI oldApi) {
		// TODO translation
		hook.sendMessage("This command has been removed, please use '/configure link' instead.").queue();
	}
}
