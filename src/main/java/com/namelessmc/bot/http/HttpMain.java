package com.namelessmc.bot.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.namelessmc.bot.Config;
import com.namelessmc.bot.Main;

public class HttpMain {

    public static void init() {
    	final Server server = new Server();
    	final ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		handler.addServlet(ConnectionTest.class, "/");
		handler.addServlet(RoleChange.class, "/roleChange");
		handler.addServlet(VerifyId.class, "/verifyId");
		server.setHandler(handler);

        final ServerConnector connector = new ServerConnector(this.server);

        int port = 8001;
        try {
            port = Config.PORT;
        } catch (final NumberFormatException e) {
            Main.log("[ERROR] Invalid port. Using fallback: " + port);
        }
        
        connector.setPort(port);
  
        server.addConnector(connector);
        
        new Thread() {
			@Override
			public void run() {
				server.start();
				server.join();
			}
		}.start();
    }
}
