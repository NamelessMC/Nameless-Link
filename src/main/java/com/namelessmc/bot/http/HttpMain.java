package com.namelessmc.bot.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.namelessmc.bot.Main;

public class HttpMain {

	public static void init() {
		final Server server = new Server();
		final ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		handler.addServlet(ConnectionTest.class, "/status");
		handler.addServlet(RoleChange.class, "/roleChange");
		handler.addServlet(Root.class, "/");
		server.setHandler(handler);

		final ServerConnector connector = new ServerConnector(server);
		connector.setPort(Main.getWebserverPort());
		connector.setHost(Main.getWebserverInterface());

		server.addConnector(connector);

		new Thread() {
			@Override
			public void run() {
				try {
					server.start();
					server.join();
				} catch (final Exception e) {
					e.printStackTrace();
					System.exit(1);
				}

			}
		}.start();
	}
}
