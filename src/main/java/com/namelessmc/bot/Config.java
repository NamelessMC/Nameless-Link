package com.namelessmc.bot;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Config {

	private static final int DEFAULT_PORT = 8001;



	public static final String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN");

	public static final String MYSQL_HOSTNAME = System.getenv("MYSQL_HOSTNAME");
	public static final String MYSQL_DATABASE = System.getenv("MYSQL_DATABASE");
	public static final String MYSQL_USERNAME = System.getenv("MYSQL_USERNAME");
	public static final String MYSQL_PASSWORD = System.getenv("MYSQL_PASSWORD");

	@SkipCheck
	public static final int PORT;

	static {
		initPort: {
			final String port = System.getenv("port");
			if (port == null) {
				PORT = DEFAULT_PORT;
			} else {
				int i;
				try {
					i = Integer.parseInt(port);
				} catch (final NumberFormatException e) {
					System.err.println("Invalid port number " + port + ", using default port " + DEFAULT_PORT);
					PORT = DEFAULT_PORT;
					break initPort;
				}
				PORT = i;
			}
		}
	}

	static boolean check() {
		boolean valid = true;
		for (final Field field : Config.class.getFields()) {
			final int modifiers = field.getModifiers();
			if (!Modifier.isPublic(modifiers)) {
				continue;
			}

			if (field.isAnnotationPresent(SkipCheck.class)) {
				continue;
			}

			try {
				if (field.get(null) == null) {
					valid = false;
					System.out.println("Missing required environment variable " + field.getName());
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		return valid;
	}

	private static @interface SkipCheck {}

}
