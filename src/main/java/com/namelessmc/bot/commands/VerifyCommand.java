package com.namelessmc.bot.commands;

import java.util.Arrays;
import java.util.Optional;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.ApiError;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class VerifyCommand extends Command {

	public VerifyCommand() {
		super("!verify", Arrays.asList("!validate", "!link"), CommandContext.PRIVATE_MESSAGE);
	}

	@Override
	public void execute(final User user, final String[] args, final Message message) {
		final Language language = Language.DEFAULT;

		if (args.length != 2) {
			message.reply(language.get(Term.VERIFY_USAGE, "command", "!verify")).queue();
			return;
		}

		final String token = args[1];

		if (token.length() < 40 || !token.contains(":")) {
			message.reply(language.get(Term.VERIFY_TOKEN_INVALID)).queue();
			return;
		}

		final long guildId;
		try {
			guildId = Long.parseLong(token.substring(0, token.indexOf(':')));
		} catch (final NumberFormatException e) {
			message.reply(language.get(Term.VERIFY_TOKEN_INVALID)).queue();
			return;
		}
		final String verify = token.substring(token.indexOf(':') + 1);

		Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			e.printStackTrace();
			message.reply(language.get(Term.ERROR_GENERIC)).queue();
			return;
		}

		if (api.isEmpty()) {
			message.reply(language.get(Term.VERIFY_NOT_USED)).queue();
			return;
		}

		try {
			api.get().verifyDiscord(verify, user.getIdLong(), user.getName() + "#" + user.getDiscriminator());
			message.reply(language.get(Term.VERIFY_SUCCESS)).queue();
		} catch (final ApiError e) {
			if (e.getError() == ApiError.INVALID_VALIDATE_CODE || e.getError() == ApiError.UNABLE_TO_FIND_USER) {
				message.reply(language.get(Term.VERIFY_TOKEN_INVALID)).queue();
				return;
			} else {
				System.out.println("Unexpected error code " + e.getError() + " when trying to verify user");
				message.reply(language.get(Term.ERROR_WEBSITE_CONNECTION)).queue();
				return;
			}
		} catch (final NamelessException e) {
			message.reply(language.get(Term.ERROR_WEBSITE_CONNECTION)).queue();
			return;
		}
	}
}
