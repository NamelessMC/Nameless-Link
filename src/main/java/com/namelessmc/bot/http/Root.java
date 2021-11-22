package com.namelessmc.bot.http;

import com.google.common.base.Preconditions;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.stream.IntStream;

public class Root extends HttpHandler {

	public static int pingSuccessCount = -1;
	public static int pingFailCount = -1;

	@Override
	public void service(Request request, Response response) throws IOException {
		Preconditions.checkArgument(request.getMethod() == Method.GET, "Only GET requests allowed");

		response.setContentType("text/html");
		response.getWriter().write("<p>This is a <a href=\"https://github.com/NamelessMC/Nameless-Link\">Nameless-Link</a> instance.</p>");

		if (System.getenv("OFFICIAL_BOT") == null) {
			response.getWriter().write("<p>If you run this bot, congratulations! It is working properly. If you are a regular person, there's nothing you can do here. Continue your day with a sense of pride you saw this page.</p>");
		} else {
			try {
				response.getWriter().write("<p>");
				long guildCount = IntStream.range(0, Main.getShardCount())
						.mapToObj(Main::getJda)
						.map(JDA::getGuildCache)
						.mapToLong(SnowflakeCacheView::size)
						.sum();
				final long connectionCount = Main.getConnectionManager().countConnections();
				response.getWriter().write("This bot is in " + guildCount + " guilds, connected to " + connectionCount + " websites. ");
				response.getWriter().write("Last startup, " + pingSuccessCount + " connections were working and " + pingFailCount + " were not. ");
				response.getWriter().write("Started at " + new Date(ManagementFactory.getRuntimeMXBean().getStartTime()) + ". ");
				response.getWriter().write("</p>");
			} catch (final BackendStorageException e) {
				e.printStackTrace();
			}
		}
	}

}
