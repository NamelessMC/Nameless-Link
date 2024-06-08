package com.namelessmc.bot.http;

import java.io.IOException;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;

import com.namelessmc.bot.Main;

public class HttpMain {

	public static void init() throws IOException {
		final HttpServer server = new HttpServer();
		final NetworkListener listener = new NetworkListener("Listener", Main.getWebserverInterface(), Main.getWebserverPort());
		server.addListener(listener);
		final ServerConfiguration config = server.getServerConfiguration();
		config.addHttpHandler(new ApplyRoleChanges(), "/applyRoleChanges");
		config.addHttpHandler(new ConnectionTest(), "/status");
		config.addHttpHandler(new RoleChange(), "/roleChange");
		config.addHttpHandler(new SendDirectMessage(), "/sendDirectMessage");
		config.addHttpHandler(new Root());
		server.start();
	}
}
