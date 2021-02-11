package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.Collections;
import java.util.Optional;

public class PrefixCommand extends Command {

	public PrefixCommand() {
		super("prefix", Collections.emptyList(), CommandContext.PRIVATE_MESSAGE);
	}

	@Override
	public void execute(User user, String[] args, Message message) {
		Language language = Language.getDefaultLanguage();

		if (Main.getConnectionManager().isReadOnly()) {
			message.reply(language.get(Language.Term.ERROR_READ_ONLY_STORAGE)).queue();
			return;
		}

		if (args.length != 2) {
			message.reply(language.get(Language.Term.PREFIX_USAGE, "command", getPrefix(message) + "prefix")).queue();
			return;
		}

		long guildId;
		try {
			guildId = Long.parseLong(args[0]);
		} catch (final NumberFormatException e) {
			message.reply(language.get(Language.Term.ERROR_GUILD_ID_INVALID)).queue();
			return;
		}

		Optional<NamelessAPI> optApi;
		try {
			optApi = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			message.reply(language.get(Language.Term.ERROR_GENERIC)).queue();
			e.printStackTrace();
			return;
		}

		if (optApi.isEmpty()) {
			// This needs to be discussed, if we should use an own translation here as the message would be the same
			message.reply(language.get(Language.Term.UNLINK_GUILD_NOT_LINKED)).queue();
			return;
		}

		final NamelessAPI api = optApi.get();

		language = Language.getDiscordUserLanguage(api, user);

		final Guild guild = Main.getJda().getGuildById(guildId);

		if (guild == null) {
			// This needs to be discussed, if we should use an own translation here as the message would be the same
			message.reply(language.get(Language.Term.UNLINK_GUILD_UNKNOWN)).queue();
			return;
		}

		if (!Main.canModifySettings(user, guild)) {
			message.reply(language.get(Language.Term.ERROR_NO_PERMISSION)).queue();
			return;
		}

		try {
			String newPrefix = args[1].equals("reset") ? "" : args[1];
			Main.getConnectionManager().setCommandPrefix(guildId, newPrefix);
			message.reply(language.get(Language.Term.PREFIX_SUCCESS,
					"newPrefix", newPrefix.isEmpty() ? Main.getDefaultCommandPrefix() : newPrefix)).queue();
			Main.getLogger().info("Modified prefix for guild " + guildId);
		} catch (final BackendStorageException e) {
			message.reply(language.get(Language.Term.ERROR_GENERIC)).queue();
			e.printStackTrace();
			return;
		}

		message.addReaction("U+2705").queue(); // ✅
	}
}
