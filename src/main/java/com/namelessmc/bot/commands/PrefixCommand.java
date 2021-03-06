package com.namelessmc.bot.commands;

import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class PrefixCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("Prefix command");

	public PrefixCommand() {
		super("prefix", Collections.emptyList(), CommandContext.PRIVATE_MESSAGE);
	}

	@Override
	public void execute(final User user, final String[] args, final Message message) {
		final Language defaultLanguage = Language.getDefaultLanguage();

		if (Main.getConnectionManager().isReadOnly()) {
			message.reply(defaultLanguage.get(Language.Term.ERROR_READ_ONLY_STORAGE)).queue();
			return;
		}

		if (args.length != 2) {
			message.reply(defaultLanguage.get(Language.Term.PREFIX_USAGE, "command", getPrefix(message) + "prefix")).queue();
			return;
		}

		long guildId;
		try {
			guildId = Long.parseLong(args[0]);
		} catch (final NumberFormatException e) {
			message.reply(defaultLanguage.get(Language.Term.ERROR_GUILD_ID_INVALID)).queue();
			return;
		}

		Optional<NamelessAPI> optApi;
		try {
			optApi = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			message.reply(defaultLanguage.get(Language.Term.ERROR_GENERIC)).queue();
			e.printStackTrace();
			return;
		}

		if (optApi.isEmpty()) {
			message.reply(defaultLanguage.get(Language.Term.ERROR_NOT_SET_UP)).queue();
			return;
		}

		final NamelessAPI api = optApi.get();

		final Language language = Language.getDiscordUserLanguage(api, user);

		final Guild guild = Main.getJdaForGuild(guildId).getGuildById(guildId);

		if (guild == null) {
			message.reply(language.get(Language.Term.ERROR_GUILD_UNKNOWN)).queue();
			return;
		}

		Main.canModifySettings(user, guild, canModifySettings -> {
			if (!canModifySettings) {
				message.reply(language.get(Language.Term.ERROR_NO_PERMISSION)).queue();
				return;
			}
			try {
				final Optional<String> newPrefix = args[1].equals("reset") ? Optional.empty() : Optional.of(args[1]);
				Main.getConnectionManager().setCommandPrefix(guildId, newPrefix);
				message.reply(language.get(Language.Term.PREFIX_SUCCESS,
						"newPrefix", newPrefix.orElse(Main.getDefaultCommandPrefix()))).queue();
				LOGGER.info("Modified prefix for guild " + guildId);
			} catch (final BackendStorageException e) {
				message.reply(language.get(Language.Term.ERROR_GENERIC)).queue();
				e.printStackTrace();
				return;
			}

			message.addReaction("U+2705").queue(); // ✅
		});
	}
}
