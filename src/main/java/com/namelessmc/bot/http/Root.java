package com.namelessmc.bot.http;

import java.io.IOException;
import java.util.stream.IntStream;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;

public class Root extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		response.getWriter().println("<p>This is a <a href=\"https://github.com/NamelessMC/Nameless-Link\">Nameless-Link</a> instance.</p>");
		response.getWriter().println("<p>If you run this bot, congratulations! It is working properly. If you are a regular person, there's nothing you can do here. Continue your day with a sense of pride you saw this page.</p>");
		try {
			long guildCount = IntStream.range(0, Main.getShardCount())
					.mapToObj(Main::getJda)
					.map(JDA::getGuildCache)
					.mapToLong(SnowflakeCacheView::size)
					.sum();
			final long connectionCount = Main.getConnectionManager().countConnections();
			response.getWriter().println("<p>This bot is in " + guildCount + " guilds, connected to " + connectionCount + " websites.");
		} catch (final BackendStorageException e) {
			e.printStackTrace();
		}

	}

}
