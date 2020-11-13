package com.namelessmc.bot.commands;

import java.util.Arrays;
import java.util.Optional;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.ApiError;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class VerifyCommand extends Command {

    public VerifyCommand() {
        super("!verify", Arrays.asList("!validate", "!link"), CommandContext.PRIVATE_MESSAGE);
    }

    @Override
    public void execute(final User user, final String[] args, final MessageChannel channel) {
    	final Language language = Language.DEFAULT;
    	
    	if (args.length != 2) {
    		channel.sendMessage(language.get("verification_usage")).queue();
    		return;
    	}
    	
    	final String token = args[1];
    	
    	if (token.length() < 20 || !token.contains(":")) { // TODO Correct length
    		channel.sendMessage(language.get("verification_token_invalid")).queue();
    		return;
    	}
    	
    	final long guildId;
    	try {
    		guildId = Long.parseLong(token.substring(0, token.indexOf(':') - 1));
    	} catch (final NumberFormatException e) {
    		channel.sendMessage(language.get("verification_token_invalid")).queue();
    		return;
    	}
    	final String verify = token.substring(token.indexOf(':') + 1);
    	
    	Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			e.printStackTrace();
			channel.sendMessage(language.get("verification_error")).queue();
			return;
		}
    	
    	if (api.isEmpty()) {
    		channel.sendMessage(language.get("verification_not_used")).queue();
    		return;
    	}
    	
    	try {
    		api.get().verifyDiscord(verify, guildId);
    	} catch (final ApiError e) {
    		if (e.getError() == ApiError.INVALID_VALIDATE_CODE) {
    			channel.sendMessage(language.get("verification_token_invalid")).queue();
        		return;
    		} else {
    			e.printStackTrace();
    			channel.sendMessage(language.get("verification_error")).queue();
        		return;
    		}
    	} catch (final NamelessException e) {
    		System.out.println("NOT AN ERROR");
    		e.printStackTrace();
    		channel.sendMessage(language.get("verification_error")).queue();
    		return;
    	}
    }
}
