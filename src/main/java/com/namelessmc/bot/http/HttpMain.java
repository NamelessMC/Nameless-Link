package com.namelessmc.bot.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.namelessmc.bot.Config;
import com.namelessmc.bot.Main;
import com.sun.net.httpserver.HttpServer;

public class HttpMain {

    public static void init() {
        HttpServer server;
        int port = 8001;
        try {
            port = Config.PORT;
        } catch (final NumberFormatException e) {
            Main.log("[ERROR] Invalid port. Using fallback: " + port);
        }
        try {
            server = HttpServer.create(new InetSocketAddress(port), 25);
            server.createContext("/", new ConnectionTest());
            server.createContext("/roleChange", new IncomingRoleChange());
            server.createContext("/verifyId", new VerifyId());
            final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            server.setExecutor(threadPoolExecutor);
            server.start();
            Main.log("HTTP Server started on port " + port);
        } catch (final IOException e) {
            e.printStackTrace();
            Main.log("[ERROR] HTTP Server could not start!");
            Main.getJda().shutdown();
        }
    }
}
