package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class Command {

	@Getter
	private final String label;
	@Getter
	private final List<String> aliases;
	@Getter
	private final CommandContext context;

	@Getter
	private static final HashMap<String, Command> registeredCommands = new HashMap<>();
	private static final List<String> registeredCommandLabels = new ArrayList<>();

	public Command(final String label, final List<String> aliases, final CommandContext context) {
		this.label = label;
		this.aliases = aliases;
		this.context = context;

		// check for duplicate labels or aliases
		if (registeredCommandLabels.contains(label)) {
			throw new IllegalStateException("Command already registered. Label: " + label);
		}
		if (registeredCommandLabels.stream().anyMatch(aliases::contains)) {
			throw new IllegalStateException("Command already registered. Label: " + label);
		}

		// add these labels and aliases to check for duplication next time
		registeredCommandLabels.add(label);
		registeredCommandLabels.addAll(aliases);

		// register the command and aliases
		registeredCommands.put(label, this);
		for (final String alias : aliases) {
			registeredCommands.put(alias, this);
		}
	}

	protected abstract void execute(User user, String[] args, Message message);
	
	public static void execute(final Message message) {
		String[] splitMessage = message.getContentRaw().split(" ");
		final String commandName = splitMessage[0];
		String[] args = Arrays.copyOfRange(splitMessage, 1, splitMessage.length);
	
		final User user = message.getAuthor();
		
		final CommandContext context = getContext(message);
		final Command command = Command.getCommand(commandName, context);
		
		if (command == null) {
			if (context == CommandContext.PRIVATE_MESSAGE) {
				final Language language = Language.getDefaultLanguage();
				final String s = language.get(Term.INVALID_COMMAND, "commands", "`!unlink`, `!updateusername`, `!apiurl`, `!verify`, `!ping`");
				message.getChannel().sendMessage(Main.getEmbedBuilder().clear().setColor(Color.GREEN)
						.setTitle(language.get(Term.COMMANDS))
						.addField(language.get(Term.HELP), s, false).build()).queue();
			}
			return;
		}
		
		message.addReaction("U+1F7E0").queue(ignored -> { // 🟠
			command.execute(user, args, message);

			message.removeReaction("U+1F7E0").queue(); // 🟠
		});
		

	}
	
	private static CommandContext getContext(final Message message) {
		if (message.getChannel() instanceof PrivateChannel) {
			return CommandContext.PRIVATE_MESSAGE;
		} else if (message.getChannel() instanceof TextChannel) {
			return CommandContext.GUILD_MESSAGE;
		} else {
			throw new IllegalArgumentException("Unknown Channel instance");
		}
	}

	public static Command getCommand(final String label, final CommandContext context) {
		for (final Command command : registeredCommands.values()) {
			if (command.getLabel().equals(label)) {
				if (checkContext(command.getContext(), context)) {
					return command;
				}
			} else {
				if (command.getAliases().contains(label)) {
					if (checkContext(command.getContext(), context)) {
						return command;
					}
				}
			}
		}
		return null;
	}

	private static boolean checkContext(final CommandContext givenContext, final CommandContext receivedContext) {
		return givenContext == receivedContext || givenContext == CommandContext.BOTH;
	}
}
