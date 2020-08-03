package com.namelessmc.bot.listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import com.namelessmc.NamelessAPI.NamelessException;
import com.namelessmc.NamelessAPI.ParameterBuilder;
import com.namelessmc.NamelessAPI.Request;
import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Utils;
import com.namelessmc.bot.commands.Command;
import com.namelessmc.bot.commands.CommandContext;
import com.namelessmc.bot.models.PendingVerification;
import lombok.SneakyThrows;
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

    @SneakyThrows
    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        User user = event.getAuthor();

        if (user.isBot()) return;
        String message = event.getMessage().getContentRaw();
        String[] args = message.split(" ");

        Command command = Command.getCommand(args[0], CommandContext.PRIVATE_MESSAGE);
        if (command != null) command.execute(user, args, event.getChannel());
        else {
            Language language = Queries.getUserLanguage(user.getId());
            if (language == null) language = new Language("EnglishUK");

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
                    Main.getEmbedBuilder().clear().setColor(Color.ORANGE).setTitle(language.get("verification_title")).addField(language.get("verification_loading"), language.get("verification_loading_message"), false);
                    Utils.messageUser(user, Main.getEmbedBuilder());
                    try {
                        String[] parameters = new ParameterBuilder().add("username", username).add("discord_id", user.getId()).build();
                        Request request = new Request(new URL(url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.SET_DISCORD_ID, parameters);
                        request.connect();
                        JsonObject response = request.getResponse();
                        if (response.has("code")) {
                            Main.getEmbedBuilder().clear().setColor(Color.GREEN).setTitle(language.get("verification_title")).addField(language.get("failed"), language.get("verification_failed_generic", response.getAsString()), false);
                            Utils.messageUser(user, Main.getEmbedBuilder());
                            Main.log("Failed to complete account link for " + user.getName() + " under username " + username + ". NamelessMC error. ");
                        } else if (Queries.removePendingVerification(user.getId())) {
                            if (role != null) {
                                guild.addRoleToMember(user.getId(), role).complete();
                                Main.log("Added role " + role.getName() + " to " + user.getName() + " upon account validation.");
                            }
                            Main.getEmbedBuilder().clear().setColor(Color.GREEN).setTitle(language.get("verification_title")).addField(language.get("success"), language.get("verification_success"), false);
                            Main.log("Processed account link for " + user.getName() + " under username " + username);
                        } else {
                            Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("verification_title")).addField(language.get("failed"), language.get("verification_failed_db"), false);
                            Main.log("[ERROR] Failed to complete account link for " + user.getName() + " under username " + username + ". Could not remove from DB");
                        }
                        Utils.messageUser(user, Main.getEmbedBuilder());
                    } catch (NamelessException | MalformedURLException exception) {
                        Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("verification_title")).addField(language.get("verification_failed"), language.get("verification_failed_generic", exception), false);
                        Utils.messageUser(user, Main.getEmbedBuilder());
                    }
                }
            } else {
                List<String> guild_ids = Queries.getUserGuilds(user.getId());
                String guild_id;
                if (guild_ids == null) {
                    Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("link_guild_title")).addField(language.get("failed"), language.get("link_guild_failed_no_guilds"), false);
                    Utils.messageUser(user, Main.getEmbedBuilder());
                    return;
                } else if (guild_ids.size() > 1) {
                    if (args.length > 1) {
                        guild_id = args[1].trim();
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
                    String api_url = args[0];
                    // TODO Replace all Requests with a new nmc api function i need to make
                    Request request = new Request(new URL(api_url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.INFO);
                    request.connect();
                    JsonObject response = request.getResponse();
                    if (response.has("nameless_version")) {
                        api_url = api_url.trim();
                        if (!api_url.endsWith("/")) api_url = api_url + "/";
                        Main.getEmbedBuilder().clear().setColor(Color.GREEN).setTitle(language.get("link_guild_title")).addField(language.get("success"), language.get("link_guild_saving"), false);
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
    }
}
