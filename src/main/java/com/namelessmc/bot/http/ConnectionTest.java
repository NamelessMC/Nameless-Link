package com.namelessmc.bot.http;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.IOException;

public class ConnectionTest extends HttpHandler {

	@Override
	public void service(Request request, Response response) throws IOException {
		response.setContentType("text/plain");
		response.getWriter().write("success");
	}

}
