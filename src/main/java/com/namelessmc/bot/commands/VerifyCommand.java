package com.namelessmc.bot.commands;

import com.google.gson.JsonObject;
import com.namelessmc.NamelessAPI.NamelessException;
import com.namelessmc.NamelessAPI.ParameterBuilder;
import com.namelessmc.NamelessAPI.Request;
import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Utils;
import com.namelessmc.bot.models.PendingVerification;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class VerifyCommand extends Command {

    public VerifyCommand() {
        super("!verify", Arrays.asList("!validate", "!link"), CommandContext.PRIVATE_MESSAGE);
    }

    @Override
    public void execute(User user, String[] args, MessageChannel channel) {
        Language language = Queries.getUserLanguage(user.getId());
        if (language == null) language = new Language("EnglishUK");

        String message;
        try {
            message = args[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            // They did not provide a token
            Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("verification_title")).addField(language.get("failed"), language.get("verification_failed_no_token"), false);
            Utils.messageUser(user, Main.getEmbedBuilder());
            return;
        }
        PendingVerification pendingVerification = Queries.getPendingVerification(user.getId());
        // If they have a pending verification proceed
        if (pendingVerification != null) {
            String token = pendingVerification.getToken();
            String guild_id = pendingVerification.getGuild_id();
            Guild guild = Main.getJda().getGuildById(guild_id);
            String role_id = pendingVerification.getRole();
            Role role = null;
            if (role_id != null) role = guild.getRoleById(role_id);
            String url = pendingVerification.getSite();
            // Ensure their message matches their pending verification
            if (message.equals(token)) {
                Main.getEmbedBuilder().clear().setColor(Color.ORANGE).setTitle(language.get("verification_title")).addField(language.get("verification_loading"), language.get("verification_loading_message"), false);
                Utils.messageUser(user, Main.getEmbedBuilder());
                try {
                    String[] parameters = new ParameterBuilder().add("token", token).add("discord_id", user.getId()).build();
                    Request request = new Request(new URL(url), "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)", Request.Action.SET_DISCORD_ID, parameters);
                    request.connect();
                    JsonObject response = request.getResponse();
                    if (response.has("code")) {
                        // NamelessMC sent an error back
                        Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("verification_title")).addField(language.get("failed"), language.get("verification_failed_generic", response.toString()), false);
                        Utils.messageUser(user, Main.getEmbedBuilder());
                        Main.log("Failed to complete account link for " + user.getName() + " under username " + token + ". NamelessMC error. ");
                    } else if (Queries.removePendingVerification(user.getId())) {
                        // Success, now apply their website role
                        if (role != null) {
                            guild.addRoleToMember(user.getId(), role).complete();
                            Main.log("Added role " + role.getName() + " to " + user.getName() + " upon account validation.");
                        }
                        Main.getEmbedBuilder().clear().setColor(Color.GREEN).setTitle(language.get("verification_title")).addField(language.get("success"), language.get("verification_success"), false);
                        Main.log("Processed account link for " + user.getName() + " with token " + token);
                        Utils.messageUser(user, Main.getEmbedBuilder());
                    } else {
                        // Database error
                        Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("verification_title")).addField(language.get("failed"), language.get("verification_failed_db"), false);
                        Main.log("[ERROR] Failed to complete account link for " + user.getName() + " with token " + token + ". Could not remove from DB");
                        Utils.messageUser(user, Main.getEmbedBuilder());
                    }
                } catch (NamelessException | MalformedURLException exception) {
                    Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("verification_title")).addField(language.get("verification_failed"), language.get("verification_failed_generic", exception), false);
                    Utils.messageUser(user, Main.getEmbedBuilder());
                }
            } else {
                Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("verification_title")).addField(language.get("verification_failed"), language.get("verification_failed_wrong_token"), false);
                Utils.messageUser(user, Main.getEmbedBuilder());
            }
        } else {
            // If they do not have a pending verification die
            Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("verification_title")).addField(language.get("failed"), language.get("verification_failed_none_pending"), false);
            Utils.messageUser(user, Main.getEmbedBuilder());
        }
    }
}
