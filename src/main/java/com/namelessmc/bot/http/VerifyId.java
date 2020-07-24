package com.namelessmc.bot.http;

import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Utils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class VerifyId implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        handleResponse(httpExchange);
    }

    private void handleResponse(HttpExchange httpExchange) throws IOException {
        String requestUri = httpExchange.getRequestURI().toString();

        Map<String, List<String>> params = HttpUtils.getParams(requestUri);

        String id = params.get("id").toString();
        id = id.substring(1, id.length() - 1);
        User user = Main.getJda().getUserById(id);
        String username = params.get("username").toString();
        username = username.substring(1, username.length() - 1);
        String guild_id = params.get("guild_id").toString();
        guild_id = guild_id.substring(1, guild_id.length() - 1);
        Guild guild = Main.getJda().getGuildById(guild_id);
        String role = null;
        try {
            role = params.get("role").toString();
            role = role.substring(1, role.length() - 1);
        } catch (NullPointerException ignored) {}
        String site = requestUri.substring(requestUri.indexOf("&site=") + 6);

        OutputStream outputStream = httpExchange.getResponseBody();
        String htmlResponse;

        if (user == null) {
            Main.debug("[ERROR] Invalid Discord ID while processing a web account (" + id + ")");
            htmlResponse = "failure-invalid-id";
        } else if (guild == null) {
            Main.debug("[ERROR] Invalid Guild ID while processing a web account (" + guild_id + ")");
            htmlResponse = "failure-invalid-guild-id";
        } else if(Queries.getPendingVerification(id) != null) {
            Main.log("[ERROR] User " + username + " with ID " + id + " already has a pending verification.");
            htmlResponse = "failure-already-pending";
        } else if (Queries.addPendingVerification(id, username, guild_id, role, site)) {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.clear().setColor(Color.ORANGE).setTitle("Verification").addField("Pending", "Someone has request to link this Discord account with a NamelessMC web account. If this was you, please reply with the username you used to register on the website. If this was not you, you can safely ignore this message.", false);
            Utils.messageUser(user, embedBuilder);
            Main.log("Added username " + username + " with ID " + id + " to pend for confirmation");
            htmlResponse = "success";
        } else {
            Main.log("[ERROR] Failed to add username " + username + " with ID " + id + " to pend for confirmation.");
            Utils.messageUser(Main.getJda().getUserById("271510274475819008"), "[ERROR] Failed to add username " + username + " with ID " + id + " to pend for confirmation. Please check logs ASAP.");
            htmlResponse = "failure-database";
        }

        httpExchange.sendResponseHeaders(200, htmlResponse.length());
        outputStream.write(htmlResponse.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
