package com.namelessmc.bot.commands;

import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class UnlinkCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("Unlink command");

	public UnlinkCommand() {
		super("unlink", Collections.emptyList(), CommandContext.PRIVATE_MESSAGE);
	}

	@Override
	public void execute(final User user, final String[] args, final Message message) {
		final Language language = Language.getDefaultLanguage();

		if (Main.getConnectionManager().isReadOnly()) {
			message.reply(language.get(Term.ERROR_READ_ONLY_STORAGE)).queue();
			return;
		}

		if (args.length != 1) {
			message.reply(language.get(Term.UNLINK_USAGE, "command", getPrefix(message) + "unlink")).queue();
			return;
		}

		long guildId;
		try {
			guildId = Long.parseLong(args[0]);
		} catch (final NumberFormatException e) {
			message.reply(language.get(Term.ERROR_GUILD_ID_INVALID)).queue();
			return;
		}

		Optional<NamelessAPI> optApi;
		try {
			optApi = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			message.reply(language.get(Term.ERROR_GENERIC)).queue();
			LOGGER.error("storage backend", e);
			return;
		}

		if (optApi.isEmpty()) {
			message.reply(language.get(Term.UNLINK_GUILD_NOT_LINKED)).queue();
			return;
		}

		final NamelessAPI api = optApi.get();

		final Language language2 = Language.getDiscordUserLanguage(api, user);

		final Guild guild = Main.getJda().getGuildById(guildId);

		if (guild == null) {
			message.reply(language2.get(Term.ERROR_GUILD_UNKNOWN)).queue();
			return;
		}

		Main.canModifySettings(user, guild, (canModifySettings) -> {
			if (!canModifySettings) {
				message.reply(language2.get(Term.ERROR_NO_PERMISSION)).queue();
				return;
			}

			Main.getExecutorService().execute(() -> {
				try {
					Main.getConnectionManager().removeConnection(guildId);
					LOGGER.info("Unlinked from guild {}", guildId);
				} catch (final BackendStorageException e) {
					message.reply(language2.get(Term.ERROR_GENERIC)).queue();
					LOGGER.error("storage backend", e);
					return;
				}

				message.addReaction("U+2705").queue(); // ✅
			});
		});
	}

}
