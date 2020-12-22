package com.namelessmc.bot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.namelessmc.bot.Language.LanguageLoadException;
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
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

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
	private static int webserverPort;

	public static void main(final String[] args) throws IOException, BackendStorageException {
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
					.setChunkingFilter(ChunkingFilter.ALL)
					.setMemberCachePolicy(MemberCachePolicy.ALL)
					.enableIntents(GatewayIntent.GUILD_MEMBERS)
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

		final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

		if (System.getenv("SKIP_SETTINGS_UPDATE") == null) {
			scheduler.schedule(() -> {
				System.out.println("Updating bot settings..");
				try {
					final User user = jda.getSelfUser();
					final String username = user.getName() + "#" + user.getDiscriminator();
					for (final URL url : connectionManager.listConnections()) {
						System.out.print("Sending to " + url.toString() + "... ");
						try {
							final NamelessAPI api = Main.newApiConnection(url);
							api.setDiscordBotUrl(botUrl);
							api.setDiscordBotUser(username, user.getIdLong());
							System.out.println("OK");
						} catch (final NamelessException e) {
							System.out.println("error");
						}
					}
				} catch (final BackendStorageException e) {
					e.printStackTrace();
				}
			}, 5, TimeUnit.SECONDS);
		}
		
		scheduler.scheduleAtFixedRate(ConnectionCleanup::run, TimeUnit.SECONDS.toMillis(2), TimeUnit.HOURS.toMillis(2), TimeUnit.MILLISECONDS);
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
		final boolean debug = false; // TODO debug configurable
		API_CACHE.computeIfAbsent(url, x -> new NamelessAPI(url, USER_AGENT, debug));
		return API_CACHE.get(url);
	}

}
