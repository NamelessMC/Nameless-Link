package com.namelessmc.bot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.net.ssl.SSLHandshakeException;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.namelessmc.bot.Language.LanguageLoadException;
import com.namelessmc.bot.commands.PingCommand;
import com.namelessmc.bot.commands.PrefixCommand;
import com.namelessmc.bot.commands.URLCommand;
import com.namelessmc.bot.commands.UnlinkCommand;
import com.namelessmc.bot.commands.UpdateUsernameCommand;
import com.namelessmc.bot.commands.VerifyCommand;
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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class Main {

	private static final String USER_AGENT = "Nameless-Link/" + Main.class.getPackage().getImplementationVersion();
	private static final String DEFAULT_LANGUAGE_CODE = "en_UK";
	public static final Set<NamelessVersion> SUPPORTED_WEBSITE_VERSIONS = EnumSet.of(
			NamelessVersion.V2_0_0_PR_10,
			NamelessVersion.V2_0_0_PR_11
	);

	private static JDA[] jda;
	public static JDA getJda(final int shardId) { return jda[shardId]; }
	public static JDA getJdaForGuild(final long guildId) {
		return getJda((int) ((guildId >> 22) % getShardCount()));
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("Core");

	private static final EmbedBuilder embedBuilder = new EmbedBuilder();
	public static EmbedBuilder getEmbedBuilder() { return embedBuilder; }

	private static final Gson gson = new GsonBuilder().create();
	public static Gson getGson() { return gson; }

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

	private static Optional<ApiLogger> apiDebugLogger;

	private static String defaultCommandPrefix;
	public static String getDefaultCommandPrefix() { return defaultCommandPrefix; }

	private static int shards;
	public static int getShardCount() { return shards; }

	public static void main(final String[] args) throws IOException, BackendStorageException, NamelessException {
		LOGGER.info("Starting Nameless Link version {}", Main.class.getPackage().getImplementationVersion());

		final String botUrlStr = Objects.requireNonNull(System.getenv("BOT_URL"),
				"Environment variable BOT_URL not specified");

		try {
			botUrl = new URL(botUrlStr);
		} catch (final MalformedURLException e) {
			System.err.println("Environment variable BOT_URL is not a valid URL");
			System.exit(1);
		}

		if (System.getenv("SERVER_PORT") != null) {
			LOGGER.info("Environment variable SERVER_PORT is set (by Pterodactyl Panel). Using that instead of WEBSERVER_PORT.");
			webserverPort = Integer.parseInt(System.getenv("SERVER_PORT"));
		} else {
			final String webserverPortStr = Objects.requireNonNull(System.getenv("WEBSERVER_PORT"), "Environment variable WEBSERVER_PORT or SERVER_PORT not specified");
			try {
				webserverPort = Integer.parseInt(webserverPortStr);
			} catch (final NumberFormatException e) {
				System.err.println("Environment variable WEBSERVER_PORT is not a valid number");
				System.exit(1);
			}
		}

		String defaultLang = System.getenv("DEFAULT_LANGUAGE");
		if (defaultLang == null) {
			System.out.println("Default language not specified, assuming " + DEFAULT_LANGUAGE_CODE);
			defaultLang = DEFAULT_LANGUAGE_CODE;
		}

		if (System.getenv("API_DEBUG") != null && Boolean.parseBoolean(System.getenv("API_DEBUG"))) {
			apiDebugLogger = Optional.of(Slf4jLogger.DEFAULT_INSTANCE);
		} else {
			apiDebugLogger = Optional.empty();
		}

		if (System.getenv("WEBSERVER_BIND") != null) {
			webserverInterface = System.getenv("WEBSERVER_BIND");
		} else {
			System.out.println("Environment variable 'WEBSERVER_BIND' not set, assuming '127.0.0.1'. Note that this means the bot only listens on your localhost interface, but this is likely what you want.");
			webserverInterface = "127.0.0.1";
		}

		if (System.getenv("DEFAULT_COMMAND_PREFIX") != null) {
			defaultCommandPrefix = System.getenv("DEFAULT_COMMAND_PREFIX");
		} else {
			System.out.println("Environment variable 'DEFAULT_COMMAND_PREFIX' not set, setting to default (!).");
			defaultCommandPrefix = "!";
		}

		try {
			Language.setDefaultLanguage(defaultLang);
		} catch (final LanguageLoadException e) {
			System.err.println("Could not load language '" + defaultLang + "'");
			System.exit(1);
		}

		if (System.getenv("SHARDS") != null) {
			shards = Integer.parseInt(System.getenv("SHARDS"));
		} else {
			shards = 1;
		}

		initializeConnectionManager();

		HttpMain.init();

		try {
			final String token = Objects.requireNonNull(System.getenv("DISCORD_TOKEN"),
					"Environment variable DISCORD_TOKEN not specified");

			final JDABuilder builder = JDABuilder.createDefault(token);



			builder.addEventListeners(new GuildJoinHandler())
					.addEventListeners(new CommandListener())
					.addEventListeners(new DiscordRoleListener());

			builder.enableIntents(
					GatewayIntent.DIRECT_MESSAGES,
					GatewayIntent.DIRECT_MESSAGE_TYPING,
					GatewayIntent.DIRECT_MESSAGE_REACTIONS,
					GatewayIntent.GUILD_MESSAGE_REACTIONS,
					GatewayIntent.GUILD_MESSAGE_TYPING,
					GatewayIntent.GUILD_MESSAGES
					);

			if (System.getenv("DISABLE_MEMBERS_INTENT") == null) {
				builder.enableIntents(GatewayIntent.GUILD_MEMBERS)
						.setMemberCachePolicy(MemberCachePolicy.ALL);
			}

			LOGGER.info("Using {} shards", shards);
			jda = new JDA[shards];
			for (int i = 0; i < shards; i++) {
				jda[i] = builder.useSharding(i, shards).build();
			}
		} catch (final LoginException e) {
			e.printStackTrace();
			return;
		}

		// Register commands
		new PingCommand();
		new PrefixCommand();
		new UnlinkCommand();
		new UpdateUsernameCommand();
		new URLCommand();
		new VerifyCommand();

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

		sendBotSettings(scheduler);

		if (!Main.getConnectionManager().isReadOnly()) {
			scheduler.scheduleAtFixedRate(() -> {
				Main.getExecutorService().execute(() -> {
					ConnectionCleanup.run();
				});
			}, 15, TimeUnit.HOURS.toMinutes(12), TimeUnit.MINUTES);
		}

		scheduler.scheduleAtFixedRate(DiscordRoleListener::processQueue, 5, 2, TimeUnit.SECONDS);
	}

	private static void sendBotSettings(final ScheduledExecutorService scheduler) throws NamelessException, BackendStorageException {
		final User user = Main.getJda(0).getSelfUser();
		final String username = user.getName() + "#" + user.getDiscriminator();
		if (Main.getConnectionManager().isReadOnly()) {
			final NamelessAPI api = newApiConnection(connectionManager.listConnections().get(0));
			LOGGER.info("Sending bot settings to " + api.getApiUrl());
			api.setDiscordBotUrl(botUrl);
			api.setDiscordBotUser(username, user.getIdLong());
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
			DiscordRoleListener.sendRoleListToWebsite(guild);
		} else {
			if (System.getenv("SKIP_SETTINGS_UPDATE") == null) {

				scheduler.schedule(() -> {
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
								try {
									final NamelessAPI api = Main.newApiConnection(url);
									api.setDiscordBotUrl(botUrl);
									api.setDiscordBotUser(username, user.getIdLong());
									LOGGER.info(url.toString() + " success");
									countSuccess.incrementAndGet();
								} catch (final NamelessException e) {
									LOGGER.info(url.toString() + " error");
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
				}, 5, TimeUnit.SECONDS);
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
			System.out.println("STORAGE_TYPE not specified, assuming STORAGE_TYPE=stateless");
			storageType = "stateless";
		}

		final StorageInitializer<? extends ConnectionManager> init = StorageInitializer.getByName(storageType);
		if (init == null) {
			System.err.println("The chosen STORAGE_TYPE is not available, please choose from "
					+ String.join(", ", StorageInitializer.getAvailableNames()));
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
			CertificateException.class
	);

	public static void logConnectionError(final Logger logger, final String message, final NamelessException e) {
		Objects.requireNonNull(logger, "Logger is null");
		Objects.requireNonNull(message, "Message is null");
		Objects.requireNonNull(e, "Exception is null");
		if (e instanceof ApiError) {
			logger.warn(message + " (API error {})", ((ApiError) e).getError());
		} else if (e.getCause() != null && IGNORED_EXCEPTIONS.contains(e.getCause().getClass())) {
			logger.warn(message + " ({})", e.getCause().getClass().getSimpleName());
		} else {
			logger.warn(message, e);
		}
	}

}
