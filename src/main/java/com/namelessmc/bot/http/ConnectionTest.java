package com.namelessmc.bot.http;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.namelessmc.bot.Main;

public class ConnectionTest extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		Main.debug("Connection test successful.");
		response.setContentType("text/plain");
		try (Writer writer = response.getWriter()) {
			response.getWriter().print("success");
		}
	}

}
