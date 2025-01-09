package com.namelessmc.bot.commands;

import static com.namelessmc.bot.Language.Term.APIURL_DESCRIPTION;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.namelessmc.bot.Language;
import com.namelessmc.java_api.NamelessAPI;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class URLCommand extends Command {

	URLCommand() {
		super("apiurl");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return Commands.slash(this.name, language.get(APIURL_DESCRIPTION))
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED);
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
