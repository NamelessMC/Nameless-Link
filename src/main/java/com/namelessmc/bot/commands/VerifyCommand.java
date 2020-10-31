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
    	
    	if (args.length != 1) {
    		channel.sendMessage("Usage: !verify <token>"); // TODO Language
    		return;
    	}
    	
    	final String token = args[0];
    	final long guildId;
    	try {
    		guildId = Long.parseLong(token.substring(0, token.indexOf('.') - 1)); // TODO Different sep character?
    	} catch (final NumberFormatException e) {
    		channel.sendMessage("Invalid token");// TODO Language
    		return;
    	}
    	final String verify = token.substring(token.indexOf('.') + 1); // TODO Different sep character?
    	
    	Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			e.printStackTrace(); // TODO handle
			return;
		}
    	
    	if (api.isEmpty()) {
    		channel.sendMessage("This bot is no longer (or never was) used by this server."); // TODO Language
    		return;
    	}
    	
    	try {
    		api.get().verifyDiscord(verify, guildId);
    	} catch (final ApiError e) {
    		if (e.getError() == ApiError.INVALID_VALIDATE_CODE) {
    			channel.sendMessage("Invalid validation code"); // TODO Language
        		return;
    		} else {
    			e.printStackTrace();
        		channel.sendMessage("An unknown error occurred"); // TODO Language
        		return;
    		}
    	} catch (final NamelessException e) {
    		e.printStackTrace();
    		channel.sendMessage("An unknown error occurred"); // TODO Language
    		return;
    	}
    }
}
