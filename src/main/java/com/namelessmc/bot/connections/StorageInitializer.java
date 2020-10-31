package com.namelessmc.bot.connections;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

public class StorageInitializer<CM extends ConnectionManager> {
	
	public static final StorageInitializer<StatelessConnectionManager> STATELESS =
			new StorageInitializer<>(() -> {
					final URL apiUrl = getEnvUrl("API_URL");
					final long guildId = getEnvLong("GUILD_ID");
					return new StatelessConnectionManager(guildId, apiUrl);
			});
	
	public static final StorageInitializer<PostgresConnectionManager> POSTGRES =
			new StorageInitializer<>(() -> {
					final String hostname = getEnvString("POSTGRES_HOSTNAME", "localhost");
					final int port = (int) getEnvLong("POSTGRES_PORT", 5432L);
					final String name = getEnvString("POSTGRES_NAME");
					final String username = getEnvString("POSTGRES_USERNAME");
					final String password = getEnvString("POSTGRES_PASSWORD");
					return new PostgresConnectionManager(hostname, port, name, username, password);
			});
	
	private final Supplier<CM> initializer;
	
	public StorageInitializer(final Supplier<CM> initializer) {
		this.initializer = initializer;
	}
	
	public CM get() {
		return this.initializer.get();
	}
	
	private static String getEnvString(final String name, final String def) {
		final String env = System.getenv(name);
		if (env != null) {
			return env;
		} else {
			if (def != null) {
				return def;
			} else {
				envMissing(name);
				return null;
			}
		}
	}
	
	private static String getEnvString(final String name) {
		return getEnvString(name, null);
	}
	
	private static long getEnvLong(final String name, final Long def) {
		final String env = System.getenv(name);
		if (env != null) {
			try {
				return Long.parseLong(env);
			} catch (final NumberFormatException e) {
				System.err.println(System.getenv(name) + " is not a valid whole number.");
				System.exit(1);
				return 0;
			}
		} else {
			if (def != null) {
				return def;
			} else {
				envMissing(name);
				return 0;
			}
		}
	}
	
	private static URL getEnvUrl(final String name) {
		final String str = getEnvString(name, null);
		try {
			return new URL(str);
		} catch (final MalformedURLException e) {
			System.err.println("Provided URL in " + name + " is malformed.");
			System.exit(1);
			return null;
		}
	}
	
	private static long getEnvLong(final String name) {
		return getEnvLong(name, null);
	}
	
	private static void envMissing(final String name) {
		System.err.println("Environment variable '" + name + "' required but not specified");
		System.exit(1);
	}

}
