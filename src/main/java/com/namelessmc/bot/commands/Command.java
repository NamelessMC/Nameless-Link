package com.namelessmc.bot.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.namelessmc.bot.Language;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class Command {

	protected final String name;

	Command(final String name) {
		this.name = name;
	}

	public abstract CommandData getCommandData(Language language);

	public abstract void execute(SlashCommandEvent event);

	private static final Command[] COMMANDS = {
			new PingCommand(),
			new UpdateUsernameCommand(),
			new URLCommand(),
			new VerifyCommand(),
	};

	private static final Map<String, Command> BY_NAME = new HashMap<>();

	static {
		for (final Command command : COMMANDS) {
			BY_NAME.put(command.name, command);
		}
	}

	public static void sendCommands(final Guild guild) {
		final Language language = Language.getGuildLanguage(guild);

		final CommandData[] commands = new CommandData[COMMANDS.length];

		for (int i = 0; i < commands.length; i++) {
			commands[i] = COMMANDS[i].getCommandData(language);
		}

		guild.updateCommands().addCommands(commands).complete();
	}

	public static Command getCommand(final String name) {
		return BY_NAME.get(Objects.requireNonNull(name));
	}
}
