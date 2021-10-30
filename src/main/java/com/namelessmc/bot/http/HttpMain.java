package com.namelessmc.bot.http;

import com.namelessmc.bot.Main;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;

import java.io.IOException;

public class HttpMain {

	public static void init() throws IOException {
		HttpServer server = new HttpServer();
		NetworkListener listener = new NetworkListener("Listener", Main.getWebserverInterface(), Main.getWebserverPort());
		server.addListener(listener);
		ServerConfiguration config = server.getServerConfiguration();
		config.addHttpHandler(new ConnectionTest(), "/status");
		config.addHttpHandler(new RoleChange(), "/roleChange");
		config.addHttpHandler(new Root());
		server.start();
	}
}
