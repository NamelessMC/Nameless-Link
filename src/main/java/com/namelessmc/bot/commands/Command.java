package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.java_api.NamelessAPI;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class Command {

	protected final String name;

	Command(final String name) {
		this.name = name;
	}

	public abstract CommandData getCommandData(final Language language);

	public abstract void execute(final @NotNull SlashCommandInteractionEvent event,
								 final @NotNull InteractionHook hook,
								 final @NotNull Language language,
								 final @NotNull Guild guild,
								 final @Nullable NamelessAPI api);

	private static final Command[] COMMANDS = {
			new PingCommand(),
			new RegisterCommand(),
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
