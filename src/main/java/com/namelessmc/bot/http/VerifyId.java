package com.namelessmc.bot.http;

import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Utils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;

public class VerifyId implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        handleResponse(httpExchange);
    }

    private void handleResponse(HttpExchange httpExchange) throws IOException {
        String requestUri = httpExchange.getRequestURI().toString();

        String id = HttpUtils.getParams(requestUri).get("id").toString();
        id = id.substring(1, id.length() - 1);

        String username = HttpUtils.getParams(requestUri).get("username").toString();
        username = username.substring(1, username.length() - 1);
        String site = requestUri.substring(requestUri.indexOf("&site=") + 6);

        OutputStream outputStream = httpExchange.getResponseBody();
        String htmlResponse;

        User user = Main.getJda().getUserById(id);
        if (user == null) {
            Main.debug("[ERROR] Invalid Discord ID while processing a web account (" + id + ")");
            htmlResponse = "failure-invalid-id";
        } else if(Queries.getPendingVerification(id) != null) {
            Main.log("[ERROR] User " + username + " with ID " + id + " already has a pending verification.");
            htmlResponse = "failure-already-pending";
        } else if (Queries.addPendingVerification(id, username, site)) {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.clear().setColor(Color.ORANGE).setTitle("Verification").addField("Pending", "Someone has request to link this Discord account with a NamelessMC web account. If this was you, please reply with the username you used to register on the website. If this was not you, you can safely ignore this message.", false);
            Utils.messageUser(user, embedBuilder);
            Main.log("Added username " + username + " with ID " + id + " to pend for confirmation");
            htmlResponse = "success";
        } else {
            Main.log("[ERROR] Failed to add username " + username + " with ID " + id + " to pend for confirmation.");
            // TODO on nmc check for this failure and let the user know
            Utils.messageUser(Main.getJda().getUserById("271510274475819008"), "[ERROR] Failed to add username " + username + " with ID " + id + " to pend for confirmation. Please check logs ASAP.");
            htmlResponse = "failure-unknown";
        }

        httpExchange.sendResponseHeaders(200, htmlResponse.length());
        outputStream.write(htmlResponse.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
