package com.namelessmc.bot.commands;

import static com.namelessmc.bot.Language.Term.PING_DESCRIPTION;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.namelessmc.bot.Language;
import com.namelessmc.java_api.NamelessAPI;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class PingCommand extends Command {

	PingCommand() {
		super("ping");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return Commands.slash(this.name, language.get(PING_DESCRIPTION))
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED);
	}

	@Override
	public void execute(final SlashCommandInteractionEvent event,
						final InteractionHook hook,
						final Language language,
						final Guild guild,
						final @Nullable NamelessAPI api) {
		hook.sendMessage("This command has been removed, please use '/configure test' instead.").queue();
	}

}
