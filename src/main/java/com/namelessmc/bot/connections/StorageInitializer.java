package com.namelessmc.bot.connections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;

public class StorageInitializer<CM extends ConnectionManager> {

	private static final Logger LOGGER = LoggerFactory.getLogger("Configuration");

	public static final StorageInitializer<StatelessConnectionManager> STATELESS = new StorageInitializer<>(() -> {
		final URL apiUrl = getEnvUrl("API_URL");
		final String apiKey = getEnvString("API_KEY");
		final long guildId = getEnvLong("GUILD_ID");
		return new StatelessConnectionManager(guildId, apiUrl, apiKey);
	});

	public static final StorageInitializer<PostgresConnectionManager> POSTGRES = new StorageInitializer<>(() -> {
		final String hostname = getEnvString("POSTGRES_HOSTNAME", "localhost");
		final int port = (int) getEnvLong("POSTGRES_PORT", 5432L);
		final String name = getEnvString("POSTGRES_DB");
		final String username = getEnvString("POSTGRES_USER");
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

	private static final Map<String, StorageInitializer<? extends ConnectionManager>> BY_STRING =
			Map.of(
					"stateless", STATELESS,
					"postgres", POSTGRES
			);

	public static StorageInitializer<? extends ConnectionManager> getByName(final @NotNull String name) {
		return BY_STRING.get(name);
	}

	public static String[] getAvailableNames() {
		return BY_STRING.keySet().toArray(String[]::new);
	}

	public static String getEnvString(final @NotNull String name, final @Nullable String def) {
		final String env = System.getenv(name);
		if (env != null) {
			return env;
		} else {
			if (def != null) {
				LOGGER.info("Environment variable {} not set, using default value '{}'", name, def);
				return def;
			} else {
				envMissing(name);
				return null;
			}
		}
	}

	private static String getEnvString(final @NotNull String name) {
		return getEnvString(name, null);
	}

	public static long getEnvLong(final @NotNull String name, final @Nullable Long def) {
		final String env = System.getenv(name);
		if (env != null) {
			try {
				return Long.parseLong(env);
			} catch (final NumberFormatException e) {
				LOGGER.error("The value of {} ('{}') is not a valid whole number.", name, System.getenv(name));
				System.exit(1);
				return 0;
			}
		} else {
			if (def != null) {
				LOGGER.info("Environment variable {} not set, using default value {}", name, def);
				return def;
			} else {
				envMissing(name);
				return 0;
			}
		}
	}

	public static URL getEnvUrl(final @NotNull String name) {
		final String str = getEnvString(name, null);
		if (str == null) {
			LOGGER.error("Environment variable {} not defined", name);
			System.exit(1);
			return null;
		}
		try {
			return new URL(str);
		} catch (final MalformedURLException e) {
			LOGGER.error("Provided URL in {} is malformed. The full URL is printed below:", name);
			LOGGER.error(str);
			LOGGER.error("The string above should not contain any quotation marks (\" or ').");
			LOGGER.error("It should look like this: https://yourdomain.com/index.php?route=/api/v2/apikeyhere");
			System.exit(1);
			return null;
		}
	}

	private static long getEnvLong(final @NotNull String name) {
		return getEnvLong(name, null);
	}

	private static void envMissing(final @NotNull String name) {
		LOGGER.error("Environment variable '{}' required but not specified", name);
		System.exit(1);
	}

}
