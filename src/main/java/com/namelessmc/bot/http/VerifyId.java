package com.namelessmc.bot.http;

import com.namelessmc.bot.Data;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.http.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VerifyId implements HttpHandler {

    @Getter
    private static final HashMap<Long, List<String>> toVerify = new HashMap<>();

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

        if (Data.addPendingVerification(id, username, site)) {
            Main.getLogger().info("Added username " + username + " with ID " + id + " to pend for confirmation");
            htmlResponse = "success";
        } else {
            Main.getLogger().severe("Failed to add username " + username + " with ID " + id + " to pend for confirmation");
            htmlResponse = "failure";
        }

        httpExchange.sendResponseHeaders(200, htmlResponse.length());
        outputStream.write(htmlResponse.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
