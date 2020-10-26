package com.namelessmc.bot.commands;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;

import com.namelessmc.bot.ConnectionManager.WebsiteConnection;
import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class URLCommand extends Command {

    public URLCommand() {
        super("!url", Collections.singletonList("!apiurl"), CommandContext.PRIVATE_MESSAGE);
    }

    @Override
    public void execute(final User user, final String[] args, final MessageChannel channel) {
    	if (args.length != 2) {
    		System.out.println("Invalid usage, use !url <guild id> <api url>"); // TODO Message
    		return;
    	}
    	
    	final Language language = new Language();
    	
    	final long guildId = Long.parseLong(args[0]); // TODO Catch exception
    	URL apiUrl;
		try {
			apiUrl = new URL(args[1]);
		} catch (final MalformedURLException e) {
			channel.sendMessage("The provided URL is malformed"); // TODO Message
    		return;
		}
    	
    	final Guild guild = Main.getJda().getGuildById(guildId);
    	
    	if (guild == null) {
    		channel.sendMessage("The provided guild ID is invalid"); // TODO Message
    		return;
    	}
    	
    	
    	if (guild.getOwnerIdLong() != user.getIdLong()) {
    		channel.sendMessage("You must be the owner of the discord server to change URL"); // TODO Message
    		return;
    	}
    	
    	// Check if API URL works
    	try {
    		new NamelessAPI(apiUrl).checkWebAPIConnection();
    	} catch (final NamelessException e) {
    		channel.sendMessage("The provided API URL is invalid or we are blocked by a proxy (e.g. cloudflare)"); // TODO Message
    		return;
    	}
    	
    	final Optional<WebsiteConnection> connection = Main.getConnectionManager().getConnection(guildId);
    	
    	if (connection.isEmpty()) {
    		// User is setting up new connection
    		Main.getConnectionManager().createNewConnection(apiUrl, guildId);
    		channel.sendMessage("Successfully changed API URL"); // TODO Message
    	} else {
    		// User is modifying API url for existing connection
    		connection.get().setApiUrl(apiUrl);
    		channel.sendMessage("Successfully set up API URL"); // TODO Message
    	}
    }
}
