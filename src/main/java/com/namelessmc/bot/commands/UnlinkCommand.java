package com.namelessmc.bot.commands;

import java.util.Collections;
import java.util.Optional;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class UnlinkCommand extends Command {

	public UnlinkCommand() {
		super("!unlink", Collections.emptyList(), CommandContext.PRIVATE_MESSAGE);
	}

	@Override
	public void execute(final User user, final String[] args, final Message message) {
		message.addReaction("U+1F7E0").queue(); // 🟠
		
		Language language = Language.DEFAULT;
		
		if (args.length != 2) {
			message.reply(language.get("unlink_usage")).queue();;
			message.removeReaction("U+1F7E0").queue(); // 🟠
			return;
		}
		
		long guildId;
		try {
			guildId = Long.parseLong(args[1]);
		} catch (final NumberFormatException e) {
			message.reply(language.get("apiurl_guild_invalid")).queue();
			message.removeReaction("U+1F7E0").queue(); // 🟠
			return;
		}
		
		Optional<NamelessAPI> optApi;
		try {
			optApi = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			message.reply(language.get("error_generic")).queue();
			message.removeReaction("U+1F7E0").queue(); // 🟠
			e.printStackTrace();
			return;
		}
		
		if (optApi.isEmpty()) {
			message.reply(language.get("unlink_guild_invalid")).queue();
			message.removeReaction("U+1F7E0").queue(); // 🟠
			return;
		}
		
		final NamelessAPI api = optApi.get();
		
		language = Language.getDiscordUserLanguage(api, user);
		
		final Guild guild = Main.getJda().getGuildById(guildId);
	
		if (guild == null) {
			message.reply(language.get("unlink_guild_invalid")).queue();
			message.removeReaction("U+1F7E0").queue(); // 🟠
			return;
		}
		
		if (!user.equals(guild.getOwner().getUser())) {
			message.reply(language.get("error_not_owner")).queue();
			message.removeReaction("U+1F7E0").queue(); // 🟠
			return;
		}
		
		try {
			Main.getConnectionManager().removeConnection(guildId);
		} catch (final BackendStorageException e) {
			message.reply(language.get("error_generic")).queue();
			message.removeReaction("U+1F7E0").queue(); // 🟠
			e.printStackTrace();
			return;
		}
		
		message.removeReaction("U+1F7E0").queue(); // 🟠
		message.addReaction("U+2705").queue(); // ✅
	}

}
