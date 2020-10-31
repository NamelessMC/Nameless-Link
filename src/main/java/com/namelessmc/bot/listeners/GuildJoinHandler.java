package com.namelessmc.bot.listeners;

import java.util.Optional;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Utils;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;

import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildJoinHandler extends ListenerAdapter {

    @Override
    public void onGuildJoin(final GuildJoinEvent event) {
        Main.log("Joined guild: " + event.getGuild().getName());

        Language language = Language.DEFAULT;
        
        Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApi(event.getGuild().getIdLong());
		} catch (final BackendStorageException e) {
			e.printStackTrace(); // TODO handle
			return;
		}
        
        if (api.isEmpty()) {
        	// DM owner that we don't have an api for this guild
			Utils.messageGuildOwner(event.getGuild().getId(), language.get("guild_join_success"));
			Main.log("Sent new join message to " + event.getGuild().retrieveOwner().complete().getEffectiveName()
					+ " for guild " + event.getGuild().getName());
        } else {
        	try {
        		api.get().checkWebAPIConnection();
        		// Good to go
        		language = Language.getDiscordUserLanguage(api.get(), event.getGuild().retrieveOwner().complete().getUser());
                Utils.messageGuildOwner(event.getGuild().getId(), language.get("guild_join_welcome_back"));
                Main.debug("Sent already complete message to " + event.getGuild().retrieveOwner().complete().getEffectiveName() + " for guild " + event.getGuild().getName());
        	} catch (final NamelessException e) {
        		// Error with their stored url. Make them update the url
                Utils.messageGuildOwner(event.getGuild().getId(), language.get("guild_join_needs_renew"));
                Main.debug("Sent update api url message to " + event.getGuild().retrieveOwner().complete().getEffectiveName() + " for guild " + event.getGuild().getName());
        	}
        }
    }

}
