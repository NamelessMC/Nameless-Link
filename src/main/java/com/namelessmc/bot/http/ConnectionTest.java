package com.namelessmc.bot.http;

import com.google.common.base.Preconditions;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.IOException;

public class ConnectionTest extends HttpHandler {

	@Override
	public void service(Request request, Response response) throws IOException {
		Preconditions.checkArgument(request.getMethod() == Method.GET, "Only GET requests allowed");
		response.setContentType("text/plain");
		response.getWriter().write("success");
	}

}
