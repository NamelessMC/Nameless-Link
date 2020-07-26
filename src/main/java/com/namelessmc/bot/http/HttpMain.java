package com.namelessmc.bot.http;

import com.namelessmc.bot.Main;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HttpMain {

    // TODO some sort of ip limitation / rate limit
    public static void init() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
        server.createContext("/roleChange", new IncomingRoleChange());
        server.createContext("/verifyId", new VerifyId());
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.setExecutor(threadPoolExecutor);
        server.start();
        Main.log("HTTP Server started on port 8001");
    }
}
