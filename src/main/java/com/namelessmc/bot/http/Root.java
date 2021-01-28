package com.namelessmc.bot.http;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Root  extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		response.getWriter().println("<p>This is a <a href=\"https://github.com/NamelessMC/Nameless-Link\">Nameless-Link</a> instance.</p>");
		response.getWriter().println("<p>If you run this bot, congratulations! It is working properly. If you are a regular person, there's nothing you can do here. Continue your day with a sense of pride you saw this page.</p>");
	}

}
