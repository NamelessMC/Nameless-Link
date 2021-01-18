package com.namelessmc.bot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.namelessmc.bot.Language.LanguageLoadException;
import com.namelessmc.bot.Language.Term;
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
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class Main {

	private static final String USER_AGENT = "Nameless-Link/" + Main.class.getPackage().getImplementationVersion();
	private static final String DEFAULT_LANGUAGE_CODE = "en_UK";

	@Getter
	private static JDA jda;
	@Getter
	private static final Logger logger = Logger.getLogger("NamelessLink");
	@Getter
	private static final EmbedBuilder embedBuilder = new EmbedBuilder();
	@Getter
	private static final Gson gson = new GsonBuilder().create();
	@Getter
	private static ConnectionManager connectionManager;
	@Getter
	private static URL botUrl;
	@Getter
	private static String webserverInterface;
	@Getter
	private static int webserverPort;
	private static boolean apiDebug;

	public static void main(final String[] args) throws IOException, BackendStorageException, NamelessException {
		System.out.println("Starting Nameless Link version " + Main.class.getPackage().getImplementationVersion());

		initializeConnectionManager();

		final String botUrlStr = System.getenv("BOT_URL");
		if (botUrlStr == null) {
			System.err.println("Environment variable BOT_URL not specified");
			System.exit(1);
		}

		try {
			botUrl = new URL(botUrlStr);
		} catch (final MalformedURLException e) {
			System.err.println("Environment variable BOT_URL is not a valid URL");
			System.exit(1);
		}

		final String webserverPortStr = System.getenv("WEBSERVER_PORT");
		if (webserverPortStr == null) {
			System.err.println("Environment variable WEBSERVER_PORT not specified");
			System.exit(1);
		}

		try {
			webserverPort = Integer.parseInt(webserverPortStr);
		} catch (final NumberFormatException e) {
			System.err.println("Environment variable WEBSERVER_PORT is not a valid number");
			System.exit(1);
		}

		String defaultLang = System.getenv("DEFAULT_LANGUAGE");
		if (defaultLang == null) {
			System.out.println("Default language not specified, assuming " + DEFAULT_LANGUAGE_CODE);
			defaultLang = DEFAULT_LANGUAGE_CODE;
		}

		if (System.getenv("API_DEBUG") != null) {
			apiDebug = Boolean.parseBoolean(System.getenv("API_DEBUG"));
		} else {
			apiDebug = false;
		}
		
		if (System.getenv("WEBSERVER_BIND") != null) {
			webserverInterface = System.getenv("WEBSERVER_BIND");
		} else {
			System.out.println("Environment variable 'WEBSERVER_BIND' not set, assuming '127.0.0.1'. Note that this means the bot only listens on your localhost interface, but this is likely what you want.");
			webserverInterface = "127.0.0.1";
		}

		try {
			Language.setDefaultLanguage(defaultLang);
		} catch (final LanguageLoadException e) {
			System.err.println("Could not load language '" + defaultLang + "'");
			System.exit(1);
		}

		HttpMain.init();

		try {
			final String token = System.getenv("DISCORD_TOKEN");
			if (token == null) {
				System.err.println("Environment variable DISCORD_TOKEN not specified");
				System.exit(1);
			}
			jda = JDABuilder.createDefault(token)
					.addEventListeners(new GuildJoinHandler())
					.addEventListeners(new CommandListener())
					.addEventListeners(new DiscordRoleListener())
//					.setChunkingFilter(ChunkingFilter.ALL)
//					.setMemberCachePolicy(MemberCachePolicy.ALL)
					// .enableIntents(GatewayIntent.GUILD_MEMBERS)
//					.setMemberCachePolicy(MemberCachePolicy.DEFAULT)
					.build();
		} catch (final LoginException e) {
			e.printStackTrace();
			return;
		}

		// Register commands
		new UnlinkCommand();
		new UpdateUsernameCommand();
		new URLCommand();
		new VerifyCommand();

		logger.info("Waiting for JDA to connect, this can take a long time (30+ seconds is not unusual)...");
		logger.info("Note: the JDA message \"Connected to WebSocket\" does not mean it is finished connecting!");

		try {
			jda.awaitStatus(Status.CONNECTED);
		} catch (final InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

		logger.info("JDA connected!");

		final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

		final User user = jda.getSelfUser();
		final String username = user.getName() + "#" + user.getDiscriminator();

		if (Main.getConnectionManager().isReadOnly()) {
			final NamelessAPI api = newApiConnection(connectionManager.listConnections().get(0));
			Main.getLogger().info("Sending bot settings to " + api.getApiUrl());
			api.setDiscordBotUrl(botUrl);
			api.setDiscordBotUser(username, user.getIdLong());
			final long guildId = connectionManager.getGuildIdByURL(api.getApiUrl()).get();
			api.setDiscordGuildId(guildId);
			final Guild guild = Main.getJda().getGuildById(guildId);
			if (guild == null) {
				logger.severe("Guild with id " + guildId + " does not exist. Is the ID wrong or is the bot not in this guild?");
				System.exit(1);
			}
			DiscordRoleListener.sendRoleListToWebsite(guild);
		} else {
			if (System.getenv("SKIP_SETTINGS_UPDATE") == null) {
				scheduler.schedule(() -> {
					try {
						Main.getLogger().info("Updating bot settings..");
						final ExecutorService service = Executors.newFixedThreadPool(10);
						final AtomicInteger countSuccess = new AtomicInteger();
						final AtomicInteger countError = new AtomicInteger();
						for (final URL url : connectionManager.listConnections()) {
							service.execute(() -> {
								try {
									final NamelessAPI api = Main.newApiConnection(url);
									api.setDiscordBotUrl(botUrl);
									api.setDiscordBotUser(username, user.getIdLong());
									logger.info(url.toString() + " success");
									countSuccess.incrementAndGet();
								} catch (final NamelessException e) {
									logger.info(url.toString() + " error");
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
						logger.info("Done updating bot settings");
						logger.info(String.format("%s websites successful, %s websites unsuccessful", countSuccess, countError));
					} catch (final BackendStorageException e) {
						e.printStackTrace();
					}
				}, 5, TimeUnit.SECONDS);
			}
		}

		if (!Main.getConnectionManager().isReadOnly()) {
			scheduler.scheduleAtFixedRate(ConnectionCleanup::run, TimeUnit.SECONDS.toMillis(2), TimeUnit.HOURS.toMillis(2), TimeUnit.MILLISECONDS);
			
			// Temporary way to reduce number of guilds for 250 guilds limit
			scheduler.scheduleAtFixedRate(() -> {
				logger.info("Sending messages for not set up guilds");
				try {
					for (final Guild guild : Main.getJda().getGuilds()) {
						final long id = guild.getIdLong();
						if (Main.getConnectionManager().getApi(id).isEmpty()) {
							logger.info("Sending message for guild " + id);
							final Language lang = Language.getDefaultLanguage();
							Main.getJda().retrieveUserById(guild.retrieveOwner().complete().getIdLong()).complete()
								.openPrivateChannel().complete()
								.sendMessage(lang.get(Term.SETUP_REMINDER));
						}
					}
				} catch (final BackendStorageException e) {
					e.printStackTrace();
				}
			}, TimeUnit.HOURS.toMillis(12), TimeUnit.HOURS.toMillis(12), TimeUnit.MILLISECONDS);
		}
	}

	public static boolean canModifySettings(final User user, final Guild guild) {
		return guild.retrieveMember(user).complete().hasPermission(Permission.ADMINISTRATOR);
	}

	private static void initializeConnectionManager() throws IOException {
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
		synchronized(API_CACHE) {
			API_CACHE.computeIfAbsent(url, x -> new NamelessAPI(url, USER_AGENT, apiDebug));
			return API_CACHE.get(url);
		}
	}

}
