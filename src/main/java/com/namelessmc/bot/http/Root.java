package com.namelessmc.bot.http;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import org.apache.commons.lang3.Validate;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.IOException;
import java.util.stream.IntStream;

public class Root extends HttpHandler {

	@Override
	public void service(Request request, Response response) throws IOException {
		Validate.isTrue(request.getMethod() == Method.GET, "Only GET requests allowed");

		response.setContentType("text/html");
		response.getWriter().write("<p>This is a <a href=\"https://github.com/NamelessMC/Nameless-Link\">Nameless-Link</a> instance.</p>");
		response.getWriter().write("<p>If you run this bot, congratulations! It is working properly. If you are a regular person, there's nothing you can do here. Continue your day with a sense of pride you saw this page.</p>");
		try {
			long guildCount = IntStream.range(0, Main.getShardCount())
					.mapToObj(Main::getJda)
					.map(JDA::getGuildCache)
					.mapToLong(SnowflakeCacheView::size)
					.sum();
			final long connectionCount = Main.getConnectionManager().countConnections();
			response.getWriter().write("<p>This bot is in " + guildCount + " guilds, connected to " + connectionCount + " websites.");
		} catch (final BackendStorageException e) {
			e.printStackTrace();
		}

	}

}
