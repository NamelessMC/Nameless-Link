package com.namelessmc.bot.http;

import java.io.IOException;
import java.io.Writer;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class ConnectionTest extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		response.setContentType("text/plain");
		try (Writer writer = response.getWriter()) {
			response.getWriter().print("success");
		}
	}

}
