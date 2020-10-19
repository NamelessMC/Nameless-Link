package com.namelessmc.bot.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.namelessmc.NamelessAPI.NamelessException;
import com.namelessmc.NamelessAPI.Request;
import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Utils;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class URLCommand extends Command {

    public URLCommand() {
        super("!url", Collections.singletonList("!apiurl"), CommandContext.PRIVATE_MESSAGE);
    }

    @Override
    public void execute(User user, String[] args, MessageChannel channel) {
        Language language = Queries.getUserLanguage(user.getId());
        if (language == null) language = new Language("EnglishUK");

        List<String> guild_ids = Queries.getUserGuilds(user.getId());
        String guild_id;
        if (guild_ids == null) {
            Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_no_guilds"), false);
            Utils.messageUser(user, Main.getEmbedBuilder());
            return;
        } else if (guild_ids.size() > 1) {
            if (args.length > 1) {
                try {
                    guild_id = args[2].trim();
                } catch (ArrayIndexOutOfBoundsException e) {
                    Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_multiple", Utils.listToString(guild_ids, "\n")), false);
                    Utils.messageUser(user, Main.getEmbedBuilder());
                    return;
                }
                if (!guild_ids.contains(guild_id)) {
                    Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_invalid_id", Utils.listToString(guild_ids, "\n")), false);
                    Utils.messageUser(user, Main.getEmbedBuilder());
                    return;
                }
            } else {
                Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_multiple", Utils.listToString(guild_ids, "\n")), false);
                Utils.messageUser(user, Main.getEmbedBuilder());
                return;
            }
        } else guild_id = guild_ids.get(0);

        try {
            String api_url;
            try {
                api_url = args[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_no_url"), false);
                Utils.messageUser(user, Main.getEmbedBuilder());
                return;
            }
            Request request = new Request(new URL(api_url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.INFO);
            request.connect();
            JsonObject response = request.getResponse();
            if (response.has("nameless_version")) {
                api_url = api_url.trim();
                if (!api_url.endsWith("/")) api_url = api_url + "/";
                Main.getEmbedBuilder().clear().setColor(Color.ORANGE).setTitle(language.get("link_guild_title")).addField(language.get("success"), language.get("link_guild_saving"), false);
                Utils.messageUser(user, Main.getEmbedBuilder());
                if (Queries.setGuildApiUrl(guild_id, api_url)) {
                    String guild_name = Main.getJda().getGuildById(guild_id).getName();
                    Main.getEmbedBuilder().clear().setColor(Color.GREEN).setTitle(language.get("link_guild_title")).addField(language.get("success"), language.get("link_guild_success", guild_name), false);
                    Utils.messageUser(user, Main.getEmbedBuilder());
                    Main.log("Processed guild API link for " + guild_name + " on behalf of " + user.getName());
                } else {
                    Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_db"), false);
                    Main.log("[ERROR] API URL for " + guild_id + " could not be saved.");
                    Utils.messageUser(user, Main.getEmbedBuilder());
                }
            } else {
                Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_generic", response.getAsString()), false);
                Utils.messageUser(user, Main.getEmbedBuilder());
            }
        } catch (NamelessException | MalformedURLException exception) {
            Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_generic", exception), false);
            Utils.messageUser(user, Main.getEmbedBuilder());
        } catch (JsonSyntaxException exception) {
            Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_api_disabled", exception), false);
            Utils.messageUser(user, Main.getEmbedBuilder());
        }
    }
}
