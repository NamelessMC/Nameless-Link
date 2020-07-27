package com.namelessmc.bot.listeners;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Utils;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildJoinHandler extends ListenerAdapter {

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Main.log("Joined guild: " + event.getGuild().getName());

        String owner_id = event.getGuild().retrieveOwner().complete().getId();

        Language language = Queries.getUserLanguage(owner_id);
        if (language == null) language = new Language("EnglishUK");

        // If we dont have an API url for this guild, DM the owner
        String api_url = Queries.getGuildApiUrl(event.getGuild().getId());
        if (api_url == null) {
            if (Queries.newGuild(event.getGuild().getId(), owner_id)) {
                Utils.messageGuildOwner(event.getGuild().getId(), language.get("guild_join_success"));
                Main.log("Sent new join message to " + event.getGuild().retrieveOwner().complete().getEffectiveName() + " for guild " + event.getGuild().getName());
            } else {
                Utils.messageGuildOwner(event.getGuild().getId(), language.get("guild_join_failed_db"));
                Main.log("Could not set new guild " + event.getGuild().getId());
            }
        }
        // Else DM the owner that we do
        else {
            if (Utils.getApiFromString(api_url) == null) {
                // Error with their stored url. Make them update the url
                Utils.messageGuildOwner(event.getGuild().getId(), language.get("guild_join_needs_renew"));
                Main.debug("Sent update api url message to " + event.getGuild().retrieveOwner().complete().getEffectiveName() + " for guild " + event.getGuild().getName());
            } else {
                // Good to go
                Utils.messageGuildOwner(event.getGuild().getId(), language.get("guild_join_welcome_back"));
                Main.debug("Sent already complete message to " + event.getGuild().retrieveOwner().complete().getEffectiveName() + " for guild " + event.getGuild().getName());
            }
        }
    }

}
