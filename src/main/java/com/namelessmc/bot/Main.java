package com.namelessmc.bot;

import com.namelessmc.bot.Language.LanguageLoadException;
import com.namelessmc.bot.commands.Command;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.bot.connections.ConnectionManager;
import com.namelessmc.bot.connections.StorageInitializer;
import com.namelessmc.bot.http.HttpMain;
import com.namelessmc.bot.listeners.CommandListener;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.bot.listeners.GuildJoinHandler;
import com.namelessmc.java_api.ApiError;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessVersion;
import com.namelessmc.java_api.logger.ApiLogger;
import com.namelessmc.java_api.logger.Slf4jLogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Main {

	private static final String USER_AGENT = "Nameless-Link/" + Main.class.getPackage().getImplementationVersion();
	private static final String DEFAULT_LANGUAGE_CODE = "en_UK";
	public static final Set<NamelessVersion> SUPPORTED_WEBSITE_VERSIONS = EnumSet.of(
			NamelessVersion.V2_0_0_PR_12,
			NamelessVersion.V2_0_0_PR_13
	);

	private static JDA[] jda;
	public static JDA getJda(final int shardId) { return jda[shardId]; }
	public static JDA getJdaForGuild(final long guildId) {
		return getJda((int) ((guildId >> 22) % getShardCount()));
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("Core");

	private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
	public static ExecutorService getExecutorService() { return executorService; }

	private static ConnectionManager connectionManager;
	public static ConnectionManager getConnectionManager() { return connectionManager; }

	private static URL botUrl;
	public static URL getBotUrl() { return botUrl; }

	private static String webserverInterface;
	public static String getWebserverInterface() { return webserverInterface; }

	private static int webserverPort;
	public static int getWebserverPort() { return webserverPort; }

	private static @Nullable ApiLogger apiDebugLogger;

	private static int shards;
	public static int getShardCount() { return shards; }

	public static void main(final String[] args) throws BackendStorageException, NamelessException {
		LOGGER.info("Starting Nameless Link version {}", Main.class.getPackage().getImplementationVersion());

		botUrl = StorageInitializer.getEnvUrl("BOT_URL");

		if (System.getenv("SERVER_PORT") != null) {
			LOGGER.info("Environment variable SERVER_PORT is set (by Pterodactyl Panel). Using that instead of WEBSERVER_PORT.");
			webserverPort = (int) StorageInitializer.getEnvLong("SERVER_PORT", null);
		} else {
			webserverPort = (int) StorageInitializer.getEnvLong("WEBSERVER_PORT", null);
		}

		String defaultLang = StorageInitializer.getEnvString("DEFAULT_LANGUAGE", DEFAULT_LANGUAGE_CODE);
		try {
			Language.setDefaultLanguage(defaultLang);
		} catch (LanguageLoadException e) {
			LOGGER.warn("Unable to set default language, '{}' is not a valid language.", defaultLang);
		}

		if (System.getenv("API_DEBUG") != null && Boolean.parseBoolean(System.getenv("API_DEBUG"))) {
			apiDebugLogger = Slf4jLogger.DEFAULT_INSTANCE;
		} else {
			apiDebugLogger = null;
		}

		webserverInterface = StorageInitializer.getEnvString("WEBSERVER_BIND", "127.0.0.1");

		shards = (int) StorageInitializer.getEnvLong("SHARDS", 1L);

		// Temporary workaround for OpenJDK 17 bug
		// https://github.com/DV8FromTheWorld/JDA/issues/1858#issuecomment-942066283
		final int cores = Runtime.getRuntime().availableProcessors();
		if (cores <= 1) {
			LOGGER.info("Available cores {}, setting parallelism flag", cores);
			System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1");
		}

		initializeConnectionManager();

		try {
			HttpMain.init();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			String token = StorageInitializer.getEnvString("DISCORD_TOKEN", null);

			final JDABuilder builder = JDABuilder.createDefault(token);

			builder.addEventListeners(new GuildJoinHandler())
					.addEventListeners(new CommandListener())
					.addEventListeners(new DiscordRoleListener());

			builder.enableIntents(GatewayIntent.DIRECT_MESSAGES);

			if (System.getenv("DISABLE_MEMBERS_INTENT") == null) {
				builder.enableIntents(GatewayIntent.GUILD_MEMBERS)
						.setMemberCachePolicy(MemberCachePolicy.ALL);
			}

			jda = new JDA[shards];
			for (int i = 0; i < shards; i++) {
				LOGGER.info("Initializing shard {}", i);
				jda[i] = builder.useSharding(i, shards).build();
			}
		} catch (final LoginException e) {
			e.printStackTrace();
			return;
		}

		LOGGER.info("Waiting for JDA to connect, this can take a long time (30+ seconds is not unusual)...");
		LOGGER.info("Note: the JDA message \"Connected to WebSocket\" does not mean it is finished connecting!");

		try {
			for (int i = 0; i < Main.getShardCount(); i++) {
				Main.getJda(i).awaitStatus(Status.CONNECTED);
				LOGGER.info("Shard {} connected", i);
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

		LOGGER.info("JDA connected!");

		final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

		sendBotSettings();

		if (!Main.getConnectionManager().isReadOnly()) {
			scheduler.scheduleAtFixedRate(
					() -> Main.getExecutorService().execute(ConnectionCleanup::run),
					15, TimeUnit.HOURS.toMinutes(12), TimeUnit.MINUTES);
		}
	}

	private static void sendBotSettings() throws NamelessException, BackendStorageException {
		final User user = Main.getJda(0).getSelfUser();
		final String userTag = user.getAsTag();
		if (Main.getConnectionManager().isReadOnly()) {
			final NamelessAPI api = newApiConnection(connectionManager.listConnections().get(0));
			LOGGER.info("Sending bot settings to " + api.getApiUrl());
			api.setDiscordBotUrl(botUrl);
			api.setDiscordBotUser(userTag, user.getIdLong());
			final long guildId = connectionManager.getGuildIdByURL(api.getApiUrl()).orElse(0L);
			if (guildId == 0L) {
				LOGGER.error("Guild id was not present in the Optional");
				System.exit(1);
			}
			api.setDiscordGuildId(guildId);
			final Guild guild = Main.getJda(0).getGuildById(guildId);
			if (guild == null) {
				LOGGER.error("Guild with id '{}' does not exist. Is the ID wrong or is the bot not in this guild?", guildId);
				System.exit(1);
			}
			try {
				Command.sendCommands(guild);
			} catch (ErrorResponseException e) {
				LOGGER.error("Failed to register slash commands: " + e.getMessage());
				LOGGER.error("Make sure you invite the bot with the 'applications.commands' scope enabled.");
			}
			DiscordRoleListener.sendRolesAsync(guildId);
		} else {
			try {
				LOGGER.info("Updating bot settings..");
				int threads;
				if (System.getenv("UPDATE_SETTINGS_THREADS") != null) {
					threads = Integer.parseInt(System.getenv("UPDATE_SETTINGS_THREADS"));
				} else {
					threads = 2;
				}

				final ExecutorService service = Executors.newFixedThreadPool(threads);
				final AtomicInteger countSuccess = new AtomicInteger();
				final AtomicInteger countError = new AtomicInteger();
				for (final URL url : connectionManager.listConnections()) {
					service.execute(() -> {
						Guild guild;
						try {
							final long guildId = connectionManager.getGuildIdByURL(url).orElseThrow(() -> new IllegalStateException("database has URL but not guild id"));
							guild = Main.getJdaForGuild(guildId).getGuildById(guildId);
							if (guild == null) {
								LOGGER.warn("Skipping guild {}, it is null (bot was kicked from this guild?)", guildId);
								return;
							}
						} catch (final BackendStorageException e) {
							LOGGER.error("command update error", e);
							return;
						}

						try {
							Command.sendCommands(guild);
						} catch (ErrorResponseException e) {
							LOGGER.warn("Failed to send commands to guild {}: {}", guild.getIdLong(), e.getMessage());
						}

						try {
							final NamelessAPI api = Main.newApiConnection(url);
							api.setDiscordBotSettings(botUrl, guild.getIdLong(), userTag, user.getIdLong());
							LOGGER.info("{} {} success", guild.getIdLong(), url.toString());
							countSuccess.incrementAndGet();
						} catch (final NamelessException e) {
							LOGGER.info("{} {} error", guild.getIdLong(), url.toString());
							countError.incrementAndGet();
						}
					});

				}
				service.shutdown();
				try {
					service.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
				LOGGER.info("Done updating bot settings");
				LOGGER.info("{} websites successful, {} websites unsuccessful", countSuccess, countError);
			} catch (final BackendStorageException e) {
				e.printStackTrace();
			}
		}
	}

	public static void canModifySettings(final User user, final Guild guild, final Consumer<Boolean> canModifySettings) {
		guild.retrieveMember(user).queue(
				// success
				member -> canModifySettings.accept(member.hasPermission(Permission.ADMINISTRATOR)),
				// failure
				t -> {
					LOGGER.warn("Error while retrieving member in canModifySettings, assuming user {} is not allowed to modify settings.", user.getId());
					canModifySettings.accept(false);
				});
	}

	private static void initializeConnectionManager() {
		String storageType = System.getenv("STORAGE_TYPE");
		if (storageType == null) {
			LOGGER.info("STORAGE_TYPE not specified, assuming STORAGE_TYPE=stateless");
			storageType = "stateless";
		}

		final StorageInitializer<? extends ConnectionManager> init = StorageInitializer.getByName(storageType);
		if (init == null) {
			LOGGER.error("The chosen STORAGE_TYPE is not available, please choose from: {}",
					String.join(", ", StorageInitializer.getAvailableNames()));
			System.exit(1);
		}

		connectionManager = init.get();
	}

	private static final Map<URL, NamelessAPI> API_CACHE = new HashMap<>();

	public static NamelessAPI newApiConnection(final URL url) {
		synchronized (API_CACHE) {
			API_CACHE.computeIfAbsent(url, x -> NamelessAPI.builder().apiUrl(url).userAgent(USER_AGENT).withCustomDebugLogger(apiDebugLogger).build());
			return API_CACHE.get(url);
		}
	}

	private static final Set<Class<?>> IGNORED_EXCEPTIONS = Set.of(
			UnknownHostException.class,
			SSLHandshakeException.class,
			CertificateException.class,
			SocketTimeoutException.class
	);

	public static void logConnectionError(final Logger logger, final String message, final NamelessException e) {
		Objects.requireNonNull(logger, "Logger is null");
		Objects.requireNonNull(message, "Message is null");
		Objects.requireNonNull(e, "Exception is null");
		if (e instanceof ApiError) {
			logger.warn(message + " (API error {})", ((ApiError) e).getError());
		} else if (e.getCause() != null &&
				apiDebugLogger == null &&
				IGNORED_EXCEPTIONS.contains(e.getCause().getClass())) {
			logger.warn(message + " ({})", e.getCause().getClass().getSimpleName());
		} else {
			logger.warn(message, e);
		}
	}

}
