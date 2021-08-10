package com.namelessmc.bot.commands;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public abstract class Command {

	private final String label;
	private final List<String> aliases;
	private final CommandContext context;

	private static final Map<String, Command> BY_LABEL = new HashMap<>(); // For command matching
	private static final Set<String> LABELS_AND_ALIASES = new HashSet<>(); // For duplicate checking

	// For help command
	private static final List<String> DM_COMMANDS = new ArrayList<>();
	private static final List<String> GUILD_COMMANDS = new ArrayList<>();

	private static final Logger LOGGER = LoggerFactory.getLogger("Command parser");

	public Command(final String label, final List<String> aliases, final CommandContext context) {
		this.label = label;
		this.aliases = aliases;
		this.context = context;

		// check for duplicate labels or aliases
		if (LABELS_AND_ALIASES.contains(label) ||
				LABELS_AND_ALIASES.stream().anyMatch(aliases::contains)) {
			throw new IllegalStateException("Command label " + label + " or one of its aliases is already registered");
		}

		LABELS_AND_ALIASES.add(label);
		aliases.forEach(LABELS_AND_ALIASES::add);

		BY_LABEL.put(label, this);
		for (final String alias : aliases) {
			BY_LABEL.put(alias, this);
		}

		if (context == CommandContext.PRIVATE_MESSAGE || context == CommandContext.BOTH) {
			DM_COMMANDS.add(label);
		}
		if (context == CommandContext.GUILD_MESSAGE || context == CommandContext.BOTH) {
			GUILD_COMMANDS.add(label);
		}
	}

	public String getLabel() {
		return this.label;
	}

	public List<String> getAliases(){
		return this.aliases;
	}

	public CommandContext getContext() {
		return this.context;
	}

	protected abstract void execute(User user, String[] args, Message message);

	public static void execute(final Message message) {
		final String commandPrefix = getPrefix(message);
		final String messageContent = message.getContentRaw();

		final CommandContext context = getContext(message);

		if (!messageContent.startsWith(commandPrefix)) {
			if (context == CommandContext.PRIVATE_MESSAGE) {
				sendHelp(commandPrefix, message);
				return;
			} else {
				return;
			}
		}

		final String[] splitMessage = commandPrefix.substring(commandPrefix.length()).split(" ");
		final String commandName = splitMessage[0];
		final String[] args = Arrays.copyOfRange(splitMessage, 1, splitMessage.length);

		final User user = message.getAuthor();

		Command.getCommand(commandName, context).ifPresentOrElse(command -> {
			message.getChannel().sendTyping().queue();
			LOGGER.info("User {}#{} ran command '{}'", user.getName(), user.getDiscriminator(), command.getLabel());
			command.execute(user, args, message);
		}, () -> {
			try {
				sendHelp(commandPrefix, message);
			} catch (final InsufficientPermissionException e) {
				LOGGER.warn("Couldn't send help message to {}#{}, insufficient permissions", user.getName(), user.getDiscriminator());
			}
		});
	}

	private static void sendHelp(final String commandPrefix, final Message originalMessage) {
		final Language language = Language.getDefaultLanguage();

		final List<String> labels = getContext(originalMessage) == CommandContext.PRIVATE_MESSAGE ? DM_COMMANDS : GUILD_COMMANDS;

		final String commandsContent = language.get(Term.HELP_COMMANDS_CONTENT, "commands",
				"`" + commandPrefix + String.join("`, `" + commandPrefix, labels) + "`");
		final MessageEmbed embed = Main.getEmbedBuilder().clear().setColor(Color.GREEN)
				.setTitle(language.get(Term.HELP_TITLE))
				.addField(language.get(Term.HELP_COMMANDS_TITLE), commandsContent, false)
				.addField(language.get(Term.HELP_CONTEXT_TITLE), language.get(Term.HELP_CONTEXT_CONTENT), false)
				.build();
		originalMessage.replyEmbeds(embed).queue();
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

	public static Optional<Command> getCommand(final String label, final CommandContext context) {
		final Command command = BY_LABEL.get(label);
		if (command == null) {
			return Optional.empty();
		}

		return checkContext(command.getContext(), context) ? Optional.of(command) : Optional.empty();
	}

	private static boolean checkContext(final CommandContext givenContext, final CommandContext receivedContext) {
		return givenContext == receivedContext || givenContext == CommandContext.BOTH;
	}

	public static String getPrefix(final Message message) {
		return getContext(message).equals(CommandContext.PRIVATE_MESSAGE) ? Main.getDefaultCommandPrefix() : getGuildPrefix(message.getGuild());
	}

	public static String getGuildPrefix(final Guild guild) {
		return getGuildPrefix(guild.getIdLong());
	}

	public static String getGuildPrefix(final long guildId) {
		try {
			return Main.getConnectionManager().getCommandPrefixByGuildId(guildId).orElse(Main.getDefaultCommandPrefix());
		} catch (final BackendStorageException e) {
			e.printStackTrace();
		}
		return Main.getDefaultCommandPrefix();
	}
}
