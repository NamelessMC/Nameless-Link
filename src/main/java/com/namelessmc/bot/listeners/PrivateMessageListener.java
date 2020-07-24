package com.namelessmc.bot.listeners;

import com.google.gson.JsonObject;
import com.namelessmc.NamelessAPI.NamelessException;
import com.namelessmc.NamelessAPI.ParameterBuilder;
import com.namelessmc.NamelessAPI.Request;
import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Utils;
import com.namelessmc.bot.models.PendingVerification;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class PrivateMessageListener extends ListenerAdapter {

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        EmbedBuilder embedBuilder = new EmbedBuilder();

        PendingVerification pendingVerification = Queries.getPendingVerification(event.getAuthor().getId());
        if (pendingVerification != null) {
            String username = pendingVerification.username;
            String url = pendingVerification.site;
            if (message.equals(username)) {
                embedBuilder.setColor(Color.ORANGE).setTitle("Verification").addField("Loading", "Please wait while I verify your web account...", false);
                Utils.messageUser(event.getAuthor(), embedBuilder);
                try {
                    String[] parameters = new ParameterBuilder().add("username", username).add("discord_id", event.getAuthor().getId()).build();
                    Request request = new Request(new URL(url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.SET_DISCORD_ID, parameters);
                    request.connect();
                    JsonObject response = request.getResponse();
                    if (response.has("code")) {
                        embedBuilder.clear().setColor(Color.GREEN).setTitle("Verification").addField("Failed", "Error: `Could not link your account. Please ensure your username has not changed. Stacktrace: " + response.getAsString() + "`", false);
                        Utils.messageUser(event.getAuthor(), embedBuilder);
                        Main.log("Failed to complete account link for " + event.getAuthor().getName() + " under username " + username + ". NamelessMC error. ");
                        return;
                    }
                    if (Queries.removePendingVerification(event.getAuthor().getId())) {
                        embedBuilder.clear().setColor(Color.GREEN).setTitle("Verification").addField("Success!", "Thank you for linking your account.", false);
                        Main.log("Processed account link for " + event.getAuthor().getName() + " under username " + username);
                    } else {
                        embedBuilder.clear().setColor(Color.RED).setTitle("Verification").addField("Failed", "Error: `Unable to remove your pending verification from the database. Please contact aberdeener#0001`", false);
                        Utils.messageUser(Main.getJda().getUserById("271510274475819008"), "[ERROR] Pending verification for " + event.getAuthor().getId() + " could not be deleted. Please check logs ASAP.");
                        Main.log("Failed to complete account link for " + event.getAuthor().getName() + " under username " + username + ". Could not remove from DB");
                    }
                    Utils.messageUser(event.getAuthor(), embedBuilder);
                } catch (NamelessException | MalformedURLException exception) {
                    embedBuilder.clear().setColor(Color.RED).setTitle("Verification").addField("Failed", "Error: `" + exception + "`", false);
                    Utils.messageUser(event.getAuthor(), embedBuilder);
                }
            }
        } else {
            List<String> guild_ids = Queries.getUserGuilds(event.getAuthor().getId());
            String[] args = message.split(" ");
            String guild_id;
            if (guild_ids == null) {
                embedBuilder.clear().setColor(Color.RED).setTitle("Link Guild").addField("Failed", "Error: `You are not the owner of a Guild with the Nameless Link bot.`", false);
                Utils.messageUser(event.getAuthor(), embedBuilder);
                return;
            } else if (guild_ids.size() > 1) {
                if (args.length > 1) {
                    guild_id = args[1].trim();
                    if (!guild_ids.contains(guild_id)) {
                        embedBuilder.clear().setColor(Color.RED).setTitle("Link Guild").addField("Failed", "Error: `Invalid Guild ID.\nAvailable Guilds:\n" + Utils.listToString(guild_ids, "\n") + "`", false);
                        Utils.messageUser(event.getAuthor(), embedBuilder);
                        return;
                    }
                } else {
                    embedBuilder.clear().setColor(Color.RED).setTitle("Link Guild").addField("Failed", "Error: `You are the owner of multiple Guild with the Nameless Link bot. Please specify a Guild ID as the second argument.\nGuilds:\n" + Utils.listToString(guild_ids, "\n") + "`", false);
                    Utils.messageUser(event.getAuthor(), embedBuilder);
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
                    embedBuilder.clear().setColor(Color.GREEN).setTitle("Link Guild").addField("Success", "The API URL and Guild ID you sent were valid. Saving now...", false);
                    Utils.messageUser(event.getAuthor(), embedBuilder);
                    if (Queries.setGuildApiUrl(guild_id, api_url)) {
                        String guild_name = Main.getJda().getGuildById(guild_id).getName();
                        embedBuilder.clear().setColor(Color.GREEN).setTitle("Link Guild").addField("Success", "Your API URL has been linked to \"" + guild_name + "\".", false);
                        Utils.messageUser(event.getAuthor(), embedBuilder);
                        Main.log("Processed guild API link for " + guild_name + " on behalf of " + event.getAuthor().getName());
                    } else {
                        embedBuilder.clear().setColor(Color.RED).setTitle("Link Guild").addField("Failed", "Failed: `Your API URL could not be saved. Please contact aberdeener#0001`", false);
                        Utils.messageUser(Main.getJda().getUserById("271510274475819008"), "[ERROR] API URL for " + guild_id + " could not be saved. Please check logs ASAP.");
                        Main.log("[ERROR] API URL for " + guild_id + " could not be saved.");
                        Utils.messageUser(event.getAuthor(), embedBuilder);
                    }
                } else {
                    embedBuilder.clear().setColor(Color.RED).setTitle("Link Guild").addField("Failed", "Stacktrace: `" + response.getAsString() + "`", false);
                    Utils.messageUser(event.getAuthor(), embedBuilder);
                }
            } catch (NamelessException | MalformedURLException exception) {
                embedBuilder.clear().setColor(Color.RED).setTitle("Link Guild").addField("Failed", "Stacktrace: `" + exception + "`", false);
                Utils.messageUser(event.getAuthor(), embedBuilder);
            }
        }
    }
}
