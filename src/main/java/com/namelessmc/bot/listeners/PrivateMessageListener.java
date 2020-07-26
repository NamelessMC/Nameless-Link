package com.namelessmc.bot.listeners;

import com.google.gson.JsonObject;
import com.namelessmc.NamelessAPI.NamelessException;
import com.namelessmc.NamelessAPI.ParameterBuilder;
import com.namelessmc.NamelessAPI.Request;
import com.namelessmc.bot.Language;
import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Utils;
import com.namelessmc.bot.models.PendingVerification;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class PrivateMessageListener extends ListenerAdapter {

    private static final EmbedBuilder embedBuilder = new EmbedBuilder();

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        User user = event.getAuthor();

        if (user.isBot()) return;

        Language language = Queries.getUserLanguage(user.getId());
        if (language == null) language = new Language("EnglishUK");

        String message = event.getMessage().getContentRaw();
        String[] args = message.split(" ");

        if (args[0].equals("!language")) {
            if (args.length == 3) {
                if (args[1].equals("set")) {
                    if (Language.isValid(args[2])) {
                        if (Queries.setUserLanguage(user.getId(), args[2])) {
                            language = new Language(args[2]);
                            embedBuilder.clear().setColor(Color.GREEN).setTitle(language.get("language_title")).addField(language.get("success"), language.get("language_update_success", args[2]), false);
                            Main.log("Updated " + user.getName() + "'s language to: " + args[2]);
                        } else {
                            embedBuilder.clear().setColor(Color.RED).setTitle(language.get("language_title")).addField(language.get("failed"), language.get("language_update_failed_db"), false);
                            Main.log("[ERROR] Failed to update language for " + user.getName() + " to: " + args[2] + ". Could not remove from DB");
                        }
                    } else {
                        embedBuilder.clear().setColor(Color.RED).setTitle(language.get("language_title")).addField(language.get("failed"), language.get("language_update_failed_invalid", args[2], Utils.listToString(Language.getLanguages(), ", ")), false);
                        Main.debug(user.getName() + " entered invalid language (" + args[2] + ")");
                    }
                } else {
                    embedBuilder.clear().setColor(Color.RED).setTitle(language.get("language_title")).addField(language.get("failed"), language.get("language_invalid_argument", args[1]), false);
                    Main.debug(user.getName() + " entered invalid argument (" + args[1] + ")");
                }
            } else if (args.length == 2) {
                if (args[1].equals("set") || args[1].equals("list")) {
                    embedBuilder.clear().setColor(Color.ORANGE).setTitle(language.get("language_title")).addField(language.get("settings"), language.get("language_list", Utils.listToString(Language.getLanguages(), ", ")), false);
                } else {
                    embedBuilder.clear().setColor(Color.RED).setTitle(language.get("language_title")).addField(language.get("failed"), language.get("language_invalid_argument", args[1]), false);
                    Main.debug(user.getName() + " entered invalid argument (" + args[1] + ")");
                }
            } else {
                Language userLanguage = Queries.getUserLanguage(user.getId());
                if (Queries.getUserLanguage(user.getId()) != null) {
                    embedBuilder.clear().setColor(Color.GREEN).setTitle(language.get("language_title")).addField(language.get("settings"), language.get("language_get", userLanguage.getLanguage()), false);
                } else {
                    embedBuilder.clear().setColor(Color.RED).setTitle(language.get("language_title")).addField(language.get("failed"), language.get("language_get_failed_db"), false);
                    Main.log("[ERROR] Failed to get language for " + user.getName() + ". Could not find from DB");
                }
            }
            Utils.messageUser(user, embedBuilder);
            return;
        }

        PendingVerification pendingVerification = Queries.getPendingVerification(user.getId());
        if (pendingVerification != null) {
            String username = pendingVerification.getUsername();
            String guild_id = pendingVerification.getGuild_id();
            Guild guild = Main.getJda().getGuildById(guild_id);
            String role_id = pendingVerification.getRole();
            Role role = null;
            if (role_id != null) role = guild.getRoleById(role_id);
            String url = pendingVerification.getSite();
            if (message.equals(username)) {
                embedBuilder.setColor(Color.ORANGE).setTitle(language.get("verification_title")).addField(language.get("verification_loading"), language.get("verification_loading_message"), false);
                Utils.messageUser(user, embedBuilder);
                try {
                    String[] parameters = new ParameterBuilder().add("username", username).add("discord_id", user.getId()).build();
                    Request request = new Request(new URL(url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.SET_DISCORD_ID, parameters);
                    request.connect();
                    JsonObject response = request.getResponse();
                    if (response.has("code")) {
                        embedBuilder.clear().setColor(Color.GREEN).setTitle(language.get("verification_title")).addField(language.get("failed"), language.get("verification_failed_generic", response.getAsString()), false);
                        Utils.messageUser(user, embedBuilder);
                        Main.log("Failed to complete account link for " + user.getName() + " under username " + username + ". NamelessMC error. ");
                    } else if (Queries.removePendingVerification(user.getId())) {
                        if (role != null) {
                            guild.addRoleToMember(user.getId(), role).complete();
                            Main.log("Added role " + role.getName() + " to " + user.getName() + " upon account validation.");
                        }
                        embedBuilder.clear().setColor(Color.GREEN).setTitle(language.get("verification_title")).addField(language.get("success"), language.get("verification_success"), false);
                        Main.log("Processed account link for " + user.getName() + " under username " + username);
                    } else {
                        embedBuilder.clear().setColor(Color.RED).setTitle(language.get("verification_title")).addField(language.get("failed"), language.get("verification_failed_db"), false);
                        Main.log("[ERROR] Failed to complete account link for " + user.getName() + " under username " + username + ". Could not remove from DB");
                    }
                    Utils.messageUser(user, embedBuilder);
                } catch (NamelessException | MalformedURLException exception) {
                    embedBuilder.clear().setColor(Color.RED).setTitle(language.get("verification_title")).addField(language.get("verification_failed"),  language.get("verification_failed_generic", exception), false);
                    Utils.messageUser(user, embedBuilder);
                }
            }
        } else {
            List<String> guild_ids = Queries.getUserGuilds(user.getId());
            String guild_id;
            if (guild_ids == null) {
                embedBuilder.clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_no_guilds"), false);
                Utils.messageUser(user, embedBuilder);
                return;
            } else if (guild_ids.size() > 1) {
                if (args.length > 1) {
                    guild_id = args[1].trim();
                    if (!guild_ids.contains(guild_id)) {
                        embedBuilder.clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_invalid_id", Utils.listToString(guild_ids, "\n")), false);
                        Utils.messageUser(user, embedBuilder);
                        return;
                    }
                } else {
                    embedBuilder.clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_multiple", Utils.listToString(guild_ids, "\n")), false);
                    Utils.messageUser(user, embedBuilder);
                    return;
                }
            } else guild_id = guild_ids.get(0);

            try {
                String api_url = args[0];
                // TODO Replace all Requests with a new nmc api function i need to make
                Request request = new Request(new URL(api_url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.INFO);
                request.connect();
                JsonObject response = request.getResponse();
                if (response.has("nameless_version")) {
                    api_url = api_url.trim();
                    if (!api_url.endsWith("/")) api_url = api_url + "/";
                    embedBuilder.clear().setColor(Color.GREEN).setTitle(language.get("link_guild_title")).addField(language.get("success"), language.get("link_guild_saving"), false);
                    Utils.messageUser(user, embedBuilder);
                    if (Queries.setGuildApiUrl(guild_id, api_url)) {
                        String guild_name = Main.getJda().getGuildById(guild_id).getName();
                        embedBuilder.clear().setColor(Color.GREEN).setTitle(language.get("link_guild_title")).addField(language.get("success"), language.get("link_guild_success", guild_name), false);
                        Utils.messageUser(user, embedBuilder);
                        Main.log("Processed guild API link for " + guild_name + " on behalf of " + user.getName());
                    } else {
                        embedBuilder.clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_db"), false);
                        Main.log("[ERROR] API URL for " + guild_id + " could not be saved.");
                        Utils.messageUser(user, embedBuilder);
                    }
                } else {
                    embedBuilder.clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"),  language.get("link_guild_failed_generic", response.getAsString()), false);
                    Utils.messageUser(user, embedBuilder);
                }
            } catch (NamelessException | MalformedURLException exception) {
                embedBuilder.clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_generic", exception), false);
                Utils.messageUser(user, embedBuilder);
            }
        }
    }
}
